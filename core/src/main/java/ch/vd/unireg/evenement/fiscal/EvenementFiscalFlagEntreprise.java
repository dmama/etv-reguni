package ch.vd.unireg.evenement.fiscal;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.tiers.FlagEntreprise;

@Entity
@DiscriminatorValue(value = "FLAG_ENTREPRISE")
public class EvenementFiscalFlagEntreprise extends EvenementFiscalTiers {

	/**
	 * Différents types d'événements fiscaux autour des allègements
	 */
	public enum TypeEvenementFiscalFlagEntreprise {
		OUVERTURE,
		FERMETURE,
		ANNULATION
	}

	private FlagEntreprise flag;
	private TypeEvenementFiscalFlagEntreprise type;

	public EvenementFiscalFlagEntreprise() {
	}

	public EvenementFiscalFlagEntreprise(RegDate dateValeur, FlagEntreprise flag, TypeEvenementFiscalFlagEntreprise type) {
		super(flag.getEntreprise(), dateValeur);
		this.flag = flag;
		this.type = type;
	}

	@ManyToOne
	@JoinColumn(name = "FLAG_ENTREPRISE_ID", foreignKey = @ForeignKey(name = "FK_EVTFISC_FLAG_ID"))
	public FlagEntreprise getFlag() {
		return flag;
	}

	public void setFlag(FlagEntreprise flag) {
		this.flag = flag;
	}

	@Column(name = "TYPE_EVT_FLAG", length = LengthConstants.EVTFISCAL_TYPE_EVT_FLAG)
	@Enumerated(EnumType.STRING)
	public TypeEvenementFiscalFlagEntreprise getType() {
		return type;
	}

	public void setType(TypeEvenementFiscalFlagEntreprise type) {
		this.type = type;
	}
}
