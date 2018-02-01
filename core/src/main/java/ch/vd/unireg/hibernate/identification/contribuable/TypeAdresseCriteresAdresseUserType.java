package ch.vd.unireg.hibernate.identification.contribuable;

import ch.vd.unireg.evenement.identification.contribuable.CriteresAdresse;
import ch.vd.unireg.hibernate.EnumUserType;

/**
 * Classe de transtypage pour Hibernate : Adresse.TypeAdresse <--> varchar
 */
public class TypeAdresseCriteresAdresseUserType extends EnumUserType<CriteresAdresse.TypeAdresse> {

	public TypeAdresseCriteresAdresseUserType() {
		super(CriteresAdresse.TypeAdresse.class);
	}

}
