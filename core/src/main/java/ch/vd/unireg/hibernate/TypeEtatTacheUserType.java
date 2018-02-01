package ch.vd.unireg.hibernate;

import ch.vd.unireg.type.TypeEtatTache;

/**
 * Classe de transtypage pour Hibernate : TypeEtatTache <--> varchar
 */
public class TypeEtatTacheUserType extends EnumUserType<TypeEtatTache> {

	public TypeEtatTacheUserType() {
		super(TypeEtatTache.class);
	}
}
