package com.univocity.shopify.controllers;

import com.univocity.shopify.email.*;
import com.univocity.shopify.exception.*;
import com.univocity.shopify.utils.*;
import org.apache.commons.lang3.*;
import org.apache.commons.lang3.exception.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.web.*;
import org.springframework.web.bind.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.*;

import javax.servlet.http.*;
import java.io.*;
import java.util.*;

/**
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */
@ControllerAdvice
public class ApplicationExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApplicationExceptionHandler.class);

	@Autowired
	SystemMailSender systemMailSender;

	private static final <T extends Throwable> String generateUIDAndLog(HttpServletRequest request, T ex) {
		if (ex instanceof MissingServletRequestParameterException) {
			return Utils.returnInvalidRequestPlain();
		}
		String uid = UUID.randomUUID().toString();
		log.error("Error " + uid + ": Error processing request '" + Utils.printRequest(request) + "'", ex);
		return "Error " + uid + ". ";
	}

	private <T extends Throwable> ResponseEntity<String> newErrorResponse(HttpServletRequest request, T ex, String msg, HttpStatus status) {
		if (ex instanceof ValidationException) {
			msg = ex.getMessage();
		} else if (ex instanceof MissingServletRequestParameterException) {
			msg = Utils.returnInvalidRequestPlain();
		} else {
			msg = generateUIDAndLog(request, ex) + msg;
		}
		return new ResponseEntity<>(msg, status);
	}

	@ExceptionHandler(NoHandlerFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public @ResponseBody
	ResponseEntity<String> handleNoMethodException(HttpServletRequest request, NoHandlerFoundException ex) {
		return newErrorResponse(request, ex, "Resource not found", HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(Throwable.class)
	public @ResponseBody
	ResponseEntity<String> handleDefaultException(HttpServletRequest request, Throwable ex) {
		return newErrorResponse(request, ex, "Internal error", HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Object mediaTypeExceptionHandler(HttpServletRequest request, IOException ex) {
		return new HttpEntity<>(generateUIDAndLog(request, ex));
	}

	@ExceptionHandler(IOException.class)
	@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
	public Object ioExceptionHandler(HttpServletRequest request, IOException ex) {
		String uid = generateUIDAndLog(request, ex);
		if (StringUtils.containsIgnoreCase(ExceptionUtils.getRootCauseMessage(ex), "Broken pipe")) {
			return null;        //socket is closed, cannot return any response
		} else {
			return new HttpEntity<>(uid);
		}
	}
}
