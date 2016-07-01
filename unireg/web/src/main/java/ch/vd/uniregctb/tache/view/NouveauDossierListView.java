package ch.vd.uniregctb.tache.view;

import java.util.List;

import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.type.TypeEtatTache;

public class NouveauDossierListView implements Comparable<NouveauDossierListView>, Annulable {

	private Long id;

	private Long numero;

	private List<String> nomCourrier;

	private Integer numeroForGestion;

	private String officeImpot;

	private TypeEtatTache etatTache;

	boolean annule;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getNumero() {
		return numero;
	}

	public void setNumero(Long numero) {
		this.numero = numero;
	}

	public List<String> getNomCourrier() {
		return nomCourrier;
	}

	public void setNomCourrier(List<String> nomCourrier) {
		this.nomCourrier = nomCourrier;
	}

	public Integer getNumeroForGestion() {
		return numeroForGestion;
	}

	public void setNumeroForGestion(Integer numeroForGestion) {
		this.numeroForGestion = numeroForGestion;
	}

	public String getOfficeImpot() {
		return officeImpot;
	}

	public void setOfficeImpot(String officeImpot) {
		this.officeImpot = officeImpot;
	}

	public TypeEtatTache getEtatTache() {
		return etatTache;
	}

	public void setEtatTache(TypeEtatTache etatTache) {
		this.etatTache = etatTache;
	}

	public boolean isAnnule() {
		return annule;
	}

	public void setAnnule(boolean annule) {
		this.annule = annule;
	}

	@Override
	public int compareTo(NouveauDossierListView o) {
		return o.numero.compareTo(numero);
	}

}
