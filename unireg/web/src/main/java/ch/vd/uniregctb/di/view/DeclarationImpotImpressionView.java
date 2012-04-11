package ch.vd.uniregctb.di.view;

import java.util.List;

import ch.vd.uniregctb.type.TypeDocument;

public class DeclarationImpotImpressionView {

	private Long idDI;

	private TypeDocument selectedTypeDocument;

	private List<ModeleDocumentView> modelesDocumentView;

	public Long getIdDI() {
		return idDI;
	}

	public void setIdDI(Long idDI) {
		this.idDI = idDI;
	}

	public TypeDocument getSelectedTypeDocument() {
		return selectedTypeDocument;
	}

	public void setSelectedTypeDocument(TypeDocument selectedTypeDocument) {
		this.selectedTypeDocument = selectedTypeDocument;
	}

	public List<ModeleDocumentView> getModelesDocumentView() {
		return modelesDocumentView;
	}

	public void setModelesDocumentView(List<ModeleDocumentView> modelesDocumentView) {
		this.modelesDocumentView = modelesDocumentView;
	}



}