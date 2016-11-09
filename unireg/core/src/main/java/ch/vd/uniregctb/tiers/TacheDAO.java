package ch.vd.uniregctb.tiers;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.type.TypeTache;

public interface TacheDAO extends GenericDAO<Tache, Long> {

	/**
	 * Recherche d'un range de toutes les tâches correspondant au critère de sélection
	 */
	List<Tache> find (TacheCriteria criterion, ParamPagination paramPagination ) ;

	/**
	 * Recherche les tâches correspondant aux critères spécifiés
	 *
	 * @param criterion
	 * @return liste de taches
	 */
	List<Tache> find(TacheCriteria criterion);

	/**
	 * Recherche les tâches associées au contribuable spécifié.
	 *
	 * @param noContribuable
	 *            le numéro de contribuable
	 * @return liste de tâches
	 */
	List<Tache> find(long noContribuable);

	/**
	 * Recherche les tâches correspondant aux critères spécifiés
	 *
	 * @param criterion
	 * @param doNotAutoFlush
	 *            si <b>vrai</b> évite de flusher les objets de la session (attention: certains objets modifiés en mémoire peuvent être
	 *            ignorés en conséquence)
	 * @return liste de taches
	 */
	List<Tache> find(TacheCriteria criterion, boolean doNotAutoFlush);

	/**
	 * Rechercher et retourne les tâches correspondant aux critères spécifiés
	 *
	 * @param criterion
	 * @return le nombre de tâches trouvées
	 */
	int count(TacheCriteria criterion);

	/**
	 * Recherche et retourne le nombre de tâches associées au contribuable spécifié.
	 *
	 * @param noContribuable
	 *            le numéro de contribuable
	 * @return le nombre de tâches trouvées
	 */
	int count(long noContribuable);

	/**
	 * Rechercher et retourne les tâches correspondant aux critères spécifiés
	 *
	 * @param criterion
	 * @param doNotAutoFlush
	 *            si <b>vrai</b> évite de flusher les objets de la session (attention: certains objets modifiés en mémoire peuvent être
	 *            ignorés en conséquence)
	 * @return le nombre de tâches trouvées
	 */
	int count(TacheCriteria criterion, boolean doNotAutoFlush);

	/**
	 * Vérifie s'il existe au moins une tâche de contrôle de dossier en instance (ou en cours) avec le commentaire spécifié
	 * <p>
	 * Cette méthode <b>ne flush pas</b> la session en cours, <b>mais tient compte</b> des éventuelles tâches non-flushées qui existeraient
	 * dans la session.
	 *
	 * @param noCtb         le numéro de contribuable
	 * @param commentaire   le commentaire associé à la tâche
	 * @return <b>vrai</b> s'il y a au moins une tâche en instance; <b>faux</b> autrement.
	 */
	boolean existsTacheControleDossierEnInstanceOuEnCours(long noCtb, @Nullable String commentaire);

	/**
	 * Vérifie s'il existe au moins une tâche d'annulation de DI en instance (ou en cours) pour le contribuable donné et la déclaration donnée.
	 * <p>
	 * Cette méthode <b>ne flush pas</b> la session en cours, <b>mais tient compte</b> des éventuelles tâches non-flushées qui existeraient
	 * dans la session.
	 *
	 * @param noCtb
	 *            le numéro de contribuable
	 * @param noDi
	 *            le numéro de déclaration
	 * @return <b>vrai</b> s'il y a au moins une tâche en instance; <b>faux</b> autrement.
	 */
	boolean existsTacheAnnulationEnInstanceOuEnCours(long noCtb, long noDi);

	/**
	 * Vérifie s'il existe au moins une tâche d'envoi de DI en instance (ou en cours) pour le contribuable donné et la période donnée.
	 * <p>
	 * Cette méthode <b>ne flush pas</b> la session en cours, <b>mais tient compte</b> des éventuelles tâches non-flushées qui existeraient
	 * dans la session.
	 *
	 * @param noCtb
	 *            le numéro de contribuable
	 * @param dateDebut
	 *            la date de début de la période de la déclaration
	 * @param dateFin
	 *            la date de fin de la période de la déclaration
	 * @return <b>vrai</b> s'il y a au moins une tâche en instance; <b>faux</b> autrement.
	 */
	boolean existsTacheEnvoiDIPPEnInstanceOuEnCours(long noCtb, RegDate dateDebut, RegDate dateFin);

	/**
	 * @return la liste (triée par ordre alphabétique), par type de tâche, des commentaires distincts non-vides
	 */
	Map<TypeTache, List<String>> getCommentairesDistincts();

	/**
	 * @return l'ensemble des numéros des collectivités administratives pour lesquelles on a des tâches, quel que soit leur état
	 */
	Set<Integer> getCollectivitesAvecTaches();

	/**
	 * Retourne la liste de toutes les tâches du type spécifié pour le contribuable spécifié.
	 *
	 * @param <T>
	 *            le type de tâche considéré
	 * @param noCtb
	 *            le numéro du contribuable sur lequel la tâche est ouverte
	 * @param type
	 *            le type de tâche considéré
	 * @return une liste de tâches
	 */
	<T extends Tache> List<T> listTaches(long noCtb, TypeTache type);

	/**
	 * Met-à-jour les collectivités administratives assignées sur les tâches en instance des tiers spécifiés.
	 *
	 * @param tiersOidsMapping le mapping numéro de tiers vers numéro d'oid.
	 */
	void updateCollAdmAssignee(Map<Long, Integer> tiersOidsMapping);

	/**
	 * Calcul et retourne les statistiques des tâches en instance au moment de l'appel.
	 *
	 * @return une map indexée par numéro d'Oid avec le nombre de tâches et de mouvements de dossier en instance.
	 */
	Map<Integer, TacheStats> getTacheStats();

	class TacheStats {
		public int tachesEnInstance;
		public int dossiersEnInstance;

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final TacheStats that = (TacheStats) o;

			return dossiersEnInstance == that.dossiersEnInstance && tachesEnInstance == that.tachesEnInstance;
		}

		@Override
		public int hashCode() {
			int result = tachesEnInstance;
			result = 31 * result + dossiersEnInstance;
			return result;
		}
	}
}
