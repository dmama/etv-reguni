package ch.vd.uniregctb.metier.assujettissement;

import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;

/**
 * Les différentes catégories de population dans le cadre des envois des DIs en masse (voir [UNIREG-1976]).
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public enum CategorieEnvoiDI {

	/**
	 * Vaudois ordinaires (DI complète)
	 */
	VAUDOIS_COMPLETE(TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, "Vaudois ordinaires (DI complète)"),
	/**
	 * Vaudois ordinaire (DI VaudTax)
	 */
	VAUDOIS_VAUDTAX(TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, "Vaudois ordinaire (DI VaudTax)"),
	/**
	 * Vaudois à la dépense (DI à la dépense)
	 */
	VAUDOIS_DEPENSE(TypeContribuable.VAUDOIS_DEPENSE, TypeDocument.DECLARATION_IMPOT_DEPENSE, "Vaudois à la dépense (DI à la dépense)"),
	/**
	 * Hors canton avec immeuble (DI HC Immeuble)
	 */
	HC_IMMEUBLE(TypeContribuable.HORS_CANTON, TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, "Hors canton avec immeuble (DI HC Immeuble)"),
	/**
	 * Hors canton avec activité indépendante (DI complète)
	 */
	HC_ACTIND_COMPLETE(TypeContribuable.HORS_CANTON, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, "Hors canton avec activité indépendante (DI complète)"),
	/**
	 * Hors canton avec activité indépendante (DI VaudTax)
	 */
	HC_ACTIND_VAUDTAX(TypeContribuable.HORS_CANTON, TypeDocument.DECLARATION_IMPOT_VAUDTAX, "Hors canton avec activité indépendante (DI VaudTax)"),
	/**
	 * Hors Suisse avec activité indépendante ou immeuble (DI complète)
	 */
	HS_COMPLETE(TypeContribuable.HORS_SUISSE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, "Hors Suisse avec activité indépendante ou immeuble (DI complète)"),
	/**
	 * Hors Suisse avec activité indépendante ou immeuble (DI VaudTax)
	 */
	HS_VAUDTAX(TypeContribuable.HORS_SUISSE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, "Hors Suisse avec activité indépendante ou immeuble (DI VaudTax)"),
	/**
	 * Les diplomates suisses basés à l'étranger ne sont pas imposés à l'ICC mais uniquement à l'IFD. Ils ne recoivent donc pas de déclaration d'impôt de la part du canton : ils reçoivent à la place une
	 * déclaration d'impôt <b>fédérale</b>. La période d'imposition existe donc bien, même si elle ne provoque pas d'émission de DIs.
	 */
	DIPLOMATE_SUISSE(TypeContribuable.DIPLOMATE_SUISSE, null, "Diplomate Suisse"),
	/**
	 * [UNIREG-1976] Les diplomates suisses basés à l'étranger et qui possèdent un ou plusieurs immeubles reçoivent quand même une déclaration d'impôt ordinaire.
	 */
	DIPLOMATE_SUISSE_IMMEUBLE_COMPLETE(TypeContribuable.DIPLOMATE_SUISSE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, "Diplomate Suisse avec immeuble (DI complète)"),
	DIPLOMATE_SUISSE_IMMEUBLE_VAUDTAX(TypeContribuable.DIPLOMATE_SUISSE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, "Diplomate Suisse avec immeuble (DI VaudTax)");

	private TypeContribuable typeContribuable;
	private TypeDocument typeDocument;
	private String description;

	private CategorieEnvoiDI(TypeContribuable typeContribuable, TypeDocument typeDocument, String description) {
		this.typeContribuable = typeContribuable;
		this.typeDocument = typeDocument;
		this.description = description;
	}

	public TypeContribuable getTypeContribuable() {
		return typeContribuable;
	}

	public TypeDocument getTypeDocument() {
		return typeDocument;
	}

	public String getDescription() {
		return description;
	}

	/**
	 * Retourne la catégorie de population d'envoi de DI dans la cas d'un contribuable devant recevoir une <b>déclaration ordinaire</b> (= complète ou VaudTax).
	 *
	 * @param typeContribuable le type de contribuable
	 * @param format           le type de déclaration ordinaire (vaudtax ou complète)
	 * @return le type de population d'envoi de DI déterminé.
	 */
	public static CategorieEnvoiDI ordinaireFor(TypeContribuable typeContribuable, FormatDIOrdinaire format) {
		switch (typeContribuable) {
		case VAUDOIS_ORDINAIRE:
			if (format == FormatDIOrdinaire.COMPLETE) {
				return VAUDOIS_COMPLETE;
			}
			else if (format == FormatDIOrdinaire.VAUDTAX) {
				return VAUDOIS_VAUDTAX;
			}
			else {
				throw new IllegalArgumentException("Type de DI ordinaire inconnu = [" + format + "]");
			}
		case VAUDOIS_DEPENSE:
			return VAUDOIS_DEPENSE;
		case HORS_CANTON:
			if (format == FormatDIOrdinaire.COMPLETE) {
				return HC_ACTIND_COMPLETE;
			}
			else if (format == FormatDIOrdinaire.VAUDTAX) {
				return HC_ACTIND_VAUDTAX;
			}
			else {
				throw new IllegalArgumentException("Type de DI ordinaire inconnu = [" + format + "]");
			}
		case HORS_SUISSE:
			if (format == FormatDIOrdinaire.COMPLETE) {
				return HS_COMPLETE;
			}
			else if (format == FormatDIOrdinaire.VAUDTAX) {
				return HS_VAUDTAX;
			}
			else {
				throw new IllegalArgumentException("Type de DI ordinaire inconnu = [" + format + "]");
			}
		case DIPLOMATE_SUISSE:
			if (format == FormatDIOrdinaire.COMPLETE) {
				return DIPLOMATE_SUISSE_IMMEUBLE_COMPLETE;
			}
			else if (format == FormatDIOrdinaire.VAUDTAX) {
				return DIPLOMATE_SUISSE_IMMEUBLE_VAUDTAX;
			}
			else {
				throw new IllegalArgumentException("Type de DI ordinaire inconnu = [" + format + "]");
			}
		default:
			throw new IllegalArgumentException("Type de contribuable inconnu = [" + typeContribuable + "]");
		}
	}
}
