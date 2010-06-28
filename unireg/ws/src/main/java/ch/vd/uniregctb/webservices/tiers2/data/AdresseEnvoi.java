package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import ch.vd.uniregctb.webservices.tiers2.impl.EnumHelper;

/**
 * Représente les 6 lignes d'adresses d'un tiers formattées selon les recommandations de la poste suisse.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AdresseEnvoi", propOrder = {
		"ligne1", "ligne2", "ligne3", "ligne4", "ligne5", "ligne6", "ligne7", "isSuisse", "salutations", "formuleAppel", "typeAffranchissement"
})
public class AdresseEnvoi {

	@XmlElement(required = true)
	public String ligne1;

	@XmlElement(required = true)
	public String ligne2;

	@XmlElement(required = true)
	public String ligne3;

	@XmlElement(required = true)
	public String ligne4;

	@XmlElement(required = false)
	public String ligne5;

	@XmlElement(required = false)
	public String ligne6;

	/**
	 * [UNIREG-1974] La poste suisse accepte :
	 * <ul>
	 * <li>au maximum 6 lignes pour les adresses en Suisse, et</li>
	 * <li>au maximum 7 lignes pour les adresses hors Suisse.</li>
	 * </ul>
	 * Cette septième ligne ne sera donc renseigné qu'avec des adresses étrangères (pour autant qu'elles tiennent sur sept lignes, évidemment).  
	 */
	@XmlElement(required = false)
	public String ligne7;

	@XmlElement(required = true)
	public boolean isSuisse; // TODO (msi) pour la version 3 : à supprimer (remplacé par le type d'affranchissement)

	/** Le type d'affranchissement demandé par la poste pour envoyer un courrier à cette adresse. */
	@XmlElement(required = true)
	public TypeAffranchissement typeAffranchissement;

	/**
	 * Les salutations selon les us et coutumes de l'ACI. Exemples :
	 * <ul>
	 * <li>Monsieur</li>
	 * <li>Madame</li>
	 * <li>Aux héritiers de</li>
	 * <li>...</li>
	 * </ul>
	 */
	@XmlElement(required = false)
	public String salutations;

	/**
	 * La formule d'appel stricte. C'est-à-dire les salutations mais <b>sans formule spéciale</b> propre à l'ACI (pas de <i>Aux héritiers
	 * de</i>). Exemples :
	 * <ul>
	 * <li>Monsieur</li>
	 * <li>Madame</li>
	 * <li>Madame, Monsieur</li>
	 * <li>...</li>
	 * </ul>
	 */
	@XmlElement(required = false)
	public String formuleAppel;

	public AdresseEnvoi() {
	}

	public AdresseEnvoi(ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee adresse) {
		this.ligne1 = adresse.getLigne1();
		this.ligne2 = adresse.getLigne2();
		this.ligne3 = adresse.getLigne3();
		this.ligne4 = adresse.getLigne4();
		this.ligne5 = adresse.getLigne5();
		this.ligne6 = adresse.getLigne6();
		this.ligne7 = adresse.getLigne7();
		this.isSuisse = adresse.isSuisse();
		this.salutations = adresse.getSalutations();
		this.formuleAppel = adresse.getFormuleAppel();
		this.typeAffranchissement = EnumHelper.coreToWeb(adresse.getTypeAffranchissement());
	}
}
