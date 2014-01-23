package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.rf.TypeImmeuble;

/**
 * Classe de transtypage pour Hibernate : TypeImmeuble <--> varchar
 */
public class TypeImmeubleUserType extends EnumUserType<TypeImmeuble> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public TypeImmeubleUserType() {
		super(TypeImmeuble.class);
	}

}
