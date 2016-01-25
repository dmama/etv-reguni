package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;

import ch.vd.uniregctb.common.Duplicable;
import ch.vd.uniregctb.common.LengthConstants;

@Embeddable
public class MontantMonetaire implements Duplicable<MontantMonetaire> {

	public static final String CHF = "CHF";

	private Long montant;
	private String monnaie;

	@SuppressWarnings("unused")
	public MontantMonetaire() {
	}

	public MontantMonetaire(Long montant, String monnaie) {
		this.montant = montant;
		this.monnaie = monnaie;
	}

	@Column(name = "MONTANT", nullable = false)
	public Long getMontant() {
		return montant;
	}

	public void setMontant(Long montant) {
		this.montant = montant;
	}

	@Column(name = "MONNAIE", length = LengthConstants.MONNAIE_ISO, nullable = false)
	public String getMonnaie() {
		return monnaie;
	}

	public void setMonnaie(String monnaie) {
		this.monnaie = monnaie;
	}

	@Transient
	public boolean isEnFrancsSuisses() {
		return CHF.equals(monnaie);
	}

	@Override
	public MontantMonetaire duplicate() {
		return new MontantMonetaire(montant, monnaie);
	}

	@Override
	public String toString() {
		return String.format("%d %s", montant, monnaie);
	}
}
