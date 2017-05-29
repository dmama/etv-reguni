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
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.registrefoncier.TiersRF;
import ch.vd.uniregctb.tiers.Tiers;

@Entity
@DiscriminatorValue(value = "RAPPROCHEMENT_RF")
public class EvenementFiscalRapprochementTiersRF extends EvenementFiscalTiers {

	/**
	 * Différents types d'événements fiscaux autour des rapprochements RF
	 */
	public enum TypeEvenementFiscalRapprochement {
		OUVERTURE,
		FERMETURE,
		ANNULATION
	}

	private TiersRF tiersRF;
	private TypeEvenementFiscalRapprochement type;

	public EvenementFiscalRapprochementTiersRF() {
	}

	public EvenementFiscalRapprochementTiersRF(@NotNull RegDate dateValeur, @NotNull Tiers tiers, @NotNull TiersRF tiersRF, @NotNull TypeEvenementFiscalRapprochement type) {
		super(tiers, dateValeur);
		this.tiersRF = tiersRF;
		this.type = type;
	}

	@JoinColumn(name = "TIERS_RF_ID")
	@ManyToOne(fetch = FetchType.EAGER)
	@ForeignKey(name = "FK_EVTFISC_TIERS_RF_ID")
	public TiersRF getTiersRF() {
		return tiersRF;
	}

	public void setTiersRF(TiersRF tiersRF) {
		this.tiersRF = tiersRF;
	}

	@Column(name = "TYPE_EVT_RAPPROCHEMENT", length = LengthConstants.EVTFISCAL_TYPE_EVT_RAPPROCHEMENT)
	@Enumerated(EnumType.STRING)
	public TypeEvenementFiscalRapprochement getType() {
		return type;
	}

	public void setType(TypeEvenementFiscalRapprochement type) {
		this.type = type;
	}
}
