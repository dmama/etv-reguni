package ch.vd.unireg.hibernate;

import ch.vd.unireg.evenement.externe.TypeQuittance;

@SuppressWarnings({"UnusedDeclaration"})
public class TypeQuittanceUserType extends EnumUserType<TypeQuittance> {

	public TypeQuittanceUserType() {
		super(TypeQuittance.class);
	}

}