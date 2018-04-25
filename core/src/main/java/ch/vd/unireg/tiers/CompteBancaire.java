package ch.vd.unireg.tiers;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import ch.vd.unireg.common.LengthConstants;

@Embeddable
public class CompteBancaire {

	/**
	 * Numéro de compte bancaire ou du compte postal au format international IBAN (longueur maximum 21 pour les comptes suisses)
	 */
	private String iban;
	private String bicSwift;

	public CompteBancaire() {
	}

	public CompteBancaire(CompteBancaire src) {
		this(src.iban, src.bicSwift);
	}

	public CompteBancaire(String iban, String bicSwift) {
		this.iban = iban;
		this.bicSwift = bicSwift;
	}

	@Column(name = "IBAN", length = LengthConstants.TIERS_NUMCOMPTE)
	public String getIban() {
		return iban;
	}

	public void setIban(String iban) {
		this.iban = iban;
	}

	@Column(name = "BIC_SWIFT", length = LengthConstants.TIERS_ADRESSEBICSWIFT)
	public String getBicSwift() {
		return bicSwift;
	}

	public void setBicSwift(String bicSwift) {
		this.bicSwift = bicSwift;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final CompteBancaire that = (CompteBancaire) o;

		if (iban != null ? !iban.equals(that.iban) : that.iban != null) return false;
		return !(bicSwift != null ? !bicSwift.equals(that.bicSwift) : that.bicSwift != null);
	}

	@Override
	public int hashCode() {
		int result = iban != null ? iban.hashCode() : 0;
		result = 31 * result + (bicSwift != null ? bicSwift.hashCode() : 0);
		return result;
	}
}
