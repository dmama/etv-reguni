package ch.vd.uniregctb.identification.contribuable;

import java.util.List;
import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.evenement.identification.contribuable.CriteresPersonne;
import ch.vd.uniregctb.evenement.identification.contribuable.Erreur;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable.Etat;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuableCriteria;
import ch.vd.uniregctb.evenement.identification.contribuable.TypeDemande;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
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
	 * @param identificationContribuableCriteria
	 * @param paramPagination
	 * @param nonTraiteOnly TODO
	 * @param archiveOnly TODO
	 * @param nonTraiterAndSuspendu TODO
	 * @param typeDemande
	 * @return
	 */
	public List<IdentificationContribuable> find(IdentificationContribuableCriteria identificationContribuableCriteria, ParamPagination paramPagination, boolean nonTraiteOnly, boolean archiveOnly,
	                                             boolean nonTraiterAndSuspendu, TypeDemande typeDemande) ;

	/**
	 * Nombre d'IdentificationContribuable en fonction de critères
	 * @param identificationContribuableCriteria
	 * @param nonTraiteOnly TODO
	 * @param archiveOnly TODO
	 * @param nonTraiterAndSuspendu TODO
	 * @param typeDemande
	 * @return
	 */
	public int count(IdentificationContribuableCriteria identificationContribuableCriteria, boolean nonTraiteOnly, boolean archiveOnly, boolean nonTraiterAndSuspendu, TypeDemande typeDemande) ;

	/**
	 * Force l'identification du contribuable
	 * @param identificationContribuable
	 * @param personne
	 * @param etat TODO
	 * @throws Exception
	 */
	public void forceIdentification(IdentificationContribuable identificationContribuable, PersonnePhysique personne, Etat etat) throws Exception;

	/**
	 * Impossible à identifier
	 * @param identificationContribuable
	 * @param erreur TODO
	 * @throws Exception
	 */
	public void impossibleAIdentifier(IdentificationContribuable identificationContribuable, Erreur erreur) throws Exception ;

	/**
	 * Soumet le message à l'identification
	 *
	 * @param message
	 */
	public void soumettre(IdentificationContribuable message) ;


	/**
	 *
	 * Calcule et retourne les statistiques  par type de message par période et pour chaque état possible
	 * @param identificationContribuableCriteria
	 * @param typeDemande
	 */
	public Map<IdentificationContribuable.Etat,Integer> calculerStats(IdentificationContribuableCriteria identificationContribuableCriteria, TypeDemande typeDemande);

	/**Retourn le nom complet du canton emetteur du message
	 * @param emetteurId TODO
	 *
	 * @return
	 * @throws ServiceInfrastructureException
	 */
	public String getNomCantonFromEmetteurId(String emetteurId) throws ServiceInfrastructureException;


	/**Retourne le nom d'un utilisateur traitant en fonction de sa nature
	 *
	 *
	 *
	 * @param visaUser
	 * @return  le nom et le visa utilisateur modifié si besoin
	 */
	public IdentifiantUtilisateur getNomUtilisateurFromVisaUser(String visaUser);

	/**Retente une identification automatique sur les messages present en base
	 *
	 * @param message
	 * @return 1 si l 'identification a réussi, 0 Sinon
	 * @throws Exception
	 */

	public boolean tenterIdentificationAutomatiqueContribuable(IdentificationContribuable message) throws Exception;

	/**
	 * Relance l'identification automatique sur les messages en etat intermediaire: A TRAITER, A EXPERTISER, SUSPENDU
	 *
	 * @param dateTraitement
	 * @param nbThreads
	 * @param status
	 * @param idMessage
	 * @return
	 */

	IdentifierContribuableResults relancerIdentificationAutomatique(RegDate dateTraitement, int nbThreads, StatusManager status, Long idMessage);
}
