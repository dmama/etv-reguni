package ch.vd.uniregctb.registrefoncier;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import ch.vd.uniregctb.common.LengthConstants;

/**
 * Une communauté représente un groupement de tiers qui possèdent ensemble un droit sur un immeuble.
 */
@Entity
@DiscriminatorValue("Communaute")
public class CommunauteRF extends AyantDroitRF {

	private TypeCommunaute type;

	@Column(name = "TYPE_COMMUNAUTE", length = LengthConstants.RF_TYPE_COMMUNAUTE)
	@Enumerated(EnumType.STRING)
	public TypeCommunaute getType() {
		return type;
	}

	public void setType(TypeCommunaute type) {
		this.type = type;
	}

	@Override
	public void copyDataTo(AyantDroitRF right) {
		super.copyDataTo(right);
		final CommunauteRF commRight = (CommunauteRF) right;
		commRight.type = this.type;
	}
}
