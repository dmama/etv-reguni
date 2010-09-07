package ch.vd.uniregctb.webservices.tiers2.params;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import ch.vd.uniregctb.webservices.common.UserLogin;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GetDebiteurInfo")
public class GetDebiteurInfo {

	/** Les informations de login de l'utilisateur de l'application */
	@XmlElement(required = true)
	public UserLogin login;
	
	/**
	 * Le numéro de tiers du débiteur de prestations imposables.
	 */
	@XmlElement(required = true)
	public long numeroDebiteur;

	/**
	 * La période fiscale considérée (1er janvier au 31 décembre).
	 */
	@XmlElement(required = true)
	public int periodeFiscale;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final GetDebiteurInfo that = (GetDebiteurInfo) o;

		return numeroDebiteur == that.numeroDebiteur && periodeFiscale == that.periodeFiscale;

	}

	@Override
	public int hashCode() {
		int result = (int) (numeroDebiteur ^ (numeroDebiteur >>> 32));
		result = 31 * result + periodeFiscale;
		return result;
	}

	@Override
	public String toString() {
		return "GetDebiteurInfo{" +
				"login=" + login +
				", numeroDebiteur=" + numeroDebiteur +
				", periodeFiscale=" + periodeFiscale +
				'}';
	}
}
