package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.type.TypeContribuable;

/**
 * Classe de transtypage pour Hibernate : TypeContribuable <--> varchar
 */
public class TypeContribuableUserType extends EnumUserType<TypeContribuable> {

	public TypeContribuableUserType() {
		super(TypeContribuable.class);
	}
}
