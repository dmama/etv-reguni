package ch.vd.unireg.evenement.reqdes;

import java.io.Serializable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.reqdes.EtatTraitement;

/**
 * Critères de recherche des unités de traitement
 */
public class ReqDesCriteriaView implements Serializable {

	private static final long serialVersionUID = -1730923816686810915L;

	private String numeroMinute;
	private String visaNotaire;
	private RegDate dateTraitementMin;
	private RegDate dateTraitementMax;
	private EtatTraitement etat;
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

	public EtatTraitement getEtat() {
		return etat;
	}

	public void setEtat(EtatTraitement etat) {
		this.etat = etat;
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
