package ch.vd.uniregctb.inbox;

import java.io.IOException;
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
	private final InboxAttachment attachment;
	private final Date incomingDate;
	private final long expirationDate;
	private boolean read;

	/**
	 * Constructeur d'un objet InboxElement dont l'identifiant unique est déjà fixé
	 * @param uuid identifiant unique de l'élément
	 * @param name nom de l'élément
	 * @param description petite description du contenu
	 * @param attachement (optionel) attachement à l'élément de l'inbox
	 * @param msUntilExpiration délai d'expiration de l'élément en millisecondes (0 = aucune expiration)
	 * @throws IOException en cas de problème à la lecture de l'input stream fourni
	 */
	public InboxElement(UUID uuid, String name, String description, InboxAttachment attachement, long msUntilExpiration) throws IOException {
		this.uuid = uuid;
		this.name = name;
		this.description = description;
		this.attachment = attachement;
		this.incomingDate = DateHelper.getCurrentDate();
		this.expirationDate = msUntilExpiration <= 0 ? Long.MAX_VALUE : this.incomingDate.getTime() + msUntilExpiration;
		this.read = false;
	}

	/**
	 * Constructeur d'un objet InboxElement pour lequel la génération de l'identifiant unique est laissée à l'implémentation
	 * @param name nom de l'élément
	 * @param description petite description du contenu
	 * @param attachement (optionel) attachement à l'élément de l'inbox
	 * @param msUntilExpiration délai d'expiration de l'élément en millisecondes (0 = aucune expiration)
	 * @throws IOException en cas de problème à la lecture de l'input stream fourni
	 * @see UUID#randomUUID()
	 */
	public InboxElement(String name, String description, InboxAttachment attachement, long msUntilExpiration) throws IOException {
		this(UUID.randomUUID(), name, description, attachement, msUntilExpiration);
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

	public InboxAttachment getAttachment() {
		return attachment;
	}

	public Date getIncomingDate() {
		return incomingDate;
	}

	@Override
	public boolean isExpired() {
		final Long tte = getTimeToExpiration();
		return tte != null && tte == 0L;
	}

	public Long getTimeToExpiration() {
		final Long timeToExpiration;
		if (expirationDate == Long.MAX_VALUE) {
			timeToExpiration = null;
		}
		else {
			final long computedTimeToExpiration = expirationDate - DateHelper.getCurrentDate().getTime();
			if (computedTimeToExpiration > 0) {
				timeToExpiration = computedTimeToExpiration;
			}
			else {
				timeToExpiration = 0L;
			}
		}
		return timeToExpiration;
	}

	public boolean isRead() {
		return read;
	}

	public void setRead(boolean read) {
		this.read = read;
	}

	/**
	 * Appelé lorsque l'élément est envoyé aux oubliettes (pour la clôture du flux d'entrée
	 * et d'éventuels nettoyages)
	 */
	public void onDiscard() {
		if (attachment != null) {
			attachment.onDiscard();
		}
	}

	@Override
	public int compareTo(InboxElement o) {
		return - this.incomingDate.compareTo(o.incomingDate);
	}

	@Override
	public String toString() {
		return String.format("{uuid=%s, name=%s, arrival=%s}", uuid, name, DateHelper.dateTimeToDisplayString(incomingDate));
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
