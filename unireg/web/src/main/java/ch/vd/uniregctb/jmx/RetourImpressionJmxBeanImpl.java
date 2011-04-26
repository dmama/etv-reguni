package ch.vd.uniregctb.jmx;

import java.util.Date;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.uniregctb.editique.EditiqueRetourImpressionStorageService;

/**
 * Implémentation du bean JMX de monitoring de la gestion des retours d'impression
 */
@ManagedResource
public class RetourImpressionJmxBeanImpl implements RetourImpressionJmxBean {

	private static final String NEVER = "Never";

	private EditiqueRetourImpressionStorageService storageService;

	private int callerTimeout;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setStorageService(EditiqueRetourImpressionStorageService storageService) {
		this.storageService = storageService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setCallerTimeout(int callerTimeout) {
		this.callerTimeout = callerTimeout;
	}

	@ManagedAttribute
	public int getReceivedAndNotDispatched() {
		return storageService.getDocumentsEnAttenteDeDispatch();
	}

	@ManagedAttribute
	public int getTimeToLiveOnceReceived() {
		return storageService.getCleanupPeriod();
	}

	@ManagedAttribute
	public void setTimeToLiveOnceReceived(int ttl) {
		if (ttl < callerTimeout) {
			throw new IllegalArgumentException("Value should not be lower than the local print timeout");
		}
		storageService.setCleanupPeriod(ttl);
	}

	@ManagedAttribute
	public int getLocalPrintTimeout() {
		return callerTimeout;
	}

	@ManagedAttribute
	public String getLastDocumentPurgeDate() {
		final String str;
		final Date lastPurge = storageService.getDateDernierePurgeEffective();
		if (lastPurge == null) {
			str = NEVER;
		}
		else {
			str = DateHelper.dateTimeToDisplayString(lastPurge);
		}
		return str;
	}

	@ManagedAttribute
	public int getPurged() {
		return storageService.getDocumentsPurges();
	}

	@ManagedAttribute
	public int getReceived() {
		return storageService.getDocumentsRecus();
	}
}
