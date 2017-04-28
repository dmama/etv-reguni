package ch.vd.uniregctb.tiers;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseMandataire;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.documentfiscal.AutreDocumentFiscal;
import ch.vd.uniregctb.foncier.AllegementFoncier;
import ch.vd.uniregctb.rf.Immeuble;

public interface TiersDAO extends GenericDAO<Tiers, Long> {

	/**
	 * @param id
	 *            l'id du tiers
	 * @param doNotAutoFlush
	 *            si <b>vrai</b> ne flush pas la session en recherchant le tiers; si <b>faux</b> comportement identique à {@link #get(java.io.Serializable)}
	 * @return le tiers avec l'id spécifié, ou <b>null</b> si le tiers n'existe pas.
	 */
	Tiers get(long id, boolean doNotAutoFlush);

	/**
	 * Retourne les <i>count</i> premiers tiers de la base de données groupés par classe.
	 * <p/>
	 * <b>Note:</b> aucune garantie n'est faite sur le contenu des tiers retournés dans le cas où la base contient plus de tiers que le nombre spécifié.
	 *
	 * @param count le nombre maximal de tiers à retourner
	 * @return une liste de tiers
	 */
	Map<Class, List<Tiers>> getFirstGroupedByClass(int count);

	/**
	 * Détermine et retourne tous les numéros de tiers liés au tiers spécifié.
	 * <p/>
	 * <b>Note:</b> l'id spécifié est toujours inclus dans le résultat retourné, l'existence du tiers spécifié n'est donc pas vérifiée.
	 *
	 * @param id       un numéro de tiers
	 * @param maxDepth la profondeur maximale de parcour du graphe des tiers liés
	 * @return l'id spécifié + les ids des tiers liés
	 */
	Set<Long> getRelatedIds(long id, int maxDepth);

	/**
	 * Liste des collections associées à un tiers.
	 */
	enum Parts {
		FORS_FISCAUX,
		RAPPORTS_ENTRE_TIERS,
		ADRESSES,
		ADRESSES_MANDATAIRES,
		SITUATIONS_FAMILLE,
		DECLARATIONS,
		PERIODICITES,
		IMMEUBLES,
		ALLEGEMENTS_FISCAUX,
		ETATS_FISCAUX,
		DONNEES_CIVILES,
		BOUCLEMENTS,
		FLAGS,
		REGIMES_FISCAUX,
		ETIQUETTES;

		public static Parts fromValue(String v) {
			return valueOf(v);
		}
	}

	/**
	 * Détermine et retourne l'ensemble des ids des tiers liés (par exemple : lié par un rapport d'appartenance ménage) aux tiers spécifiés par leurs ids.
	 *
	 * @param ids                        des ids de tiers (au maximum 500 ids à la fois)
	 * @param includeContactsImpotSource <b>vrai</b> s'il faut ignorer les rapports de type <i>contact impôt-source</i>.
	 * @return l'ensemble des ids des tiers liés, en incluant les ids spécifiés en entrée.
	 */
	Set<Long> getIdsTiersLies(Collection<Long> ids, boolean includeContactsImpotSource);

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
	 *
	 * @param includeCancelled <b>vrai</b> s'il faut inclure les tiers annulés; <b>faux</b> autrement.
	 * @param types            les types demandés
	 * @return les IDs de tous les tiers des types spécifiés qui existent dans la base
	 */
	List<Long> getAllIdsFor(boolean includeCancelled, TypeTiers... types);

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
	Set<Long> getNumerosIndividu(Collection<Long> tiersIds, boolean includesComposantsMenage);

	/**
	 * @param noIndividuSource le numéro de l'individu à l'origine de la recherche
	 * @return un ensemble des numéros d'individu distincts directement liés par un lien de parenté à la personne physique dont le numéro d'individu est donné
	 */
	Set<Long> getNumerosIndividusLiesParParente(long noIndividuSource);

	/**
	 * Détermine et retourne la liste des numéros des PMs contenues dans le liste de tiers spécifiée.
	 *
	 * @param tiersIds une liste d'ids de tiers
	 * @return les numéros des PMs trouvées; ou <b>null</b> si aucune PM n'est trouvée.
	 */
	@Nullable
	List<Long> getNumerosPMs(Collection<Long> tiersIds);

