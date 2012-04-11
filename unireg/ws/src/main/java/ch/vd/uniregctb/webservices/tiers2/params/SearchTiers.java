package ch.vd.uniregctb.webservices.tiers2.params;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.webservices.tiers2.data.CategorieDebiteur;
import ch.vd.uniregctb.webservices.tiers2.data.Date;
import ch.vd.uniregctb.webservices.tiers2.data.Tiers;
import ch.vd.uniregctb.webservices.tiers2.data.TypeRecherche;

/**
 * <b>Dans la version 3 du web-service :</b> <i>searchPartyRequestType</i> (xml) / <i>SearchPartyRequest</i> (client java)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SearchTiers")
public class SearchTiers {

	/**
	 * Les informations de login de l'utilisateur de l'application
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>login</i>.
	 */
	@XmlElement(required = true)
	public UserLogin login;

	/**
	 * Un ou plusieurs numéro de contribuable séparés par des "+".
	 * <p/>
	 * Si ce champ est renseigné, tous les autres champs sont ignorés.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>number</i>.
	 */
	@XmlElement(required = false)
	public String numero;

	/**
	 * Nom courrier des tiers à rechercher (= prénoms et noms des personnes physiques ou raisons sociales des personnes morales).
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>contactName</i>.
	 */
	@XmlElement(required = false)
	public String nomCourrier;

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>townOrCountry</i>.
	 */
	@XmlElement(required = false)
	public String localiteOuPays;

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>dateOfBirth</i>.
	 */
	@XmlElement(required = false)
	public Date dateNaissance;

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>socialInsuranceNumber</i>.
	 */
	@XmlElement(required = false)
	public String numeroAVS;

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>taxResidenceFSOId</i>.
	 */
	@XmlElement(required = false)
	public Integer noOfsFor;

	/**
	 * Si <i>vrai</i>, restreint le critère {@link #noOfsFor} sur les fors principaux actifs.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>activeMainTaxResidence</i>.
	 */
	@XmlElement(required = false)
	public Boolean forPrincipalActif;

	/**
	 * Le type de recherche sur le nomCourrier.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>searchMode</i>.
	 */
	@XmlElement(required = false)
	public TypeRecherche typeRecherche;

	/**
	 * Si renseigné, restreint la recherche sur un type de tiers
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>partyTypes</i>.
	 */
	public Tiers.Type typeTiers;

	/**
	 * La catégorie de débiteur (uniquement valable sur les débiteurs)
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>debtorCategory</i>.
	 */
	@XmlElement(required = false)
	public CategorieDebiteur categorieDebiteur;

	/**
	 * Si renseigné, restreint la recherche sur les tiers actifs (= un for fiscal principal ouvert) ou inactifs (= pas de for fiscal principal ouvert).
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>activeParty</i>.
	 */
	public Boolean tiersActif;

	@Override
	public String toString() {
		return "SearchTiers{" +
				"login=" + login +
				", numero='" + numero + '\'' +
				", nomCourrier='" + nomCourrier + '\'' +
				", localiteOuPays='" + localiteOuPays + '\'' +
				", dateNaissance=" + dateNaissance +
				", numeroAVS='" + numeroAVS + '\'' +
				", noOfsFor=" + noOfsFor +
				", forPrincipalActif=" + forPrincipalActif +
				", typeRecherche=" + typeRecherche +
				", typeTiers=" + typeTiers +
				", categorieDebiteur=" + categorieDebiteur +
				", tiersActif=" + tiersActif +
				'}';
	}
}