package ch.vd.unireg.hibernate;

import ch.vd.unireg.type.Localisation;

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
