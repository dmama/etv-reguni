package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Informations sur le capital à disposition d'une personne morale.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType
public class Capital {

	/**
	 * Informations permettant d'identifier une édition de la Feuille officielle suisse du commerce.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType
	public static class EditionFosc {

		/** Année de parution */
		public Integer anneeFosc;

		/** Numéro dans l'année de parution */
		public Integer noFosc;
	}

	/** La date de début de validité du régime fiscal. */
	@XmlElement(required = true)
	public Date dateDebut;

	/** La date de fin de validité du régime fiscal; ou <i>null</i> s'il est toujours valide. */
	@XmlElement(required = false)
	public Date dateFin;

	/** La valeur du capital action de la PM. */
	@XmlElement(required = true)
	public Long capitalAction;

	/** La valeur du capital libéré de la PM. */
	@XmlElement(required = true)
	public Long capitalLibere;

	/** Retourne <i>vrai</i> si l'absence de capital libéré est normale, ou <i>faux</i> si elle est anormale. */
	@XmlElement(required = true)
	public boolean absenceCapitalLibereNormale;

	/** L'édition de la Feuille officielle suisse du commerce dans laquelle ces informations sont parues */
	@XmlElement(required = false)
	public EditionFosc editionFosc;
}
