package ch.vd.uniregctb.webservices.tiers2.params;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Arrays;
import java.util.Set;

import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.webservices.tiers2.data.TiersPart;

/**
 * <b>Dans la version 3 du web-service :</b> <i>getBatchPartyRequestType</i> (xml) / <i>GetBatchPartyRequest</i> (client java)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GetBatchTiersHisto", propOrder = {
		"login", "tiersNumbers", "parts"
})
public class GetBatchTiersHisto {

	/**
	 * Les informations de login de l'utilisateur de l'application
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>login</i>.
	 */
	@XmlElement(required = true)
	public UserLogin login;

	/**
	 * Les ids des tiers à retourner
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>partyNumbers</i>.
	 */
	@XmlElement(required = true)
	public Set<Long> tiersNumbers;

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>parts</i>.
	 */
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
				", tiersNumbers=" + (tiersNumbers == null ? "null" : Arrays.toString(tiersNumbers.toArray())) +
				", parts=" + (parts == null ? "null" : Arrays.toString(parts.toArray())) +
				'}';
	}
}