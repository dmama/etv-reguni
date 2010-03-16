package ch.vd.uniregctb.adresse;

import ch.vd.uniregctb.adresse.AdresseGenerique.Source;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * Ensemble restreint des types de rapports-entre-tiers permettant une redirection de l'adresse courrier d'un tiers.
 */
public enum TypeAdresseRepresentant {

	REPRESENTATION(TypeRapportEntreTiers.REPRESENTATION, Source.REPRESENTATION),
	TUTELLE(TypeRapportEntreTiers.TUTELLE, Source.TUTELLE),
	CURATELLE(TypeRapportEntreTiers.CURATELLE, Source.CURATELLE),
	CONSEIL_LEGAL(TypeRapportEntreTiers.CONSEIL_LEGAL, Source.CONSEIL_LEGAL);
	
	final TypeRapportEntreTiers typeRapport;
	final Source typeSource;

	private TypeAdresseRepresentant(TypeRapportEntreTiers typeRapport, Source typeSource) {
		this.typeRapport = typeRapport;
		this.typeSource = typeSource;
	}

	public TypeRapportEntreTiers getTypeRapport() {
		return typeRapport;
	}

	public Source getTypeSource() {
		return typeSource;
	}

	/**
	 * Retrouve la correspondance entre un Source donnée et un TypeAdresseRepresentant, ou null si une
	 * telle représentation n'existe pas
	 * @param source
	 * @return
	 */
	public static TypeAdresseRepresentant getTypeAdresseRepresentantFromSource(Source source) {
		for (TypeAdresseRepresentant typeAdresseRepresentant : values()) {
			if (typeAdresseRepresentant.getTypeSource() == source) {
				return typeAdresseRepresentant;
			}
		}
		return null;
	}
}
