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
import ch.vd.unireg.registrefoncier.ImmeubleRF;

@Entity
@DiscriminatorValue(value = "IMMEUBLE_RF")
public class EvenementFiscalImmeuble extends EvenementFiscalRF {

	/**
	 * Différents types d'événements fiscaux autour des fors
	 */
	public enum TypeEvenementFiscalImmeuble {
		CREATION,
		RADIATION,
		REACTIVATION,
		MODIFICATION_EGRID,
		MODIFICATION_SITUATION,
		MODIFICATION_SURFACE_TOTALE,
		MODIFICATION_SURFACE_AU_SOL,
		MODIFICATION_QUOTE_PART,
		DEBUT_ESTIMATION,
		FIN_ESTIMATION,
		MODIFICATION_STATUT_REVISION_ESTIMATION,
		ANNULATION_ESTIMATION
	}

	private ImmeubleRF immeuble;
	private TypeEvenementFiscalImmeuble type;

	public EvenementFiscalImmeuble() {
	}

	public EvenementFiscalImmeuble(@Nullable RegDate dateValeur, @NotNull ImmeubleRF immeuble, @NotNull EvenementFiscalImmeuble.TypeEvenementFiscalImmeuble type) {
		super(dateValeur);
		this.immeuble = immeuble;
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

	@Column(name = "TYPE_EVT_IMMEUBLE", length = LengthConstants.EVTFISCAL_TYPE_EVT_IMMEUBLE)
	@Enumerated(EnumType.STRING)
	public TypeEvenementFiscalImmeuble getType() {
		return type;
	}

	public void setType(TypeEvenementFiscalImmeuble type) {
		this.type = type;
	}
}
