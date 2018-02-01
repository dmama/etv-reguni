package ch.vd.uniregctb.evenement.identification.contribuable;

import javax.persistence.Column;

import org.hibernate.annotations.Type;

/**
 * Contient les critères de recherche sur l'adresse d'un contribuable pour le traitement d'une requête d'identification d'un contribuable.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class CriteresAdresse {

	public enum TypeAdresse {
		SUISSE, ETRANGERE
	}

	private String ligneAdresse1;
	private String ligneAdresse2;
	private String rue;
	private String noPolice;
	private String noAppartement;
	private Integer numeroCasePostale;
	private String texteCasePostale;
	private String localite;
	private String lieu;
	private Integer npaSuisse;
	private String chiffreComplementaire;
	private Integer noOrdrePosteSuisse;
	private String npaEtranger;
	private String codePays;
	private TypeAdresse typeAdresse;

	/**
	 * Lignes libres additionnelles pour les données d’adresse supplémentaires qui ne trouvent pas leur place dans les autres champs de
	 * l’adresse (p. ex. pour la mention c/o, etc.) :
	 * <ul>
	 * <li>adressLine1 doit être utilisé pour des indications personnifiées (p.ex. c/o-adresse)</li>
	 * <li>adressLine2 doit être utilisé pour des indications non personnifiées (indications supplémentaires pour la localisation, p.ex.
	 * Chalet Edelweiss)</li>
	 * </ul>
	 */
	@Column(name = "ADR_LIGNE_1", length = 60)
	public String getLigneAdresse1() {
		return ligneAdresse1;
	}

	public void setLigneAdresse1(String ligneAdresse1) {
		this.ligneAdresse1 = ligneAdresse1;
	}

	/**
	 * @see #getLigneAdresse1()
	 */
	@Column(name = "ADR_LIGNE_2", length = 60)
	public String getLigneAdresse2() {
		return ligneAdresse2;
	}

	public void setLigneAdresse2(String ligneAdresse2) {
		this.ligneAdresse2 = ligneAdresse2;
	}

	/**
	 * Noms de rue dans l’adresse postale. Il peut aussi s’agir du nom d’une localité, d’un hameau, etc.
	 */
	@Column(name = "ADR_RUE", length = 60)
	public String getRue() {
		return rue;
	}

	public void setRue(String rue) {
		this.rue = rue;
	}

	/**
	 * Numéro de la maison dans l’adresse postale.
	 */
	@Column(name = "ADR_NO_POLICE", length = 12)
	public String getNoPolice() {
		return noPolice;
	}

	public void setNoPolice(String noPolice) {
		this.noPolice = noPolice;
	}

	/**
	 * Numéro de l’appartement. Ce numéro est éventuellement nécessaire dans le cadre de grands ensembles.
	 */
	@Column(name = "ADR_NO_APPART", length = 10)
	public String getNoAppartement() {
		return noAppartement;
	}

	public void setNoAppartement(String noAppartement) {
		this.noAppartement = noAppartement;
	}

	/**
	 * Numéro de case postale
	 */
	@Column(name = "ADR_NO_CP")
	public Integer getNumeroCasePostale() {
		return numeroCasePostale;
	}

	public void setNumeroCasePostale(Integer numeroCasePostale) {
		this.numeroCasePostale = numeroCasePostale;
	}

	/**
	 * Texte de la case postale dans la langue voulue. En générale « Case Postale ».
	 */
	@Column(name = "ADR_TEXT_CP", length = 15)
	public String getTexteCasePostale() {
		return texteCasePostale;
	}

	public void setTexteCasePostale(String texteCasePostale) {
		this.texteCasePostale = texteCasePostale;
	}

	/**
	 * Dans les adresses étrangères il arrive que en plus du lieu et du pays, une autre indication géographique doit être introduite
	 * occasionnellement. Le champ localité est prévu pour ce cas. Il contient des données complémentaires quant au lieu, comme par exemple
	 * la région, la province, l’état fédéral ou le quartier. Etant donné qu’avec les adresses à l’étranger, il est difficile de déterminer
	 * s’il s’agit d’une indication de lieu d’ordre supérieur ou inférieur, il a été décidé de renoncer à cette indication supplémentaire.
	 */
	@Column(name = "ADR_LOCALITE", length = 40)
	public String getLocalite() {
		return localite;
	}

	public void setLocalite(String localite) {
		this.localite = localite;
	}

	/**
	 * Lieu de l’adresse (si nécessaire pour une adresse étrangère, y compris la province, etc.).
	 */
	@Column(name = "ADR_LIEU", length = 40)
	public String getLieu() {
		return lieu;
	}

	public void setLieu(String lieu) {
		this.lieu = lieu;
	}

	/**
	 * Numéro d’acheminement attribué par la poste suisse, sous la forme qui sera imprimée sur les lettres : nombre à 4 chiffres.
	 */
	@Column(name = "ADR_NPA_SUISSE")
	public Integer getNpaSuisse() {
		return npaSuisse;
	}

	public void setNpaSuisse(Integer suisse) {
		npaSuisse = suisse;
	}

	/**
	 * Uniquement pour les numéros postaux suisses ; les numéros postaux suisses ne sont pas univoques. Le même numéro postal peut être
	 * utilisé pour différents endroits. Cependant, ajouté au chiffre complémentaire mentionné ici en deuxième position, il est sans
	 * équivoque. Lorsque le système d’origine possède cette information, il doit la transférer de manière à ce qu’elle puisse être utilisée
	 * par le système récepteur en cas de besoin.
	 */
	@Column(name = "ADR_CH_COMPL", length = 2)
	public String getChiffreComplementaire() {
		return chiffreComplementaire;
	}

	public void setChiffreComplementaire(String chiffreComplementaire) {
		this.chiffreComplementaire = chiffreComplementaire;
	}

	/**
	 * Uniquement pour les numéros d’acheminement postaux suisses : les numéros postaux suisses peuvent changer au cours des années et être
	 * utilisés à d’autres fins. Le chiffre d’ordre reste stable et n’est attribué à nouveau en aucun cas. Lorsque le système d’origine
	 * possède cette information, il doit la transférer de manière à ce qu’elle puisse être utilisée par le système récepteur en cas de
	 * besoin.
	 */
	@Column(name = "ADR_ORDRE_POSTE")
	public Integer getNoOrdrePosteSuisse() {
		return noOrdrePosteSuisse;
	}

	public void setNoOrdrePosteSuisse(Integer noOrdrePosteSuisse) {
		this.noOrdrePosteSuisse = noOrdrePosteSuisse;
	}

	/**
	 * Numéro d’acheminement postal attribué par une poste étrangère. Ce numéro peut être composé de chiffres, de lettres ou une combinaison
	 * des deux, voire même de caractères spéciaux.
	 */
	@Column(name = "ADR_NPA_ETRANGER", length = 15)
	public String getNpaEtranger() {
		return npaEtranger;
	}

	public void setNpaEtranger(String etranger) {
		npaEtranger = etranger;
	}

	/**
	 * Abréviation ISO [ISO 3166-1] du pays dans lequel se trouve le lieu faisant partie de l’adresse postale. Le pays définit les
	 * conventions pour la présentation des adresses. L'indication nationale doit aussi être donnée avec des adresses postales suisses.
	 * Attention : Des modifications politiques ou des changements de nom des pays entraînent des adaptations de la liste de pays selon ISO.
	 */
	@Column(name = "ADR_CODE_PAYS", length = 12)
	public String getCodePays() {
		return codePays;
	}

	public void setCodePays(String code) {
		this.codePays = code;
	}

	/**
	 * Détermine s'il s'agit d'une adresse suisse ou étrangère.
	 */
	@Column(name = "ADR_TYPE")
	@Type(type = "ch.vd.uniregctb.hibernate.identification.contribuable.TypeAdresseCriteresAdresseUserType")
	public TypeAdresse getTypeAdresse() {
		return typeAdresse;
	}

	public void setTypeAdresse(TypeAdresse typeAdresse) {
		this.typeAdresse = typeAdresse;
	}
}