	/**
	 * Retourne la liste de tous les Numéros d'habitant des tiers en base
	 * @return la liste de tous les Numéros d'habitant
	 */
	List<Long> getHabitantsForMajorite(RegDate dateReference);

	/**
	 * Retourne une liste de numero de Tiers dans le range passé
	 *
	 * @return la liste des numéros de CTBs trouvés
	 */
	List<Long> getTiersInRange(int ctbStart, int ctbEnd);

	/**
	 * Renvoie le contribuable dont le numero est passe en parametre.
	 *
	 * @param numeroContribuable
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
	PersonnePhysique getPPByNumeroIndividu(long numeroIndividu);

	PersonnePhysique getPPByNumeroIndividu(long numeroIndividu, boolean doNotAutoFlush);

	Entreprise getEntrepriseByNumeroOrganisation(long numeroOrganisation);

	Etablissement getEtablissementByNumeroSite(long numeroSite);

	/**
	 * Renvoie le numéro de la personne physique dont le numéro d'individu est passé en paramètre
	 *
	 * @param numeroIndividu le numéro de l'individu
	 * @param doNotAutoFlush <b>vrai</b> s'il ne faut pas flusher la session hibernate avant d'exécuter la recherche; <b>faux</b> autrement.
	 * @return la personne physique (non-annulée) dont le numéro d'individu est passé en paramètre
	 */
	Long getNumeroPPByNumeroIndividu(long numeroIndividu, boolean doNotAutoFlush);

	/**
	 * Renvoie l'habitant correspondant au numéro d'individu passé en paramètre.
	 *
	 * @param numeroIndividu le numéro de l'individu.
	 * @return l'habitant (tiers non-annulé) correspondant au numéro d'individu passé en paramètre, ou <b>null</b> s'il n'existe pas.
	 */
	PersonnePhysique getHabitantByNumeroIndividu(long numeroIndividu);

	PersonnePhysique getHabitantByNumeroIndividu(long numeroIndividu, boolean doNotAutoFlush);

	/**
	 * Renvoie la collectivité administrative rattachée au numero de collectivité donné.
	 *
	 * @param numeroTechnique
	 *            le numero de la collectivité
	 * @return le tiers représentant la collectivité administrative correspondant
	 */
	CollectiviteAdministrative getCollectiviteAdministrativesByNumeroTechnique(int numeroTechnique);

	/**
	 * Renvoie la collectivité administrative rattachée au numero de district donné.
	 *
	 * @param numeroDistrict le numéro du district
	 * @param doNotAutoFlush <code>true</code> s'il faut empêcher Hibernate de flusher la session en cours
	 * @return le tiers représentant la collectivité administrative correspondante
	 */
	CollectiviteAdministrative getCollectiviteAdministrativeForDistrict(int numeroDistrict, boolean doNotAutoFlush);

	/**
	 * Renvoie la collectivité administrative rattachée au numero de région donné.
	 *
	 * @param numeroRegion le numéro du région
	 * @return le tiers représentant la collectivité administrative correspondante
	 */
	CollectiviteAdministrative getCollectiviteAdministrativeForRegion(int numeroRegion);


	CollectiviteAdministrative getCollectiviteAdministrativesByNumeroTechnique(int numeroTechnique, boolean doNotAutoFlush);

	List<PersonnePhysique> getSourciers(int noSourcier);

	List<PersonnePhysique> getAllMigratedSourciers();

	/**
	 * Retourne le tiers spécifié en initialisant les collections qui sont utilisées par l'indexation.
	 */
	Tiers getTiersForIndexation(long id);

	/**
	 * @param debiteur un débiteur
	 * @return le contribuable associé au débiteur, s'il existe
	 */
	Contribuable getContribuable(DebiteurPrestationImposable debiteur);

	/**
	 * @return la liste des identifiants des entreprises ne possèdant pas de régimes fiscals non annulés.
	 */
	List<Long> getEntreprisesSansRegimeFiscal();

	/**
	 * Met-à-jour les oids assignés sur le tiers spécifiés.
	 *
	 * @param tiersOidsMapping le mapping numéro de tiers vers numéro d'oid.
	 */
	void updateOids(Map<Long, Integer> tiersOidsMapping);

