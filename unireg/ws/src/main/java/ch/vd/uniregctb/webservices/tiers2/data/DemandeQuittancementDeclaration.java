package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Demande de quittancement (= enregistrement du retour) d'une déclaration d'impôt ordinaire
 * <p/>
 * <b>Dans la version 3 du web-service :</b> <i>taxDeclarationReturnRequestType</i> (xml) / <i>TaxDeclarationReturnRequest</i> (client java)
 *
 * @see ch.vd.uniregctb.webservices.tiers2.data.ReponseQuittancementDeclaration
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DemandeQuittancementDeclaration")
public class DemandeQuittancementDeclaration {

	/**
	 * La clé qui permet d'identification la déclaration
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>key</i>.
	 */
	@XmlElement(required = true)
	public DeclarationImpotOrdinaireKey key;

	/**
	 * La date de retour (= date de quittancement) de la déclaration
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>returnDate</i>.
	 */
	@XmlElement(required = true)
	public Date dateRetour;

	/**
	 * La source du quittancement (= nom de l'application à l'origine du quittancement : CEDI, ADDI, ...)
	 */
	@XmlElement(required = false) // [SIFISC-1782] optionel pour permettre une transition en douceur des anciens clients
	public String source;

	@Override
	public String toString() {
		return "DemandeQuittancementDeclaration{" +
				"key=" + key +
				", dateRetour=" + dateRetour +
				", source='" + source + '\'' +
				'}';
	}
}