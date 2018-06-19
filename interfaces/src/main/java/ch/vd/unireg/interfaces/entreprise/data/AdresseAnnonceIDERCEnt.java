package ch.vd.unireg.interfaces.entreprise.data;

import java.io.Serializable;

/**
 * @author Raphaël Marmier, 2016-08-26, <raphael.marmier@vd.ch>
 */
public class AdresseAnnonceIDERCEnt implements AdresseAnnonceIDE, Serializable {

	private static final long serialVersionUID = 2363783059825198687L;

	/*
		Numéro fédéral du bâtiment
	 */
	private Integer egid;

	/*
		La rue
	 */
	private String rue;
	/*
		Numéro de la maison
	 */
	private String numero;
	/*
		Numéro de l'appartement
	 */
	private String numeroAppartement;

	/*
		Numéro de la case postale
	 */
	private Integer numeroCasePostale;
	/*
		Texte de la case postale dans la langue voulue. Cette mention peut figurer sans numéro de case postale.
	 */
	private String texteCasePostale;

	/*
		Localité postale. Désignation du lieu.
	 */
	private String ville;
	/*
		Numéro postal d'acheminement (Suisse).
	 */
	private Integer npa;
	/*
		Numéro postal d'acheminement (Etranger).
	 */
	private String npaEtranger;

	/*
		Le numéro d'ordre postal (SwissZipCodeId pour RCEnt)
	 */
	private Integer numeroOrdrePostal;

	/*
		Le pays.
	 */
	private AdresseAnnonceIDE.Pays pays;


	@Override
	public Integer getEgid() {
		return egid;
	}

	public void setEgid(Integer egid) {
		this.egid = egid;
	}

	@Override
	public String getRue() {
		return rue;
	}

	public void setRue(String rue) {
		this.rue = rue;
	}

	@Override
	public String getNumero() {
		return numero;
	}

	public void setNumero(String numero) {
		this.numero = numero;
	}

	@Override
	public String getNumeroAppartement() {
		return numeroAppartement;
	}

	public void setNumeroAppartement(String numeroAppartement) {
		this.numeroAppartement = numeroAppartement;
	}

	@Override
	public Integer getNumeroCasePostale() {
		return numeroCasePostale;
	}

	public void setNumeroCasePostale(Integer numeroCasePostale) {
		this.numeroCasePostale = numeroCasePostale;
	}

	@Override
	public String getTexteCasePostale() {
		return texteCasePostale;
	}

	public void setTexteCasePostale(String texteCasePostale) {
		this.texteCasePostale = texteCasePostale;
	}

	@Override
	public String getVille() {
		return ville;
	}

	public void setVille(String ville) {
		this.ville = ville;
	}

	@Override
	public Integer getNpa() {
		return npa;
	}

	public void setNpa(Integer npa) {
		this.npa = npa;
	}

	@Override
	public String getNpaEtranger() {
		return npaEtranger;
	}

	public void setNpaEtranger(String npaEtranger) {
		this.npaEtranger = npaEtranger;
	}

	@Override
	public Integer getNumeroOrdrePostal() {
		return numeroOrdrePostal;
	}

	public void setNumeroOrdrePostal(Integer numeroOrdrePostal) {
		this.numeroOrdrePostal = numeroOrdrePostal;
	}

	@Override
	public Pays getPays() {
		return pays;
	}

	public void setPays(Pays pays) {
		this.pays = pays;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final AdresseAnnonceIDERCEnt that = (AdresseAnnonceIDERCEnt) o;

		if (getEgid() != null ? !getEgid().equals(that.getEgid()) : that.getEgid() != null) return false;
		if (getRue() != null ? !getRue().equals(that.getRue()) : that.getRue() != null) return false;
		if (getNumero() != null ? !getNumero().equals(that.getNumero()) : that.getNumero() != null) return false;
		if (getNumeroAppartement() != null ? !getNumeroAppartement().equals(that.getNumeroAppartement()) : that.getNumeroAppartement() != null) return false;
		if (getNumeroCasePostale() != null ? !getNumeroCasePostale().equals(that.getNumeroCasePostale()) : that.getNumeroCasePostale() != null) return false;
		if (getTexteCasePostale() != null ? !getTexteCasePostale().equals(that.getTexteCasePostale()) : that.getTexteCasePostale() != null) return false;
		if (getVille() != null ? !getVille().equals(that.getVille()) : that.getVille() != null) return false;
		if (getNpa() != null ? !getNpa().equals(that.getNpa()) : that.getNpa() != null) return false;
		if (getNpaEtranger() != null ? !getNpaEtranger().equals(that.getNpaEtranger()) : that.getNpaEtranger() != null) return false;
		if (getNumeroOrdrePostal() != null ? !getNumeroOrdrePostal().equals(that.getNumeroOrdrePostal()) : that.getNumeroOrdrePostal() != null) return false;
		return getPays() != null ? getPays().equals(that.getPays()) : that.getPays() == null;
	}

	@Override
	public int hashCode() {
		int result = getEgid() != null ? getEgid().hashCode() : 0;
		result = 31 * result + (getRue() != null ? getRue().hashCode() : 0);
		result = 31 * result + (getNumero() != null ? getNumero().hashCode() : 0);
		result = 31 * result + (getNumeroAppartement() != null ? getNumeroAppartement().hashCode() : 0);
		result = 31 * result + (getNumeroCasePostale() != null ? getNumeroCasePostale().hashCode() : 0);
		result = 31 * result + (getTexteCasePostale() != null ? getTexteCasePostale().hashCode() : 0);
		result = 31 * result + (getVille() != null ? getVille().hashCode() : 0);
		result = 31 * result + (getNpa() != null ? getNpa().hashCode() : 0);
		result = 31 * result + (getNpaEtranger() != null ? getNpaEtranger().hashCode() : 0);
		result = 31 * result + (getNumeroOrdrePostal() != null ? getNumeroOrdrePostal().hashCode() : 0);
		result = 31 * result + (getPays() != null ? getPays().hashCode() : 0);
		return result;
	}

	public static class PaysRCEnt implements AdresseAnnonceIDE.Pays, Serializable {

		private static final long serialVersionUID = 710711448241742510L;

		protected final Integer noOfs;
		protected final String codeISO2;
		protected final String nomCourt;

		public PaysRCEnt(Integer noOfs, String codeISO2, String nomCourt) {
			this.noOfs = noOfs;
			this.codeISO2 = codeISO2;
			this.nomCourt = nomCourt;
		}

		public Integer getNoOfs() {
			return noOfs;
		}

		public String getCodeISO2() {
			return codeISO2;
		}

		public String getNomCourt() {
			return nomCourt;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final PaysRCEnt paysRCEnt = (PaysRCEnt) o;

			if (getNoOfs() != null ? !getNoOfs().equals(paysRCEnt.getNoOfs()) : paysRCEnt.getNoOfs() != null) return false;
			if (getCodeISO2() != null ? !getCodeISO2().equals(paysRCEnt.getCodeISO2()) : paysRCEnt.getCodeISO2() != null) return false;
			return getNomCourt() != null ? getNomCourt().equals(paysRCEnt.getNomCourt()) : paysRCEnt.getNomCourt() == null;

		}

		@Override
		public int hashCode() {
			int result = getNoOfs() != null ? getNoOfs().hashCode() : 0;
			result = 31 * result + (getCodeISO2() != null ? getCodeISO2().hashCode() : 0);
			result = 31 * result + (getNomCourt() != null ? getNomCourt().hashCode() : 0);
			return result;
		}
	}
}
