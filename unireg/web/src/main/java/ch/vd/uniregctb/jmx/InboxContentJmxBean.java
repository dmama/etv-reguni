package ch.vd.uniregctb.jmx;

import java.util.List;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

@ManagedResource
public interface InboxContentJmxBean {

	@ManagedAttribute(description = "Inbox content")
	List<String> getContent();

	@ManagedAttribute(description = "Inbox size")
	int getSize();

	@ManagedAttribute(description = "Number of unread elements")
	int getUnread();
}
