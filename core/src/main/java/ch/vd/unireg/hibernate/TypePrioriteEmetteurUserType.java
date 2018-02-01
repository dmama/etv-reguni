package ch.vd.unireg.hibernate;

import ch.vd.unireg.evenement.identification.contribuable.Demande;

/**
 * Classe de transtypage pour Hibernate : Demande.PrioriteEmetteur <--> varchar
 */
public class TypePrioriteEmetteurUserType extends EnumUserType<Demande.PrioriteEmetteur> {

	public TypePrioriteEmetteurUserType() {
		super(Demande.PrioriteEmetteur.class);
	}

}
