package ch.vd.unireg.declaration.snc.liens.associes;

/**
 * Exception levée lorsqu'un lien associe/commanditaire ne peut être ajouter ou modifier en raison de problème de données (incohérence sur les tiers, données manquantes, ...)
 */
public class LienAssociesEtSNCException extends Exception {

	private static final long serialVersionUID = -5537034464167489894L;

	/**
	 * Enumeration des motifs d'invalidité possibles lors de l'intégration lien entre tiers Associé-SNC
	 */
	public enum EnumErreurLienAssocieSNC {
		/**
		 * Le tiers associé (sujet) n'est pas d'un type acceptable
		 */
		MAUVAIS_TYPE_ASSOCIE,
		/**
		 * Le tiers associé (objet) n'est pas d'un type acceptable
		 */
		MAUVAIS_TYPE_SNC,
		/**
		 * Le tiers objet  %s n'est pas une SNC
		 */
		TIERS_PAS_SNC,
		/**
		 * Deux liens entre les même contribuables ne peuvent se chevaucher dans le temps
		 */
		CHEVAUCHEMENT_LIEN;


		EnumErreurLienAssocieSNC() {
		}
	}

	private EnumErreurLienAssocieSNC erreur;

	public LienAssociesEtSNCException(EnumErreurLienAssocieSNC erreur, String message) {
		super(message);
		this.erreur = erreur;
	}

	public EnumErreurLienAssocieSNC getErreur() {
		return erreur;
	}


}

