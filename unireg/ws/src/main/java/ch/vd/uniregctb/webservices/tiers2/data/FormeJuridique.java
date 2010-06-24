package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Informations sur la forme juridique d'une personne morale.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType
public class FormeJuridique implements Range {

	/** La date de début de validité de la forme juridique. */
	@XmlElement(required = true)
	public Date dateDebut;

	/** La date de fin de validité de la forme juridique; ou <i>null</i> si elle est toujours valide. */
	@XmlElement(required = false)
	public Date dateFin;

	/** Le code de la forme juridique, tel que défini dans le table FORME_JURIDIQ_ACI du Host. */
	public String code;

	public Date getDateDebut() {
		return dateDebut;
	}

	public Date getDateFin() {
		return dateFin;
	}

	public void setDateDebut(Date v) {
		dateDebut = v;
	}

	public void setDateFin(Date v) {
		dateFin = v;
	}
}
