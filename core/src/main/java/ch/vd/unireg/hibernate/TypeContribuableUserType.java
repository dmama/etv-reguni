package ch.vd.unireg.hibernate;

import ch.vd.unireg.type.TypeContribuable;

/**
 * Classe de transtypage pour Hibernate : TypeContribuable <--> varchar
 */
public class TypeContribuableUserType extends EnumUserType<TypeContribuable> {

	public TypeContribuableUserType() {
		super(TypeContribuable.class);
	}
}
