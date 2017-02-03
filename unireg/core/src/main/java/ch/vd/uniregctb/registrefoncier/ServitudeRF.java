package ch.vd.uniregctb.registrefoncier;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.common.LengthConstants;

/**
 * Servitude sur un immeuble. L'ayant-droit d'un droit habitation est soit une personne morale, soit une personne physique.
 */
@Entity
public abstract class ServitudeRF extends DroitRF {

	/**
	 * Si renseigné, la communauté à travers laquelle la personne possède le droit de propriété.
	 */
	@Nullable
	private CommunauteRF communaute;

	/**
	 * L'identifiant métier public du droit.
	 */
	private IdentifiantDroitRF identifiantDroit;

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

	@Override
	public void setAyantDroit(AyantDroitRF ayantDroit) {
		if (ayantDroit != null && !(ayantDroit instanceof TiersRF)) {
			throw new IllegalArgumentException("Seuls les tiers peuvent avoir un droit d'habitation");
		}
		super.setAyantDroit(ayantDroit);
	}

	@Column(name = "IDENTIFIANT_DROIT", length = LengthConstants.RF_IDENTIFIANT_DROIT)
	@Type(type = "ch.vd.uniregctb.hibernate.IdentifiantDroitRFUserType")
	public IdentifiantDroitRF getIdentifiantDroit() {
		return identifiantDroit;
	}

	public void setIdentifiantDroit(IdentifiantDroitRF identifiantDroit) {
		this.identifiantDroit = identifiantDroit;
	}

	@NotNull
	@Override
	@Transient
	public TypeDroit getTypeDroit() {
		return TypeDroit.SERVITUDE;
	}
}
