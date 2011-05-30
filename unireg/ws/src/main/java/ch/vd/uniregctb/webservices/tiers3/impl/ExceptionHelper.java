package ch.vd.uniregctb.webservices.tiers3.impl;

import ch.vd.uniregctb.webservices.tiers3.AccessDeniedExceptionInfo;
import ch.vd.uniregctb.webservices.tiers3.BusinessExceptionCode;
import ch.vd.uniregctb.webservices.tiers3.BusinessExceptionInfo;
import ch.vd.uniregctb.webservices.tiers3.TechnicalExceptionInfo;
import ch.vd.uniregctb.webservices.tiers3.WebServiceException;

public class ExceptionHelper {

	public static WebServiceException newAccessDeniedException(String message) {
		final AccessDeniedExceptionInfo info = new AccessDeniedExceptionInfo();
		info.setMessage(message);
		return new WebServiceException(message, info);
	}

	public static WebServiceException newAccessDeniedException(Exception exception) {
		final AccessDeniedExceptionInfo info = new AccessDeniedExceptionInfo();
		final String message = exception.getMessage();
		info.setMessage(message);
		return new WebServiceException(message, info);
	}

	public static WebServiceException newBusinessException(String message, BusinessExceptionCode code) {
		final BusinessExceptionInfo info = new BusinessExceptionInfo();
		info.setMessage(message);
		info.setCode(code.value());
		return new WebServiceException(message, info);
	}

	public static WebServiceException newBusinessException(Exception exception, BusinessExceptionCode code) {
		final BusinessExceptionInfo info = new BusinessExceptionInfo();
		final String message = exception.getMessage();
		info.setMessage(message);
		info.setCode(code.value());
		return new WebServiceException(message, info);
	}

	public static WebServiceException newTechnicalException(String message) {
		final TechnicalExceptionInfo info = new TechnicalExceptionInfo();
		info.setMessage(message);
		return new WebServiceException(message, info);
	}

	public static WebServiceException newTechnicalException(Exception exception) {
		final TechnicalExceptionInfo info = new TechnicalExceptionInfo();
		final String message = exception.getMessage();
		info.setMessage(message);
		return new WebServiceException(message, info);
	}
}
