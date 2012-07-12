package ch.vd.uniregctb.param.view;

import ch.vd.uniregctb.type.ModeleFeuille;
import ch.vd.uniregctb.type.TypeDocument;


public class ModeleFeuilleDocumentView {
	
	private Long idPeriode;
	private Integer periodeAnnee;
	private Long idModele;
	private Long idFeuille;
	private TypeDocument modeleDocumentTypeDocument;
	private ModeleFeuille modeleFeuille;
	
	
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

	public ModeleFeuille getModeleFeuille() {
		return modeleFeuille;
	}

	public void setModeleFeuille(ModeleFeuille modeleFeuille) {
		this.modeleFeuille = modeleFeuille;
	}
}
