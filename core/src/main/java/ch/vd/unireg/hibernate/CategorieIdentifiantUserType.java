package ch.vd.unireg.hibernate;

import ch.vd.unireg.type.CategorieIdentifiant;

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
