package ch.vd.uniregctb.webservices.v7;

public enum PartySearchType {

	/**
	 * Une personne physique.
	 */
	NATURAL_PERSON,

	/**
	 * Une personne physique résidente
	 */
	RESIDENT_NATURAL_PERSON,

	/**
	 * Une personne physique non-résidente
	 */
	NON_RESIDENT_NATURAL_PERSON,

	/**
	 * Une ménage-commun (couple).
	 */
	HOUSEHOLD,

	/**
	 * Un débiteur de prestations imposables.
	 */
	DEBTOR,

	/**
	 * Une personne morale.
	 */
	CORPORATION,

	/**
	 * Un établissement.
	 */
	ESTABLISHMENT,

	/**
	 * Une collectivité administrative.
	 */
	ADMINISTRATIVE_AUTHORITY,

	/**
	 * Une "autre communauté", i.e. une personne morale inconnue au registre des PM.
	 */
	OTHER_COMMUNITY

}
