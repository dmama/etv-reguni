package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.type.GenreImpot;

/**
 * Classe de transtypage pour Hibernate : GenreImpot <--> varchar
 * 
 * @author Ludovic BERTIN
 */
public class GenreImpotUserType extends EnumUserType<GenreImpot> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public GenreImpotUserType() {
		super(GenreImpot.class);
	}

}
