package ch.vd.uniregctb.identification.contribuable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.mutable.Mutable;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.uniregctb.common.pagination.ParamPagination;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.evenement.identification.contribuable.CriteresEntreprise;
import ch.vd.uniregctb.evenement.identification.contribuable.CriteresPersonne;
import ch.vd.uniregctb.evenement.identification.contribuable.Erreur;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable.Etat;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuableCriteria;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuableEtatFilter;
import ch.vd.uniregctb.evenement.identification.contribuable.TypeDemande;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.PersonnePhysique;

/**
 * Service d'identification de contribuable (fonctionnalité initialement demandée par l'application Meldewesen).
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface IdentificationContribuableService {

	/**
	 * Au delà de 5 résultats positifs d'identification pour une demande, on ne donne plus le détail...
	 */
	int NB_MAX_RESULTS_POUR_LISTE_IDENTIFICATION = 5;

	/**
	 * Effectue une recherche des contribuables (= personnes physiques) en utilisant les critères spécifiés. Toutes les critères spécifiés
	 * sont déterminants (c'est-à-dire qu'ils sont combinés en utilisant l'opérateur booléen ET).
	 *
	 * @param criteres les critères de recherche du contribuable
	 * @param upiAutreNavs si non-null, en sortie, contient le cas échéant le numéro AVS fournit par l'UPI (s'il est différent du numéro AVS présent dans la demande)
	 * @return une liste contenant 0 ou plus numéros de contribuable.
	 * @throws TooManyIdentificationPossibilitiesException si le nombre de résultats de l'identification dépasse le seuil {@link #NB_MAX_RESULTS_POUR_LISTE_IDENTIFICATION}
	 * @see #NB_MAX_RESULTS_POUR_LISTE_IDENTIFICATION
	 */
	List<Long> identifiePersonnePhysique(CriteresPersonne criteres, @Nullable Mutable<String> upiAutreNavs) throws TooManyIdentificationPossibilitiesException;

	/**
	 * Effectue une recherche des contribuables (= entreprises et autres communautés) en utilisant les critères spécifiés
	 * @param criteres les critères de recherche
	 * @return une liste contenant 0 ou plus numéros de contribuable
	 * @throws TooManyIdentificationPossibilitiesException si le nombre de résultats de l'identification dépasse le seuil {@link #NB_MAX_RESULTS_POUR_LISTE_IDENTIFICATION}
	 * @see #NB_MAX_RESULTS_POUR_LISTE_IDENTIFICATION
	 */
	List<Long> identifieEntreprise(CriteresEntreprise criteres) throws TooManyIdentificationPossibilitiesException;

	/**
	 * Recherche une liste d'IdentificationContribuable en fonction de critères
	 */
	List<IdentificationContribuable> find(IdentificationContribuableCriteria identificationContribuableCriteria, ParamPagination paramPagination,
	                                      IdentificationContribuableEtatFilter filter, TypeDemande... typeDemande);

	/**
	 * Nombre d'IdentificationContribuable en fonction de critères
	 */
	int count(IdentificationContribuableCriteria identificationContribuableCriteria, IdentificationContribuableEtatFilter filter, TypeDemande... typeDemande);

	/**
	 * Force l'identification du contribuable personne physique
	 */
	void forceIdentification(IdentificationContribuable identificationContribuable, PersonnePhysique personne, Etat etat);

	/**
	 * Force l'identification du contribuable entreprise
	 */
	void forceIdentification(IdentificationContribuable identificationContribuable, Entreprise entreprise, Etat etat);

	/**
	 * Impossible à identifier
	 */
	void impossibleAIdentifier(IdentificationContribuable identificationContribuable, Erreur erreur);

	/**
	 * Soumet le message à l'identification
	 */
	void soumettre(IdentificationContribuable message);

	/**
	 * Calcule et retourne les statistiques  par type de message par période et pour chaque état possible
	 */
	Map<IdentificationContribuable.Etat, Integer> calculerStats(IdentificationContribuableCriteria identificationContribuableCriteria);

	/**
	 * Retourn le nom complet du canton emetteur du message
	 */
	String getNomCantonFromEmetteurId(String emetteurId) throws ServiceInfrastructureException;

	/**
	 * Retourne le nom d'un utilisateur traitant en fonction de sa nature
	 *
	 * @return le nom et le visa utilisateur modifié si besoin
	 */
	IdentifiantUtilisateur getNomUtilisateurFromVisaUser(String visaUser);

	/**
	 * Retente une identification automatique sur les messages présents en base
	 * @return <code>true</code> si l'identification a réussi, <code>false</code> sinon
	 */
	boolean tenterIdentificationAutomatiqueContribuable(IdentificationContribuable message);

	/**
	 * Relance l'identification automatique sur les messages en etat intermediaire: A TRAITER, A EXPERTISER, SUSPENDU
	 */
	IdentifierContribuableResults relancerIdentificationAutomatique(RegDate dateTraitement, int nbThreads, StatusManager status, Long idMessage);

	/**
	 * Permet de mettre à jour les caches des critères de recherche
	 */
	void updateCriteres();

	/**
	 * Récupère les valeurs des id emetteurs
	 * @param filter filtre qui permet de ne renvoyer que les valeurs référencées par des demandes dans certains états seulement
	 * @return emetteurs ids
	 */
	Collection<String> getEmetteursId(IdentificationContribuableEtatFilter filter);

	/**
	 * Récupère les valeurs des types de messages
	 * @param filter filtre qui permet de ne renvoyer que les valeurs référencées par des demandes dans certains états seulement
	 * @return emetteurs ids
	 */
	Collection<String> getTypesMessages(IdentificationContribuableEtatFilter filter);

	/**
	 * Récupère les valeurs des types de messages en fonction des types de demande
	 * @param filter filtre qui permet de ne renvoyer que les valeurs référencées par des demandes dans certains états seulement
	 * @param typesDemande MELDEWESEN, NCS, IMPOT_SOURCE
	 * @return emetteurs ids
	 */
	Collection<String> getTypeMessages(IdentificationContribuableEtatFilter filter, TypeDemande... typesDemande);

	/**
	 * Récupère les valeurs des Périodes fiscales
	 * @param filter filtre qui permet de ne renvoyer que les valeurs référencées par des demandes dans certains états seulement
	 * @return periodes fiscales
	 */
	Collection<Integer> getPeriodesFiscales(IdentificationContribuableEtatFilter filter);

	/**
	 * Récupère les ids des users ayant traiter des messages
	 * @return id des users
	 */
	List<String> getTraitementUser();

	/**
	 * Récupère les états des messages
	 * @param filter filtre qui permet de ne renvoyer que les valeurs référencées par des demandes dans certains états seulement
	 * @return liste des états
	 */
	Collection<Etat> getEtats(IdentificationContribuableEtatFilter filter);


	/**Permet de lancer l'identification de contribuable à partir d'une liste de critères
	 *
	 * @param listeCriteresPersonnes
	 * @param status
	 * @param regDate
	 * @param nbThreads
	 * @return
	 */
	IdentifierContribuableFromListeResults identifieFromListe(List<CriteresPersonne> listeCriteresPersonnes, StatusManager status, RegDate regDate, int nbThreads);
}
