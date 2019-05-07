package ch.vd.unireg.evenement.fiscal.registrefoncier;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.registrefoncier.BatimentRF;

@Entity
@DiscriminatorValue(value = "BATIMENT_RF")
public class EvenementFiscalBatiment extends EvenementFiscalRF {

	/**
	 * Différents types d'événements fiscaux autour des fors
	 */
	public enum TypeEvenementFiscalBatiment {
		CREATION,
		RADIATION,
		MODIFICATION_DESCRIPTION
	}

	private BatimentRF batiment;
	private TypeEvenementFiscalBatiment type;

	public EvenementFiscalBatiment() {
	}

	public EvenementFiscalBatiment(@Nullable RegDate dateValeur, @NotNull BatimentRF batiment, @NotNull TypeEvenementFiscalBatiment type) {
		super(dateValeur);
		this.batiment = batiment;
		this.type = type;
	}

	@JoinColumn(name = "BATIMENT_ID", foreignKey = @ForeignKey(name = "FK_EVTFISC_BATIMENT_ID"))
	@ManyToOne(fetch = FetchType.EAGER)
	public BatimentRF getBatiment() {
		return batiment;
	}

	public void setBatiment(BatimentRF batiment) {
		this.batiment = batiment;
	}

	@Column(name = "TYPE_EVT_BATIMENT", length = LengthConstants.EVTFISCAL_TYPE_EVT_BATIMENT)
	@Enumerated(EnumType.STRING)
	public TypeEvenementFiscalBatiment getType() {
		return type;
	}

	public void setType(TypeEvenementFiscalBatiment type) {
		this.type = type;
	}
}
