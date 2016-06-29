package ch.vd.uniregctb.tiers.validator;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

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

	public static Set<MotifFor> getMotifsOuverture(TypeFor type) {
		if (GenreImpot.REVENU_FORTUNE == type.genreImpot && isPersonnePhysique(type.natureTiers)) {
			return getMotifsOuvertureRevenuFortune(type);
		}
		else if (GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE == type.genreImpot) {
			return getMotifsOuvertureDebiteursPrestationsImposables();
		}
		else if (isPersonneMorale(type.natureTiers)) {
			return getMotifsOuvertureEntreprise(type);
		}
		return Collections.emptySet();
	}

	private static Set<MotifFor> getMotifsOuvertureEntreprise(TypeFor type) {

		final Set<MotifFor> motifs = EnumSet.noneOf(MotifFor.class);
		switch (type.rattachement) {
		case DOMICILE:
			motifs.add(MotifFor.DEBUT_EXPLOITATION);
			motifs.add(MotifFor.DEMENAGEMENT_VD);
			motifs.add(MotifFor.ARRIVEE_HS);
			motifs.add(MotifFor.ARRIVEE_HC);
			motifs.add(MotifFor.DEPART_HS);
			motifs.add(MotifFor.DEPART_HC);
			break;
		case IMMEUBLE_PRIVE:
			motifs.add(MotifFor.ACHAT_IMMOBILIER);
			break;
		case ETABLISSEMENT_STABLE:
			motifs.add(MotifFor.DEBUT_EXPLOITATION);
			break;
		default:
			break;
		}

		// fusion entreprises & fusion communes pour les rattrapages
		motifs.add(MotifFor.FUSION_COMMUNES);
		motifs.add(MotifFor.FUSION_ENTREPRISES);

		return motifs;
	}

	private static Set<MotifFor> getMotifsOuvertureDebiteursPrestationsImposables() {
		return EnumSet.of(MotifFor.DEBUT_PRESTATION_IS,
		                  MotifFor.FUSION_COMMUNES,
		                  MotifFor.DEMENAGEMENT_SIEGE);
	}

	private static Set<MotifFor> getMotifsOuvertureRevenuFortune(TypeFor type) {

		// Motifs interdits depuis la GUI
		// motifs.add(MotifFor.CHGT_MODE_IMPOSITION);

		// Motifs impossibles en ouverture
		// motifs.add(MotifFor.VENTE_IMMOBILIER);
		// motifs.add(MotifFor.FIN_EXPLOITATION);

		final Set<MotifFor> motifs = EnumSet.noneOf(MotifFor.class);

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

	public static boolean isPersonnePhysique(NatureTiers nature) {
		switch (nature) {
		case Habitant:
		case NonHabitant:
		case MenageCommun:
			return true;
		default:
			return false;
		}
	}

	public static boolean isPersonneMorale(NatureTiers nature) {
		return nature == NatureTiers.Entreprise;
	}

	public static Set<MotifFor> getMotifsFermeture(TypeFor type) {
		if (GenreImpot.REVENU_FORTUNE == type.genreImpot && isPersonnePhysique(type.natureTiers)) {
			return getMotifsFermetureRevenuFortune(type);
		}
		else if (GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE == type.genreImpot) {
			return getMotifsFermetureDebiteursPrestationsImposables();
		}
		else if (isPersonneMorale(type.natureTiers)) {
			return getMotifsFermetureEntreprise(type);
		}
		return Collections.emptySet();
	}

	private static Set<MotifFor> getMotifsFermetureEntreprise(TypeFor type) {
		final Set<MotifFor> motifs = EnumSet.noneOf(MotifFor.class);
		switch (type.rattachement) {
		case DOMICILE:
			motifs.add(MotifFor.FIN_EXPLOITATION);
			motifs.add(MotifFor.FAILLITE);
			break;
		case IMMEUBLE_PRIVE:
			motifs.add(MotifFor.VENTE_IMMOBILIER);
			break;
		case ETABLISSEMENT_STABLE:
			motifs.add(MotifFor.FIN_EXPLOITATION);
			break;
		default:
			break;
		}

		// fusion entreprises & fusion communes pour les rattrapages
		motifs.add(MotifFor.FUSION_COMMUNES);
		motifs.add(MotifFor.FUSION_ENTREPRISES);

		return motifs;
	}

	private static Set<MotifFor> getMotifsFermetureDebiteursPrestationsImposables() {
		return EnumSet.of(MotifFor.FIN_PRESTATION_IS,
		                  MotifFor.CESSATION_ACTIVITE_FUSION_FAILLITE,
		                  MotifFor.FUSION_COMMUNES,
		                  MotifFor.DEMENAGEMENT_SIEGE);
	}

	private static Set<MotifFor> getMotifsFermetureRevenuFortune(TypeFor type) {

		// Motifs interdits depuis la GUI
		// motifs.add(MotifFor.CHGT_MODE_IMPOSITION);

		// [SIFISC-11145] nouveaux motifs interdits depuis la GUI en fermeture de for "domicile"
		// motifs.add(MotifFor.DEMENAGEMENT_VD);
		// motifs.add(MotifFor.PERMIS_C_SUISSE);
		// motifs.add(MotifFor.ARRIVEE_HS);
		// motifs.add(MotifFor.ARRIVEE_HC);
		// motifs.add(MotifFor.DEPART_HS);
		// motifs.add(MotifFor.DEPART_HC);

		// Motifs impossibles en fermeture
		// motifs.add(MotifFor.ACHAT_IMMOBILIER);
		// motifs.add(MotifFor.DEBUT_EXPLOITATION);
		// motifs.add(MotifFor.MAJORITE);

		final Set<MotifFor> motifs = EnumSet.noneOf(MotifFor.class);

		switch (type.rattachement) {
		case DIPLOMATE_SUISSE:
			motifs.add(MotifFor.FIN_ACTIVITE_DIPLOMATIQUE);
		case DIPLOMATE_ETRANGER:
		case DOMICILE:
			// for principal
			if (NatureTiers.Habitant == type.natureTiers || NatureTiers.NonHabitant == type.natureTiers) {
				motifs.add(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			}
			else if (NatureTiers.MenageCommun == type.natureTiers) {
				motifs.add(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
				motifs.add(MotifFor.VEUVAGE_DECES);
			}
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
