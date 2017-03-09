package ch.vd.uniregctb.hibernate.meta;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.hibernate.RegDateUserType;

public class RegDatePropertyType extends UserTypePropertyType {
	public RegDatePropertyType(RegDateUserType regDateUserType) {
		super(RegDate.class, regDateUserType);
	}
}
