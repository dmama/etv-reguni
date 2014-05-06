package ch.vd.uniregctb.tiers.validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ch.vd.uniregctb.tiers.NatureTiers;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;

/**
 * Classe utilitaire qui retourne les motifs d'ouverture et de fermeture acceptables lors de l'ajout ou de la modification d'un for fiscal
 * par l'utilisateur.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class MotifsForHelper {

	public static class TypeFor {
		public final NatureTiers natureTiers;
		public final GenreImpot genreImpot;
		public final MotifRattachement rattachement;

		public TypeFor(NatureTiers natureTiers, GenreImpot genreImpot, MotifRattachement rattachement) {
			this.natureTiers = natureTiers;
			this.genreImpot = genreImpot;
			this.rattachement = rattachement;
		}
	}

	public static List<MotifFor> getMotifsOuverture(TypeFor type) {
		if (GenreImpot.REVENU_FORTUNE == type.genreImpot) {
			return getMotifsOuvertureRevenuFortune(type);
		}
		else if (GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE == type.genreImpot) {
			return getMotifsOuvertureDebiteursPrestationsImposables();
		}
		return Collections.emptyList();
	}

	private static List<MotifFor> getMotifsOuvertureDebiteursPrestationsImposables() {
		return Arrays.asList(MotifFor.DEBUT_PRESTATION_IS,
		                     MotifFor.FUSION_COMMUNES,
		                     MotifFor.DEMENAGEMENT_SIEGE);
	}

	private static List<MotifFor> getMotifsOuvertureRevenuFortune(TypeFor type) {

		// Motifs interdits depuis la GUI
		// motifs.add(MotifFor.CHGT_MODE_IMPOSITION);

		// Motifs impossibles en ouverture
		// motifs.add(MotifFor.VENTE_IMMOBILIER);
		// motifs.add(MotifFor.FIN_EXPLOITATION);

		final List<MotifFor> motifs = new ArrayList<>();

		switch (type.rattachement) {
		case DIPLOMATE_SUISSE:
			motifs.add(MotifFor.DEBUT_ACTIVITE_DIPLOMATIQUE);
		case DIPLOMATE_ETRANGER:
		case DOMICILE:
			// for principal
			motifs.add(MotifFor.DEMENAGEMENT_VD);
			if (NatureTiers.Habitant == type.natureTiers || NatureTiers.NonHabitant == type.natureTiers) {
				motifs.add(MotifFor.MAJORITE);
				motifs.add(MotifFor.PERMIS_C_SUISSE);
				motifs.add(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
				motifs.add(MotifFor.VEUVAGE_DECES);
			}
			else if (NatureTiers.MenageCommun == type.natureTiers) {
				motifs.add(MotifFor.PERMIS_C_SUISSE);
				motifs.add(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			}
			motifs.add(MotifFor.ARRIVEE_HS);
			motifs.add(MotifFor.ARRIVEE_HC);
			motifs.add(MotifFor.DEPART_HS);
			motifs.add(MotifFor.DEPART_HC);
			break;

		case IMMEUBLE_PRIVE:
			motifs.add(MotifFor.ACHAT_IMMOBILIER);
			if (NatureTiers.Habitant == type.natureTiers || NatureTiers.NonHabitant == type.natureTiers) {
				motifs.add(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
			}
			else if (NatureTiers.MenageCommun == type.natureTiers) {
				motifs.add(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			}
			break;

		case ACTIVITE_INDEPENDANTE:
			motifs.add(MotifFor.DEBUT_EXPLOITATION);
			if (NatureTiers.Habitant == type.natureTiers || NatureTiers.NonHabitant == type.natureTiers) {
				motifs.add(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
			}
			else if (NatureTiers.MenageCommun == type.natureTiers) {
				motifs.add(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			}
			break;

		case ETABLISSEMENT_STABLE:
		case DIRIGEANT_SOCIETE:
		case ACTIVITE_LUCRATIVE_CAS:
		case ADMINISTRATEUR:
		case CREANCIER_HYPOTHECAIRE:
		case PRESTATION_PREVOYANCE:
		case LOI_TRAVAIL_AU_NOIR:
		case PARTICIPATIONS_HORS_SUISSE:
		case EFFEUILLEUSES:
			motifs.add(MotifFor.DEBUT_EXPLOITATION);
			break;

		case SEJOUR_SAISONNIER:
			motifs.add(MotifFor.SEJOUR_SAISONNIER);
			break;

		default:
			throw new IllegalArgumentException("Le motif de rattachement [" + type.rattachement + "] est inconnu");
		}

		// [SIFISC-5220] le motif fusion de communes est désormais autorisé pour permettre le rattrapage de données sans passer par SuperGra
		motifs.add(MotifFor.FUSION_COMMUNES);

		return motifs;
	}

	public static List<MotifFor> getMotifsFermeture(TypeFor type) {
		if (GenreImpot.REVENU_FORTUNE == type.genreImpot) {
			return getMotifsFermetureRevenuFortune(type);
		}
		else if (GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE == type.genreImpot) {
			return getMotifsFermetureDebiteursPrestationsImposables();
		}
		return Collections.emptyList();
	}

	private static List<MotifFor> getMotifsFermetureDebiteursPrestationsImposables() {
		return Arrays.asList(MotifFor.FIN_PRESTATION_IS,
		                     MotifFor.CESSATION_ACTIVITE_FUSION_FAILLITE,
		                     MotifFor.FUSION_COMMUNES,
		                     MotifFor.DEMENAGEMENT_SIEGE);
	}

	private static List<MotifFor> getMotifsFermetureRevenuFortune(TypeFor type) {

		// Motifs interdits depuis la GUI
		// motifs.add(MotifFor.CHGT_MODE_IMPOSITION);

		// Motifs impossibles en fermeture
		// motifs.add(MotifFor.ACHAT_IMMOBILIER);
		// motifs.add(MotifFor.DEBUT_EXPLOITATION);
		// motifs.add(MotifFor.MAJORITE);

		final List<MotifFor> motifs = new ArrayList<>();

		switch (type.rattachement) {
		case DIPLOMATE_SUISSE:
			motifs.add(MotifFor.FIN_ACTIVITE_DIPLOMATIQUE);
		case DIPLOMATE_ETRANGER:
		case DOMICILE:
			// for principal
			motifs.add(MotifFor.DEMENAGEMENT_VD);
			if (NatureTiers.Habitant == type.natureTiers || NatureTiers.NonHabitant == type.natureTiers) {
				motifs.add(MotifFor.PERMIS_C_SUISSE);
				motifs.add(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			}
			else if (NatureTiers.MenageCommun == type.natureTiers) {
				motifs.add(MotifFor.PERMIS_C_SUISSE);
				motifs.add(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
				motifs.add(MotifFor.VEUVAGE_DECES);
			}
			motifs.add(MotifFor.ARRIVEE_HS);
			motifs.add(MotifFor.ARRIVEE_HC);
			motifs.add(MotifFor.DEPART_HS);
			motifs.add(MotifFor.DEPART_HC);
			break;

		case IMMEUBLE_PRIVE:
			motifs.add(MotifFor.VENTE_IMMOBILIER);
			if (NatureTiers.Habitant == type.natureTiers || NatureTiers.NonHabitant == type.natureTiers) {
				motifs.add(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			}
			else if (NatureTiers.MenageCommun == type.natureTiers) {
				motifs.add(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
			}
			break;

		case ACTIVITE_INDEPENDANTE:
			motifs.add(MotifFor.FIN_EXPLOITATION);
			if (NatureTiers.Habitant == type.natureTiers || NatureTiers.NonHabitant == type.natureTiers) {
				motifs.add(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			}
			else if (NatureTiers.MenageCommun == type.natureTiers) {
				motifs.add(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
			}
			break;

		case ETABLISSEMENT_STABLE:
		case DIRIGEANT_SOCIETE:
		case ACTIVITE_LUCRATIVE_CAS:
		case ADMINISTRATEUR:
		case CREANCIER_HYPOTHECAIRE:
		case PRESTATION_PREVOYANCE:
		case LOI_TRAVAIL_AU_NOIR:
		case PARTICIPATIONS_HORS_SUISSE:
		case EFFEUILLEUSES:
			motifs.add(MotifFor.FIN_EXPLOITATION);
			break;

		case SEJOUR_SAISONNIER:
			motifs.add(MotifFor.SEJOUR_SAISONNIER);
			break;

		default:
			throw new IllegalArgumentException("Le motif de rattachement [" + type.rattachement + "] est inconnu");
		}

		// [SIFISC-5220] le motif fusion de communes est désormais autorisé pour permettre le rattrapage de données sans passer par SuperGra
		motifs.add(MotifFor.FUSION_COMMUNES);

		return motifs;
	}
}
