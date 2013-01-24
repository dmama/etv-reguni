package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.type.CategorieIdentifiant;

/**
 * Classe de transtypage pour Hibernate : CategorieIdentifiant <--> varchar
 */
public class CategorieIdentifiantUserType extends EnumUserType<CategorieIdentifiant> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public CategorieIdentifiantUserType() {
		super(CategorieIdentifiant.class);
	}

}
