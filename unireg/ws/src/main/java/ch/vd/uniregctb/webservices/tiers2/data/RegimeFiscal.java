package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Informations sur un régime fiscal d'une personne morale.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType
public class RegimeFiscal implements Range {

	/** La date de début de validité du régime fiscal. */
	@XmlElement(required = true)
	public Date dateDebut;

	/** La date de fin de validité du régime fiscal; ou <i>null</i> s'il est toujours valide. */
	@XmlElement(required = false)
	public Date dateFin;

	/** Le code du régime fiscal, tel que défini dans le table TY_REGIME_FISCAL du Host. */
	public String code;

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
