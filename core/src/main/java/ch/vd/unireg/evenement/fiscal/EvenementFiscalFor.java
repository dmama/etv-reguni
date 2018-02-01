package ch.vd.unireg.evenement.fiscal;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.ForeignKey;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.tiers.ForFiscal;

@Entity
@DiscriminatorValue(value = "FOR")
public class EvenementFiscalFor extends EvenementFiscalTiers {

	/**
	 * Différents types d'événements fiscaux autour des fors
	 */
	public enum TypeEvenementFiscalFor {
		OUVERTURE,
		FERMETURE,
		ANNULATION,
		CHGT_MODE_IMPOSITION
	}

	private ForFiscal forFiscal;
	private TypeEvenementFiscalFor type;

	public EvenementFiscalFor() {
	}

	public EvenementFiscalFor(RegDate dateValeur, ForFiscal forFiscal, TypeEvenementFiscalFor type) {
		super(forFiscal.getTiers(), dateValeur);
		this.forFiscal = forFiscal;
		this.type = type;
	}

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "FOR_FISCAL_ID")
	@ForeignKey(name = "FK_EVTFISC_FOR_ID")
	public ForFiscal getForFiscal() {
		return forFiscal;
	}

	public void setForFiscal(ForFiscal forFiscal) {
		this.forFiscal = forFiscal;
	}

	@Column(name = "TYPE_EVT_FOR", length = LengthConstants.EVTFISCAL_TYPE_EVT_FOR)
	@Enumerated(EnumType.STRING)
	public TypeEvenementFiscalFor getType() {
		return type;
	}

	public void setType(TypeEvenementFiscalFor type) {
		this.type = type;
	}
}
