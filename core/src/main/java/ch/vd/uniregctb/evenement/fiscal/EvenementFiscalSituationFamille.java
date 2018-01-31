package ch.vd.uniregctb.evenement.fiscal;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;

@Entity
@DiscriminatorValue(value = "SITUATION_FAMILLE")
public class EvenementFiscalSituationFamille extends EvenementFiscalTiers {

	public EvenementFiscalSituationFamille() {
	}

	public EvenementFiscalSituationFamille(RegDate dateValeur, ContribuableImpositionPersonnesPhysiques contribuable) {
		super(contribuable, dateValeur);
	}
}
