package ch.vd.uniregctb.webservices.tiers.params;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.webservices.tiers.TiersPart;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GetTiersPeriode", propOrder = {
		"login", "tiersNumber", "periode", "parts"
})
public class GetTiersPeriode {

	/** Les informations de login de l'utilisateur de l'application */
	@XmlElement(required = true)
	public UserLogin login;

	@XmlElement(required = true)
	public long tiersNumber;
	@XmlElement(required = true)
	public int periode;
	@XmlElement(required = false)
	public Set<TiersPart> parts;

	public GetTiersPeriode() {
	}

	public GetTiersPeriode(UserLogin login, long tiersNumber, int periode, Set<TiersPart> parts) {
		this.login = login;
		this.tiersNumber = tiersNumber;
		this.periode = periode;
		this.parts = parts;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((parts == null) ? 0 : parts.hashCode());
		result = prime * result + periode;
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
		GetTiersPeriode other = (GetTiersPeriode) obj;
		if (parts == null) {
			if (other.parts != null)
				return false;
		}
		else if (!parts.equals(other.parts))
			return false;
		if (periode != other.periode)
			return false;
		return tiersNumber == other.tiersNumber;
	}

	public GetTiersPeriode clone(Set<TiersPart> newParts) {
		return new GetTiersPeriode(login, tiersNumber, periode, newParts);
	}
}
