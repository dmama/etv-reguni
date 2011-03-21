package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

import ch.vd.uniregctb.webservices.tiers2.impl.EnumHelper;

/**
 * Représente les 6 lignes d'adresses d'un tiers formattées selon les recommandations de la poste suisse.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AdresseEnvoi", propOrder = {
		"ligne1", "ligne2", "ligne3", "ligne4", "ligne5", "ligne6", "isSuisse", "salutations", "formuleAppel", "nomsPrenoms", "complement", "pourAdresse", "rueNumero", "casePostale", "npaLocalite",
		"pays", "typeAffranchissement"
})
public class AdresseEnvoi {

	/**
	 * La première ligne d'adresse à utiliser impérativement pour tout envoi de courrier par la poste.
	 */
	@XmlElement(required = true)
	public String ligne1;

	/**
	 * La deuxième ligne d'adresse à utiliser impérativement pour tout envoi de courrier par la poste.
	 */
	@XmlElement(required = true)
	public String ligne2;

	/**
	 * La troisième ligne d'adresse à utiliser impérativement pour tout envoi de courrier par la poste.
	 */
	@XmlElement(required = true)
	public String ligne3;

	/**
	 * La quatrième ligne d'adresse à utiliser impérativement pour tout envoi de courrier par la poste.
	 */
	@XmlElement(required = true)
	public String ligne4;

	/**
	 * La cinquième ligne d'adresse à utiliser impérativement pour tout envoi de courrier par la poste.
	 */
	@XmlElement(required = false)
	public String ligne5;

	/**
	 * La sixième ligne d'adresse à utiliser impérativement pour tout envoi de courrier par la poste.
	 */
	@XmlElement(required = false)
	public String ligne6;

	@XmlElement(required = true)
	public boolean isSuisse;

	/**
	 * Les salutations selon les us et coutumes de l'ACI. Exemples : <ul> <li>Monsieur</li> <li>Madame</li> <li>Aux héritiers de</li> <li>...</li> </ul>
	 * <p/>
	 * <b>Attention !</b> Cette donnée est exposée pour permettre un affichage spécialisée dans les applications fiscales. <b>Elle ne doit pas être utilisé pour reconstruire une adresse qui serait
	 * utilisée pour envoyer du courrier par le poste !</b>
	 */
	@XmlElement(required = false)
	public String salutations;

	/**
	 * La formule d'appel stricte. C'est-à-dire les salutations mais <b>sans formule spéciale</b> propre à l'ACI (pas de <i>Aux héritiers de</i>). Exemples : <ul> <li>Monsieur</li> <li>Madame</li>
	 * <li>Madame, Monsieur</li> <li>...</li> </ul>
	 * <p/>
	 * <b>Attention !</b> Cette donnée est exposée pour permettre un affichage spécialisée dans les applications fiscales. <b>Elle ne doit pas être utilisé pour reconstruire une adresse qui serait
	 * utilisée pour envoyer du courrier par le poste !</b>
	 */
	@XmlElement(required = false)
	public String formuleAppel;

	/**
	 * Les noms et prénoms (un nom + prénom par ligne, au maximum 2 lignes) pour les personnes physiques/ménages communs, ou les raisons sociales pour les débiteurs/PM.
	 * <p/>
	 * <b>Attention !</b> Cette donnée est exposée pour permettre un affichage spécialisée dans les applications fiscales. <b>Elle ne doit pas être utilisé pour reconstruire une adresse qui serait
	 * utilisée pour envoyer du courrier par le poste !</b>
	 */
	@XmlElement(required = false)
	public List<String> nomsPrenoms;

	/**
	 * Le complément d'adresse.
	 * <p/>
	 * <b>Attention !</b> Cette donnée est exposée pour permettre un affichage spécialisée dans les applications fiscales. <b>Elle ne doit pas être utilisé pour reconstruire une adresse qui serait
	 * utilisée pour envoyer du courrier par le poste !</b>
	 */
	@XmlElement(required = false)
	public String complement;

	/**
	 * Le pour adresse.
	 * <p/>
	 * <b>Attention !</b> Cette donnée est exposée pour permettre un affichage spécialisée dans les applications fiscales. <b>Elle ne doit pas être utilisé pour reconstruire une adresse qui serait
	 * utilisée pour envoyer du courrier par le poste !</b>
	 */
	@XmlElement(required = false)
	public String pourAdresse;

	/**
	 * La rue et son numéro.
	 * <p/>
	 * <b>Attention !</b> Cette donnée est exposée pour permettre un affichage spécialisée dans les applications fiscales. <b>Elle ne doit pas être utilisé pour reconstruire une adresse qui serait
	 * utilisée pour envoyer du courrier par le poste !</b>
	 */
	@XmlElement(required = false)
	public String rueNumero;

	/**
	 * La case postale et son numéro.
	 * <p/>
	 * <b>Attention !</b> Cette donnée est exposée pour permettre un affichage spécialisée dans les applications fiscales. <b>Elle ne doit pas être utilisé pour reconstruire une adresse qui serait
	 * utilisée pour envoyer du courrier par le poste !</b>
	 */
	@XmlElement(required = false)
	public String casePostale;

	/**
	 * Le NPA et la localité.
	 * <p/>
	 * <b>Attention !</b> Cette donnée est exposée pour permettre un affichage spécialisée dans les applications fiscales. <b>Elle ne doit pas être utilisé pour reconstruire une adresse qui serait
	 * utilisée pour envoyer du courrier par le poste !</b>
	 */
	@XmlElement(required = false)
	public String npaLocalite;

	/**
	 * Le pays si hors-Suisse, ou <b>null</b> en cas de la Suisse.
	 * <p/>
	 * <b>Attention !</b> Cette donnée est exposée pour permettre un affichage spécialisée dans les applications fiscales. <b>Elle ne doit pas être utilisé pour reconstruire une adresse qui serait
	 * utilisée pour envoyer du courrier par le poste !</b>
	 */
	@XmlElement(required = false)
	public String pays;

	/**
	 * Le type d'affranchissement demandé par la poste pour envoyer un courrier à cette adresse.
	 */
	@XmlElement(required = true)
	public TypeAffranchissement typeAffranchissement;

	public AdresseEnvoi() {
	}

	public AdresseEnvoi(ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee adresse) {
		this.ligne1 = adresse.getLigne1();
		this.ligne2 = adresse.getLigne2();
		this.ligne3 = adresse.getLigne3();
		this.ligne4 = adresse.getLigne4();
		this.ligne5 = adresse.getLigne5();
		this.ligne6 = adresse.getLigne6();
		this.isSuisse = adresse.isSuisse();
		this.salutations = adresse.getSalutations();
		this.formuleAppel = adresse.getFormuleAppel();
		this.nomsPrenoms = adresse.getNomPrenom();
		this.complement = adresse.getComplement();
		this.pourAdresse = adresse.getPourAdresse();
		this.rueNumero = adresse.getRueEtNumero();
		this.casePostale = adresse.getCasePostale();
		this.npaLocalite = adresse.getNpaEtLocalite();
		this.pays = adresse.getPays();
		this.typeAffranchissement = EnumHelper.coreToWeb(adresse.getTypeAffranchissement());
	}
}
