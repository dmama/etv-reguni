package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import ch.vd.uniregctb.common.LengthConstants;

@Embeddable
public class CoordonneesFinancieres {

	private String iban;
	private String bicSwift;

	public CoordonneesFinancieres() {
	}

	public CoordonneesFinancieres(CoordonneesFinancieres src) {
		this(src.iban, src.bicSwift);
	}

	public CoordonneesFinancieres(String iban, String bicSwift) {
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

		final CoordonneesFinancieres that = (CoordonneesFinancieres) o;

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
