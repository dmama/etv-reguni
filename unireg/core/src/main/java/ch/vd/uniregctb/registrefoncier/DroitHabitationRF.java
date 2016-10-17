package ch.vd.uniregctb.registrefoncier;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;

/**
 * Droit d'habitation sur un immeuble.
 */
@Entity
@DiscriminatorValue("DROIT_HABITATION")
public class DroitHabitationRF extends DroitRF {

	/**
	 * L'identifiant interne du registre foncier du droit.
	 */
	private String idRF;

	/**
	 * L'identifiant métier public du droit.
	 */
	private IdentifiantDroitRF identifiantDroit;

	@Column(name = "ID_RF")
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
