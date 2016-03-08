package ch.vd.uniregctb.declaration.ordinaire.pm;

import java.util.Set;

import ch.vd.uniregctb.type.GroupeTypesDocumentBatchLocal;
import ch.vd.uniregctb.type.TypeDocument;

/**
 * Les différents type de déclarations PM accessible au job d'envoi en masse
 */
public enum TypeDeclarationImpotPM {

	PM(GroupeTypesDocumentBatchLocal.DI_PM),
	APM(GroupeTypesDocumentBatchLocal.DI_APM);

	private final GroupeTypesDocumentBatchLocal groupe;

	TypeDeclarationImpotPM(GroupeTypesDocumentBatchLocal groupe) {
		this.groupe = groupe;
	}

	public Set<TypeDocument> getTypesDocument() {
		return groupe.getComposants();
	}
}
