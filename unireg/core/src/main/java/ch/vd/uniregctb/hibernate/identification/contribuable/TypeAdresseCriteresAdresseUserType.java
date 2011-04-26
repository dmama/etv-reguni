package ch.vd.uniregctb.hibernate.identification.contribuable;

import ch.vd.uniregctb.evenement.identification.contribuable.CriteresAdresse;
import ch.vd.uniregctb.hibernate.EnumUserType;

/**
 * Classe de transtypage pour Hibernate : Adresse.TypeAdresse <--> varchar
 */
public class TypeAdresseCriteresAdresseUserType extends EnumUserType<CriteresAdresse.TypeAdresse> {

	public TypeAdresseCriteresAdresseUserType() {
		super(CriteresAdresse.TypeAdresse.class);
	}

}
