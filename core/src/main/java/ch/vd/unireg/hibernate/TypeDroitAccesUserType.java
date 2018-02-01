package ch.vd.unireg.hibernate;

import ch.vd.unireg.type.TypeDroitAcces;

/**
 * Classe de transtypage pour Hibernate : TypeDroitAcces <--> varchar
 */
public class TypeDroitAccesUserType extends EnumUserType<TypeDroitAcces> {

	public TypeDroitAccesUserType() {
		super(TypeDroitAcces.class);
	}
}
