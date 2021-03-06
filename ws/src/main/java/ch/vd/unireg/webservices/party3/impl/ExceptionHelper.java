package ch.vd.unireg.webservices.party3.impl;

import ch.vd.unireg.webservices.party3.WebServiceException;
import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.unireg.xml.exception.v1.TechnicalExceptionInfo;
import ch.vd.unireg.xml.ServiceException;

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

	public static WebServiceException newException(ServiceException e) {
		return new WebServiceException(e.getMessage(), e.getInfo());
	}
}
