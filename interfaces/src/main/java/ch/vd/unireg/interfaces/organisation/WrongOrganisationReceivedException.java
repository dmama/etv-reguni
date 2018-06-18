package ch.vd.unireg.interfaces.organisation;

/**
 * Exception lancée par le service d'organisation quand une organisation a été demandée mais une autre reçue...
 */
public class WrongOrganisationReceivedException extends ServiceOrganisationException {

	private static final long serialVersionUID = 7863428115633247946L;

	private final long askedForId;
	private final long receivedId;

	private static String buildMessage(long askedForId, long receivedId) {
		return String.format("Incohérence des données retournées détectée: organisation demandée = %d, organisation retournée = %d. " +
				                     "Verifiez que le numéro soumis est bien l'identifiant cantonal d'une organisation et non " +
				                     "celui d'un établissement civil.",
		                     askedForId,
		                     receivedId);
	}

	public WrongOrganisationReceivedException(long askedForId, long receivedId) {
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
