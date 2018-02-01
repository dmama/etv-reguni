package ch.vd.unireg.hibernate;

import ch.vd.unireg.type.CategorieEtranger;

/**
 * Classe de transtypage pour Hibernate : CategorieEtranger <--> varchar
 * 
 * @author Ludovic BERTIN
 */
public class CategorieEtrangerUserType extends EnumUserType<CategorieEtranger> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public CategorieEtrangerUserType() {
		super(CategorieEtranger.class);
	}

}
