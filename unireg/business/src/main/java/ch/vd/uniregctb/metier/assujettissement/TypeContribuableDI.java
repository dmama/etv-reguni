package ch.vd.uniregctb.metier.assujettissement;

import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;

/**
 * Version étendue de l'enum TypeContribuable qui lie un type de contribuable avec le type de document (DI) correspondant.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public enum TypeContribuableDI {

	VAUDOIS_ORDINAIRE(TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, "Vaudois ordinaires"), // ------------------
	VAUDOIS_ORDINAIRE_VAUD_TAX(TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, "Vaudois ordinaire VaudTax"), // ---------
	VAUDOIS_DEPENSE(TypeContribuable.VAUDOIS_DEPENSE, TypeDocument.DECLARATION_IMPOT_DEPENSE, "Vaudois à la dépense"), // ---------------------------
	HORS_CANTON(TypeContribuable.HORS_CANTON, TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, "Hors canton"), // ----------------------------------------
	HORS_SUISSE(TypeContribuable.HORS_SUISSE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, "Hors Suisse"), // -------------------------------------
	/**
	 * Les diplomates Suisse basés à l'étranger ne sont pas imposés à l'ICC mais uniquement à l'IFD. Ils ne recoivent donc pas de déclaration d'impôt de la part du canton : ils reçoivent à la place
	 * une déclaration d'impôt <b>fédérale</b>. La période d'imposition existe donc bien, même si elle ne provoque pas d'émission de DIs.
	 */
	DIPLOMATE_SUISSE(TypeContribuable.VAUDOIS_ORDINAIRE, null, "Diplomate Suisse");

	private TypeContribuable typeContribuable;
	private TypeDocument typeDocument;
	private String description;

	private TypeContribuableDI(TypeContribuable typeContribuable, TypeDocument typeDocument, String description) {
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
}
