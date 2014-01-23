package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.rf.GenrePropriete;

/**
 * Classe de transtypage pour Hibernate : GenrePropriete <--> varchar
 */
public class GenreProprieteUserType extends EnumUserType<GenrePropriete> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public GenreProprieteUserType() {
		super(GenrePropriete.class);
	}

}
