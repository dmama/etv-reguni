package ch.vd.unireg.hibernate.meta;

import ch.vd.unireg.hibernate.IdentifiantAffaireRFUserType;

public class IdentifiantAffaireRFPropertyType extends UserTypePropertyType {
	public IdentifiantAffaireRFPropertyType(IdentifiantAffaireRFUserType userType) {
		super(userType, String.class);
	}
}
