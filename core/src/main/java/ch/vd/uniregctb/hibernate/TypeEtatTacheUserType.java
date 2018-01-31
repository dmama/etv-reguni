package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.type.TypeEtatTache;

/**
 * Classe de transtypage pour Hibernate : TypeEtatTache <--> varchar
 */
public class TypeEtatTacheUserType extends EnumUserType<TypeEtatTache> {

	public TypeEtatTacheUserType() {
		super(TypeEtatTache.class);
	}
}
