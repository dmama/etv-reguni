package ch.vd.uniregctb.webservices.tiers3.impl;

import ch.vd.unireg.webservices.tiers3.WebServiceException;
import ch.vd.unireg.webservices.tiers3.exception.AccessDeniedExceptionInfo;
import ch.vd.unireg.webservices.tiers3.exception.BusinessExceptionCode;
import ch.vd.unireg.webservices.tiers3.exception.BusinessExceptionInfo;
import ch.vd.unireg.webservices.tiers3.exception.TechnicalExceptionInfo;

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
