package ch.vd.uniregctb.evenement.fiscal;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.ForeignKey;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Tiers;

/**
 * Classe de base des événements fiscaux rattachés à un tiers Unireg.
 */
@Entity
public abstract class EvenementFiscalTiers extends EvenementFiscal {

	private Tiers tiers;

	public EvenementFiscalTiers() {
	}

	public EvenementFiscalTiers(Tiers tiers, RegDate dateValeur) {
		super(dateValeur);
		this.tiers = tiers;
	}

	@JoinColumn(name = "TIERS_ID")
	@ManyToOne(fetch = FetchType.EAGER)
	@ForeignKey(name = "FK_EVTFISC_TIERS_ID")
	public Tiers getTiers() {
		return tiers;
	}

	public void setTiers(Tiers tiers) {
		this.tiers = tiers;
	}

	@Override
	public String toString() {
		return String.format("%s{id=%d, dateValeur=%s, tiers=%d}", getClass().getSimpleName(), getId(), getDateValeur(), tiers.getNumero());
	}
}
