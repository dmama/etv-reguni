package ch.vd.unireg.hibernate.identification.contribuable;

import ch.vd.unireg.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.unireg.hibernate.EnumUserType;

/**
 * Classe de transtypage pour Hibernate : IdentificationContribuable.Etat <--> varchar
 */
public class TypeEtatIdentCtbUserType extends EnumUserType<IdentificationContribuable.Etat> {

	public TypeEtatIdentCtbUserType() {
		super(IdentificationContribuable.Etat.class);
	}
}
