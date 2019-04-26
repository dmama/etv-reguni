package ch.vd.unireg.audit;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.document.Document;
import ch.vd.unireg.utils.LogLevel;

public class AuditManager implements InitializingBean, DisposableBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(AuditManager.class);

	private AuditLineDAO dao;
	private String appName;

	public void setAuditLineDAO(AuditLineDAO dao) {
		this.dao = dao;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		AuthenticationHelper.pushPrincipal(AuthenticationHelper.SYSTEM_USER);
		try {
			info(String.format("Démarrage de l'application %s.", appName));
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	@Override
	public void destroy() throws Exception {
		AuthenticationHelper.pushPrincipal(AuthenticationHelper.SYSTEM_USER);
		try {
			info(String.format("Arrêt de l'application %s.", appName));
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	private static final String PREFIX = "[AUDIT] ";

	public void success(String message) {
		logAuditLine(AuditLevel.SUCCESS, message, null, null);
	}

	public void info(String message) {
		logAuditLine(AuditLevel.INFO, message, null, null);
	}

	public void warn(String message) {
		logAuditLine(AuditLevel.WARN, message, null, null);
	}

	public void error(String message) {
		logAuditLine(AuditLevel.ERROR, message, null, null);
	}

	/**
	 * Methode qui log une ligtne d'audit en SUCCESS
	 *
	 * @param evtId   le numéro de l'événement
	 * @param message le message à logger
	 */
	public void success(long evtId, String message) {
		logAuditLine(AuditLevel.SUCCESS, message, evtId, null);
	}

	public void success(int evtId, String message) {
		logAuditLine(AuditLevel.SUCCESS, message, (long) evtId, null);
	}

	public void success(Long evtId, String message) {
		logAuditLine(AuditLevel.SUCCESS, message, evtId, null);
	}

	/**
	 * Methode qui log une ligtne d'audit en INFO
	 *
	 * @param evtId   le numéro de l'événement
	 * @param message le message à logger
	 */
	public void info(long evtId, String message) {
		logAuditLine(AuditLevel.INFO, message, evtId, null);
	}

	public void info(int evtId, String message) {
		logAuditLine(AuditLevel.INFO, message, (long) evtId, null);
	}

	public void info(Long evtId, String message) {
		logAuditLine(AuditLevel.INFO, message, evtId, null);
	}

	/**
	 * Methode qui log une ligtne d'audit en WARNING
	 *
	 * @param evtId   le numéro de l'événement
	 * @param message le message à logger
	 */
	public void warn(long evtId, String message) {
		logAuditLine(AuditLevel.WARN, message, evtId, null);
	}

	public void warn(int evtId, String message) {
		logAuditLine(AuditLevel.WARN, message, (long) evtId, null);
	}

	public void warn(Long evtId, String message) {
		logAuditLine(AuditLevel.WARN, message, evtId, null);
	}

	/**
	 * Methode qui log une ligtne d'audit en ERROR
	 *
	 * @param evtId le numéro de l'événement
	 * @param e     l'exception à levée
	 */
	public void error(Long evtId, Exception e) {
		String m = e.getMessage();
		if (m == null) {
			m = e.getClass().getSimpleName();
		}
		logAuditLine(AuditLevel.ERROR, m, evtId, null);
	}

	public void error(long evtId, String message) {
		logAuditLine(AuditLevel.ERROR, message, evtId, null);
	}

	public void error(int evtId, String message) {
		logAuditLine(AuditLevel.ERROR, message, (long) evtId, null);
	}

	public void error(Long evtId, String message) {
		logAuditLine(AuditLevel.ERROR, message, evtId, null);
	}

	/**
	 * Stocke un message de succès dans l'audit avec une référence sur un document
	 *
	 * @param message le message à logger
	 * @param doc     le document associé au message
	 */
	public void success(String message, Document doc) {
		Long id = doc.getId();
		if (id == null) {
			throw new IllegalArgumentException();
		}
		logAuditLine(AuditLevel.SUCCESS, message, null, id);
	}

	/**
	 * Ajoute un message d'erreur dans l'audit avec une référence sur un document
	 *
	 * @param message le message à logger
	 * @param doc     le document associé au message
	 */
	public void error(String message, Document doc) {
		Long id = doc.getId();
		if (id == null) {
			throw new IllegalArgumentException();
		}
		logAuditLine(AuditLevel.ERROR, message, null, id);
	}

	/**
	 * Ajoute un message d'info dans l'audit avec une référence sur un document
	 *
	 * @param message le message à logger
	 * @param doc     le document associé au message
	 */
	public void info(String message, Document doc) {
		Long id = doc.getId();
		if (id == null) {
			throw new IllegalArgumentException();
		}
		logAuditLine(AuditLevel.INFO, message, null, id);
	}

	private static long getThreadId() {
		return Thread.currentThread().getId();
	}

	private void logAuditLine(AuditLevel level, String message, @Nullable Long evtId, @Nullable Long docId) {
		LogLevel.log(LOGGER, level.asLogLevel(), PREFIX + message);

		try {
			AuditLine line = new AuditLine(getThreadId(), evtId, AuthenticationHelper.getCurrentPrincipal(), level, message, docId);
			dao.insertLineInNewTx(line);
		}
		catch (Exception e) {
			LOGGER.error("Impossible de logger la ligne d'AUDIT: " + message);
		}
	}

}
