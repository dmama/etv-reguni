package ch.vd.uniregctb.registrefoncier;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.ForeignKey;
import org.jetbrains.annotations.Nullable;

@Entity
@DiscriminatorValue("DroitProprietePM")
public class DroitProprietePersonneMoraleRF extends DroitProprieteRF {

	/**
	 * Si renseigné, la communauté à travers laquelle la personne morale possède le droit de propriété.
	 */
	@Nullable
	private CommunauteRF communaute;

	@Nullable
	@ManyToOne
	@JoinColumn(name = "COMMUNAUTE_ID")
	@ForeignKey(name = "FK_DROIT_RF_COMMUNAUTE_ID")
	public CommunauteRF getCommunaute() {
		return communaute;
	}

	public void setCommunaute(@Nullable CommunauteRF communaute) {
		this.communaute = communaute;
	}
}
