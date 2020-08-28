package com.univocity.shopify.utils;

import com.vladsch.flexmark.ext.abbreviation.*;
import com.vladsch.flexmark.ext.autolink.*;
import com.vladsch.flexmark.ext.definition.*;
import com.vladsch.flexmark.ext.footnotes.*;
import com.vladsch.flexmark.ext.tables.*;
import com.vladsch.flexmark.ext.typographic.*;
import com.vladsch.flexmark.html.*;
import com.vladsch.flexmark.parser.*;
import com.vladsch.flexmark.util.ast.*;
import com.vladsch.flexmark.util.data.*;

import java.util.*;

public class MarkdownToHtml {

	private final HtmlRenderer renderer;
	private final Parser parser;

	public MarkdownToHtml() {
		MutableDataSet options = new MutableDataSet();
		options.setFrom(ParserEmulationProfile.KRAMDOWN);

		List l = new ArrayList();
		l.add(AbbreviationExtension.create());
		l.add(DefinitionExtension.create());
		l.add(FootnoteExtension.create());
		l.add(TablesExtension.create());
		l.add(TypographicExtension.create());
		l.add(AutolinkExtension.create());

		options.set(Parser.EXTENSIONS, l);

		parser = Parser.builder(options).build();
		renderer = HtmlRenderer.builder(options).build();


	}

	public String plaintextToHtml(String markdownText) {
		final Node document;
		synchronized (parser) {
			document = parser.parse(markdownText);
		}
		synchronized (renderer) {
			return renderer.render(document);
		}
	}
}
