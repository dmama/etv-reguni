package ch.vd.uniregctb.evenement.identification.contribuable;

/**
 * Différents types de demande d'identification de contribuable
 */
public enum TypeDemande {

	/**
	 * Demande d'identification de provenance extra-cantonale, Meldewesen
	 */
	MELDEWESEN,

	/**
	 * Certificats de salaire
	 */
	NCS,

	/**
	 * Demande d'identification pour les listes récapitulatives de l'IS
	 */
	IMPOT_SOURCE,

	/**
	 * Demande d'identification liée à la e-Facture (????)
	 */
	E_FACTURE,

	/**
	 * Demande d'identification liée au rapprochement des propriétaires du registre foncier
	 */
	RAPPROCHEMENT_RF
}
