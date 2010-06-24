/**
 *
 */
package ch.vd.uniregctb.type;

/** 
 * <!-- begin-user-doc -->
 * Longueur de colonne : 10
 * <!-- end-user-doc -->
 * @author jec
 * 
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_WqFYYMgEEdyvxsruSlJY5Q"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_WqFYYMgEEdyvxsruSlJY5Q"
 */
public enum EtatEvenementCivil {

	/**
	 * Vient d'arriver : n'a pas encore été traité
	 */
	A_TRAITER(false),

	/**
	 * Evénement complétement traité sans erreur
	 */
	TRAITE(true),

	/**
	 * Evénement dont le traitement (inachevé, donc) a mené à des erreurs
	 */
	EN_ERREUR(false),

	/**
	 * Evénement dont le traitement (achevé) mérite qu'on y jette encore un oeil
	 */
	A_VERIFIER(true),

	/**
	 * Evénement initialement en erreur mais qu'un opérateur a traité
	 * manuellement (aucun contrôle du traitement effectif n'est fait)
	 */
	FORCE(true);

	private boolean isTraite;

	private EtatEvenementCivil(boolean isTraite) {
		this.isTraite = isTraite;
	}

	public final boolean isTraite() {
		return isTraite;
	}
}