package ch.vd.unireg.interfaces.civil.data;

/**
 * L'état civil à connotation <i>fiscale</i> d'une personne physique. Par rapport à l'état civil officiel, cet état civil possède les états <i>séparé</i> et <i>pacs interrompu</i> en plus.
 */
public enum TypeEtatCivil {

	CELIBATAIRE,
	DIVORCE,
	MARIE,
	PACS,
	/**
	 * Etat civil équivalent à l'état {@link #DIVORCE} mais pour les pacs.
	 */
	PACS_TERMINE,
	/**
	 * Etat civil équivalent à l'état {@link #VEUF} mais pour les pacs.
	 */
	PACS_VEUF,
	/**
	 * Etat civil équivalent à l'état {@link #SEPARE} mais pour les pacs.
	 */
	PACS_SEPARE,
	SEPARE,
	VEUF,
	NON_MARIE;
}
