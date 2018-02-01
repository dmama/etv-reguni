package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.evenement.externe.TypeQuittance;

@SuppressWarnings({"UnusedDeclaration"})
public class TypeQuittanceUserType extends EnumUserType<TypeQuittance> {

	public TypeQuittanceUserType() {
		super(TypeQuittance.class);
	}

}