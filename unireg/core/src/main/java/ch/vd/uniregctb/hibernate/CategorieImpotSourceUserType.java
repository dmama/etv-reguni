package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.type.CategorieImpotSource;

/**
 * Classe de transtypage pour Hibernate : CategorieImpotSource <--> varchar
 * 
 * @author Ludovic BERTIN
 */
public class CategorieImpotSourceUserType extends EnumUserType<CategorieImpotSource> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public CategorieImpotSourceUserType() {
		super(CategorieImpotSource.class);
	}

}
