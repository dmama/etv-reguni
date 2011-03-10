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
	private AdresseGenerique.SourceType source;
	private Long representantId;

	public AdresseGenerique.SourceType getSource() {
		return source;
	}

	public void setSource(AdresseGenerique.SourceType source) {
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

	/**
	 * @return l'id du représentant associé à l'adresse de reprise (<b>null</b> dans le cas d'une adresse reprise du civil).
	 */
	public Long getRepresentantId() {
		return representantId;
	}

	public void setRepresentantId(Long representantId) {
		this.representantId = representantId;
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
