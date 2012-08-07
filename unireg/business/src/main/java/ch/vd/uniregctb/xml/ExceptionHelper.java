package ch.vd.uniregctb.xml;

import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.unireg.xml.exception.v1.TechnicalExceptionInfo;

public class ExceptionHelper {

	public static ServiceException newAccessDeniedException(String message) {
		final AccessDeniedExceptionInfo info = new AccessDeniedExceptionInfo();
		info.setMessage(message);
		return new ServiceException(message, info);
	}

	public static ServiceException newAccessDeniedException(Exception exception) {
		final AccessDeniedExceptionInfo info = new AccessDeniedExceptionInfo();
		final String message = exception.getMessage();
		info.setMessage(message);
		return new ServiceException(message, info);
	}

	public static ServiceException newBusinessException(String message, BusinessExceptionCode code) {
		final BusinessExceptionInfo info = new BusinessExceptionInfo();
		info.setMessage(message);
		info.setCode(code.value());
		return new ServiceException(message, info);
	}

	public static ServiceException newBusinessException(Exception exception, BusinessExceptionCode code) {
		final BusinessExceptionInfo info = new BusinessExceptionInfo();
		final String message = exception.getMessage();
		info.setMessage(message);
		info.setCode(code.value());
		return new ServiceException(message, info);
	}

	public static ServiceException newTechnicalException(String message) {
		final TechnicalExceptionInfo info = new TechnicalExceptionInfo();
		info.setMessage(message);
		return new ServiceException(message, info);
	}

	public static ServiceException newTechnicalException(Exception exception) {
		final TechnicalExceptionInfo info = new TechnicalExceptionInfo();
		final String message = exception.getMessage();
		info.setMessage(message);
		return new ServiceException(message, info);
	}
}
