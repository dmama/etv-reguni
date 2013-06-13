package ch.vd.uniregctb.di.view;

import java.util.List;

import ch.vd.uniregctb.declaration.ordinaire.ModeleFeuilleDocumentEditique;
import ch.vd.uniregctb.type.TypeDocument;

public class ModeleDocumentView {

	private TypeDocument typeDocument;
	private List<ModeleFeuilleDocumentEditique> modelesFeuilles;

	public TypeDocument getTypeDocument() {
		return typeDocument;
	}
	public void setTypeDocument(TypeDocument typeDocument) {
		this.typeDocument = typeDocument;
	}

	public List<ModeleFeuilleDocumentEditique> getModelesFeuilles() {
		return modelesFeuilles;
	}
	public void setModelesFeuilles(List<ModeleFeuilleDocumentEditique> modelesFeuilles) {
		this.modelesFeuilles = modelesFeuilles;
	}

}
