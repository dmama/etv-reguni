package ch.vd.unireg.tiers.view;

import ch.vd.registre.base.date.RegDate;

public class EtablissementCivilView {

	private String sDateDebut;
	private RegDate dateDebut;
	private String sDateFin;
	private RegDate dateFin;
	private String raisonSociale;
	private String nomEnseigne;
	private String nomCommune;
	private Integer noOfsCommune;
	private String numeroIDE;

	public EtablissementCivilView() {
	}

	public EtablissementCivilView(String raisonSociale) {
		this.raisonSociale = raisonSociale;
	}

	public String getsDateDebut() {
		return sDateDebut;
	}

	public void setsDateDebut(String sDateDebut) {
		this.sDateDebut = sDateDebut;
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	public String getsDateFin() {
		return sDateFin;
	}

	public void setsDateFin(String sDateFin) {
		this.sDateFin = sDateFin;
	}

	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	public String getNomEnseigne() {
		return nomEnseigne;
	}

	public String getRaisonSociale() {
		return raisonSociale;
	}

	public void setRaisonSociale(String raisonSociale) {
		this.raisonSociale = raisonSociale;
	}

	public void setNomEnseigne(String nomEnseigne) {
		this.nomEnseigne = nomEnseigne;
	}

	public String getNomCommune() {
		return nomCommune;
	}

	public void setNomCommune(String nomCommune) {
		this.nomCommune = nomCommune;
	}

	public Integer getNoOfsCommune() {
		return noOfsCommune;
	}

	public void setNoOfsCommune(Integer noOfsCommune) {
		this.noOfsCommune = noOfsCommune;
	}

	public String getNumeroIDE() {
		return numeroIDE;
	}

	public void setNumeroIDE(String numeroIDE) {
		this.numeroIDE = numeroIDE;
	}
}
