package ch.vd.unireg.common;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.tiers.MontantMonetaire;

public class MontantMonetaireView implements Serializable {

	private static final long serialVersionUID = -6815735676123353600L;

	private final long montant;
	private final String monnaie;

	public MontantMonetaireView(long montant, String monnaie) {
		this.montant = montant;
		this.monnaie = monnaie;
		if (StringUtils.isBlank(monnaie)) {
			throw new IllegalArgumentException("La monnaie doit être indiquée!");
		}
	}

	public MontantMonetaireView(MontantMonetaire source) {
		this(source.getMontant(), source.getMonnaie());
	}

	public long getMontant() {
		return montant;
	}

	@NotNull
	public String getMonnaie() {
		return monnaie;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final MontantMonetaireView that = (MontantMonetaireView) o;
		return montant == that.montant && monnaie.equals(that.monnaie);
	}

	@Override
	public int hashCode() {
		int result = (int) (montant ^ (montant >>> 32));
		result = 31 * result + monnaie.hashCode();
		return result;
	}
}
