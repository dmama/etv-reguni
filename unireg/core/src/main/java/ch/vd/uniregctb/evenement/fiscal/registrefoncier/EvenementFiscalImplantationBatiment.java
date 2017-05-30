package ch.vd.uniregctb.evenement.fiscal.registrefoncier;

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
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.registrefoncier.BatimentRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;

@Entity
@DiscriminatorValue(value = "IMPLANTATION_RF")
public class EvenementFiscalImplantationBatiment extends EvenementFiscalRF {

	/**
	 * Différents types d'événements fiscaux autour des fors
	 */
	public enum TypeEvenementFiscalImplantation {
		CREATION,
		RADIATION
	}

	private ImmeubleRF immeuble;
	private BatimentRF batiment;
	private TypeEvenementFiscalImplantation type;

	public EvenementFiscalImplantationBatiment() {
	}

	public EvenementFiscalImplantationBatiment(@Nullable RegDate dateValeur, @NotNull ImmeubleRF immeuble, @NotNull BatimentRF batiment, @NotNull EvenementFiscalImplantationBatiment.TypeEvenementFiscalImplantation type) {
		super(dateValeur);
		this.immeuble = immeuble;
		this.batiment = batiment;
		this.type = type;
	}

	@JoinColumn(name = "IMMEUBLE_ID")
	@ManyToOne(fetch = FetchType.EAGER)
	@ForeignKey(name = "FK_EVTFISC_IMMEUBLE_ID")
	public ImmeubleRF getImmeuble() {
		return immeuble;
	}

	public void setImmeuble(ImmeubleRF immeuble) {
		this.immeuble = immeuble;
	}

	@JoinColumn(name = "BATIMENT_ID")
	@ManyToOne(fetch = FetchType.EAGER)
	@ForeignKey(name = "FK_EVTFISC_BATIMENT_ID")
	public BatimentRF getBatiment() {
		return batiment;
	}

	public void setBatiment(BatimentRF batiment) {
		this.batiment = batiment;
	}

	@Column(name = "TYPE_EVT_IMPLANTATION", length = LengthConstants.EVTFISCAL_TYPE_EVT_IMPLANTATION)
	@Enumerated(EnumType.STRING)
	public TypeEvenementFiscalImplantation getType() {
		return type;
	}

	public void setType(TypeEvenementFiscalImplantation type) {
		this.type = type;
	}
}
