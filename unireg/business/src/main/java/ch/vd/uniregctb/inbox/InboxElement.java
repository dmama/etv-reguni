package ch.vd.uniregctb.inbox;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.UUID;

import ch.vd.registre.base.date.DateHelper;

/**
 * Un élément d'une inbox (= un message).<p/>
 * L'ordre naturel de tri correspond à l'ordre inverse de l'ordre d'arrivée (= les derniers arrivés d'abord).<p/>
 * L'égalité est basée sur l'identifiant {@link #uuid}.
 */
public class InboxElement implements Comparable<InboxElement>, Expirable {

	private final UUID uuid;
	private final String name;
	private final String description;
	private final String mimeType;
	private final TempFileInputStreamProvider contentProvider;
	private final Date incomingDate;
	private final long expirationDate;

	/**
	 * Flag qui indique si la méthode {@link #getContent()} a déjà été appelée sur cet objet
	 */
	private boolean read;

	/**
	 * Constructeur d'un objet InboxElement dont l'identifiant unique est déjà fixé
	 * @param uuid identifiant unique de l'élément
	 * @param name nom de l'élément
	 * @param description petite description du contenu
	 * @param mimeType mime-type du contenu
	 * @param content input stream depuis lequel le contenu peut-être lu (il sera lu directement dans le constructeur, et le stream sera fermé)
	 * @param msUntilExpiration délai d'expiration de l'élément en millisecondes (0 = aucune expiration)
	 * @throws IOException en cas de problème à la lecture de l'input stream fourni
	 */
	public InboxElement(UUID uuid, String name, String description, String mimeType, InputStream content, long msUntilExpiration) throws IOException {
		this.uuid = uuid;
		this.name = name;
		this.description = description;
		this.mimeType = mimeType;
		this.contentProvider = content != null ? new TempFileInputStreamProvider("ur-inbox-elt-", content) : null;
		this.incomingDate = DateHelper.getCurrentDate();
		this.expirationDate = msUntilExpiration <= 0 ? Long.MAX_VALUE : this.incomingDate.getTime() + msUntilExpiration;
		this.read = false;
		if (content != null) {
			content.close();
		}
	}

	/**
	 * Constructeur d'un objet InboxElement pour lequel la génération de l'identifiant unique est laissée à l'implémentation
	 * @param name nom de l'élément
	 * @param description petite description du contenu
	 * @param mimeType mime-type du contenu
	 * @param content input stream depuis lequel le contenu peut-être lu (il sera lu directement dans le constructeur, et le stream sera fermé)
	 * @param msUntilExpiration délai d'expiration de l'élément en millisecondes (0 = aucune expiration)
	 * @throws IOException en cas de problème à la lecture de l'input stream fourni
	 * @see UUID#randomUUID()
	 */
	public InboxElement(String name, String description, String mimeType, InputStream content, long msUntilExpiration) throws IOException {
		this(UUID.randomUUID(), name, description, mimeType, content, msUntilExpiration);
	}

	public UUID getUuid() {
		return uuid;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getMimeType() {
		return mimeType;
	}

	public InputStream getContent() throws IOException {
		read = true;
		return contentProvider != null ? contentProvider.getInputStream() : null;
	}

	public Date getIncomingDate() {
		return incomingDate;
	}

	public boolean isExpired() {
		return DateHelper.getCurrentDate().getTime() > expirationDate;
	}

	public boolean isRead() {
		return read;
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final InboxElement that = (InboxElement) o;
		return uuid.equals(that.uuid);
	}

	@Override
	public int hashCode() {
		return uuid.hashCode();
	}
}
