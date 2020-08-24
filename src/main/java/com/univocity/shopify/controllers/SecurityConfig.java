package com.univocity.shopify.controllers;

import com.univocity.shopify.utils.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.security.config.annotation.authentication.builders.*;
import org.springframework.security.config.annotation.web.builders.*;
import org.springframework.security.config.annotation.web.configuration.*;
import org.springframework.security.web.header.writers.*;

import java.util.*;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

	@Autowired
	App app;

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		String csp;
		if (app.isTestingLocally()) {
			csp = "default-src * 'unsafe-inline' 'unsafe-eval' data: blob:;";
		} else {
			csp = "default-src * 'unsafe-inline'; img-src *; frame-ancestors *;";
		}
		http
//				.requiresChannel().anyRequest().requiresSecure().and()
				.authorizeRequests()
				.antMatchers(HttpMethod.OPTIONS, "/**").denyAll()
				.antMatchers(HttpMethod.TRACE, "/**").denyAll()
				.antMatchers(HttpMethod.PATCH, "/**").denyAll()
				.antMatchers(HttpMethod.HEAD, "/**").denyAll()
				.antMatchers(HttpMethod.DELETE, "/**").denyAll()
				.antMatchers("/admin/**").authenticated()
				.antMatchers("/refresh_tokens").authenticated()
				.anyRequest().permitAll()
				.and()
				.httpBasic()
				.and()
//				.headers().frameOptions().disable()
//				.and()
				.csrf().disable()
				.headers()
//				.addHeaderWriter(new StaticHeadersWriter("Access-Control-Allow-Origin", "*"))
//				.addHeaderWriter(new StaticHeadersWriter("Access-Control-Allow-Methods", "POST, GET"))
//				.addHeaderWriter(new StaticHeadersWriter("Access-Control-Max-Age", "3600"))
//				.addHeaderWriter(new StaticHeadersWriter("Access-Control-Allow-Credentials", "true"))
//				.addHeaderWriter(new StaticHeadersWriter("Access-Control-Allow-Headers", "Origin,Accept,X-Requested-With,Content-Type,Access-Control-Request-Method,Access-Control-Request-Headers,Authorization"))
				.contentSecurityPolicy(csp);

	}

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		String password = UUID.randomUUID().toString();
		log.info("local_admin password set to: " + password);

		auth
				.inMemoryAuthentication()
				.withUser("local_admin").password(password).roles("ADMIN");
	}

}
