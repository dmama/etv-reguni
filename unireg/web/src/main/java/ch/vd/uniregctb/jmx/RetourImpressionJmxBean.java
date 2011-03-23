package ch.vd.uniregctb.jmx;

import org.springframework.jmx.export.annotation.ManagedAttribute;

/**
 * Interface du bean JMX qui expose les données relatives à la gestion des retours d'impression
 */
public interface RetourImpressionJmxBean {

	@ManagedAttribute(description = "Number of printed documents that were received but not yet dispatched to the requestor")
	int getReceivedAndNotDispatched();

	@ManagedAttribute(description = "Time-to-live (seconds) for a received-and-not-dispatched document")
	int getTimeToLiveOnceReceived();

	@ManagedAttribute(description = "Time-to-live (seconds) for a received-and-not-dispatched document")
	void setTimeToLiveOnceReceived(int ttl);

	@ManagedAttribute(description = "Requestor timeout for local printing")
	int getLocalPrintTimeout();

	@ManagedAttribute(description = "Last time a document fell victim to the Time-to-live constraint")
	String getLastDocumentPurgeDate();

	@ManagedAttribute(description = "Number of documents fallen victim to the Time-to-live constraint since application start")
	int getPurged();

	@ManagedAttribute(description = "Number of documents received since application start")
	int getReceived();
}
