package ch.vd.uniregctb.webservices.tiers2.params;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.webservices.tiers2.data.Date;

/**
 * <b>Dans la version 3 du web-service :</b> <i>searchCorporationEventsRequestType</i> (xml) / <i>SearchCorporationEventsRequest</i> (client java)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType
public class SearchEvenementsPM {

	/**
	 * Les informations de login de l'utilisateur de l'application
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>login</i>.
	 */
	@XmlElement(required = true)
	public UserLogin login;

	/**
	 * Critère sur le numéro de personne morale des événements à retourner.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>corporationNumber</i>.
	 */
	@XmlElement(required = false)
	public Long tiersNumber;

	/**
	 * Critère sur le code des événement à retourner
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>eventCode</i>.
	 */
	@XmlElement(required = false)
	public String codeEvenement;

	/**
	 * Critère sur la date minimale des événements à retourner.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>startDate</i>.
	 */
	@XmlElement(required = false)
	public Date dateMinimale;

	/**
	 * Critère sur la date maximale des événements à retourner.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>endDate</i>.
	 */
	@XmlElement(required = false)
	public Date dateMaximale;

	@Override
	public String toString() {
		return "SearchEvenementsPM{" +
				"login=" + login +
				", tiersNumber=" + tiersNumber +
				", codeEvenement='" + codeEvenement + '\'' +
				", dateMinimale=" + dateMinimale +
				", dateMaximale=" + dateMaximale +
				'}';
	}
}
