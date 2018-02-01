package ch.vd.unireg.hibernate;

import ch.vd.unireg.evenement.identification.contribuable.TypeDemande;

@SuppressWarnings({"UnusedDeclaration"})
public class TypeDemandeUserType extends EnumUserType<TypeDemande> {

	public TypeDemandeUserType() {
		super(TypeDemande.class);
	}

}