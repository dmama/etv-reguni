package ch.vd.uniregctb.stats.evenements;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;

public class StatsEvenementsCivilsOrganisationsResults {

	/**
	 * Clé de regroupement pour la statistique des mutations traitées (par type de mutation et état actuel de l'ếvénement)
	 */
	public static class MutationsTraiteesStatsKey implements Comparable<MutationsTraiteesStatsKey> {

		private final String description;
		private final EtatEvenementOrganisation etat;

		public MutationsTraiteesStatsKey(@NotNull String description, @NotNull  EtatEvenementOrganisation etat) {
			this.description = description;
			this.etat = etat;
		}

		public String getDescription() {
			return description;
		}

		public EtatEvenementOrganisation getEtat() {
			return etat;
		}

		@Override
		public int compareTo(@NotNull MutationsTraiteesStatsKey o) {
			int comparison = description.compareTo(o.description);
			if (comparison == 0) {
				comparison = etat.compareTo(o.etat);
			}
			return comparison;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final MutationsTraiteesStatsKey that = (MutationsTraiteesStatsKey) o;
			return description.equals(that.description) && etat == that.etat;
		}

		@Override
		public int hashCode() {
			return 31 * description.hashCode() + etat.hashCode();
		}

		@Override
		public String toString() {
			return String.format("Mutation '%s' sur événement à l'état final %s", description, etat);
		}
	}

	/**
	 * Elément constitutif des détails des mutations traitées individuellement
	 */
	public static class DetailMutationTraitee implements StatistiqueEvenementInfo {

		private final long noOrganisation;
		private final EtatEvenementOrganisation etatFinal;
		private final String description;
		private final long noEvenement;
		private final RegDate dateEvenement;
		private final Timestamp dateTraitement;

		public DetailMutationTraitee(long noOrganisation, EtatEvenementOrganisation etatFinal, String description, long noEvenement, RegDate dateEvenement, Timestamp dateTraitement) {
			this.noOrganisation = noOrganisation;
			this.etatFinal = etatFinal;
			this.description = description;
			this.noEvenement = noEvenement;
			this.dateEvenement = dateEvenement;
			this.dateTraitement = dateTraitement;
		}

		private static final String[] COLONNES = {"NO_ORGANISATION", "NO_EVT", "DATE_EVT", "ETAT_FINAL_EVT", "DATE_TRAITEMENT", "TYPE_MUTATION"};

		@Override
		public String[] getNomsColonnes() {
			return COLONNES;
		}

		@Override
		public String[] getValeursColonnes() {
			return new String[] {
					Long.toString(noOrganisation),
					Long.toString(noEvenement),
					RegDateHelper.dateToDashString(dateEvenement),
					etatFinal.name(),
					DateHelper.dateTimeToDisplayString(dateTraitement),
					description
			};
		}
	}

	/**
	 * Erreurs rencontrées lors des traitements des événements organisation
	 */
	public static class ErreurInfo implements StatistiqueEvenementInfo {

		private final long id;
		private final long noOrganisation;
		private final long noEvenement;
		private final RegDate dateEvenement;
		private final Date dateTraitement;
		private final EtatEvenementOrganisation etat;
		private final String erreur;

		public ErreurInfo(long id, long noOrganisation, long noEvenement, RegDate dateEvenement, Date dateTraitement, EtatEvenementOrganisation etat, String erreur) {
			this.id = id;
			this.noOrganisation = noOrganisation;
			this.noEvenement = noEvenement;
			this.dateEvenement = dateEvenement;
			this.dateTraitement = dateTraitement;
			this.etat = etat;
			this.erreur = erreur;
		}

		private static final String[] COLONNES = {"ID", "NO_EVT", "NO_ORGANISATION", "DATE_EVT", "DATE_TRAITEMENT", "ETAT", "MESSAGE"};

		@Override
		public String[] getNomsColonnes() {
			return COLONNES;
		}

		@Override
		public String[] getValeursColonnes() {
			return new String[] {
					Long.toString(id),
					Long.toString(noEvenement),
					Long.toString(noOrganisation),
					RegDateHelper.dateToDashString(dateEvenement),
					DateHelper.dateTimeToDisplayString(dateTraitement),
					etat.name(),
					erreur
			};
		}
	}

