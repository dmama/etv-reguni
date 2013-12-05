package ch.vd.uniregctb.audit;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.document.Document;

public class Audit {

	private static final Logger LOGGER = Logger.getLogger(Audit.class);

	private static AuditLineDAO dao;
	private static final String PREFIX = "[AUDIT] ";

	public static void success(String message) {
		logAuditLine(AuditLevel.SUCCESS, message, null, null);
	}

	public static void info(String message) {
		logAuditLine(AuditLevel.INFO, message, null, null);
	}

	public static void warn(String message) {
		logAuditLine(AuditLevel.WARN, message, null, null);
	}

	public static void error(String message) {
		logAuditLine(AuditLevel.ERROR, message, null, null);
	}

	/**
	 * Methode qui log une ligtne d'audit en SUCCESS
	 *
	 * @param evtId   le numéro de l'événement
	 * @param message le message à logger
	 */
	public static void success(long evtId, String message) {
		logAuditLine(AuditLevel.SUCCESS, message, evtId, null);
	}

	public static void success(int evtId, String message) {
		logAuditLine(AuditLevel.SUCCESS, message, (long) evtId, null);
	}

	public static void success(Long evtId, String message) {
		logAuditLine(AuditLevel.SUCCESS, message, evtId, null);
	}

	/**
	 * Methode qui log une ligtne d'audit en INFO
	 *
	 * @param evtId   le numéro de l'événement
	 * @param message le message à logger
	 */
	public static void info(long evtId, String message) {
		logAuditLine(AuditLevel.INFO, message, evtId, null);
	}

	public static void info(int evtId, String message) {
		logAuditLine(AuditLevel.INFO, message, (long) evtId, null);
	}

	public static void info(Long evtId, String message) {
		logAuditLine(AuditLevel.INFO, message, evtId, null);
	}

	/**
	 * Methode qui log une ligtne d'audit en WARNING
	 *
	 * @param evtId   le numéro de l'événement
	 * @param message le message à logger
	 */
	public static void warn(long evtId, String message) {
		logAuditLine(AuditLevel.WARN, message, evtId, null);
	}

	public static void warn(int evtId, String message) {
		logAuditLine(AuditLevel.WARN, message, (long) evtId, null);
	}

	public static void warn(Long evtId, String message) {
		logAuditLine(AuditLevel.WARN, message, evtId, null);
	}

	/**
	 * Methode qui log une ligtne d'audit en ERROR
	 *
	 * @param evtId le numéro de l'événement
	 * @param e     l'exception à levée
	 */
	public static void error(Long evtId, Exception e) {
		String m = e.getMessage();
		if (m == null) {
			m = e.getClass().getSimpleName();
		}
		logAuditLine(AuditLevel.ERROR, m, evtId, null);
	}

	public static void error(long evtId, String message) {
		logAuditLine(AuditLevel.ERROR, message, evtId, null);
	}

	public static void error(int evtId, String message) {
		logAuditLine(AuditLevel.ERROR, message, (long) evtId, null);
	}

	public static void error(Long evtId, String message) {
		logAuditLine(AuditLevel.ERROR, message, evtId, null);
	}

	/**
	 * Stocke un message de succès dans l'audit avec une référence sur un document
	 *
	 * @param message le message à logger
	 * @param doc     le document associé au message
	 */
	public static void success(String message, Document doc) {
		Long id = doc.getId();
		Assert.notNull(id);
		logAuditLine(AuditLevel.SUCCESS, message, null, id);
	}

	/**
	 * Ajoute un message d'erreur dans l'audit avec une référence sur un document
	 *
	 * @param message le message à logger
	 * @param doc     le document associé au message
	 */
	public static void error(String message, Document doc) {
		Long id = doc.getId();
		Assert.notNull(id);
		logAuditLine(AuditLevel.ERROR, message, null, id);
	}

	/**
	 * Ajoute un message d'info dans l'audit avec une référence sur un document
	 *
	 * @param message le message à logger
	 * @param doc     le document associé au message
	 */
	public static void info(String message, Document doc) {
		Long id = doc.getId();
		Assert.notNull(id);
		logAuditLine(AuditLevel.INFO, message, null, id);
	}

	private static long getThreadId() {
		return Thread.currentThread().getId();
	}

	private static void logAuditLine(AuditLevel level, String message, @Nullable Long evtId, @Nullable Long docId) {
		LOGGER.log(level.asLog4j(), PREFIX + message);

		try {
			Assert.notNull(dao, "La DAO est Nulle! Le logging ne peut se faire que pendant que le contexte Spring se met en place");
			AuditLine line = new AuditLine(getThreadId(), evtId, AuthenticationHelper.getCurrentPrincipal(), level, message, docId);
			dao.insertLineInNewTx(line);
		}
		catch (Exception e) {
			LOGGER.error("Impossible de logger la ligne d'AUDIT: " + message);
		}
	}

	public static void setAuditLineDao(AuditLineDAO dao) {
		Audit.dao = dao;
	}
}
