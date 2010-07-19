package ch.vd.uniregctb.tiers;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.registre.base.date.RegDate;

public interface TiersDAO extends GenericDAO<Tiers, Long> {

	/**
	 * @param id
	 *            l'id du tiers
	 * @param doNotAutoFlush
	 *            si <b>vrai</b> ne flush pas la session en recherchant le tiers; si <b>faux</b> comportement identique à {@link #get(Long)}
	 * @return le tiers avec l'id spécifié, ou <b>null</b> si le tiers n'existe pas.
	 */
	Tiers get(long id, boolean doNotAutoFlush);

	/**
	 * Retourne les <i>count</i> premiers tiers de la base de données.
	 * <p/>
	 * <b>Note:</b> aucune garantie n'est faite sur le contenu des tiers retournés dans le cas où la base contient plus de tiers que le nombre spécifié.
	 *
	 * @param count le nombre maximal de tiers à retourner
	 * @return une liste de tiers
	 */
	List<Tiers> getFirst(int count);

	/**
	 * Liste des collections associées à un tiers.
	 */
	public enum Parts {
		FORS_FISCAUX,
		RAPPORTS_ENTRE_TIERS,
		ADRESSES,
		SITUATIONS_FAMILLE,
		DECLARATIONS,
		PERIODICITES;

		public static Parts fromValue(String v) {
			return valueOf(v);
		}
	}

	/**
	 * Charge un seul lot les tiers dont les ids sont spécifiées en paramètre.
	 * <p>
	 * <b>Attention !</b> Les tiers chargés de cette manière ne doivent en aucun cas être modifiés et sauvé en base : les collections sont
	 * triturées de manière peu charitables et Hibernate n'y retrouverait pas ses petits.
	 * <b>Attention 2 !</b> La session hibernate sera forcée au flushMode=MANUAL.
	 *
	 * @param ids
	 *            les ids des tiers à charger. Au maximum 500 à la fois.
	 * @param parts
	 *            les collections associées aux tiers à précharger.
	 * @return une liste de tiers
	 */
	@Transactional(readOnly = true)
	List<Tiers> getBatch(Collection<Long> ids, Set<Parts> parts);

	/**
	 * Sauve le rapport-entre-tiers spécifié.
	 *
	 * @param object
	 *            le rapport-entre-tiers à sauver
	 * @return le rapport-entre-tiers persisté et à utiliser dans le futur
	 */
	RapportEntreTiers save(RapportEntreTiers object);

	/**
	 * @return la liste de tous les IDs des tiers en base
	 */
	List<Long> getAllIds();

	/**
	 * @return la liste des IDs des tiers flaggé comme "dirty" ou flaggés comme devant être réindexés dans le futur.
	 */
	List<Long> getDirtyIds();

	/**
	 * Retourne la liste de tous les Numéros d'individu des tiers en base
	 *
	 * @return la liste des Numéros d'individu
	 */
	List<Long> getAllNumeroIndividu();

	/**
	 * @param tiersIds                 des numéros de tiers
	 * @param includesComposantsMenage <b>vrai</b> s'il faut inclure les numéros d'individus des personnes physiques faisant partie des ménages communs
	 * @return la liste des numéros d'individu correspondants aux numéros de tiers spécifiés.
	 */
	Set<Long> getNumerosIndividu(Set<Long> tiersIds, boolean includesComposantsMenage);

	/**
	 * Retourne la liste de tous les Numéros d'habitant des tiers en base
	 * @return  la liste de tous les Numéros d'habitant
	 */
	 List<Long> getHabitantsForMajorite( RegDate dateReference) ;

	/**
	 * Retourne une liste de numero de Tiers dans le range passé
	 *
	 * @return la liste des numéros de CTBs trouvés
	 */
	List<Long> getTiersInRange(int ctbStart, int ctbEnd);

	/**
	 * Renvoie le contribuable dont le numero est passe en parametre.
	 *
	 * @param numeroIndividu
	 * @return le contribuable dont le numero est passe en parametre.
	 */
	Contribuable getContribuableByNumero(Long numeroContribuable);


