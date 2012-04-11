package ch.vd.uniregctb.adresse;

import ch.vd.uniregctb.adresse.AdresseGenerique.SourceType;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RepresentationConventionnelle;
import ch.vd.uniregctb.tiers.Tutelle;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * Ensemble restreint des types de rapports-entre-tiers permettant une redirection de l'adresse courrier d'un tiers.
 */
public enum TypeAdresseRepresentant {

	REPRESENTATION(TypeRapportEntreTiers.REPRESENTATION, SourceType.REPRESENTATION),
	REPRESENTATION_AVEC_EXECUTION_FORCEE(TypeRapportEntreTiers.REPRESENTATION, SourceType.REPRESENTATION) {
		@Override
		public Long getRepresentantId(RapportEntreTiers rapport) {
			// cas spécial de la représentation conventionnelle avec exécution forcée : on ignore les représentations sans ce flag
			final RepresentationConventionnelle representation = (RepresentationConventionnelle) rapport;
			if (representation.getExtensionExecutionForcee() == Boolean.TRUE) {
				return representation.getObjetId();
			}
			else {
				return null;
			}
		}},
	TUTELLE(TypeRapportEntreTiers.TUTELLE, SourceType.TUTELLE),
	AUTORITE_TUTELAIRE(TypeRapportEntreTiers.TUTELLE, SourceType.TUTELLE) {
		@Override
		public Long getRepresentantId(RapportEntreTiers rapport) {
			// cas spécial de l'autorité tutelaire dont le représentant se trouve sur un attribut spécifique.
			return ((Tutelle) rapport).getAutoriteTutelaireId();
		}},
	CURATELLE(TypeRapportEntreTiers.CURATELLE, SourceType.CURATELLE),
	CONSEIL_LEGAL(TypeRapportEntreTiers.CONSEIL_LEGAL, SourceType.CONSEIL_LEGAL);

	final TypeRapportEntreTiers typeRapport;
	final SourceType typeSource;

	private TypeAdresseRepresentant(TypeRapportEntreTiers typeRapport, SourceType typeSource) {
		this.typeRapport = typeRapport;
		this.typeSource = typeSource;
	}

	public TypeRapportEntreTiers getTypeRapport() {
		return typeRapport;
	}

	public SourceType getTypeSource() {
		return typeSource;
	}

	/**
	 * Retourne le représentant (c'est-à-dire le tiers objet dans la majorité des cas) pour le type d'adresse courante et le rapport spécifié.
	 *
	 * @param rapport un rapport entre tiers
	 * @return le représentant du rapport pour le type courant
	 */
	public Long getRepresentantId(RapportEntreTiers rapport) {
		return rapport.getObjetId();
	}

	/**
	 * Retrouve la correspondance entre un Source donnée et un TypeAdresseRepresentant, ou null si une telle représentation n'existe pas
	 *
	 * @param source la source
	 * @return le type d'adresse représentant trouvé; ou <b>null</b> si la source ne correspond à aucun type.
	 */
	public static TypeAdresseRepresentant getTypeAdresseRepresentantFromSource(SourceType source) {
		for (TypeAdresseRepresentant typeAdresseRepresentant : values()) {
			if (typeAdresseRepresentant.getTypeSource() == source) {
				return typeAdresseRepresentant;
			}
		}
		return null;
	}
}
