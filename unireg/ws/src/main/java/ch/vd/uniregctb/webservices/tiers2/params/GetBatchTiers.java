package ch.vd.uniregctb.webservices.tiers2.params;

import java.util.Arrays;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.webservices.tiers2.data.Date;
import ch.vd.uniregctb.webservices.tiers2.data.TiersPart;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GetBatchTiers", propOrder = {
		"login", "tiersNumbers", "date", "parts"
})
public class GetBatchTiers {

	/** Les informations de login de l'utilisateur de l'application */
	@XmlElement(required = true)
	public UserLogin login;

    /** Les ids des tiers à retourner */
	@XmlElement(required = true)
	public Set<Long> tiersNumbers;
	@XmlElement(required = true)
	public Date date;
	@XmlElement(required = false)
	public Set<TiersPart> parts;

	public GetBatchTiers() {
	}

	public GetBatchTiers(UserLogin login, Set<Long> tiersNumbers, Date date, Set<TiersPart> parts) {
		this.login = login;
		this.tiersNumbers = tiersNumbers;
		this.date = date;
		this.parts = parts;
	}

	@Override
	public String toString() {
		return "GetBatchTiers{" +
				"login=" + login +
				", tiersNumbers=" + (tiersNumbers == null ? "null" : Arrays.toString(tiersNumbers.toArray())) +
				", date=" + date +
				", parts=" + (parts == null ? "null" : Arrays.toString(parts.toArray())) +
				'}';
	}
}
