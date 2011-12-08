package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Contient les données qui permettent d'identifier de manière unique une déclaration d'impôt ordinaire
 * <p/>
 * <b>Dans la version 3 du web-service :</b> <i>ordinaryTaxDeclarationKeyType</i> (xml) / <i>OrdinaryTaxDeclarationKey</i> (client java)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DeclarationImpotOrdinaireKey")
public class DeclarationImpotOrdinaireKey {
	
	/**
	 * Le numéro de contribuable auquel appartient la déclaration.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>taxpayerNumber</i>
	 */
	@XmlElement(required = true)
	public long ctbId;

	/**
	 * La période fiscale complète (ex. 2010) pour laquelle la déclaration a été émise
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>taxPeriod</i>
	 */
	@XmlElement(required = true)
	public int periodeFiscale;

	/**
	 * Le numéro de séquence de la déclaration (pour le contribuable et la période fiscale considérée).
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>sequenceNumber</i>
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
