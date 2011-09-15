package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.rf.NatureImmeuble;

/**
 * Classe de transtypage pour Hibernate : NatureImmeuble <--> varchar
 */
public class NatureImmeubleUserType extends EnumUserType<NatureImmeuble> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public NatureImmeubleUserType() {
		super(NatureImmeuble.class);
	}

}
