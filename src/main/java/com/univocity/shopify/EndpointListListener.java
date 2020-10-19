package com.univocity.shopify;


import com.univocity.parsers.common.input.*;
import org.slf4j.*;
import org.springframework.context.*;
import org.springframework.context.event.*;
import org.springframework.stereotype.*;
import org.springframework.web.servlet.mvc.method.annotation.*;

import java.util.*;

import static com.univocity.shopify.utils.Utils.*;

@Component
public class EndpointListListener implements ApplicationListener<ContextRefreshedEvent> {

	private static final Logger log = LoggerFactory.getLogger(EndpointListListener.class);

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		ApplicationContext applicationContext = ((ContextRefreshedEvent) event).getApplicationContext();
		applicationContext.getBean(RequestMappingHandlerMapping.class).getHandlerMethods()
				.forEach((requestMappingInfo, m) ->
						Main.endpoints.addAll(requestMappingInfo.getPatternsCondition().getPatterns()));


		Main.endpoints.removeIf(e -> e.startsWith("/shopify") || e.startsWith("/apps/") || e.startsWith("/tools/"));
		Main.endpoints.remove("/webhooks/");
		Main.endpoints.add("/favicon.ico");

		ElasticCharAppender out = borrowBuilder();
		try {
			new TreeSet<>(Main.endpoints).forEach(endpoint -> {
				out.append('\n');
				out.append(endpoint);
			});

			log.info("Available endpoints from this server: " + out.toString());
		} finally {
			releaseBuilder(out);
		}
	}
}