package ch.vd.uniregctb.adresse;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

@Entity
@DiscriminatorValue(value = "AdresseMandataireSuisse")
public class AdresseMandataireSuisse extends AdresseMandataire implements AdresseFiscaleSuisse {

	private Integer numeroRue;
	private Integer numeroOrdrePoste;
	private Integer npaCasePostale;

	public AdresseMandataireSuisse() {
	}

	public AdresseMandataireSuisse(AdresseMandataireSuisse src) {
		super(src);
		this.numeroRue = src.numeroRue;
		this.numeroOrdrePoste = src.numeroOrdrePoste;
		this.npaCasePostale = src.npaCasePostale;
	}

	@Transient
	@Override
	public AdresseMandataireSuisse duplicate() {
		return new AdresseMandataireSuisse(this);
	}

	@Override
	@Column(name = "NUMERO_RUE")
	public Integer getNumeroRue() {
		return numeroRue;
	}

	public void setNumeroRue(Integer numeroRue) {
		this.numeroRue = numeroRue;
	}

	@Override
	@Column(name = "NUMERO_ORDRE_POSTE")
	public Integer getNumeroOrdrePoste() {
		return numeroOrdrePoste;
	}

	public void setNumeroOrdrePoste(Integer numeroOrdrePoste) {
		this.numeroOrdrePoste = numeroOrdrePoste;
	}

	@Override
	@Column(name = "NPA_CASE_POSTALE")
	public Integer getNpaCasePostale() {
		return npaCasePostale;
	}

	public void setNpaCasePostale(Integer npaCasePostale) {
		this.npaCasePostale = npaCasePostale;
	}
}
