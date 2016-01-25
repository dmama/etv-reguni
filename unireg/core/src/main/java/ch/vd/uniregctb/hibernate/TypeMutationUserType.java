package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.rf.TypeMutation;

/**
 * Classe de transtypage pour Hibernate : TypeMutation <--> varchar
 */
public class TypeMutationUserType extends EnumUserType<TypeMutation> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public TypeMutationUserType() {
		super(TypeMutation.class);
	}

}
