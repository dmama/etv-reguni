package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Contient les données qui permettent d'identifier de manière unique une déclaration d'impôt ordinaire
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DeclarationImpotOrdinaireKey")
public class DeclarationImpotOrdinaireKey {
	
	/**
	 * Le numéro de contribuable auquel appartient la déclaration.
	 */
	@XmlElement(required = true)
	public long ctbId;

	/**
	 * La période fiscale complète (ex. 2010) pour laquelle la déclaration a été émise
	 */
	@XmlElement(required = true)
	public int periodeFiscale;

	/**
	 * Le numéro de séquence de la déclaration (pour le contribuable et la période fiscale considérée).
	 */
	@XmlElement(required = true)
	public int numeroSequenceDI;

	@Override
	public String toString() {
		return "DeclarationImpotOrdinaireKey{" +
				"ctbId=" + ctbId +
				", periodeFiscale=" + periodeFiscale +
				", numeroSequenceDI=" + numeroSequenceDI +
				'}';
	}
}