	/**
	 * Renvoie le DPI dont le numero est passe en parametre.
	 *
	 * @param numeroDPI
	 * @return le DPI dont le numero est passe en parametre.
	 */
	DebiteurPrestationImposable getDebiteurPrestationImposableByNumero(Long numeroDPI);

	/**
	 * Renvoie la personne physique dont le numéro d'individu est passé en paramètre
	 *
	 * @param numeroIndividu le numéro de l'individu
	 * @return la personne physique (non-annulée) dont le numéro d'individu est passé en paramètre
	 * <p/><b>Attention !</b> La PP retournée peut être "habitant" ou "non habitant" (ancien habitant)
	 */
	PersonnePhysique getPPByNumeroIndividu(Long numeroIndividu);

	PersonnePhysique getPPByNumeroIndividu(Long numeroIndividu, boolean doNotAutoFlush);

	/**
	 * Renvoie le numéro de la personne physique dont le numéro d'individu est passé en paramètre
	 *
	 * @param numeroIndividu le numéro de l'individu
	 * @param doNotAutoFlush <b>vrai</b> s'il ne faut pas flusher la session hibernate avant d'exécuter la recherche; <b>faux</b> autrement.
	 * @return la personne physique (non-annulée) dont le numéro d'individu est passé en paramètre
	 */
	Long getNumeroPPByNumeroIndividu(Long numeroIndividu, boolean doNotAutoFlush);

	/**
	 * Renvoie l'habitant correspondant au numéro d'individu passé en paramètre.
	 *
	 * @param numeroIndividu le numéro de l'individu.
	 * @return l'habitant (tiers non-annulé) correspondant au numéro d'individu passé en paramètre, ou <b>null</b> s'il n'existe pas.
	 */
	PersonnePhysique getHabitantByNumeroIndividu(Long numeroIndividu);

	PersonnePhysique getHabitantByNumeroIndividu(Long numeroIndividu, boolean doNotAutoFlush);

	/**
	 * Renvoie la collectivité administrative rattachée au numero de collectivité donné.
	 *
	 * @param noTechnique
	 *            le numero de la collectivité
	 * @return le tiers représentant la collectivité administrative correspondant
	 */
	CollectiviteAdministrative getCollectiviteAdministrativesByNumeroTechnique(int numeroTechnique);

	CollectiviteAdministrative getCollectiviteAdministrativesByNumeroTechnique(int numeroTechnique, boolean doNotAutoFlush);

	public List<PersonnePhysique> getSourciers(int noSourcier);

	public List<PersonnePhysique> getAllMigratedSourciers();

	/**
	 * Retourne le tiers spécifié en initialisant les collections qui sont utilisées par l'indexation.
	 */
	public Tiers getTiersForIndexation(long id);

	/**
	 * @return la liste des ménages communs contenus dans la liste des ids spécifiés.
	 */
	List<MenageCommun> getMenagesCommuns(List<Long> ids, Set<Parts> parts);

	/**
	 * @param debiteur un débiteur
	 * @return le contribuable associé au débiteur, s'il existe
	 */
	Contribuable getContribuable(DebiteurPrestationImposable debiteur);

	/**
	 * Met-à-jour les oids assignés sur le tiers spécifiés.
	 *
	 * @param tiersOidsMapping le mapping numéro de tiers vers numéro d'oid.
	 */
	void updateOids(Map<Long,Integer> tiersOidsMapping);

	/**
	 * Retourne la liste de débiteurs qui n'on pas de périodicité
	 * utilisée dans la création de l'historique des périodicités pour chaque debiteur
	 *
	 * @return la liste des debiteurs pour qui on devra construire l'historique de periodicite
	 */
	public List<Long> getListeDebiteursSansPeriodicites();
	
	/**
	 * Ajoute un nouveau for fiscal à un tiers.
	 *
	 * @param tiers
	 *            le tiers sur lequel on veut ajouter un for fiscal
	 * @param forFiscal
	 *            le nouveau for fiscal
	 * @return une nouvelle instance du for fiscal avec son id renseigné.
	 */
	ForFiscal addAndSave(Tiers tiers, ForFiscal forFiscal);
}
