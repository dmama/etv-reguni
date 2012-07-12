package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * Contient les informations du siège d'une personne morale.
 * <p/>
 * <b>Dans la version 3 du web-service :</b> <i>legalSeatType</i> (xml) / <i>LegalSeat</i> (client java)
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType
public class Siege implements Range {

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>legalSeatTypeType</i> (xml) / <i>LegalSeatType</i> (client java)
	 */
	@XmlType(name = "TypeSiege")
	@XmlEnum(String.class)
	public static enum TypeSiege {
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>SWISS_MUNICIPALITY</i>.
		 */
		COMMUNE_CH,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>FOREIGN_COUNTRY</i>.
		 */
		PAYS_HS
	}

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>dateFrom</i>.
	 */
	@XmlElement(required = true)
	public Date dateDebut;

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>dateTo</i>.
	 */
	@XmlElement(required = false)
	public Date dateFin;

	/**
	 * Cet enum permet d'interpréter le numéro OFS contenu dans noOfsSiege
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>type</i>.
	 */
	@XmlElement(required = true)
	public TypeSiege typeSiege;

	/**
	 * Numéro OFS étendu de la commune suisse ou du pays de siège
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>fsoId</i>.
	 */
	@XmlElement(required = true)
	public int noOfsSiege;

	@Override
	public Date getDateDebut() {
		return dateDebut;
	}

	@Override
	public Date getDateFin() {
		return dateFin;
	}

	@Override
	public void setDateDebut(Date v) {
		dateDebut = v;
	}

	@Override
	public void setDateFin(Date v) {
		dateFin = v;
	}
}
