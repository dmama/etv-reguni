package ch.vd.unireg.metier.assujettissement;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import ch.vd.unireg.type.MotifFor;

public enum MotifAssujettissement {

	//
	// les motifs repris des motifs de fors
	//

	DEMENAGEMENT_VD(MotifFor.DEMENAGEMENT_VD),
	VEUVAGE_DECES(MotifFor.VEUVAGE_DECES),
	MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION),
	SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT),
	PERMIS_C_SUISSE(MotifFor.PERMIS_C_SUISSE),
	MAJORITE(MotifFor.MAJORITE),
	ARRIVEE_HS(MotifFor.ARRIVEE_HS),
	ARRIVEE_HC(MotifFor.ARRIVEE_HC),
	FUSION_COMMUNES(MotifFor.FUSION_COMMUNES),
	ACHAT_IMMOBILIER(MotifFor.ACHAT_IMMOBILIER),
	VENTE_IMMOBILIER(MotifFor.VENTE_IMMOBILIER),
	DEBUT_EXPLOITATION(MotifFor.DEBUT_EXPLOITATION),
	FIN_EXPLOITATION(MotifFor.FIN_EXPLOITATION),
	DEPART_HS(MotifFor.DEPART_HS),
	DEPART_HC(MotifFor.DEPART_HC),
	INDETERMINE(MotifFor.INDETERMINE),
	SEJOUR_SAISONNIER(MotifFor.SEJOUR_SAISONNIER),
	CHGT_MODE_IMPOSITION(MotifFor.CHGT_MODE_IMPOSITION),
	ANNULATION(MotifFor.ANNULATION),
	REACTIVATION(MotifFor.REACTIVATION),
	DEBUT_ACTIVITE_DIPLOMATIQUE(MotifFor.DEBUT_ACTIVITE_DIPLOMATIQUE),
	FIN_ACTIVITE_DIPLOMATIQUE(MotifFor.FIN_ACTIVITE_DIPLOMATIQUE),
	DEBUT_PRESTATION_IS(MotifFor.DEBUT_PRESTATION_IS),
	FIN_PRESTATION_IS(MotifFor.FIN_PRESTATION_IS),
	CESSATION_ACTIVITE_FUSION_FAILLITE(MotifFor.CESSATION_ACTIVITE_FUSION_FAILLITE),
	DEMENAGEMENT_SIEGE(MotifFor.DEMENAGEMENT_SIEGE),
	FUSION_ENTREPRISES(MotifFor.FUSION_ENTREPRISES),
	FAILLITE(MotifFor.FAILLITE),

	//
	// A partir d'ici, ce sont des motifs qui ne sont pas issus des motifs de fors
	//

	/**
	 * Motif utilisé dans les assujettissements pour indiquer que l'assujettissement s'arrête avant (resp. commence après)
	 * une période d'exonération totale : utilisé en tant que motif de début d'assujettissement, il correspond donc
	 * à une fin d'exonération ; utilisé en tant que motif de fin d'assujettissement, il correspond à un début d'exonération.
	 */
	EXONERATION("Fin d'exonération", "Début d'éxonération");

	/**
	 * Mapping pour passer d'un MotifFor à un MotifAssujettissement
	 */
	private static final Map<MotifFor, MotifAssujettissement> MOTIFS_FOR_TO_ASSUJ = buildMappingForToAssuj();

	private static Map<MotifFor, MotifAssujettissement> buildMappingForToAssuj() {
		final Map<MotifFor, MotifAssujettissement> map = new EnumMap<>(MotifFor.class);
		for (MotifAssujettissement motifAssujettissement : values()) {
			final MotifFor motifFor = motifAssujettissement.motifFor;
			if (motifFor != null) {
				final MotifAssujettissement previous = map.put(motifFor, motifAssujettissement);
				if (previous != null) {
					throw new IllegalArgumentException("Le motif de for " + motifFor + " est associé à plusieurs motifs d'assujettissement (au moins " + previous + " et " + motifAssujettissement + ")");
				}
			}
		}
		return Collections.unmodifiableMap(map);
	}

	private final String descriptionOuverture;
	private final String descriptionFermeture;

	/**
	 * Exclusivement destiné à construire la table {@link #MOTIFS_FOR_TO_ASSUJ} qui sert à l'implémentation
	 * de la méthode {@link #of(MotifFor)}... Le membre lui-même n'est pas destiné à être exposé...
	 */
	private final MotifFor motifFor;

	MotifAssujettissement(String descriptionOuverture, String descriptionFermeture) {
		this.descriptionOuverture = descriptionOuverture;
		this.descriptionFermeture = descriptionFermeture;
		this.motifFor = null;
	}

	MotifAssujettissement(String description) {
		this(description, description);
	}

	MotifAssujettissement(MotifFor motifFor) {
		this.descriptionOuverture = motifFor.getDescription(true);
		this.descriptionFermeture = motifFor.getDescription(false);
		this.motifFor = motifFor;
	}

	/**
	 * Point d'entrée du mapping entre un {@link MotifFor} et un {@link MotifAssujettissement}
	 * @param motifFor le {@link MotifFor} à transformer
	 * @return le {@link MotifAssujettissement} correspondant
	 */
	public static MotifAssujettissement of(MotifFor motifFor) {
		return MOTIFS_FOR_TO_ASSUJ.get(motifFor);
	}

	public String getDescription(boolean ouverture) {
		return ouverture ? descriptionOuverture : descriptionFermeture;
	}

}
