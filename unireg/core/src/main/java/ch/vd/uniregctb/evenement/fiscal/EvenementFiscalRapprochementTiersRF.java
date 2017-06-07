package ch.vd.uniregctb.evenement.fiscal;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.ForeignKey;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.registrefoncier.RapprochementRF;

@Entity
@DiscriminatorValue(value = "RAPPROCHEMENT_RF")
public class EvenementFiscalRapprochementTiersRF extends EvenementFiscal {

	/**
	 * Différents types d'événements fiscaux autour des rapprochements RF
	 */
	public enum TypeEvenementFiscalRapprochement {
		OUVERTURE,
		FERMETURE,
		ANNULATION
	}

	private RapprochementRF rapprochement;
	private TypeEvenementFiscalRapprochement type;

	public EvenementFiscalRapprochementTiersRF() {
	}

	public EvenementFiscalRapprochementTiersRF(@Nullable RegDate dateValeur, RapprochementRF rapprochement, TypeEvenementFiscalRapprochement type) {
		super(dateValeur);
		this.rapprochement = rapprochement;
		this.type = type;
	}

	@JoinColumn(name = "RAPPROCHEMENT_ID")
	@ManyToOne(fetch = FetchType.EAGER)
	@ForeignKey(name = "FK_EVTFISC_RAPPR_ID")
	public RapprochementRF getRapprochement() {
		return rapprochement;
	}

	public void setRapprochement(RapprochementRF rapprochement) {
		this.rapprochement = rapprochement;
	}

	@Column(name = "TYPE_EVT_RAPPROCHEMENT", length = LengthConstants.EVTFISCAL_TYPE_EVT_RAPPROCHEMENT)
	@Enumerated(EnumType.STRING)
	public TypeEvenementFiscalRapprochement getType() {
		return type;
	}

	public void setType(TypeEvenementFiscalRapprochement type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return String.format("%s{id=%d, dateValeur=%s, rapprochement=%d}", getClass().getSimpleName(), getId(), getDateValeur(), rapprochement.getId());
	}
}
