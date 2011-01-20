package ch.vd.uniregctb.tache.view;

import java.util.List;

import ch.vd.uniregctb.common.NomCourrierViewPart;
import ch.vd.uniregctb.type.TypeEtatTache;

public class NouveauDossierListView implements Comparable<NouveauDossierListView>{

	private Long id;

	private Long numero;

	private final NomCourrierViewPart nomCourrier = new NomCourrierViewPart();

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

	public String getNomCourrier1() {
		return this.nomCourrier.getNomCourrier1();
	}

	public void setNomCourrier1(String nomCourrier1) {
		this.nomCourrier.setNomCourrier1(nomCourrier1);
	}

	public String getNomCourrier2() {
		return this.nomCourrier.getNomCourrier2();
	}

	public void setNomCourrier2(String nomCourrier2) {
		this.nomCourrier.setNomCourrier2(nomCourrier2);
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

	public void setNomCourrier(List<String> nomCourrier) {
		this.nomCourrier.setNomCourrier(nomCourrier);
	}

	public boolean isAnnule() {
		return annule;
	}

	public void setAnnule(boolean annule) {
		this.annule = annule;
	}

	public int compareTo(NouveauDossierListView o) {
		return o.numero.compareTo(numero);
	}

}
