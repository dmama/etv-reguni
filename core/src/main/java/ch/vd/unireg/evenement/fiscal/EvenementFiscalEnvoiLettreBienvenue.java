package ch.vd.unireg.evenement.fiscal;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.tiers.Entreprise;

@Entity
@DiscriminatorValue(value = "LETTRE_BIENVENUE")
public class EvenementFiscalEnvoiLettreBienvenue extends EvenementFiscalTiers {

	public EvenementFiscalEnvoiLettreBienvenue() {
	}

	public EvenementFiscalEnvoiLettreBienvenue(Entreprise entreprise, RegDate dateValeur) {
		super(entreprise, dateValeur);
	}
}
