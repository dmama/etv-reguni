package ch.vd.unireg.adresse;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.unireg.common.LengthConstants;

@Entity
@DiscriminatorValue("AdresseMandataireEtrangere")
public class AdresseMandataireEtrangere extends AdresseMandataire implements AdresseFiscaleEtrangere {

	private String complementLocalite;
	private String numeroPostalLocalite;
	private Integer numeroOfsPays;

	public AdresseMandataireEtrangere() {
	}

	public AdresseMandataireEtrangere(AdresseMandataireEtrangere src) {
		super(src);
		this.complementLocalite = src.complementLocalite;
		this.numeroPostalLocalite = src.numeroPostalLocalite;
		this.numeroOfsPays = src.numeroOfsPays;
	}

	@Transient
	@Override
	public AdresseMandataireEtrangere duplicate() {
		return new AdresseMandataireEtrangere(this);
	}

	@Override
	@Column(name = "COMPLEMENT_LOCALITE", length = LengthConstants.ADRESSE_NOM)
	public String getComplementLocalite() {
		return complementLocalite;
	}

	public void setComplementLocalite(String complementLocalite) {
		this.complementLocalite = complementLocalite;
	}

	@Override
	@Column(name = "NUMERO_POSTAL_LOCALITE", length = LengthConstants.ADRESSE_NUM)
	public String getNumeroPostalLocalite() {
		return numeroPostalLocalite;
	}

	public void setNumeroPostalLocalite(String numeroPostalLocalite) {
		this.numeroPostalLocalite = numeroPostalLocalite;
	}

	@Override
	@Column(name = "NUMERO_OFS_PAYS")
	public Integer getNumeroOfsPays() {
		return numeroOfsPays;
	}

	public void setNumeroOfsPays(Integer numeroOfsPays) {
		this.numeroOfsPays = numeroOfsPays;
	}
}
