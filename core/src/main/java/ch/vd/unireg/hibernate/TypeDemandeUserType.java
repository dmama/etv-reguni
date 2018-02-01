package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.evenement.identification.contribuable.TypeDemande;

@SuppressWarnings({"UnusedDeclaration"})
public class TypeDemandeUserType extends EnumUserType<TypeDemande> {

	public TypeDemandeUserType() {
		super(TypeDemande.class);
	}

}