package ch.vd.uniregctb.adresse;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import ch.vd.uniregctb.common.LengthConstants;

@Entity
@DiscriminatorValue("AdresseEtrangere")
public class AdresseEtrangere extends AdresseSupplementaire {

	/**
	 * Données complémentaires quant au lieu, comme par exemple la région, la province, l'état fédéral ou le quartier.
	 * Longueur selon eCH-0010 : 40
	 */
	private String complementLocalite;

	/**
	 * @return the complementLocalite
	 */
	@Column(name = "COMPLEMENT_LOCALITE", length = LengthConstants.ADRESSE_NOM)
	public String getComplementLocalite() {
		return complementLocalite;
	}

	/**
	 * @param theComplementLocalite the complementLocalite to set
	 */
	public void setComplementLocalite(String theComplementLocalite) {
		complementLocalite = theComplementLocalite;
	}

	/**
	 * Lieu de l'adresse.
	 * Longueur selon eCH-0010 : 40
	 */
	private String numeroPostalLocalite;

	/**
	 * @return the numeroPostalLocalite
	 */
	@Column(name = "NUMERO_POSTAL_LOCALITE", length = LengthConstants.ADRESSE_NUM)
	public String getNumeroPostalLocalite() {
		return numeroPostalLocalite;
	}

	/**
	 * @param theNumeroPostalLocalite the numeroPostalLocalite to set
	 */
	public void setNumeroPostalLocalite(String theNumeroPostalLocalite) {
		numeroPostalLocalite = theNumeroPostalLocalite;
	}

	/**
	 * Abréviation ISO 3166-1 du pays dans lequel se trouve le lieu faisant partie de l'adresse postale.
	 * Longueur selon eCH-0010 : 2
	 */
	private Integer numeroOfsPays;

	/**
	 * @return the numeroOfsPays
	 */
	@Column(name = "NUMERO_OFS_PAYS")
	public Integer getNumeroOfsPays() {
		return numeroOfsPays;
	}

	/**
	 * @param theNumeroOfsPays the numeroOfsPays to set
	 */
	public void setNumeroOfsPays(Integer theNumeroOfsPays) {
		numeroOfsPays = theNumeroOfsPays;
	}

	public AdresseEtrangere() {
	}

	protected AdresseEtrangere(AdresseEtrangere src) {
		super(src);
		this.complementLocalite = src.complementLocalite;
		this.numeroPostalLocalite = src.numeroPostalLocalite;
		this.numeroOfsPays = src.numeroOfsPays;
	}

	@Override
	public AdresseEtrangere duplicate() {
		return new AdresseEtrangere(this);
	}
}