	public static class EvenementEnSouffranceInfo implements StatistiqueEvenementInfo {

		private final long id;
		private final long noOrganisation;
		private final long noEvenement;
		private final RegDate dateEvenement;
		private final Date dateReception;
		private final EtatEvenementOrganisation etat;

		public EvenementEnSouffranceInfo(long id, long noOrganisation, long noEvenement, RegDate dateEvenement, Date dateReception, EtatEvenementOrganisation etat) {
			this.id = id;
			this.noOrganisation = noOrganisation;
			this.noEvenement = noEvenement;
			this.dateEvenement = dateEvenement;
			this.dateReception = dateReception;
			this.etat = etat;
		}

		private static final String[] COLONNES = {"ID", "NO_EVT", "NO_ORGANISATION", "DATE_EVT", "DATE_RECEPTION", "ETAT"};

		@Override
		public String[] getNomsColonnes() {
			return COLONNES;
		}

		@Override
		public String[] getValeursColonnes() {
			return new String[] {
					Long.toString(id),
					Long.toString(noEvenement),
					Long.toString(noOrganisation),
					RegDateHelper.dateToDashString(dateEvenement),
					DateHelper.dateTimeToDisplayString(dateReception),
					etat.name()
			};
		}
	}

	private final Map<EtatEvenementOrganisation, Integer> etats;
	private final Map<EtatEvenementOrganisation, Integer> etatsNouveaux;                    // <-- sur les événements reçus récemment
	private final Map<MutationsTraiteesStatsKey, Integer> mutationsTraitees;
	private final Map<MutationsTraiteesStatsKey, Integer> mutationsRecentesTraitees;        // <-- sur les événements reçus récemment seulement
	private final List<DetailMutationTraitee> detailsMutationsTraiteesRecentes;             // <-- sur les événements traités récemment seulement
	private final List<ErreurInfo> erreurs;
	private final List<EvenementEnSouffranceInfo> enSouffrance;

	public StatsEvenementsCivilsOrganisationsResults(Map<EtatEvenementOrganisation, Integer> etats, Map<EtatEvenementOrganisation, Integer> etatsNouveaux,
	                                                 Map<MutationsTraiteesStatsKey, Integer> mutationsTraitees, Map<MutationsTraiteesStatsKey, Integer> mutationsRecentesTraitees,
	                                                 List<DetailMutationTraitee> detailsMutationsTraiteesRecentes,
	                                                 List<ErreurInfo> erreurs,
	                                                 List<EvenementEnSouffranceInfo> enSouffrance) {
		this.etats = CollectionsUtils.unmodifiableNeverNull(etats);
		this.etatsNouveaux = CollectionsUtils.unmodifiableNeverNull(etatsNouveaux);
		this.mutationsTraitees = CollectionsUtils.unmodifiableNeverNull(mutationsTraitees);
		this.mutationsRecentesTraitees = CollectionsUtils.unmodifiableNeverNull(mutationsRecentesTraitees);
		this.detailsMutationsTraiteesRecentes = CollectionsUtils.unmodifiableNeverNull(detailsMutationsTraiteesRecentes);
		this.erreurs = CollectionsUtils.unmodifiableNeverNull(erreurs);
		this.enSouffrance = CollectionsUtils.unmodifiableNeverNull(enSouffrance);
	}

	@NotNull
	public Map<EtatEvenementOrganisation, Integer> getEtats() {
		return etats;
	}

	@NotNull
	public Map<EtatEvenementOrganisation, Integer> getEtatsNouveaux() {
		return etatsNouveaux;
	}

	@NotNull
	public Map<MutationsTraiteesStatsKey, Integer> getMutationsTraitees() {
		return mutationsTraitees;
	}

	@NotNull
	public Map<MutationsTraiteesStatsKey, Integer> getMutationsRecentesTraitees() {
		return mutationsRecentesTraitees;
	}

	@NotNull
	public List<DetailMutationTraitee> getDetailsMutationsTraiteesRecentes() {
		return detailsMutationsTraiteesRecentes;
	}

	@NotNull
	public List<ErreurInfo> getErreurs() {
		return erreurs;
	}

	@NotNull
	public List<EvenementEnSouffranceInfo> getEnSouffrance() {
		return enSouffrance;
	}
}
