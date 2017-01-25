package ch.vd.uniregctb.registrefoncier;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.jetbrains.annotations.Nullable;

@Entity
public abstract class DroitProprietePersonneRF extends DroitProprieteRF {

	/**
	 * Si renseigné, la communauté à travers laquelle la personne possède le droit de propriété.
	 */
	@Nullable
	private CommunauteRF communaute;

	@Nullable
	@ManyToOne
	@JoinColumn(name = "COMMUNAUTE_ID")
	@ForeignKey(name = "FK_DROIT_RF_COMMUNAUTE_ID")
	@Index(name = "IDX_DROIT_RF_COMM_ID")
	public CommunauteRF getCommunaute() {
		return communaute;
	}

	public void setCommunaute(@Nullable CommunauteRF communaute) {
		this.communaute = communaute;
	}
}
