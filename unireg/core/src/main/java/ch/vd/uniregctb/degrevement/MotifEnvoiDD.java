package ch.vd.uniregctb.degrevement;

/**
 * Motif d'envoi du formulaire de démande de dégrèvement.
 */
public enum MotifEnvoiDD {
	/**
	 * Envoi initial
	 */
	ENVOI_INITIAL,
	/**
	 * Délai (de cinq ans) expiré.
	 */
	DELAI_EXPIRE,
	/**
	 * Nouveau propriétaire.
	 */
	NOUVEAU_PROPRIETAIRE,
	/**
	 * Nouveau membre de consortium.
	 */
	NOUVEAU_MEMBRE_CONSORTIUM,
	/**
	 * Nouvelle estimation fiscale.
	 */
	NOUVELLE_ESTIMATION_FISCALE,
	/**
	 * Changement part de copropriété.
	 */
	CHANGEMENT_PART_COPROPRIETE,
	/**
	 * Changement de part de membre de consortium.
	 */
	CHANGEMENT_PART_MEMBRE_CONSORTIUM,
	/**
	 * Changement du revenu locatif.
	 */
	CHANGEMENT_REVENU_LOCATIF,
}
