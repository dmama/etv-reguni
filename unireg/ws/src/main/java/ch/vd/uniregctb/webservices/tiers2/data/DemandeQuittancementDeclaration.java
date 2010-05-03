package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Demande de quittancement (= enregistrement du retour) d'une déclaration d'impôt ordinaire
 *
 * @see ch.vd.uniregctb.webservices.tiers2.data.ReponseQuittancementDeclaration
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DemandeQuittancementDeclaration")
public class DemandeQuittancementDeclaration {

	/**
	 * La clé qui permet d'identification la déclaration
	 */
	@XmlElement(required = true)
	public DeclarationImpotOrdinaireKey key;

	/**
	 * La date de retour (= date de quittancement) de la déclaration
	 */
	@XmlElement(required = true)
	public Date dateRetour;

	@Override
	public String toString() {
		return "QuittanceDeclarationDemande{" +
				"key=" + key +
				", dateRetour=" + dateRetour +
				'}';
	}
}