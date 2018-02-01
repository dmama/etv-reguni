package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.type.TypeDroitAcces;

/**
 * Classe de transtypage pour Hibernate : TypeDroitAcces <--> varchar
 */
public class TypeDroitAccesUserType extends EnumUserType<TypeDroitAcces> {

	public TypeDroitAccesUserType() {
		super(TypeDroitAcces.class);
	}
}
