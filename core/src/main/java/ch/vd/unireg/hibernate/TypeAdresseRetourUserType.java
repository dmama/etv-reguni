package ch.vd.unireg.hibernate;

import ch.vd.unireg.type.TypeAdresseRetour;

/**
 * Classe de transtypage pour Hibernate : TypeAdresseRetour <--> varchar
 */
public class TypeAdresseRetourUserType extends EnumUserType<TypeAdresseRetour> {

	public TypeAdresseRetourUserType() {
		super(TypeAdresseRetour.class);
	}
}