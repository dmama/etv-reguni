package ch.vd.unireg.hibernate.meta;

import ch.vd.unireg.hibernate.ActionAutoEtiquetteUserType;

public class ActionAutoEtiquetteUserTypePropertyType extends UserTypePropertyType {
	public ActionAutoEtiquetteUserTypePropertyType(ActionAutoEtiquetteUserType userType) {
		super(userType, String.class);
	}
}
