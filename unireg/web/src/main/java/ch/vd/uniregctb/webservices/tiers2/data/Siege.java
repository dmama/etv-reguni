package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * Contient les informations du siège d'une personne morale.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType
public class Siege {

	@XmlType(name = "TypeSiege")
	@XmlEnum(String.class)
	public static enum TypeSiege {
		COMMUNE_CH,
		PAYS_HS;
	}

	@XmlElement(required = true)
	public Date dateDebut;

	@XmlElement(required = false)
	public Date dateFin;

	/** Cet enum permet d'interpréter le numéro OFS contenu dans noOfsSiege */
	@XmlElement(required = true)
	public TypeSiege typeSiege;

	/** Numéro OFS étendu de la commune suisse ou du pays de siège */
	@XmlElement(required = true)
	public int noOfsSiege;
}
