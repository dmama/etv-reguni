package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Evénement d'une personne morale.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType
public class EvenementPM {

	/** La date de l'événement. */
	@XmlElement(required = true)
	public Date dateEvenement;

	/** Le numéro de personne morale associée à l'événement. */
	@XmlElement(required = false)
	public Long tiersNumber;

	/** Le code de l'événement à retourner */
	@XmlElement(required = false)
	public String codeEvenement;
}
