package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.type.Niveau;

/**
 * Classe de transtypage pour Hibernate : Niveau <--> varchar
 */
public class NiveauUserType extends EnumUserType<Niveau> {

	public NiveauUserType() {
		super(Niveau.class);
	}
}
