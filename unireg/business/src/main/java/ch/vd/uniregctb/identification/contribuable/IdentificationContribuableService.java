package ch.vd.uniregctb.identification.contribuable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.evenement.identification.contribuable.CriteresPersonne;
import ch.vd.uniregctb.evenement.identification.contribuable.Erreur;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable.Etat;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuableCriteria;
import ch.vd.uniregctb.evenement.identification.contribuable.TypeDemande;
import ch.vd.uniregctb.tiers.PersonnePhysique;

/**
 * Service d'identification de contribuable (fonctionnalité initialement demandée par l'application Meldewesen).
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface IdentificationContribuableService {

	/**
	 * Effectue une recherche des contribuables (= personnes physiques) en utilisant les critères spécifiés. Toutes les critères spécifiés
	 * sont déterminants (c'est-à-dire qu'ils sont combinés en utilisant l'opérateur booléen ET).
	 *
	 * @param criteres
	 *            les critères de recherche du contribuable
	 * @return une liste contenant 0 ou plus contribuables.
	 */
	List<PersonnePhysique> identifie(CriteresPersonne criteres);

	/**
	 * Recherche une liste d'IdentificationContribuable en fonction de critères
	 */
	public List<IdentificationContribuable> find(IdentificationContribuableCriteria identificationContribuableCriteria, ParamPagination paramPagination, boolean nonTraiteOnly, boolean archiveOnly,
	                                             boolean suspenduOnly, TypeDemande... typeDemande);

	/**
	 * Nombre d'IdentificationContribuable en fonction de critères
	 */
	public int count(IdentificationContribuableCriteria identificationContribuableCriteria, boolean nonTraiteOnly, boolean archiveOnly, boolean suspenduOnly,
	                 TypeDemande... typeDemande);

	/**
	 * Force l'identification du contribuable
	 */
	public void forceIdentification(IdentificationContribuable identificationContribuable, PersonnePhysique personne, Etat etat) throws Exception;

	/**
	 * Impossible à identifier
	 */
	public void impossibleAIdentifier(IdentificationContribuable identificationContribuable, Erreur erreur) throws Exception;

	/**
	 * Soumet le message à l'identification
	 */
	public void soumettre(IdentificationContribuable message);

	/**
	 * Calcule et retourne les statistiques  par type de message par période et pour chaque état possible
	 */
	public Map<IdentificationContribuable.Etat, Integer> calculerStats(IdentificationContribuableCriteria identificationContribuableCriteria);

	/**
	 * Retourn le nom complet du canton emetteur du message
	 */
	public String getNomCantonFromEmetteurId(String emetteurId) throws ServiceInfrastructureException;

	/**
	 * Retourne le nom d'un utilisateur traitant en fonction de sa nature
	 *
	 * @return le nom et le visa utilisateur modifié si besoin
	 */
	public IdentifiantUtilisateur getNomUtilisateurFromVisaUser(String visaUser);

	/**
	 * Retente une identification automatique sur les messages present en base
	 *
	 * @return 1 si l 'identification a réussi, 0 Sinon
	 */

	public boolean tenterIdentificationAutomatiqueContribuable(IdentificationContribuable message) throws Exception;

	/**
	 * Relance l'identification automatique sur les messages en etat intermediaire: A TRAITER, A EXPERTISER, SUSPENDU
	 */

	IdentifierContribuableResults relancerIdentificationAutomatique(RegDate dateTraitement, int nbThreads, StatusManager status, Long idMessage);

	/**
	 * Permet de mettre à jour les caches des critères de recherche
	 */
	public void updateCriteres();

	/**
	 * Récupère les valeurs des id emetteurs
	 * @param filter filtre qui permet de ne renvoyer que les valeurs référencées par des demandes dans certains états seulement
	 * @return emetteurs ids
	 */
	public Collection<String> getEmetteursId(IdentificationContribuableEtatFilter filter);

	/**
	 * Récupère les valeurs des types de messages
	 * @param filter filtre qui permet de ne renvoyer que les valeurs référencées par des demandes dans certains états seulement
	 * @return emetteurs ids
	 */
	public Collection<String> getTypesMessages(IdentificationContribuableEtatFilter filter);

	/**
	 * Récupère les valeurs des types de messages en fonction des types de demande
	 * @param filter filtre qui permet de ne renvoyer que les valeurs référencées par des demandes dans certains états seulement
	 * @param typesDemande MELDEWESEN, NCS, IMPOT_SOURCE
	 * @return emetteurs ids
	 */
	public Collection<String> getTypeMessages(IdentificationContribuableEtatFilter filter, TypeDemande... typesDemande);

	/**
	 * Récupère les valeurs des Périodes fiscales
	 * @param filter filtre qui permet de ne renvoyer que les valeurs référencées par des demandes dans certains états seulement
	 * @return periodes fiscales
	 */
	public Collection<Integer> getPeriodesFiscales(IdentificationContribuableEtatFilter filter);

	/**
	 * Récupère les ids des users ayant traiter des messages
	 * @return id des users
	 */
	public List<String> getTraitementUser();

	/**
	 * Récupère les états des messages
	 * @param filter filtre qui permet de ne renvoyer que les valeurs référencées par des demandes dans certains états seulement
	 * @return liste des états
	 */
	public Collection<Etat> getEtats(IdentificationContribuableEtatFilter filter);
}