	/**
	 * Retourne la liste de débiteurs qui n'on pas de périodicité
	 * utilisée dans la création de l'historique des périodicités pour chaque debiteur
	 *
	 * @return la liste des debiteurs pour qui on devra construire l'historique de periodicite
	 */
	List<Long> getListeDebiteursSansPeriodicites();

	/**
	 * Ajoute un nouveau for fiscal à un tiers.
	 *
	 * @param tiers
	 *            le tiers sur lequel on veut ajouter un for fiscal
	 * @param forFiscal
	 *            le nouveau for fiscal
	 * @return une nouvelle instance du for fiscal avec son id renseigné.
	 */
	<T extends ForFiscal> T addAndSave(Tiers tiers, T forFiscal);

	/**
	 * Ajoute un nouvel immeuble à un tiers.
	 *
	 * @param tiers    le tiers sur lequel on veut ajouter un for fiscal
	 * @param immeuble le nouvel immeuble
	 * @return une nouvelle instance de l'immeuble avec son id renseigné.
	 */
	Immeuble addAndSave(Contribuable tiers, Immeuble immeuble);

	/**
	 * Ajoute une nouvelle decision Aci sur un tiers
	 * @param tiers
	 * @param decisionAci
	 * @return la nouvelle décision avec un numéro
	 */
	DecisionAci addAndSave(Contribuable tiers, DecisionAci decisionAci);

	/**
	 * Ajoute une nouvelle déclaration à un tiers
	 *
	 * @param tiers       le tiers auquel on veut ajouter la déclaration
	 * @param declaration déclaration à ajouter
	 * @return la déclaration une fois sauvegardée, avec son ID renseigné
	 */
	Declaration addAndSave(Tiers tiers, Declaration declaration);

	/**
	 * Ajoute une nouvelle periodicite à un debiteur.
	 *
	 * @param debiteur    le debiteur sur lequel on va ajouter la periodicite
	 * @param periodicite la nouvelle periodicite
	 * @return une nouvelle instance de la periodicite avec son id renseigné.
	 */
	Periodicite addAndSave(DebiteurPrestationImposable debiteur, Periodicite periodicite);

	/**
	 * Ajoute une nouvelle situation de famille à un contribuable.
	 *
	 * @param contribuable le contribuable sur lequel on veut ajouter un for fiscal
	 * @param situation    la nouvelle situation de famille
	 * @return une nouvelle instance de la situation de famille avec son id renseigné.
	 */
	SituationFamille addAndSave(ContribuableImpositionPersonnesPhysiques contribuable, SituationFamille situation);

	/**
	 * Ajoute une nouvelle adresse à un tiers.
	 *
	 * @param tiers   le tiers sur lequel on veut ajouter un for fiscal
	 * @param adresse la nouvelle adresse
	 * @return une nouvelle instance de l'adresse avec son id renseigné.
	 */
	AdresseTiers addAndSave(Tiers tiers, AdresseTiers adresse);

	/**
	 * Ajoute une nouvelle adresse mandataire à un contribuable
	 * @param contribuable le contribuable sur lequel on veut ajouter une adesse mandataire
	 * @param adresse la nouvelle adresse
	 * @return une nouvelle instance de l'adresse mandataire avec son id renseigné
	 */
	AdresseMandataire addAndSave(Contribuable contribuable, AdresseMandataire adresse);

	/**
	 * Ajoute une nouvelle identifiant de personne à une personne physique
	 *
	 * @param pp    une personne physique
	 * @param ident l'identifiant à ajouter
	 * @return une nouvelle instande de l'identificant avec son id renseigné.
	 */
	IdentificationPersonne addAndSave(PersonnePhysique pp, IdentificationPersonne ident);

	/**
	 * Ajoute un nouvel identifiant d'entreprise à un contribuable
	 * @param ctb   un contribuable
	 * @param ident le numéro IDE à ajouter
	 * @return une nouvelle instance de l'identifiant avec son ID renseigné
	 */
	IdentificationEntreprise addAndSave(Contribuable ctb, IdentificationEntreprise ident);

