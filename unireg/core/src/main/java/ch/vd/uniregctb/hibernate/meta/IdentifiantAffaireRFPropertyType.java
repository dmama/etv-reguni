package ch.vd.uniregctb.hibernate.meta;

import ch.vd.uniregctb.hibernate.IdentifiantAffaireRFUserType;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;

public class IdentifiantAffaireRFPropertyType extends UserTypePropertyType {
	public IdentifiantAffaireRFPropertyType(IdentifiantAffaireRFUserType userType) {
		super(IdentifiantAffaireRF.class, userType);
	}
}
