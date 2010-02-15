package ch.vd.uniregctb.webservices.tiers2.params;

import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.webservices.tiers2.data.Date;
import ch.vd.uniregctb.webservices.tiers2.data.TiersPart;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Arrays;
import java.util.Set;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GetBatchTiersHisto", propOrder = {
		"login", "tiersNumbers", "parts"
})
public class GetBatchTiersHisto {

	/** Les informations de login de l'utilisateur de l'application */
	@XmlElement(required = true)
	public UserLogin login;

    /** Les ids des tiers à retourner */
	@XmlElement(required = true)
	public Set<Long> tiersNumbers;
	
	@XmlElement(required = false)
	public Set<TiersPart> parts;

	public GetBatchTiersHisto() {
	}

	public GetBatchTiersHisto(UserLogin login, Set<Long> tiersNumbers, Set<TiersPart> parts) {
		this.login = login;
		this.tiersNumbers = tiersNumbers;
		this.parts = parts;
	}

	@Override
	public String toString() {
		return "GetBatchTiersHisto{" +
				"login=" + login +
				", tiersNumbers=" + Arrays.toString(tiersNumbers.toArray()) +
				", parts=" + Arrays.toString(parts.toArray()) +
				'}';
	}
}