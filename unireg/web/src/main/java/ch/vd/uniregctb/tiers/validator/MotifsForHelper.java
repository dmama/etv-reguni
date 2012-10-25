package ch.vd.uniregctb.tiers.validator;

import java.util.ArrayList;
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

		if (GenreImpot.REVENU_FORTUNE != type.genreImpot) {
			// par définition
			return Collections.emptyList();
		}

		// Motifs interdits depuis la GUI
		// motifs.add(MotifFor.FUSION_COMMUNES);
		// motifs.add(MotifFor.CHGT_MODE_IMPOSITION);

		// Motifs impossibles en ouverture
		// motifs.add(MotifFor.VENTE_IMMOBILIER);
		// motifs.add(MotifFor.FIN_EXPLOITATION);

		final List<MotifFor> motifs = new ArrayList<MotifFor>();

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
			break;

		case ACTIVITE_INDEPENDANTE:
		case ETABLISSEMENT_STABLE:
		case DIRIGEANT_SOCIETE:
		case ACTIVITE_LUCRATIVE_CAS:
		case ADMINISTRATEUR:
		case CREANCIER_HYPOTHECAIRE:
		case PRESTATION_PREVOYANCE:
		case LOI_TRAVAIL_AU_NOIR:
			motifs.add(MotifFor.DEBUT_EXPLOITATION);
			break;

		case SEJOUR_SAISONNIER:
			motifs.add(MotifFor.SEJOUR_SAISONNIER);
			break;

		default:
			throw new IllegalArgumentException("Le motif de rattachement [" + type.rattachement + "] est inconnu");
		}

		return motifs;
	}

	public static List<MotifFor> getMotifsFermeture(TypeFor type) {

		if (GenreImpot.REVENU_FORTUNE != type.genreImpot) {
			// par définition
			return Collections.emptyList();
		}

		// Motifs interdits depuis la GUI
		// motifs.add(MotifFor.FUSION_COMMUNES);
		// motifs.add(MotifFor.CHGT_MODE_IMPOSITION);

		// Motifs impossibles en fermeture
		// motifs.add(MotifFor.ACHAT_IMMOBILIER);
		// motifs.add(MotifFor.DEBUT_EXPLOITATION);
		// motifs.add(MotifFor.MAJORITE);

		final List<MotifFor> motifs = new ArrayList<MotifFor>();

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
			break;

		case ACTIVITE_INDEPENDANTE:
		case ETABLISSEMENT_STABLE:
		case DIRIGEANT_SOCIETE:
		case ACTIVITE_LUCRATIVE_CAS:
		case ADMINISTRATEUR:
		case CREANCIER_HYPOTHECAIRE:
		case PRESTATION_PREVOYANCE:
		case LOI_TRAVAIL_AU_NOIR:
			motifs.add(MotifFor.FIN_EXPLOITATION);
			break;

		case SEJOUR_SAISONNIER:
			motifs.add(MotifFor.SEJOUR_SAISONNIER);
			break;

		default:
			throw new IllegalArgumentException("Le motif de rattachement [" + type.rattachement + "] est inconnu");
		}

		return motifs;
	}
}
