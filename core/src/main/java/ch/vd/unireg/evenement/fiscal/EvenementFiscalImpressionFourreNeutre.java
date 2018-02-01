package ch.vd.unireg.evenement.fiscal;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.tiers.Tiers;

@Entity
@DiscriminatorValue(value = "IMPRESSION_FOURRE_NEUTRE")
public class EvenementFiscalImpressionFourreNeutre extends EvenementFiscalTiers {
	private Integer periodeFiscale;
	public EvenementFiscalImpressionFourreNeutre() {
	}

	public EvenementFiscalImpressionFourreNeutre(Tiers tiers, Integer periodeFiscale, RegDate dateValeur) {
		super(tiers, dateValeur);
		this.periodeFiscale = periodeFiscale;
	}

	@Column(name = "PERIODE_FISCALE")
	public Integer getPeriodeFiscale() {
		return periodeFiscale;
	}

	public void setPeriodeFiscale(Integer periodeFiscale) {
		this.periodeFiscale = periodeFiscale;
	}
}
