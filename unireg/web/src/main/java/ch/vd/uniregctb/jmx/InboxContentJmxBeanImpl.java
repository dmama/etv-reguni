package ch.vd.uniregctb.jmx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.jmx.export.annotation.ManagedResource;

import ch.vd.uniregctb.common.TimeHelper;
import ch.vd.uniregctb.inbox.InboxElement;
import ch.vd.uniregctb.inbox.InboxService;

@ManagedResource
public class InboxContentJmxBeanImpl implements InboxContentJmxBean {

	private final String visa;

	private final InboxService inboxService;

	public InboxContentJmxBeanImpl(String visa, InboxService inboxService) {
		this.visa = visa;
		this.inboxService = inboxService;
	}

	@Override
	public List<String> getContent() {
		final List<InboxElement> elts = getElements();
		final List<String> descriptions = new ArrayList<String>(elts.size());
		for (InboxElement elt : elts) {
			final String expiration;
			if (elt.isExpired()) {
				expiration = ", expired";
			}
			else if (elt.getTimeToExpiration() != null) {
				expiration = String.format(", expiration in %s", TimeHelper.formatDureeShort(elt.getTimeToExpiration()));
			}
			else {
				expiration = StringUtils.EMPTY;
			}
			final String attachment;
			if (elt.getAttachment() != null) {
				attachment = String.format(", attachment (%s, %d bytes)", elt.getAttachment().getMimeType(), elt.getAttachment().getSize());
			}
			else {
				attachment = StringUtils.EMPTY;
			}
			final String desc = String.format("%s \"%s\", income on %s%s%s, %s (%s)", elt.getName(),
			                                  elt.getDescription(), elt.getIncomingDate(), expiration, attachment,
			                                  elt.isRead() ? "read" : "unread", elt.getUuid());
			descriptions.add(desc);
		}
		return descriptions;
	}

	@Override
	public int getSize() {
		return getElements().size();
	}

	@Override
	public int getUnread() {
		final List<InboxElement> elts = getElements();
		int count = 0;
		for (InboxElement elt : elts) {
			if (!elt.isRead()) {
				++ count;
			}
		}
		return count;
	}

	private List<InboxElement> getElements() {
		final List<InboxElement> elts = inboxService.getInboxContent(visa);
		return elts != null ? elts : Collections.<InboxElement>emptyList();
	}
}
