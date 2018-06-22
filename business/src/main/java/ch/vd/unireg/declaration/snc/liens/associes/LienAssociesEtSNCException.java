package ch.vd.unireg.declaration.snc.liens.associes;

/**
 * Exception levée lorsqu'un lien associe/commanditaire ne peut être ajouter ou modifier en raison de problème de données (incohérence sur les tiers, données manquantes, ...)
 */
public class LienAssociesEtSNCException extends Exception {

	private static final long serialVersionUID = -5537034464167489894L;

	public LienAssociesEtSNCException(String message) {
		super(message);
	}
}

