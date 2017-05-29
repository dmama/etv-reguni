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
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.PersonnePhysique;

@Entity
@DiscriminatorValue(value = "PARENTE")
public class EvenementFiscalParente extends EvenementFiscalTiers {

	public enum TypeEvenementFiscalParente {
		NAISSANCE,
		FIN_AUTORITE_PARENTALE
	}

	private PersonnePhysique enfant;
	private TypeEvenementFiscalParente type;

	public EvenementFiscalParente() {
	}

	public EvenementFiscalParente(ContribuableImpositionPersonnesPhysiques tiers, RegDate dateValeur, PersonnePhysique enfant, TypeEvenementFiscalParente type) {
		super(tiers, dateValeur);
		this.enfant = enfant;
		this.type = type;
	}

	@ManyToOne
	@JoinColumn(name = "PARENTE_ENFANT_ID")
	@ForeignKey(name = "FK_EVTFISC_ENFANT_ID")
	public PersonnePhysique getEnfant() {
		return enfant;
	}

	public void setEnfant(PersonnePhysique enfant) {
		this.enfant = enfant;
	}

	@Column(name = "TYPE_EVT_PARENTE", length = LengthConstants.EVTFISCAL_TYPE_EVT_PARENTE)
	@Enumerated(EnumType.STRING)
	public TypeEvenementFiscalParente getType() {
		return type;
	}

	public void setType(TypeEvenementFiscalParente type) {
		this.type = type;
	}
}
