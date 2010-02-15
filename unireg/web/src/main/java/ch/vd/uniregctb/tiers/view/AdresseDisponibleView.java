/**
 *
 */
package ch.vd.uniregctb.tiers.view;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.uniregctb.adresse.AdresseGenerique;

/**
 * @author xsikce
 *
 */
public class AdresseDisponibleView {


	private EnumTypeAdresse typeAdresse;

	private String representantLegal ;

	private Integer numeroCasePostale;

	private String rue;

	private String localite;

	private String paysNpa;

	private AdresseGenerique.Source source;

	private Long numeroTiers;

	public AdresseGenerique.Source getSource() {
		return source;
	}

	public void setSource(AdresseGenerique.Source source) {
		this.source = source;
	}


	public EnumTypeAdresse getTypeAdresse() {
		return typeAdresse;
	}

	public void setTypeAdresse(EnumTypeAdresse typeAdresse) {
		this.typeAdresse = typeAdresse;
	}

	public String getRepresentantLegal() {
		return representantLegal;
	}

	public void setRepresentantLegal(String representantLegal) {
		this.representantLegal = representantLegal;
	}

	public Integer getNumeroCasePostale() {
		return numeroCasePostale;
	}

	public void setNumeroCasePostale(Integer numeroCasePostale) {
		this.numeroCasePostale = numeroCasePostale;
	}

	public String getRue() {
		return rue;
	}

	public void setRue(String rue) {
		this.rue = rue;
	}

	public String getLocalite() {
		return localite;
	}

	public void setLocalite(String localite) {
		this.localite = localite;
	}

	public String getPaysNpa() {
		return paysNpa;
	}

	public void setPaysNpa(String paysNpa) {
		this.paysNpa = paysNpa;
	}

	public Long getNumeroTiers() {
		return numeroTiers;
	}

	public void setNumeroTiers(Long numeroTiers) {
		this.numeroTiers = numeroTiers;
	}

	public String getTypeAdresseToString() {

		if(getTypeAdresse()!= null){
				return getTypeAdresse().getName();

		}else {
			return null ;
		}
	}
}
