package ch.vd.unireg.metier.assujettissement;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.type.GroupeTypesDocumentBatchLocal;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;

/**
 * Les différentes catégories de population dans le cadre des envois des DIs PM en masse.
 */
public enum CategorieEnvoiDIPM {

	DI_PM(GroupeTypesDocumentBatchLocal.DI_PM, "Déclaration PM", TypeContribuable.VAUDOIS_ORDINAIRE, TypeContribuable.HORS_CANTON, TypeContribuable.HORS_SUISSE),
	DI_APM(GroupeTypesDocumentBatchLocal.DI_APM, "Déclaration APM", TypeContribuable.VAUDOIS_ORDINAIRE, TypeContribuable.HORS_CANTON, TypeContribuable.HORS_SUISSE),
	DI_PM_UTILITE_PUBLIQUE(GroupeTypesDocumentBatchLocal.DI_PM, "Déclaration PM (LIASF)", TypeContribuable.UTILITE_PUBLIQUE),
	DI_APM_UTILITE_PUBLIQUE(GroupeTypesDocumentBatchLocal.DI_APM, "Déclaration APM (LIASF)", TypeContribuable.UTILITE_PUBLIQUE),
	;

	private final GroupeTypesDocumentBatchLocal groupeBatchLocal;
	private final Set<TypeContribuable> typesContribuables;
	private final String description;

	CategorieEnvoiDIPM(@NotNull GroupeTypesDocumentBatchLocal groupeBatchLocal, @NotNull String description, TypeContribuable... typesContribuables) {
		this.groupeBatchLocal = groupeBatchLocal;
		this.description = description;
		this.typesContribuables = EnumSet.noneOf(TypeContribuable.class);
		if (typesContribuables != null) {
			Collections.addAll(this.typesContribuables, typesContribuables);
		}
	}

	@NotNull
	public Set<TypeDocument> getTypesDocument() {
		return groupeBatchLocal.getComposants();
	}

	@NotNull
	public Set<TypeContribuable> getTypesContribuables() {
		return Collections.unmodifiableSet(typesContribuables);
	}

	@NotNull
	public String getDescription() {
		return description;
	}
}
