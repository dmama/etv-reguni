package ch.vd.uniregctb.di.view;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import ch.vd.uniregctb.type.GroupeTypesDocumentBatchLocal;
import ch.vd.uniregctb.type.TypeDocument;

public enum TypeDeclaration {

	DI_PP(GroupeTypesDocumentBatchLocal.DI_PP_COMPLETE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL, TypeDocument.DECLARATION_IMPOT_DEPENSE, TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, TypeDocument.DECLARATION_IMPOT_VAUDTAX),
	DI_PM(GroupeTypesDocumentBatchLocal.DI_PM, TypeDocument.DECLARATION_IMPOT_PM_BATCH, TypeDocument.DECLARATION_IMPOT_PM_LOCAL),
	DI_APM(GroupeTypesDocumentBatchLocal.DI_APM, TypeDocument.DECLARATION_IMPOT_APM_BATCH, TypeDocument.DECLARATION_IMPOT_APM_LOCAL),

	;

	private final GroupeTypesDocumentBatchLocal batchLocal;
	private final Set<TypeDocument> typesDocument;

	TypeDeclaration(GroupeTypesDocumentBatchLocal batchLocal, TypeDocument... population) {
		this.batchLocal = batchLocal;
		this.typesDocument = EnumSet.noneOf(TypeDocument.class);
		if (population != null) {
			Collections.addAll(this.typesDocument, population);
		}
	}

	public GroupeTypesDocumentBatchLocal getBatchLocal() {
		return batchLocal;
	}

	public Set<TypeDocument> getTypesDocument() {
		return Collections.unmodifiableSet(typesDocument);
	}

	public static TypeDeclaration of(GroupeTypesDocumentBatchLocal batchLocal) {
		for (TypeDeclaration type : values()) {
			if (type.getBatchLocal() == batchLocal) {
				return type;
			}
		}
		throw new IllegalArgumentException("Pas de TypeDeclaration défini pour le groupe " + batchLocal);
	}
}
