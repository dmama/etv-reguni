package ch.vd.uniregctb.webservices.tiers2.params;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import ch.vd.uniregctb.webservices.common.UserLogin;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GetTiersType")
public class GetTiersType {

	/** Les informations de login de l'utilisateur de l'application */
	@XmlElement(required = true)
	public UserLogin login;

	@XmlElement(required = true)
	public long tiersNumber;

	@Override
	public String toString() {
		return "GetTiersType{" +
				"login=" + login +
				", tiersNumber=" + tiersNumber +
				'}';
	}
}