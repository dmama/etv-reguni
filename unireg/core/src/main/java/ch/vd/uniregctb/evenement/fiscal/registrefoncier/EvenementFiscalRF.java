package ch.vd.uniregctb.evenement.fiscal.registrefoncier;

import javax.persistence.Entity;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscal;

/**
 * Classe de base des événements fiscaux qui concernent les données du Registre Foncier. A la différences des événements fiscaux traditionnels ({@link ch.vd.uniregctb.evenement.fiscal.EvenementFiscalTiers}), ces événements ne sont pas forcément
 * rattaché à un tiers.
 */
@Entity
public abstract class EvenementFiscalRF extends EvenementFiscal {

	public EvenementFiscalRF() {
	}

	public EvenementFiscalRF(@Nullable RegDate dateValeur) {
		super(dateValeur);
	}

	@Override
	public String toString() {
		return String.format("%s{id=%d, dateValeur=%s}", getClass().getSimpleName(), getId(), getDateValeur());
	}
}
