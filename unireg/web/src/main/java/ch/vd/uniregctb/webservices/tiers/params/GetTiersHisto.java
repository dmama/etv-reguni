package ch.vd.uniregctb.webservices.tiers.params;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.webservices.tiers.TiersPart;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GetTiersHisto", propOrder = {
		"login", "tiersNumber", "parts"
})
public class GetTiersHisto {

	/** Les informations de login de l'utilisateur de l'application */
	@XmlElement(required = true)
	public UserLogin login;

	@XmlElement(required = true)
	public long tiersNumber;
	@XmlElement(required = false)
	public Set<TiersPart> parts;

	public GetTiersHisto() {
	}

	public GetTiersHisto(UserLogin login, long tiersNumber, Set<TiersPart> parts) {
		this.login = login;
		this.tiersNumber = tiersNumber;
		this.parts = parts;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((parts == null) ? 0 : parts.hashCode());
		result = prime * result + (int) (tiersNumber ^ (tiersNumber >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GetTiersHisto other = (GetTiersHisto) obj;
		if (parts == null) {
			if (other.parts != null)
				return false;
		}
		else if (!parts.equals(other.parts))
			return false;
		if (tiersNumber != other.tiersNumber)
			return false;
		return true;
	}

	public GetTiersHisto clone(Set<TiersPart> newParts) {
		return new GetTiersHisto(login, tiersNumber, newParts);
	}
}
