package ch.vd.unireg.hibernate;

import ch.vd.unireg.type.Niveau;

/**
 * Classe de transtypage pour Hibernate : Niveau <--> varchar
 */
public class NiveauUserType extends EnumUserType<Niveau> {

	public NiveauUserType() {
		super(Niveau.class);
	}
}
