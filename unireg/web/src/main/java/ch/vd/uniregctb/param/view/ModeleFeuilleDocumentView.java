package ch.vd.uniregctb.param.view;

import ch.vd.uniregctb.type.TypeDocument;


public class ModeleFeuilleDocumentView {
	
	private Long idPeriode;
	private Integer periodeAnnee;
	private Long idModele;
	private Long idFeuille;
	private String intituleFeuille; 
	private String numeroFormulaire;
	private TypeDocument modeleDocumentTypeDocument;
	
	
	public void setModeleDocumentTypeDocument(TypeDocument modeleDocumentTypeDocument) {
		this.modeleDocumentTypeDocument = modeleDocumentTypeDocument;
	}
	public TypeDocument getModeleDocumentTypeDocument() {
		return modeleDocumentTypeDocument;
	}
	public Long getIdPeriode() {
		return idPeriode;
	}
	public void setIdPeriode(Long idPeriode) {
		this.idPeriode = idPeriode;
	}
	public Integer getPeriodeAnnee() {
		return periodeAnnee;
	}
	public void setPeriodeAnnee(Integer periodeAnnee) {
		this.periodeAnnee = periodeAnnee;
	}
	public Long getIdModele() {
		return idModele;
	}
	public void setIdModele(Long idModele) {
		this.idModele = idModele;
	}
	public Long getIdFeuille() {
		return idFeuille;
	}
	public void setIdFeuille(Long idFeuille) {
		this.idFeuille = idFeuille;
	}
	public String getIntituleFeuille() {
		return intituleFeuille;
	}
	public void setIntituleFeuille(String intituleFeuille) {
		this.intituleFeuille = intituleFeuille;
	}
	public String getNumeroFormulaire() {
		return numeroFormulaire;
	}
	public void setNumeroFormulaire(String numeroFormulaire) {
		this.numeroFormulaire = numeroFormulaire;
	}

	
}
