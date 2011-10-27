package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Evénement d'une personne morale.
 * <p/>
 * <b>Dans la version 3 du web-service :</b> <i>corporationEventType</i> (xml) / <i>CorporationEvent</i> (client java)
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType
public class EvenementPM {

	/**
	 * La date de l'événement.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>date</i>.
	 */
	@XmlElement(required = true)
	public Date dateEvenement;

	/**
	 * Le numéro de personne morale associée à l'événement.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>partyNumber</i>.
	 */
	@XmlElement(required = false)
	public Long tiersNumber;

	/**
	 * Le code de l'événement à retourner
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>code</i>.
	 */
	@XmlElement(required = false)
	public String codeEvenement;
}
