package ch.vd.unireg.jmx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.unireg.common.DelayedDownloadService;
import ch.vd.unireg.common.TimeHelper;
import ch.vd.unireg.editique.EditiqueRetourImpressionStorageService;
import ch.vd.unireg.editique.RetourImpressionToInboxTrigger;
import ch.vd.unireg.editique.RetourImpressionTrigger;

/**
 * Implémentation du bean JMX de monitoring de la gestion des retours d'impression
 */
@ManagedResource
public class RetourImpressionJmxBeanImpl implements RetourImpressionJmxBean {

	private static final String NEVER = "Never";

	private EditiqueRetourImpressionStorageService storageService;
	private DelayedDownloadService delayedDownloadService;

	private int callerTimeout;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setStorageService(EditiqueRetourImpressionStorageService storageService) {
		this.storageService = storageService;
	}

	public void setDelayedDownloadService(DelayedDownloadService delayedDownloadService) {
		this.delayedDownloadService = delayedDownloadService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setCallerTimeout(int callerTimeout) {
		this.callerTimeout = callerTimeout;
	}

	@Override
	@ManagedAttribute
	public int getReceivedAndNotDispatched() {
		return storageService.getDocumentsEnAttenteDeDispatch();
	}

	@Override
	@ManagedAttribute
	public int getTimeToLiveOnceReceived() {
		return storageService.getCleanupPeriod();
	}

	@Override
	@ManagedAttribute
	public int getLocalPrintTimeout() {
		return callerTimeout;
	}

	@Override
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

	@Override
	@ManagedAttribute
	public int getPurged() {
		return storageService.getDocumentsPurges();
	}

	@Override
	@ManagedAttribute
	public int getReceived() {
		return storageService.getDocumentsRecus();
	}

	@Override
	@ManagedAttribute
	public List<String> getAwaitingInboxRedirection() {
		final Collection<Pair<Long, RetourImpressionTrigger>> triggers = storageService.getTriggersEnregistres();
		final List<String> logs = new ArrayList<>(triggers.size());
		final long now = System.nanoTime();
		for (Pair<Long, RetourImpressionTrigger> trigger : triggers) {
			if (trigger.getRight() instanceof RetourImpressionToInboxTrigger) {
				final RetourImpressionToInboxTrigger inboxTrigger = (RetourImpressionToInboxTrigger) trigger.getRight();
				final String duration = TimeHelper.formatDureeShort(TimeUnit.NANOSECONDS.toMillis(now - trigger.getLeft()));
				logs.add(String.format("%s (opérateur %s, depuis %s)", inboxTrigger.getDescription(), inboxTrigger.getVisa(), duration));
			}
		}
		return logs;
	}

	@Override
	@ManagedAttribute
	public int getPendingDelayedDownloads() {
		return delayedDownloadService.getPendingSize();
	}
}
