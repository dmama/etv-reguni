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
import ch.vd.unireg.registrefoncier.DroitProprieteRF;

@Entity
@DiscriminatorValue(value = "DROIT_PROPRIETE_RF")
public class EvenementFiscalDroitPropriete extends EvenementFiscalDroit {

	private DroitProprieteRF droit;

	public EvenementFiscalDroitPropriete() {
	}

	public EvenementFiscalDroitPropriete(@Nullable RegDate dateValeur, @NotNull DroitProprieteRF droit, TypeEvenementFiscalDroitPropriete type) {
		super(dateValeur, type);
		this.droit = droit;
	}

	@JoinColumn(name = "DROIT_PROP_ID")
	@ManyToOne(fetch = FetchType.EAGER)
	@ForeignKey(name = "FK_EVTFISC_DROIT_PROP_ID")
	public DroitProprieteRF getDroit() {
		return droit;
	}

	public void setDroit(DroitProprieteRF droit) {
		this.droit = droit;
	}
}
