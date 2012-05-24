package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.type.Sexe;

/**
 * Classe de transtypage pour Hibernate : Sexe <--> varchar
 * 
 * @author Ludovic BERTIN
 */
public class SexeUserType extends EnumUserType<Sexe> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public SexeUserType() {
		super(Sexe.class);
	}

}
