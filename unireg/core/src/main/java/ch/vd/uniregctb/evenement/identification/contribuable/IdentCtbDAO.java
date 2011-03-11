package ch.vd.uniregctb.evenement.identification.contribuable;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.uniregctb.common.ParamPagination;

/**
 * Le DAO pour les message d'identification de contribuable.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface IdentCtbDAO extends GenericDAO<IdentificationContribuable, Long> {

	/**
	 * Recherche une liste d'IdentificationContribuable en fonction de critères
	 *
	 * @param identificationContribuableCriteria
	 *
	 * @param paramPagination
	 * @param nonTraiteOnly   TODO
	 * @param archiveOnly     TODO
	 * @param typeDemande
	 * @return
	 */

	public List<IdentificationContribuable> find(IdentificationContribuableCriteria identificationContribuableCriteria, ParamPagination paramPagination, boolean nonTraiteOnly, boolean archiveOnly,
	                                             boolean nonTraiteAndSuspendu, TypeDemande typeDemande);

	/**
	 * Nombre d'IdentificationContribuable en fonction de critères
	 *
	 * @param identificationContribuableCriteria
	 *
	 * @param nonTraiteOnly TODO
	 * @param archiveOnly   TODO
	 * @param typeDemande
	 * @return
	 */

	public int count(IdentificationContribuableCriteria identificationContribuableCriteria, boolean nonTraiteOnly, boolean archiveOnly, boolean nonTraiteAndSuspendu, TypeDemande typeDemande);



	/**
	 * Récupère la liste des types de message quel que soit l'etat
	 *
	 * @return
	 */

	public List<String> getTypesMessage();


	/**
	 * Récupère la liste des types de message avec un état non traité
	 *
	 * @return
	 */

	public List<String> getTypesMessageEtatsNonTraites();

/**
	 * Récupère la liste des types de message
	 *
	 * @return
	 */

	public List<String> getTypesMessageEtatsTraites();


	/**
	 * Récupère la liste des émetteurs
	 *
	 * @return
	 */
	public List<String> getEmetteursId();

	/**
	 * Récupère la liste des émetteurs pour les messages non traités
	 *
	 * @return
	 */
	public List<String> getEmetteursIdEtatsNonTraites();

	/**
	 * Récupère la liste des émetteurs pour les messages traités
	 *
	 * @return
	 */
	public List<Integer> getPeriodeEtatsTraites();


	/**
	 * Récupère la liste des périodes quel que soit l'état
	 *
	 * @return
	 */
	public List<Integer> getPeriodes();

	/**
	 * Récupère la liste des périodes pour les messages non traités
	 *
	 * @return
	 */
	public List<Integer> getPeriodeEtatsNonTraites();

	/**
	 * Récupère la liste des périodes pour les messages traités
	 *
	 * @return
	 */
	public List<String> getEmetteursIdEtatsTraites();


	/**Recupère la lliste des utilisateurs ayant effectué un traitement
	 *
	 * @return
	 */
	public List<String> getTraitementUser();

	/**Retourne la liste des états des messages non traités
	 *
	 * @return
	 */
	public List<IdentificationContribuable.Etat> getListeEtatsMessagesNonTraites();

	/**Retourne la liste des états des messages traités
	 *
	 * @return
	 */
	public List<IdentificationContribuable.Etat> getListeEtatsMessagesTraites();


	/**Retourne la liste des types de priorité des messages non traitées
	 *
	 */
	public List<Demande.PrioriteEmetteur> getListePrioriteMessagesNonTraites();


	/**Retourne la liste des types de priorité des messages traitées
	 *
	 */
	public List<Demande.PrioriteEmetteur> getListePrioriteMessagesTraites();
}
