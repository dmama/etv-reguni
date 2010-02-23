package ch.vd.uniregctb.identification.contribuable;

import java.util.List;
import java.util.Map;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.evenement.identification.contribuable.CriteresPersonne;
import ch.vd.uniregctb.evenement.identification.contribuable.Erreur;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuableCriteria;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable.Etat;
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
	 * @return
	 */
	public List<IdentificationContribuable> find (IdentificationContribuableCriteria identificationContribuableCriteria, ParamPagination paramPagination, boolean nonTraiteOnly, boolean archiveOnly, boolean nonTraiterAndSuspendu) ;

	/**
	 * Nombre d'IdentificationContribuable en fonction de critères
	 * @param identificationContribuableCriteria
	 * @param nonTraiteOnly TODO
	 * @param archiveOnly TODO
	 * @param nonTraiterAndSuspendu TODO
	 * @return
	 */
	public int count (IdentificationContribuableCriteria identificationContribuableCriteria, boolean nonTraiteOnly, boolean archiveOnly, boolean nonTraiterAndSuspendu) ;

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
	 */
	public Map<IdentificationContribuable.Etat,Integer> calculerStats(IdentificationContribuableCriteria identificationContribuableCriteria);

	/**Retourn le nom complet du canton emetteur du message
	 * @param emetteurId TODO
	 *
	 * @return
	 * @throws InfrastructureException
	 */
	public String getNomCantonFromEmetteurId(String emetteurId) throws InfrastructureException;
}
