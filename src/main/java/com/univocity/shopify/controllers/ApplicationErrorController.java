package com.univocity.shopify.controllers;

import com.univocity.shopify.*;
import com.univocity.shopify.utils.*;
import org.slf4j.*;
import org.springframework.boot.web.servlet.error.*;
import org.springframework.web.bind.annotation.*;

import javax.servlet.*;
import javax.servlet.http.*;

/**
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */
@RestController
public class ApplicationErrorController implements ErrorController {

	private static final Logger log = LoggerFactory.getLogger(ApplicationErrorController.class);

	private static final String PATH = "/error";

	public static final IpBlocker invalidRequestIpBlocker = new IpBlocker(5_000);

	@RequestMapping(value = PATH)
	public String error(HttpServletRequest request) {
		Object originalUrl = request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
		if (originalUrl != null && Main.endpoints.contains(originalUrl)) {
			log.info("Error serving request: {}\n{} ", originalUrl, Utils.printRequest(request));
			return Utils.returnInvalidRequestPlain();
		} else {
			invalidRequestIpBlocker.register(IpBlocker.getIp(request));
		}
		return "";
	}

	@Override
	public String getErrorPath() {
		return PATH;
	}
}
