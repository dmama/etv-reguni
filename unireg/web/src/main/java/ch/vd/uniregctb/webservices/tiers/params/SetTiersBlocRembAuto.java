package ch.vd.uniregctb.webservices.tiers.params;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.webservices.tiers.TiersPart;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SetTiersBlocRembAuto", propOrder = {
		"login", "tiersNumber", "blocage"
})
public class SetTiersBlocRembAuto {

	/** Les informations de login de l'utilisateur de l'application */
	@XmlElement(required = true)
	public UserLogin login;

	@XmlElement(required = true)
	public long tiersNumber;
	@XmlElement(required = true)
	public boolean blocage;

	public SetTiersBlocRembAuto() {
	}

	public SetTiersBlocRembAuto(UserLogin login, long tiersNumber, boolean blocage) {
		super();
		this.login = login;
		this.tiersNumber = tiersNumber;
		this.blocage = blocage;
	}

	public SetTiersBlocRembAuto clone(Set<TiersPart> newParts) {
		return new SetTiersBlocRembAuto(login, tiersNumber, blocage);
	}
}
