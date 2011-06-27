package ch.vd.uniregctb.webservices.tiers2.params;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import ch.vd.uniregctb.webservices.common.UserLogin;

/**
 * <b>Dans la version 3 du web-service :</b> <i>getPartyTypeRequestType</i> (xml) / <i>GetPartyTypeRequest</i> (client java)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GetTiersType")
public class GetTiersType {

	/**
	 * Les informations de login de l'utilisateur de l'application
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>login</i>.
	 */
	@XmlElement(required = true)
	public UserLogin login;

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>partyNumber</i>.
	 */
	@XmlElement(required = true)
	public long tiersNumber;

	@Override
	public String toString() {
		return "GetTiersType{" +
				"login=" + login +
				", tiersNumber=" + tiersNumber +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final GetTiersType that = (GetTiersType) o;

		return tiersNumber == that.tiersNumber && !(login != null ? !login.equals(that.login) : that.login != null);

	}

	@Override
	public int hashCode() {
		int result = login != null ? login.hashCode() : 0;
		result = 31 * result + (int) (tiersNumber ^ (tiersNumber >>> 32));
		return result;
	}
}