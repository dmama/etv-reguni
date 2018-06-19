package ch.vd.unireg.interfaces.entreprise;

/**
 * Exception lancée par le service d'entreprise quand une entreprise a été demandée mais une autre reçue...
 */
public class WrongEntrepriseReceivedException extends ServiceEntrepriseException {

	private static final long serialVersionUID = 7863428115633247946L;

	private final long askedForId;
	private final long receivedId;

	private static String buildMessage(long askedForId, long receivedId) {
		return String.format("Incohérence des données retournées détectée: entreprise demandée = %d, entreprise retournée = %d. " +
				                     "Verifiez que le numéro soumis est bien l'identifiant cantonal d'une entreprise et non " +
				                     "celui d'un établissement civil.",
		                     askedForId,
		                     receivedId);
	}

	public WrongEntrepriseReceivedException(long askedForId, long receivedId) {
		super(buildMessage(askedForId, receivedId));
		this.askedForId = askedForId;
		this.receivedId = receivedId;
	}

	public long getAskedForId() {
		return askedForId;
	}

	public long getReceivedId() {
		return receivedId;
	}
}
