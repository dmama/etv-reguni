package ch.vd.unireg.type;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum ModeleFeuille {

	//
	// Modèles pour les DI PP
	//

	ANNEXE_200(200, "Déclaration Hors canton immeuble", true, TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE),
	ANNEXE_210(210, "Déclaration d'impôt ordinaire complète", true, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL),
	ANNEXE_220(220, "Annexe 1", false, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL),
	ANNEXE_230(230, "Annexe 2 et 3", false, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL),
	ANNEXE_240(240, "Annexe 4 et 5", false, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL),
	ANNEXE_250(250, "Déclaration VaudTax", true, TypeDocument.DECLARATION_IMPOT_VAUDTAX),
	ANNEXE_270(270, "Déclaration Dépense", true, TypeDocument.DECLARATION_IMPOT_DEPENSE),
	ANNEXE_310(310, "Annexe 1-1", false, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL),
	ANNEXE_320(320, "Annexe 7", false, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL),
	ANNEXE_330(330, "Annexe 2 et 3", false, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL),

	//
	// Modèles pour les documents PM (APM)
	//

	ANNEXE_130(130, 11121, "Déclaration APM", true, TypeDocument.DECLARATION_IMPOT_APM_BATCH, TypeDocument.DECLARATION_IMPOT_APM_LOCAL),
	ANNEXE_132(132, 11123, "Annexe 01a", false, TypeDocument.DECLARATION_IMPOT_APM_BATCH, TypeDocument.DECLARATION_IMPOT_APM_LOCAL),
	ANNEXE_134(134, 11125, "Annexe 01b", false, TypeDocument.DECLARATION_IMPOT_APM_BATCH, TypeDocument.DECLARATION_IMPOT_APM_LOCAL),
	ANNEXE_136(136, 11127, "Annexe 02", false, TypeDocument.DECLARATION_IMPOT_APM_BATCH, TypeDocument.DECLARATION_IMPOT_APM_LOCAL),
	ANNEXE_137(137, 11128, "Annexe 03a", false, TypeDocument.DECLARATION_IMPOT_APM_BATCH, TypeDocument.DECLARATION_IMPOT_APM_LOCAL),
	ANNEXE_138(138, 11129, "Annexe 03b", false, TypeDocument.DECLARATION_IMPOT_APM_BATCH, TypeDocument.DECLARATION_IMPOT_APM_LOCAL),
	ANNEXE_139(139, 11130, "Annexe 04", false, TypeDocument.DECLARATION_IMPOT_APM_BATCH, TypeDocument.DECLARATION_IMPOT_APM_LOCAL),

	//
	// Modèles pour les documents PM (PM)
	//

	ANNEXE_140(140, 11110, "Déclaration PM", true, TypeDocument.DECLARATION_IMPOT_PM_BATCH, TypeDocument.DECLARATION_IMPOT_PM_LOCAL),
	ANNEXE_141(141, 11111, "Annexe 01a", false, TypeDocument.DECLARATION_IMPOT_PM_BATCH, TypeDocument.DECLARATION_IMPOT_PM_LOCAL),
	ANNEXE_142(142, 11112, "Annexe 01b", false, TypeDocument.DECLARATION_IMPOT_PM_BATCH, TypeDocument.DECLARATION_IMPOT_PM_LOCAL),
	ANNEXE_143(143, 11113, "Annexe 01c", false, TypeDocument.DECLARATION_IMPOT_PM_BATCH, TypeDocument.DECLARATION_IMPOT_PM_LOCAL),
	ANNEXE_144(144, 11114, "Annexe 01d", false, TypeDocument.DECLARATION_IMPOT_PM_BATCH, TypeDocument.DECLARATION_IMPOT_PM_LOCAL),
	ANNEXE_145(145, 11115, "Annexe 01e", false, TypeDocument.DECLARATION_IMPOT_PM_BATCH, TypeDocument.DECLARATION_IMPOT_PM_LOCAL),
	ANNEXE_146(146, 11116, "Annexe 02", false, TypeDocument.DECLARATION_IMPOT_PM_BATCH, TypeDocument.DECLARATION_IMPOT_PM_LOCAL),
	ANNEXE_147(147, 11117, "Annexe 03", false, TypeDocument.DECLARATION_IMPOT_PM_BATCH, TypeDocument.DECLARATION_IMPOT_PM_LOCAL),
	ANNEXE_148(148, 11118, "Annexe 04a", false, TypeDocument.DECLARATION_IMPOT_PM_BATCH, TypeDocument.DECLARATION_IMPOT_PM_LOCAL),
	ANNEXE_149(149, 11119, "Annexe 04b", false, TypeDocument.DECLARATION_IMPOT_PM_BATCH, TypeDocument.DECLARATION_IMPOT_PM_LOCAL),

	//
	// Modèle pour le document QSNC
	//

	ANNEXE_280(280, 21025, "Questionnaire SNC", true, TypeDocument.QUESTIONNAIRE_SNC),

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

	/**
	 * Numéro CADEV (4 chiffres max)
	 */
	private final int noCADEV;

	/**
	 * Numéro de formulaire ACI (5 chiffres max)
	 */
	private final Integer noFormulaireACI;

	/**
	 * <code>true</code> si l'annexe correspond au document principal
	 */
	private final boolean principal;

	/**
	 * Description textuelle de l'annexe
	 */
	private final String description;

	/**
	 * Types de document sur lesquels l'annexe est utilisable
	 */
	private final Set<TypeDocument> typesDocumentsAutorises;

	ModeleFeuille(int noCADEV, String description, boolean principal, TypeDocument... typesDocumentsAutorises) {
		this(noCADEV, null, description, principal, typesDocumentsAutorises);
	}

	ModeleFeuille(int noCADEV, Integer noFormulaireACI, String description, boolean principal, TypeDocument... typesDocumentsAutorises) {
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
		this.principal = principal;

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

	public boolean isPrincipal() {
		return principal;
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
		final String prn = principal ? " (feuillet principal)" : StringUtils.EMPTY;
		final String noACI = noFormulaireACI != null ? String.format(" (%05d)", noFormulaireACI) : StringUtils.EMPTY;
		return String.format("%04d - %s%s%s", noCADEV, description, noACI, prn);
	}
}
