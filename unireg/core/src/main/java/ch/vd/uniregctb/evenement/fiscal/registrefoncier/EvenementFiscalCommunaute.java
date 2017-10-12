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
import ch.vd.uniregctb.registrefoncier.CommunauteRF;

@Entity
@DiscriminatorValue(value = "COMMUNAUTE_RF")
public class EvenementFiscalCommunaute extends EvenementFiscalRF {

	/**
	 * Différents types d'événements fiscaux autour des fors
	 */
	public enum TypeEvenementFiscalCommunaute {
		CHANGEMENT_PRINCIPAL
	}

	private CommunauteRF communaute;
	private TypeEvenementFiscalCommunaute type;

	public EvenementFiscalCommunaute() {
	}

	public EvenementFiscalCommunaute(@Nullable RegDate dateValeur, @NotNull CommunauteRF communaute, @NotNull TypeEvenementFiscalCommunaute type) {
		super(dateValeur);
		this.communaute = communaute;
		this.type = type;
	}

	@JoinColumn(name = "COMMUNAUTE_ID")
	@ManyToOne(fetch = FetchType.EAGER)
	@ForeignKey(name = "FK_EVTFISC_COMMUNAUTE_ID")
	public CommunauteRF getCommunaute() {
		return communaute;
	}

	public void setCommunaute(CommunauteRF communaute) {
		this.communaute = communaute;
	}

	@Column(name = "TYPE_EVT_COMMUNAUTE", length = LengthConstants.EVTFISCAL_TYPE_EVT_COMMUNAUTE)
	@Enumerated(EnumType.STRING)
	public TypeEvenementFiscalCommunaute getType() {
		return type;
	}

	public void setType(TypeEvenementFiscalCommunaute type) {
		this.type = type;
	}
}