	/**
	 * Ajoute un nouveau domicile à l'établissement fourni
	 * @param etb l'établissement
	 * @param domicile le domicile à ajouter
	 * @return une nouvelle instance du domicile avec son ID renseigné
	 */
	DomicileEtablissement addAndSave(Etablissement etb, DomicileEtablissement domicile);

	/**
	 * Ajoute un nouvel allègement fiscal à l'entreprise fournie
	 * @param entreprise l'entreprise en question
	 * @param allegement l'allègement fiscal à ajouter
	 * @return une nouvelle instance de l'allègement fiscal avec son ID renseigné
	 */
	<T extends AllegementFiscal> T addAndSave(Entreprise entreprise, T allegement);

	DonneeCivileEntreprise addAndSave(Entreprise entreprise, DonneeCivileEntreprise donneeCivile);

	/**
	 * Ajoute une nouvelle donnée de bouclement à l'entreprise fournie
	 * @param entreprise l'entreprise en question
	 * @param bouclement la donnée de bouclement à ajouter
	 * @return une nouvelle instance de bouclement avec son ID renseigné
	 */
	Bouclement addAndSave(Entreprise entreprise, Bouclement bouclement);

	/**
	 * Ajoute un nouveau régime fiscal à l'entreprise fournie
	 * @param entreprise l'entreprise en question
	 * @param regime le régime fiscal à ajouter
	 * @return une nouvelle instance de régime fiscal avec son ID renseigné
	 */
	RegimeFiscal addAndSave(Entreprise entreprise, RegimeFiscal regime);

	/**
	 * Ajoute un nouvau flag à l'entrprise fournie
	 * @param entreprise l'entreprise en question
	 * @param etat l'état à ajouter
	 * @return une nouvelle instance de flag avec son ID renseigné
	 */
	EtatEntreprise addAndSave(Entreprise entreprise, EtatEntreprise etat);

	/**
	 * Ajoute un nouvau flag à l'entrprise fournie
	 * @param entreprise l'entreprise en question
	 * @param flag le flag à ajouter
	 * @return une nouvelle instance de flag avec son ID renseigné
	 */
	FlagEntreprise addAndSave(Entreprise entreprise, FlagEntreprise flag);

	/**
	 * Ajoute un nouvel "autre document fiscal" à l'entreprise fournie
	 * @param entreprise l'entreprise en question
	 * @param document le document à ajouter
	 * @param <T> le type exact du document
	 * @return une nouvelle instance du document avec son ID renseigné
	 */
	<T extends AutreDocumentFiscal> T addAndSave(Entreprise entreprise, T document);

	/**
	 * Ajoute un nouvel "allègement foncier" au contribuable fourni
	 * @param contribuable le contribuable en question
	 * @param allegementFoncier l'allègement à ajouter
	 * @param <T> le type exact de l'allègement
	 * @return une nouvelle instance de l'allègement avec son ID renseigné
	 */
	<T extends AllegementFoncier> T addAndSave(Contribuable contribuable, T allegementFoncier);

	/**
	 * Retourne les numéros des contribuables modifiés entre un intervalle de temps passé en paramètre.
	 *
	 *
	 * @param dateDebutRech Date de début de la recherche
	 * @param dateFinRech Date de fin de la recherche
	 * @return la liste des ids des contribuables modifiés
	 */
	List<Long> getListeCtbModifies(Date dateDebutRech, Date dateFinRech);

	/**
	 * @return la liste des identifiants de tiers personnes physiques connues dans le registre civil (= avec un numéro d'individu assigné)
	 */
	List<Long> getIdsConnusDuCivil();

	/**
	 * @return la liste des identifiants de tiers personnes physiques dont le flag "parentesDirty" est vrai
	 */
	List<Long> getIdsParenteDirty();

	/**
	 * Assigne la valeur donnée du flag de blocage de remboursement automatique sur le tiers indiqué
	 * @param tiersId identifiant du tiers concerné
	 * @param newFlag nouvelle valeur du flag de blocage de remboursement automatique
	 * @return <code>true</code> si le tiers a été modifié, <code>false</code> sinon
	 */
	boolean setFlagBlocageRemboursementAutomatique(long tiersId, boolean newFlag);
}
