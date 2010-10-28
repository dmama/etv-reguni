/**
 *
 */
package ch.vd.uniregctb.tiers.view;

import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.type.TypeAdresseCivil;

/**
 * @author xsikce
 */
public class AdresseDisponibleView {

	private TypeAdresseCivil typeAdresse;
	private String representantLegal;
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

	public TypeAdresseCivil getTypeAdresse() {
		return typeAdresse;
	}

	public void setTypeAdresse(TypeAdresseCivil typeAdresse) {
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
		if (typeAdresse == null) {
			return null;
		}
		else {
			return typeAdresse.name();
		}
	}
}
