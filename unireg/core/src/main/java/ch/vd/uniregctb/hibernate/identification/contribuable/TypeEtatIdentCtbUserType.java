package ch.vd.uniregctb.hibernate.identification.contribuable;

import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.uniregctb.hibernate.EnumUserType;

/**
 * Classe de transtypage pour Hibernate : IdentificationContribuable.Etat <--> varchar
 */
public class TypeEtatIdentCtbUserType extends EnumUserType<IdentificationContribuable.Etat> {

	public TypeEtatIdentCtbUserType() {
		super(IdentificationContribuable.Etat.class);
	}
}
