package ch.vd.unireg.evenement.fiscal.registrefoncier;

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
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.registrefoncier.ImplantationRF;

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

	private ImplantationRF implantation;
	private TypeEvenementFiscalImplantation type;

	public EvenementFiscalImplantationBatiment() {
	}

	public EvenementFiscalImplantationBatiment(@Nullable RegDate dateValeur, @NotNull ImplantationRF implantation, @NotNull TypeEvenementFiscalImplantation type) {
		super(dateValeur);
		this.implantation = implantation;
		this.type = type;
	}

	@JoinColumn(name = "IMPLANTATION_ID")
	@ManyToOne(fetch = FetchType.EAGER)
	@ForeignKey(name = "FK_EVTFISC_IMPLANTATION_ID")
	public ImplantationRF getImplantation() {
		return implantation;
	}

	public void setImplantation(ImplantationRF implantation) {
		this.implantation = implantation;
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
