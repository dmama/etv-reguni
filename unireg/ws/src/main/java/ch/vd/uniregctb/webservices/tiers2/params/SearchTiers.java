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

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SearchTiers")
public class SearchTiers {

	/** Les informations de login de l'utilisateur de l'application */
	@XmlElement(required = true)
	public UserLogin login;

	/**
	 * Un ou plusieurs numéro de contribuable séparés par des "+".
	 * <p>
	 * Si ce champ est renseigné, tous les autres champs sont ignorés.
	 */
	@XmlElement(required = false)
	public String numero;

	/**
	 * Nom courrier des tiers à rechercher (= prénoms et noms des personnes physiques ou raisons sociales des personnes morales).
	 */
	@XmlElement(required = false)
	public String nomCourrier;

	@XmlElement(required = false)
	public String localiteOuPays;
	@XmlElement(required = false)
	public Date dateNaissance;
	@XmlElement(required = false)
	public String numeroAVS;
	@XmlElement(required = false)
	public Integer noOfsFor;

	/**
	 * Si <i>vrai</i>, restreint le critère {@link #noOfsFor} sur les fors principaux actifs.
	 */
	@XmlElement(required = false)
	public Boolean forPrincipalActif;

	/**
	 * Le type de recherche sur le nomCourrier.
	 */
	@XmlElement(required = false)
	public TypeRecherche typeRecherche;

	/**
	 * Si renseigné, restreint la recherche sur un type de tiers
	 */
	public Tiers.Type typeTiers;

	/**
	 * La catégorie de débiteur (uniquement valable sur les débiteurs)
	 */
	@XmlElement(required = false)
	public CategorieDebiteur categorieDebiteur;

	/**
	 * Si renseigné, restreint la recherche sur les tiers actifs (= un for fiscal principal ouvert) ou inactifs (= pas de for fiscal principal ouvert).
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