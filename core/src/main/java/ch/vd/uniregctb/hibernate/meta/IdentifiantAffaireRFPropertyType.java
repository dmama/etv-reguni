package ch.vd.uniregctb.hibernate.meta;

import ch.vd.uniregctb.hibernate.IdentifiantAffaireRFUserType;

public class IdentifiantAffaireRFPropertyType extends UserTypePropertyType {
	public IdentifiantAffaireRFPropertyType(IdentifiantAffaireRFUserType userType) {
		super(userType, String.class);
	}
}
