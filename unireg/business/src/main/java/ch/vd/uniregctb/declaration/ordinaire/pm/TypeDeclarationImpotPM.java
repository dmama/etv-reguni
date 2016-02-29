package ch.vd.uniregctb.declaration.ordinaire.pm;

import ch.vd.uniregctb.type.TypeDocument;

/**
 * Les différents type de déclarations PM accessible au job d'envoi en masse
 */
public enum TypeDeclarationImpotPM {

	PM(TypeDocument.DECLARATION_IMPOT_PM),
	APM(TypeDocument.DECLARATION_IMPOT_APM);

	private final TypeDocument typeDocument;

	TypeDeclarationImpotPM(TypeDocument typeDocument) {
		this.typeDocument = typeDocument;
	}

	public TypeDocument getTypeDocument() {
		return typeDocument;
	}
}
