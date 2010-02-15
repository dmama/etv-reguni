package ch.vd.uniregctb.webservices.tiers.params;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.webservices.tiers.Date;
import ch.vd.uniregctb.webservices.tiers.TypeRecherche;

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
	 * Si vrai, restreint la recherche sur les fors principaux actifs.
	 */
	@XmlElement(required = false)
	public Boolean forPrincipalActif;

	/**
	 * Le type de recherche sur le nomCourrier.
	 */
	@XmlElement(required = false)
	public TypeRecherche typeRecherche;
}