package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Informations métier supplémentaires sur d'un débiteur de prestations imposables (voir cas JIRA UNIREG-2110).
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DebiteurInfo")
public class DebiteurInfo {

	/**
	 * Le numéro de tiers du débiteur de prestations imposables.
	 */
	@XmlElement(required = true)
	public long numeroDebiteur;

	/**
	 * La période fiscale considérée (1er janvier au 31 décembre).
	 */
	@XmlElement(required = true)
	public int periodeFiscale;

	/**
	 * Le nombre de déclarations impôt source (LRs) qui peuvent théoriquement être émises dans la période fiscale considérée.
	 */
	@XmlElement(required = true)
	public int nbLRsTheorique;

	/**
	 * Le nombre de déclarations impôts source (LRs) réellement émises (et non-annulées) dans la période fiscale considérée.
	 */
	@XmlElement(required = true)
	public int nbLRsEmises;
}
