package ch.vd.uniregctb.registrefoncier;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;

/**
 * Droit d'habitation sur un immeuble. L'ayant-droit d'un droit habitation est soit une personne morale, soit une personne physique.
 */
@Entity
@DiscriminatorValue("DroitHabitation")
public class DroitHabitationRF extends DroitRF {

	/**
	 * L'identifiant interne du registre foncier du droit.
	 */
	private String idRF;

	/**
	 * L'identifiant métier public du droit.
	 */
	private IdentifiantDroitRF identifiantDroit;

	@Override
	public void setAyantDroit(AyantDroitRF ayantDroit) {
		if (ayantDroit != null && !(ayantDroit instanceof TiersRF)) {
			throw new IllegalArgumentException("Seuls les tiers peuvent avoir un droit d'habitation");
		}
		super.setAyantDroit(ayantDroit);
	}

	@Column(name = "ID_RF", length = 33)
	public String getIdRF() {
		return idRF;
	}

	public void setIdRF(String idRF) {
		this.idRF = idRF;
	}

	@Column(name = "IDENTIFIANT_DROIT")
	@Type(type = "ch.vd.uniregctb.hibernate.IdentifiantDroitRFUserType")
	public IdentifiantDroitRF getIdentifiantDroit() {
		return identifiantDroit;
	}

	public void setIdentifiantDroit(IdentifiantDroitRF identifiantDroit) {
		this.identifiantDroit = identifiantDroit;
	}
}
