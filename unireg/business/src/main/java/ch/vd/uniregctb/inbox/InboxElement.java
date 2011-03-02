package ch.vd.uniregctb.inbox;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import ch.vd.registre.base.date.DateHelper;

/**
 * Un élément d'une inbox (= un message).<p/>
 * L'ordre naturel de tri correspond à l'ordre inverse de l'ordre d'arrivée (= les derniers arrivés d'abord)
 */
public class InboxElement implements Comparable<InboxElement>, Expirable {

	private final String name;
	private final String mimeType;
	private final TempFileInputStreamProvider contentProvider;
	private final Date incomingDate;
	private final long expirationDate;

	public InboxElement(String name, String mimeType, InputStream content, long msUntilExpiration) throws IOException {
		this.name = name;
		this.mimeType = mimeType;
		this.contentProvider = content != null ? new TempFileInputStreamProvider("ur-inbox-elt-", content) : null;
		this.incomingDate = DateHelper.getCurrentDate();
		this.expirationDate = msUntilExpiration <= 0 ? Long.MAX_VALUE : this.incomingDate.getTime() + msUntilExpiration;
		if (content != null) {
			content.close();
		}
	}

	public String getName() {
		return name;
	}

	public String getMimeType() {
		return mimeType;
	}

	public InputStream getContent() throws IOException {
		return contentProvider != null ? contentProvider.getInputStream() : null;
	}

	public Date getIncomingDate() {
		return incomingDate;
	}

	public boolean isExpired() {
		return DateHelper.getCurrentDate().getTime() > expirationDate;
	}

	/**
	 * Appelé lorsque l'élément est envoyé aux oubliettes (pour la clôture du flux d'entrée
	 * et d'éventuels nettoyages)
	 */
	public void onDiscard() {
		if (contentProvider != null) {
			contentProvider.close();
		}
	}

	@Override
	public int compareTo(InboxElement o) {
		return - this.incomingDate.compareTo(o.incomingDate);
	}

	@Override
	public String toString() {
		return String.format("{name=%s, arrival=%s}", name, DateHelper.dateTimeToDisplayString(incomingDate));
	}
}
