package com.univocity.shopify.utils;

import com.univocity.shopify.*;
import com.univocity.shopify.controllers.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.core.*;
import org.springframework.core.annotation.*;
import org.springframework.stereotype.*;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IpFilter implements Filter {

	private static final IpBlocker invalidRequestIpBlocker = ApplicationErrorController.invalidRequestIpBlocker;

	private static final IpBlocker abusiveIpBlocker = new IpBlocker(15_000);

	@Autowired
	App app;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		String ip = IpBlocker.getIp(request);

//		if (!request.isSecure()) {
//			((HttpServletResponse) response).setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);
//			return;
//		}

		String endpoint = IpBlocker.getEndpoint(request);
		if(endpoint.endsWith(".ico")){
			((HttpServletResponse) response).setStatus(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		if (ip == null || (!Main.endpoints.contains(endpoint) && invalidRequestIpBlocker.shouldBlock(ip, 10000, "Blocking IP trying to run exploits: {}"))) { //multiple invalid hits at an average 1 hit every less than 5 seconds? That's an exploit
			((HttpServletResponse) response).setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);
		} else {
			if (endpoint.endsWith("/register")) {
				endpoint = "/register";
			} else if (endpoint.endsWith("/validate")) {
				endpoint = "/validate";
			} else if (endpoint.endsWith("/validate")) {
				endpoint = "/release";
			} else {
				endpoint = null;
			}

			if (endpoint != null) {
				if (app.isLive()) {
					String apiCall = ip + endpoint;
					abusiveIpBlocker.register(apiCall);
					if (abusiveIpBlocker.shouldBlock(apiCall, 5000, "Blocking IP abusing API: {}")) {
						((HttpServletResponse) response).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
						return;
					}
				}
			}
			chain.doFilter(request, response);
		}
	}

	@Override
	public void destroy() {

	}
}