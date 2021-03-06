/**
 *
 */
package ch.vd.unireg.adresse;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;

import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.type.TexteCasePostale;

/**
 * Adresse libre non issue d'une adresse de l'individu ou de l'adresse courrier du représentant légal
 */
@Entity
public abstract class AdresseSupplementaire extends AdresseTiers implements AdresseFiscale {

	/**
	 * Ligne libre additionnelle pour les données d'adresse supplémentaires qui ne trouvent pas leur place dans les autres champs de l'adresse (p. ex. pour la mention c/o, etc.).
	 * Longueur maximum selon eCH-0010 : 60
	 */
	private String complement;

	/**
	 * Désignation de la rue en texte libre.
	 */
	private String rue;

	/**
	 * Numéro de la maison dans l'adresse postale, y compris des indications additionnelles.
	 * Longueur maximum selon eCH-0010 : 12
	 */
	private String numeroMaison;

	/**
	 * Numéro de l'appartement. Ce numéro est éventuellement nécessaire dans le cadre de grands ensembles.
	 * Longueur maximum selon eCH-0011 : 10
	 */
	private String numeroAppartement;

	/**
	 * Texte de la case postale dans la langue voulue.
	 * Dans la plupart des cas, le texte "Case postale" ou "Boîte postale" suffit.
	 * Longueur maximum selon eCH-0010 : 15
	 */
	private TexteCasePostale texteCasePostale;

	/**
	 * Numéro de la case postale
	 * Valeurs admises selon eCH-0010 : 0-9999
	 */
	private Integer numeroCasePostale;

	/**
	 * Adresse permanente
	 */
	private boolean permanente = false;

	@Override
	@Column(name = "COMPLEMENT", length = LengthConstants.ADRESSE_NOM)
	public String getComplement() {
		return complement;
	}

	public void setComplement(String theComplement) {
		complement = theComplement;
	}

	@Override
	@Column(name = "RUE", length = LengthConstants.ADRESSE_NOM)
	public String getRue() {
		return rue;
	}

	public void setRue(String theRue) {
		rue = theRue;
	}

	@Override
	@Column(name = "NUMERO_MAISON", length = LengthConstants.ADRESSE_NUM_MAISON)
	public String getNumeroMaison() {
		return numeroMaison;
	}

	public void setNumeroMaison(String theNumeroMaison) {
		numeroMaison = theNumeroMaison;
	}

	@Column(name = "NUMERO_APPARTEMENT", length = LengthConstants.ADRESSE_NUM)
	public String getNumeroAppartement() {
		return numeroAppartement;
	}

	public void setNumeroAppartement(String theNumeroAppartement) {
		numeroAppartement = theNumeroAppartement;
	}

	@Override
	@Column(name = "TEXTE_CASE_POSTALE", length = LengthConstants.ADRESSE_TYPESUPPLEM)
	@Type(type = "ch.vd.unireg.hibernate.TexteCasePostaleUserType")
	public TexteCasePostale getTexteCasePostale() {
		return texteCasePostale;
	}

	public void setTexteCasePostale(TexteCasePostale theTexteCasePostale) {
		texteCasePostale = theTexteCasePostale;
	}

	@Override
	@Column(name = "NUMERO_CASE_POSTALE")
	public Integer getNumeroCasePostale() {
		return numeroCasePostale;
	}

	public void setNumeroCasePostale(Integer theNumeroCasePostale) {
		numeroCasePostale = theNumeroCasePostale;
	}

	@Override
	@Column(name = "PERMANENTE")
	public boolean isPermanente() {
		return permanente;
	}

	public void setPermanente(boolean thePermanente) {
		permanente = thePermanente;
	}

	protected AdresseSupplementaire() {
	}

	protected AdresseSupplementaire(AdresseSupplementaire src) {
		super(src);
		this.complement = src.complement;
		this.rue = src.rue;
		this.numeroMaison = src.numeroMaison;
		this.numeroAppartement = src.numeroAppartement;
		this.numeroCasePostale = src.numeroCasePostale;
		this.permanente = src.permanente;
		this.texteCasePostale = src.texteCasePostale;
	}
}

