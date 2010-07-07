package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.type.Localisation;

/**
 * Classe de transtypage pour Hibernate : Localisation <--> varchar
 *
 * @author Ludovic BERTIN
 */
public class LocalisationUserType extends EnumUserType<Localisation> {

	public LocalisationUserType() {
		super(Localisation.class);
	}

}
