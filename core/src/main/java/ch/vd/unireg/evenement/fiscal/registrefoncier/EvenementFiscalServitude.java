package ch.vd.unireg.evenement.fiscal.registrefoncier;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.ForeignKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.registrefoncier.ServitudeRF;

@Entity
@DiscriminatorValue(value = "SERVITUDE_RF")
public class EvenementFiscalServitude extends EvenementFiscalDroit {

	private ServitudeRF servitude;

	public EvenementFiscalServitude() {
	}

	public EvenementFiscalServitude(@Nullable RegDate dateValeur, @NotNull ServitudeRF servitude, TypeEvenementFiscalDroitPropriete type) {
		super(dateValeur, type);
		this.servitude = servitude;
	}

	@JoinColumn(name = "SERVITUDE_ID")
	@ManyToOne(fetch = FetchType.EAGER)
	@ForeignKey(name = "FK_EVTFISC_SERVITUDE_ID")
	public ServitudeRF getServitude() {
		return servitude;
	}

	public void setServitude(ServitudeRF servitude) {
		this.servitude = servitude;
	}
}
