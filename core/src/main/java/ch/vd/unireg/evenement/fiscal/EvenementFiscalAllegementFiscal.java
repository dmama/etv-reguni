package ch.vd.unireg.evenement.fiscal;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.ForeignKey;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.tiers.AllegementFiscal;

@Entity
@DiscriminatorValue(value = "ALLEGEMENT_FISCAL")
public class EvenementFiscalAllegementFiscal extends EvenementFiscalTiers {

	/**
	 * Différents types d'événements fiscaux autour des allègements
	 */
	public enum TypeEvenementFiscalAllegement {
		OUVERTURE,
		FERMETURE,
		ANNULATION
	}

	private AllegementFiscal allegementFiscal;
	private TypeEvenementFiscalAllegement type;

	public EvenementFiscalAllegementFiscal() {
	}

	public EvenementFiscalAllegementFiscal(RegDate dateValeur, AllegementFiscal allegementFiscal, TypeEvenementFiscalAllegement type) {
		super(allegementFiscal.getEntreprise(), dateValeur);
		this.allegementFiscal = allegementFiscal;
		this.type = type;
	}

	@ManyToOne
	@JoinColumn(name = "ALLEGEMENT_ID")
	@ForeignKey(name = "FK_EVTFISC_ALLGMT_ID")
	public AllegementFiscal getAllegementFiscal() {
		return allegementFiscal;
	}

	public void setAllegementFiscal(AllegementFiscal allegementFiscal) {
		this.allegementFiscal = allegementFiscal;
	}

	@Column(name = "TYPE_EVT_ALLEGEMENT", length = LengthConstants.EVTFISCAL_TYPE_EVT_ALLEGEMENT)
	@Enumerated(EnumType.STRING)
	public TypeEvenementFiscalAllegement getType() {
		return type;
	}

	public void setType(TypeEvenementFiscalAllegement type) {
		this.type = type;
	}
}
