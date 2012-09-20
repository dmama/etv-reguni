/**
 *
 */
package ch.vd.uniregctb.adresse;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;

import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.TexteCasePostale;

/**
 * Adresse libre non issue d'une adresse de l'individu ou de l'adresse courrier du représentant légal
 */
@Entity
public abstract class AdresseSupplementaire extends AdresseTiers {

	private static final long serialVersionUID = -9160275750639533984L;

	/**
	 * Ligne libre additionnelle pour les données d'adresse supplémentaires qui ne trouvent pas leur place dans les autres champs de l'adresse (p. ex. pour la mention c/o, etc.).
	 * Longueur maximum selon eCH-0010 : 60
	 */
	private String complement;

	@Column(name = "COMPLEMENT", length = LengthConstants.ADRESSE_NOM)
	public String getComplement() {
		return complement;
	}

	public void setComplement(String theComplement) {
		complement = theComplement;
	}

	/**
	 * Désignation de la rue en texte libre.
	 */
	private String rue;

	@Column(name = "RUE", length = LengthConstants.ADRESSE_NOM)
	public String getRue() {
		return rue;
	}

	public void setRue(String theRue) {
		rue = theRue;
	}

	/**
	 * Numéro de la maison dans l'adresse postale, y compris des indications additionnelles.
	 * Longueur maximum selon eCH-0010 : 12
	 */
	private String numeroMaison;

	@Column(name = "NUMERO_MAISON", length = LengthConstants.ADRESSE_NUM)
	public String getNumeroMaison() {
		return numeroMaison;
	}

	public void setNumeroMaison(String theNumeroMaison) {
		numeroMaison = theNumeroMaison;
	}

	/**
	 * Numéro de l'appartement. Ce numéro est éventuellement nécessaire dans le cadre de grands ensembles.
	 * Longueur maximum selon eCH-0011 : 10
	 */
	private String numeroAppartement;

	@Column(name = "NUMERO_APPARTEMENT", length = LengthConstants.ADRESSE_NUM)
	public String getNumeroAppartement() {
		return numeroAppartement;
	}

	public void setNumeroAppartement(String theNumeroAppartement) {
		numeroAppartement = theNumeroAppartement;
	}

	/**
	 * Texte de la case postale dans la langue voulue.
	 * Dans la plupart des cas, le texte "Case postale" ou "Boîte postale" suffit.
	 * Longueur maximum selon eCH-0010 : 15
	 */
	private TexteCasePostale texteCasePostale = TexteCasePostale.CASE_POSTALE;

	@Column(name = "TEXTE_CASE_POSTALE", length = LengthConstants.ADRESSE_TYPESUPPLEM)
	@Type(type = "ch.vd.uniregctb.hibernate.TexteCasePostaleUserType")
	public TexteCasePostale getTexteCasePostale() {
		return texteCasePostale;
	}

	public void setTexteCasePostale(TexteCasePostale theTexteCasePostale) {
		texteCasePostale = theTexteCasePostale;
	}

	/**
	 * Numéro de la case postale
	 * Valeurs admises selon eCH-0010 : 0-9999
	 */
	private Integer numeroCasePostale;

	@Column(name = "NUMERO_CASE_POSTALE")
	public Integer getNumeroCasePostale() {
		return numeroCasePostale;
	}

	public void setNumeroCasePostale(Integer theNumeroCasePostale) {
		numeroCasePostale = theNumeroCasePostale;
	}

	/**
	 * Adresse permanente
	 */
	private boolean permanente = false;

	@Column(name = "PERMANENTE")
	public boolean isPermanente() {
		return permanente;
	}

	public void setPermanente(boolean thePermanente) {
		permanente = thePermanente;
	}
}