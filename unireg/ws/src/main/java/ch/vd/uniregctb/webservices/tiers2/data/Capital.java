package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Informations sur le capital à disposition d'une personne morale.
 * <p/>
 * <b>Dans la version 3 du web-service :</b> <i>capitalType</i> (xml) / <i>Capital</i> (client java)
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType
public class Capital implements Range {

	/**
	 * Informations permettant d'identifier une édition de la Feuille officielle suisse du commerce.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>sogcEditionType</i> (xml) / <i>SogcEdition</i> (client java)
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType
	public static class EditionFosc {

		/**
		 * Année de parution
		 * <p/>
		 * <b>Dans la version 3 du web-service :</b> <i>year</i>.
		 */
		public Integer anneeFosc;

		/**
		 * Numéro dans l'année de parution
		 * <p/>
		 * <b>Dans la version 3 du web-service :</b> <i>number</i>.
		 */
		public Integer noFosc;
	}

	/**
	 * La date de début de validité du régime fiscal.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>dateFrom</i>.
	 */
	@XmlElement(required = true)
	public Date dateDebut;

	/**
	 * La date de fin de validité du régime fiscal; ou <i>null</i> s'il est toujours valide.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>dateTo</i>.
	 */
	@XmlElement(required = false)
	public Date dateFin;

	/**
	 * La valeur du capital action de la PM.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>shareCapital</i>.
	 */
	@XmlElement(required = true)
	public Long capitalAction;

	/**
	 * La valeur du capital libéré de la PM.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>paidInCapital</i>.
	 */
	@XmlElement(required = true)
	public Long capitalLibere;

	/**
	 * Retourne <i>vrai</i> si l'absence de capital libéré est normale, ou <i>faux</i> si elle est anormale.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>absentPaidInCapitalNormal</i>.
	 */
	@XmlElement(required = true)
	public boolean absenceCapitalLibereNormale;

	/**
	 * L'édition de la Feuille officielle suisse du commerce dans laquelle ces informations sont parues
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>sogcEdition</i>.
	 */
	@XmlElement(required = false)
	public EditionFosc editionFosc;

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
