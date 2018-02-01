package ch.vd.unireg.tiers;

/**
 * Enumération du niveau d'association avec le registre civil d'une entreprise.
 * @author Raphaël Marmier, 2016-10-12, <raphael.marmier@vd.ch>
 */
public enum DegreAssociationRegistreCivil {
	/*
		Le tiers n'est pas connu au civil, et Unireg possède tous les droits.
	 */
	FISCAL,
	/*
		Le tiers est connu au civil, mais Unireg reste prépondérant pour certains attributs (en tant que service IDE à obligations étendues).
	 */
	CIVIL_MAITRE,
	/*
		Le tiers est connu au civil, et Unireg adopte les données du registre civil (à priori pas d'édition possible).
	 */
	CIVIL_ESCLAVE
}
