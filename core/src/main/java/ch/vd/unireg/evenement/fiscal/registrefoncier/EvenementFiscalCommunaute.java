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
import ch.vd.unireg.registrefoncier.CommunauteRF;

@Entity
@DiscriminatorValue(value = "COMMUNAUTE_RF")
public class EvenementFiscalCommunaute extends EvenementFiscalRF {

	/**
	 * Différents types d'événements fiscaux autour des communautés RF
	 */
	public enum TypeEvenementFiscalCommunaute {
		/**
		 * Le principal de la communauté à changé.
		 */
		CHANGEMENT_PRINCIPAL,
		/**
		 * Un tiers est décédé et a été substitués par ses héritiers dans la communauté.
		 */
		HERITAGE
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

	@JoinColumn(name = "COMMUNAUTE_ID", foreignKey = @ForeignKey(name = "FK_EVTFISC_COMMUNAUTE_ID"))
	@ManyToOne(fetch = FetchType.EAGER)
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
