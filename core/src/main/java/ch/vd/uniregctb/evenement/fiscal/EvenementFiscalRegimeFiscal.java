package ch.vd.uniregctb.evenement.fiscal;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.ForeignKey;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.tiers.RegimeFiscal;

@Entity
@DiscriminatorValue(value = "REGIME_FISCAL")
public class EvenementFiscalRegimeFiscal extends EvenementFiscalTiers {

	/**
	 * Différents types d'événements fiscaux autour des allègements
	 */
	public enum TypeEvenementFiscalRegime {
		OUVERTURE,
		FERMETURE,
		ANNULATION
	}

	private RegimeFiscal regimeFiscal;
	private TypeEvenementFiscalRegime type;

	public EvenementFiscalRegimeFiscal() {
	}

	public EvenementFiscalRegimeFiscal(RegDate dateValeur, RegimeFiscal regimeFiscal, TypeEvenementFiscalRegime type) {
		super(regimeFiscal.getEntreprise(), dateValeur);
		this.regimeFiscal = regimeFiscal;
		this.type = type;
	}

	@ManyToOne
	@JoinColumn(name = "REGIME_FISCAL_ID")
	@ForeignKey(name = "FK_EVTFISC_REGFISC_ID")
	public RegimeFiscal getRegimeFiscal() {
		return regimeFiscal;
	}

	public void setRegimeFiscal(RegimeFiscal regimeFiscal) {
		this.regimeFiscal = regimeFiscal;
	}

	@Column(name = "TYPE_EVT_REGIME", length = LengthConstants.EVTFISCAL_TYPE_EVT_REGIME)
	@Enumerated(EnumType.STRING)
	public TypeEvenementFiscalRegime getType() {
		return type;
	}

	public void setType(TypeEvenementFiscalRegime type) {
		this.type = type;
	}
}
