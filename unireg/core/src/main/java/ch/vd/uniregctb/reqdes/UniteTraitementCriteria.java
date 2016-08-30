package ch.vd.uniregctb.reqdes;

import ch.vd.registre.base.date.RegDate;

public class UniteTraitementCriteria {

	private String numeroMinute;
	private String visaNotaire;
	private RegDate dateTraitementMin;
	private RegDate dateTraitementMax;
	private EtatTraitement etatTraitement;
	private RegDate dateReceptionMin;
	private RegDate dateReceptionMax;
	private RegDate dateActeMin;
	private RegDate dateActeMax;

	public String getNumeroMinute() {
		return numeroMinute;
	}

	public void setNumeroMinute(String numeroMinute) {
		this.numeroMinute = numeroMinute;
	}

	public String getVisaNotaire() {
		return visaNotaire;
	}

	public void setVisaNotaire(String visaNotaire) {
		this.visaNotaire = visaNotaire;
	}

	public RegDate getDateTraitementMin() {
		return dateTraitementMin;
	}

	public void setDateTraitementMin(RegDate dateTraitementMin) {
		this.dateTraitementMin = dateTraitementMin;
	}

	public RegDate getDateTraitementMax() {
		return dateTraitementMax;
	}

	public void setDateTraitementMax(RegDate dateTraitementMax) {
		this.dateTraitementMax = dateTraitementMax;
	}

	public EtatTraitement getEtatTraitement() {
		return etatTraitement;
	}

	public void setEtatTraitement(EtatTraitement etatTraitement) {
		this.etatTraitement = etatTraitement;
	}

	public RegDate getDateReceptionMin() {
		return dateReceptionMin;
	}

	public void setDateReceptionMin(RegDate dateReceptionMin) {
		this.dateReceptionMin = dateReceptionMin;
	}

	public RegDate getDateReceptionMax() {
		return dateReceptionMax;
	}

	public void setDateReceptionMax(RegDate dateReceptionMax) {
		this.dateReceptionMax = dateReceptionMax;
	}

	public RegDate getDateActeMin() {
		return dateActeMin;
	}

	public void setDateActeMin(RegDate dateActeMin) {
		this.dateActeMin = dateActeMin;
	}

	public RegDate getDateActeMax() {
		return dateActeMax;
	}

	public void setDateActeMax(RegDate dateActeMax) {
		this.dateActeMax = dateActeMax;
	}
}
