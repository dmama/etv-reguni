package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.type.Qualification;
import ch.vd.uniregctb.type.TypeAdresseRetour;

/**
 * Classe de transtypage pour Hibernate : TypeAdresseRetour <--> varchar
 */
public class TypeAdresseRetourUserType extends EnumUserType<TypeAdresseRetour> {

	public TypeAdresseRetourUserType() {
		super(TypeAdresseRetour.class);
	}
}