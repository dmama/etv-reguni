package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.evenement.identification.contribuable.Demande;

/**
 * Classe de transtypage pour Hibernate : Demande.ModeIdentificationType <--> varchar
 */
public class ModeIdentificationTypeUserType extends EnumUserType<Demande.ModeIdentificationType> {

	public ModeIdentificationTypeUserType() {
		super(Demande.ModeIdentificationType.class);
	}

}