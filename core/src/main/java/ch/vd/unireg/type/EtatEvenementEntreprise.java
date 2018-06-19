package ch.vd.unireg.type;

/**
 * Les différents état que peut prendre un événement entreprise.
 * <p/>
 * Longueur de colonne : 10
 *
 * @since 6.x
 */
public enum EtatEvenementEntreprise {

	/**
	 * Vient d'arriver : n'a pas encore été traité ni même analysé
	 */
	A_TRAITER(false),

	/**
	 * Evénement dont le traitement n'a pas été tenté en raison de la présence d'autres événements antérieurs eux-mêmes en attente ou en erreur
	 */
	EN_ATTENTE(false),

	/**
	 * Evénement complétement traité sans erreur. Inclue les événements d'indexation pure et les événements qui n'ont finallement nécessité
	 * aucune modification d'états en base. Ne pas confondre ces derniers avec redondant.
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
	 * Evénement initialement en erreur mais qu'un opérateur a traité manuellement (aucun contrôle du traitement effectif n'est fait)
	 */
	FORCE(true),

	/**
	 * Evénement dont l'effet sur Unireg a été nul car les données étaient déjà dans l'état voulu. (Des modifications en base ou d'autres
	 * actions avec impact fiscal étaient nécessaire, mais on ne les a pas faites, car on à trouvé la base dans l'état recherché ou les actions
	 * avaient déjà été accomplies)
	 */
	REDONDANT(true);

	private final boolean isTraite;

	EtatEvenementEntreprise(boolean isTraite) {
		this.isTraite = isTraite;
	}

	public final boolean isTraite() {
		return isTraite;
	}
}