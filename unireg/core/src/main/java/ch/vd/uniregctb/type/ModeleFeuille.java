package ch.vd.uniregctb.type;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum ModeleFeuille {

	//
	// Modèles pour les DI PP
	//

	ANNEXE_200(200, "Déclaration Hors canton immeuble", TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE),
	ANNEXE_210(210, "Déclaration d'impôt ordinaire complète", TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL),
	ANNEXE_220(220, "Annexe 1", TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL),
	ANNEXE_230(230, "Annexe 2 et 3", TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL),
	ANNEXE_240(240, "Annexe 4 et 5", TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL),
	ANNEXE_250(250, "Déclaration VaudTax", TypeDocument.DECLARATION_IMPOT_VAUDTAX),
	ANNEXE_270(270, "Déclaration Dépense", TypeDocument.DECLARATION_IMPOT_DEPENSE),
	ANNEXE_310(310, "Annexe 1-1", TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL),
	ANNEXE_320(320, "Annexe 7", TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL),
	ANNEXE_330(330, "Annexe 2 et 3", TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL),

	//
	// Modèles pour les documents PM (APM)
	//

	ANNEXE_130(130, 11121, "Déclaration APM", TypeDocument.DECLARATION_IMPOT_APM_BATCH, TypeDocument.DECLARATION_IMPOT_APM_LOCAL),
	ANNEXE_132(132, 11123, "Annexe 01a", TypeDocument.DECLARATION_IMPOT_APM_BATCH, TypeDocument.DECLARATION_IMPOT_APM_LOCAL),
	ANNEXE_134(134, 11125, "Annexe 01b", TypeDocument.DECLARATION_IMPOT_APM_BATCH, TypeDocument.DECLARATION_IMPOT_APM_LOCAL),
	ANNEXE_136(136, 11127, "Annexe 02", TypeDocument.DECLARATION_IMPOT_APM_BATCH, TypeDocument.DECLARATION_IMPOT_APM_LOCAL),
	ANNEXE_137(137, 11128, "Annexe 03a", TypeDocument.DECLARATION_IMPOT_APM_BATCH, TypeDocument.DECLARATION_IMPOT_APM_LOCAL),
	ANNEXE_138(138, 11129, "Annexe 03b", TypeDocument.DECLARATION_IMPOT_APM_BATCH, TypeDocument.DECLARATION_IMPOT_APM_LOCAL),
	ANNEXE_139(139, 11130, "Annexe 04", TypeDocument.DECLARATION_IMPOT_APM_BATCH, TypeDocument.DECLARATION_IMPOT_APM_LOCAL),

	//
	// Modèles pour les documents PM (PM)
	//

	ANNEXE_140(140, 11110, "Déclaration PM", TypeDocument.DECLARATION_IMPOT_PM_BATCH, TypeDocument.DECLARATION_IMPOT_PM_LOCAL),
	ANNEXE_141(141, 11111, "Annexe 01a", TypeDocument.DECLARATION_IMPOT_PM_BATCH, TypeDocument.DECLARATION_IMPOT_PM_LOCAL),
	ANNEXE_142(142, 11112, "Annexe 01b", TypeDocument.DECLARATION_IMPOT_PM_BATCH, TypeDocument.DECLARATION_IMPOT_PM_LOCAL),
	ANNEXE_143(143, 11113, "Annexe 01c", TypeDocument.DECLARATION_IMPOT_PM_BATCH, TypeDocument.DECLARATION_IMPOT_PM_LOCAL),
	ANNEXE_144(144, 11114, "Annexe 01d", TypeDocument.DECLARATION_IMPOT_PM_BATCH, TypeDocument.DECLARATION_IMPOT_PM_LOCAL),
	ANNEXE_145(145, 11115, "Annexe 01e", TypeDocument.DECLARATION_IMPOT_PM_BATCH, TypeDocument.DECLARATION_IMPOT_PM_LOCAL),
	ANNEXE_146(146, 11116, "Annexe 02", TypeDocument.DECLARATION_IMPOT_PM_BATCH, TypeDocument.DECLARATION_IMPOT_PM_LOCAL),
	ANNEXE_147(147, 11117, "Annexe 03", TypeDocument.DECLARATION_IMPOT_PM_BATCH, TypeDocument.DECLARATION_IMPOT_PM_LOCAL),
	ANNEXE_148(148, 11118, "Annexe 04a", TypeDocument.DECLARATION_IMPOT_PM_BATCH, TypeDocument.DECLARATION_IMPOT_PM_LOCAL),
	ANNEXE_149(149, 11119, "Annexe 04b", TypeDocument.DECLARATION_IMPOT_PM_BATCH, TypeDocument.DECLARATION_IMPOT_PM_LOCAL),

	;

	private static final Map<Integer, ModeleFeuille> byNoCADEV = buildMapByNoCADEV();

	private static Map<Integer, ModeleFeuille> buildMapByNoCADEV() {
		final Map<Integer, ModeleFeuille> map = new HashMap<>();
		for (ModeleFeuille modele : ModeleFeuille.values()) {
			final ModeleFeuille old = map.put(modele.getNoCADEV(), modele);
			if (old != null) {
				throw new IllegalArgumentException("Code CADEV utilisé plusieurs fois : " + old.getNoCADEV());
			}
		}
		return map;
	}

	//
	// Membres
	//

	private final int noCADEV;
	private final Integer noFormulaireACI;
	private final String description;
	private final Set<TypeDocument> typesDocumentsAutorises;

	ModeleFeuille(int noCADEV, String description, TypeDocument... typesDocumentsAutorises) {
		this(noCADEV, null, description, typesDocumentsAutorises);
	}

	ModeleFeuille(int noCADEV, Integer noFormulaireACI, String description, TypeDocument... typesDocumentsAutorises) {
		// blindages...
		if (noCADEV < 0 || noCADEV > 9999) {
			// 4 chiffres décimaux seulement...
			throw new IllegalArgumentException("Numéro CADEV invalide (4 chiffres autorisés) : " + noCADEV);
		}
		if (noFormulaireACI != null && (noFormulaireACI < 0 || noFormulaireACI > 99999)) {
			// 5 chiffres décimaux seulement...
			throw new IllegalArgumentException("Numéro de formulaire ACI invalide (5 chiffres autorisés) : " + noFormulaireACI);
		}

		this.noCADEV = noCADEV;
		this.noFormulaireACI = noFormulaireACI;
		this.description = description;

		this.typesDocumentsAutorises = EnumSet.noneOf(TypeDocument.class);
		if (typesDocumentsAutorises != null) {
			Collections.addAll(this.typesDocumentsAutorises, typesDocumentsAutorises);
		}
	}

	public int getNoCADEV() {
		return noCADEV;
	}

	@Nullable
	public Integer getNoFormulaireACI() {
		return noFormulaireACI;
	}

	public String getDescription() {
		return description;
	}

	public static ModeleFeuille fromNoCADEV(int noCADEV) {
		return byNoCADEV.get(noCADEV);
	}

	@NotNull
	public static Set<ModeleFeuille> forDocument(TypeDocument typeDocument) {
		final Set<ModeleFeuille> set = EnumSet.noneOf(ModeleFeuille.class);
		for (ModeleFeuille mf : values()) {
			if (mf.typesDocumentsAutorises.contains(typeDocument)) {
				set.add(mf);
			}
		}
		return set;
	}

	@Override
	public String toString() {
		if (noFormulaireACI == null) {
			return String.format("%04d - %s", noCADEV, description);
		}
		else {
			return String.format("%04d - %s (%05d)", noCADEV, description, noFormulaireACI);
		}
	}
}
