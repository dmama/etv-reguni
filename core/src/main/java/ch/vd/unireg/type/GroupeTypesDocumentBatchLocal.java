package ch.vd.uniregctb.type;

import java.util.EnumSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum GroupeTypesDocumentBatchLocal {

	DI_PP_COMPLETE(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL),
	DI_PM(TypeDocument.DECLARATION_IMPOT_PM_BATCH, TypeDocument.DECLARATION_IMPOT_PM_LOCAL),
	DI_APM(TypeDocument.DECLARATION_IMPOT_APM_BATCH, TypeDocument.DECLARATION_IMPOT_APM_LOCAL);

	private final TypeDocument batch;
	private final TypeDocument local;

	GroupeTypesDocumentBatchLocal(TypeDocument batch, TypeDocument local) {
		if (!batch.isForBatch() || local.isForBatch()) {
			throw new IllegalArgumentException("Mauvais mapping...");
		}
		this.batch = batch;
		this.local = local;
	}

	@Nullable
	public static GroupeTypesDocumentBatchLocal of(TypeDocument type) {
		for (GroupeTypesDocumentBatchLocal groupe : values()) {
			if (groupe.hasType(type)) {
				return groupe;
			}
		}
		return null;
	}

	public boolean hasType(TypeDocument type) {
		return type == batch || type == local;
	}

	@NotNull
	public Set<TypeDocument> getComposants() {
		return EnumSet.of(batch, local);
	}

	public TypeDocument getBatch() {
		return batch;
	}

	public TypeDocument getLocal() {
		return local;
	}
}
