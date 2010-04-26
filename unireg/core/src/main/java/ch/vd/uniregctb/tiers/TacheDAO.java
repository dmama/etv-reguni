package ch.vd.uniregctb.tiers;

import java.util.List;
import java.util.Map;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.type.TypeTache;

public interface TacheDAO extends GenericDAO<Tache, Long> {

	/**
	 * Recherche d'un range de toutes les tâches correspondant au critère de sélection
	 *
	 * @param criterion
	 * @param page
	 * @param pageSize
	 * @return
	 */
	public List<Tache> find (TacheCriteria criterion, ParamPagination paramPagination ) ;

	/**
	 * Recherche les tâches correspondant aux critères spécifiés
	 *
	 * @param criterion
	 * @return liste de taches
	 */
	public List<Tache> find(TacheCriteria criterion);

	/**
	 * Recherche les tâches associées au contribuable spécifié.
	 *
	 * @param noContribuable
	 *            le numéro de contribuable
	 * @return liste de tâches
	 */
	public List<Tache> find(long noContribuable);

	/**
	 * Recherche les tâches correspondant aux critères spécifiés
	 *
	 * @param criterion
	 * @param doNotAutoFlush
	 *            si <b>vrai</b> évite de flusher les objets de la session (attention: certains objets modifiés en mémoire peuvent être
	 *            ignorés en conséquence)
	 * @return liste de taches
	 */
	public List<Tache> find(TacheCriteria criterion, boolean doNotAutoFlush);

	/**
	 * Rechercher et retourne les tâches correspondant aux critères spécifiés
	 *
	 * @param criterion
	 * @return le nombre de tâches trouvées
	 */
	public int count(TacheCriteria criterion);

	/**
	 * Recherche et retourne le nombre de tâches associées au contribuable spécifié.
	 *
	 * @param noContribuable
	 *            le numéro de contribuable
	 * @return le nombre de tâches trouvées
	 */
	public int count(long noContribuable);

	/**
	 * Rechercher et retourne les tâches correspondant aux critères spécifiés
	 *
	 * @param criterion
	 * @param doNotAutoFlush
	 *            si <b>vrai</b> évite de flusher les objets de la session (attention: certains objets modifiés en mémoire peuvent être
	 *            ignorés en conséquence)
	 * @return le nombre de tâches trouvées
	 */
	public int count(TacheCriteria criterion, boolean doNotAutoFlush);

	/**
	 * Vérifie s'il existe au moins une tâche en instance (ou en cours) du type spécifié sur le contribuable donnée.
	 * <p>
	 * Cette méthode <b>ne flush pas</b> la session en cours, <b>mais tient compte</b> des éventuelles tâches non-flushées qui existeraient
	 * dans la session.
	 *
	 * @param noCtb
	 *            le numéro de contribuable
	 * @param type
	 *            le type de tâche
	 * @return <b>vrai</b> s'il y a au moins une tâche en instance; <b>faux</b> autrement.
	 */
	public boolean existsTacheEnInstanceOuEnCours(long noCtb, TypeTache type);

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
	public boolean existsTacheAnnulationEnInstanceOuEnCours(long noCtb, long noDi);

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
	public boolean existsTacheEnvoiEnInstanceOuEnCours(long noCtb, RegDate dateDebut, RegDate dateFin);

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
	public <T extends Tache> List<T> listTaches(long noCtb, TypeTache type);

	/**
	 * Calcul et retourne les statistiques des tâches en instance au moment de l'appel.
	 *
	 * @return une map indexée par numéro d'Oid avec le nombre de tâches et de mouvements de dossier en instance.
	 */
	Map<Integer, TacheStats> getTacheStats();

	public static class TacheStats {
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
