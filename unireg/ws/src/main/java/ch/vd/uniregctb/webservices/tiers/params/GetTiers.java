package ch.vd.uniregctb.webservices.tiers.params;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.webservices.tiers.Date;
import ch.vd.uniregctb.webservices.tiers.TiersPart;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GetTiers", propOrder = {
		"login", "tiersNumber", "date", "parts"
})
public class GetTiers {

	/** Les informations de login de l'utilisateur de l'application */
	@XmlElement(required = true)
	public UserLogin login;

	@XmlElement(required = true)
	public long tiersNumber;
	@XmlElement(required = true)
	public Date date;
	@XmlElement(required = false)
	public Set<TiersPart> parts;

	public GetTiers() {
	}

	public GetTiers(UserLogin login, long tiersNumber, Date date, Set<TiersPart> parts) {
		this.login = login;
		this.tiersNumber = tiersNumber;
		this.date = date;
		this.parts = parts;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((date == null) ? 0 : date.hashCode());
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
		GetTiers other = (GetTiers) obj;
		if (date == null) {
			if (other.date != null)
				return false;
		}
		else if (!date.equals(other.date))
			return false;
		if (parts == null) {
			if (other.parts != null)
				return false;
		}
		else if (!parts.equals(other.parts))
			return false;
		return tiersNumber == other.tiersNumber;
	}

	public GetTiers clone(Set<TiersPart> newParts) {
		return new GetTiers(login, tiersNumber, date, newParts);
	}
}
