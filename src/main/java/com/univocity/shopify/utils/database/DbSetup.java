package com.univocity.shopify.utils.database;

import com.univocity.shopify.utils.*;
import org.apache.commons.io.*;
import org.apache.commons.lang3.*;
import org.slf4j.*;
import org.springframework.dao.*;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.support.*;

import java.io.*;
import java.sql.*;
import java.util.*;

import static java.nio.charset.StandardCharsets.*;


public class DbSetup extends JdbcDaoSupport {

    private static final Logger log = LoggerFactory.getLogger(DbSetup.class);

    private static final String SCRIPT_ORDER = "script.order";
    private static final String SCRIPT_DIR = "script.dir";

    private final ExtendedJdbcTemplate template;
    private final PropertyBasedConfiguration config;

    private String scriptOrderProperty = SCRIPT_ORDER;
    private String scriptDirProperty = SCRIPT_DIR;

    public DbSetup(ExtendedJdbcTemplate template, final PropertyBasedConfiguration config) {
        this.template = template;
        this.config = config;
        super.setJdbcTemplate(template);
    }

    public PropertyBasedConfiguration getConfig() {
        return config;
    }

    public ExtendedJdbcTemplate getDb() {
        return template;
    }

    public String getScriptOrderProperty() {
        return scriptOrderProperty;
    }

    public void setScriptOrderProperty(String scriptOrderProperty) {
        this.scriptOrderProperty = scriptOrderProperty;
    }

    public String getScriptDirProperty() {
        return scriptDirProperty;
    }

    public void setScriptDirProperty(String scriptDirProperty) {
        this.scriptDirProperty = scriptDirProperty;
    }

    public void dropTables() {
        template.execute(new StatementCallback<Void>() {
            @Override
            public Void doInStatement(Statement statement) throws SQLException, DataAccessException {
                for (String table : config.getList(scriptOrderProperty)) {
                    try {
                        if (tableExists(table)) {
                            statement.execute("DROP TABLE " + table + " CASCADE");
                            log.debug("Removed table {}", table);
                        }
                    } catch (Exception e) {
                        log.debug("Table {} does not exist yet", table);
                    }
                }
                return null;
            }
        });
    }

    public Boolean tableExists(final String tableName) {
        return template.execute((StatementCallback<Boolean>) statement -> {
            try {
                template.queryForObject("SELECT count(*) FROM " + tableName + " WHERE 0 = 1", Number.class);
                return true;
            } catch (Exception e) {
                //if error then table doesn't exist - this is not reliable enough yet as the table name should be escaped.
                return false;
            }
        });
    }

    private interface ScriptRunner {
        void runScript(Statement statement, String file, String script) throws SQLException;
    }

    private void runScript(final String table, final String extension, final ScriptRunner scriptRunner) {
        final File definitionDir = config.getDirectory(scriptDirProperty, false, false, false);
        log.debug("Reading scripts from directory: " + definitionDir);

        template.execute((StatementCallback<Void>) statement -> {
            for (String file : config.getList(scriptOrderProperty)) {
                if (table != null && !table.equalsIgnoreCase(file)) {
                    continue;
                }
                String resource = null;

                try {
                    String extensionInName = FilenameUtils.getExtension(file);
                    if (StringUtils.isBlank(extensionInName)) {
                        extensionInName = '.' + extension;
                    } else if (extensionInName.equalsIgnoreCase(extension)) {
                        extensionInName = "";
                    } else {
                        continue;
                    }
                    resource = definitionDir + "/" + file + extensionInName;

                    String script = null;
                    try {
                        script = Utils.readTextFromResource(resource, UTF_8);

                        log.debug("Executing script(s) in {}", resource);
                        scriptRunner.runScript(statement, file, script);
                    } catch (Exception ex) {
                        if (script != null) {
                            if ("tbl".equalsIgnoreCase(extension)) {
                                throw ex;
                            }
                            log.warn("Error processing script defined in " + resource + ": " + script, ex);
                        }  // else script doesn't exist
                    }
                } catch (Exception ex) {
                    throw new IllegalStateException("Error executing scripts defined in " + resource, ex);
                }
            }
            return null;
        });
    }

    public String toCreateTablesScript() {
        StringBuilder out = new StringBuilder();

        createTables(out);

        return out.toString();
    }

    public void createTables() {
        createTables(null);
    }

    private void createTables(final StringBuilder out) {
        runScript(null, "tbl", (statement, file, script) -> {
            file = FilenameUtils.removeExtension(file);
            if (!tableExists(file)) {
                log.info("Creating table '{}'", file);

                if (out != null) {
                    out.append(script);
                    out.append('\n');
                } else {
                    statement.execute(script);
                }


                executeScripts(file, "sql", out);
                executeScripts(file, "idx", out);
            } else {
                log.info("Table '{}' already exists", file);
            }
        });
    }

    private void executeScripts(String table, String extension, final StringBuilder out) {
        runScript(table, extension, (statement, file, script) -> {
            for (String line : script.split("\n")) {
                line = line.trim();
                if (StringUtils.isNotBlank(line)) {
                    if (out != null) {
                        out.append(line);
                        out.append('\n');
                    } else {
                        log.debug("Executing SQL statement: " + line);
                        statement.execute(line);
                    }
                }
            }
        });
    }

    public void truncateTables() {
        truncateTables(Collections.<String>emptySet());
    }

    public void truncateTables(final Set<String> toSkip) {
        runScript(null, "tbl", (statement, file, script) -> {
            statement.execute("SET REFERENTIAL_INTEGRITY FALSE");

            file = FilenameUtils.removeExtension(file);
            if ((toSkip != null && !toSkip.contains(file)) && tableExists(file)) {
                log.info("truncating table table '{}'", file);
                statement.executeUpdate("TRUNCATE TABLE " + file);

            }
            statement.execute("SET REFERENTIAL_INTEGRITY TRUE");
        });
    }
}
