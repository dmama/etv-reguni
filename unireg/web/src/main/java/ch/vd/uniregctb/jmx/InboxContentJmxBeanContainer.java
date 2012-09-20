package ch.vd.uniregctb.jmx;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.export.MBeanExportOperations;

import ch.vd.uniregctb.inbox.InboxManagementListener;
import ch.vd.uniregctb.inbox.InboxService;

/**
 * Classe qui va exporter en JMX les inboxes existantes et leur contenu
 */
public class InboxContentJmxBeanContainer implements InboxManagementListener, InitializingBean {

	private static final Logger LOGGER = Logger.getLogger(InboxContentJmxBeanContainer.class);

	private InboxService inboxService;

	private MBeanExportOperations exporter;

	private String objectNamePrefix;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setInboxService(InboxService inboxService) {
		this.inboxService = inboxService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setExporter(MBeanExportOperations exporter) {
		this.exporter = exporter;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setObjectNamePrefix(String objectNamePrefix) {
		this.objectNamePrefix = objectNamePrefix;
	}

	@Override
	public void onNewInbox(String visa) {
		try {
			final String newName = String.format("%s%s", objectNamePrefix, visa);
			exporter.registerManagedResource(new InboxContentJmxBeanImpl(visa, inboxService), ObjectName.getInstance(newName));
		}
		catch (MalformedObjectNameException e) {
			LOGGER.error("Mauvais algorithme de formation du nom du bean JMX", e);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		inboxService.registerInboxManagementListener(this);
	}
}
