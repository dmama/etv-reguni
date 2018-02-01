package ch.vd.uniregctb.xml;

import java.sql.SQLException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

	/**
	 * @param e une exception
	 * @return le message d'erreur de l'exception ou le nom de l'exception si le message est nul.
	 */
	@NotNull
	public static String getMessage(@NotNull Exception e) {
		String message = e.getMessage();
		if (message == null) {
			message = e.getClass().getSimpleName();
		}
		return message;
	}

	/**
	 * @param e une exception
	 * @return le message d'erreur SQL si l'exception (ou un de ses parents) contient un message d'erreur SQL.
	 */
	@Nullable
	public static String getSqlExceptionMessage(@NotNull Exception e) {
		String sqlMessage = null;
		Throwable t = e;
		while (t != null) {
			if (t instanceof SQLException) {
				SQLException sqle = (SQLException) t;
				sqlMessage = sqle.getMessage();
				break;
			}
			t = t.getCause();
		}
		return sqlMessage;
	}

	/**
	 * Construit un message d'erreur aussi informatif que possible. Cette méthode va notamment rechercher le message d'erreur SQL s'il est présent dans la callstack et l'ajoute au message d'erreur (si nécessaire).
	 *
	 * @param e une exception
	 * @return un message d'erreur.
	 */
	@NotNull
	public static String getEnhancedMessage(@NotNull Exception e) {
		final String sqlMessage = getSqlExceptionMessage(e);
		final String message = getMessage(e);
		if (sqlMessage == null || message.contains(sqlMessage)) {
			return message;
		}
		else {
			return message + "; " + sqlMessage;
		}
	}
}
