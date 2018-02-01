package ch.vd.uniregctb.evenement.identification.contribuable;

import java.util.List;
import java.util.Map;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.uniregctb.common.pagination.ParamPagination;

/**
 * Le DAO pour les message d'identification de contribuable.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface IdentCtbDAO extends GenericDAO<IdentificationContribuable, Long> {

	/**
	 * Recherche une liste d'IdentificationContribuable en fonction de critères
	 */
	List<IdentificationContribuable> find(IdentificationContribuableCriteria identificationContribuableCriteria, ParamPagination paramPagination, boolean nonTraiteOnly, boolean archiveOnly,
	                                      boolean suspenduOnly, TypeDemande... typeDemande);

	/**
	 * Nombre d'IdentificationContribuable en fonction de critères
	 */
	int count(IdentificationContribuableCriteria identificationContribuableCriteria, boolean nonTraiteOnly, boolean archiveOnly,
	          boolean suspenduOnly, TypeDemande... typeDemande);


	/**
	 * @param typeDemande type des demandes recherchées
	 * @param emetteur valeur du champ émetteur recherchée
	 * @param businessIdStart début du champ businessId
	 * @return la liste des demandes d'identification qui correspondent aux critères
	 */
	List<IdentificationContribuable> find(TypeDemande typeDemande, String emetteur, String businessIdStart);

	/**
	 * @return une structure de données qui permet de savoir quels sont les types de messages utilisés par type de demande et par etat
	 */
	Map<TypeDemande, Map<IdentificationContribuable.Etat, List<String>>> getTypesMessages();

	/**
	 * @return la liste des émetteurs par etat de la demande d'identification
	 */
	Map<IdentificationContribuable.Etat, List<String>> getEmetteursIds();

	/**
	 * @return la liste des émetteurs par état de la demande d'identification
	 */
	Map<IdentificationContribuable.Etat, List<Integer>> getPeriodesFiscales();

	/**
	 * @return la liste des utilisateurs ayant effectué un traitement
	 */
	List<String> getTraitementUser();

	/**
	 * @return  la liste des états des messages non traités
	 */
	Map<IdentificationContribuable.Etat, List<IdentificationContribuable.Etat>> getEtats();

	/**
	 * @return la liste des types de priorité des messages non traitées
	 */
	Map<IdentificationContribuable.Etat, List<Demande.PrioriteEmetteur>> getPriorites();

	List<IdentificationContribuable> find(IdentificationContribuableCriteria identificationContribuableCriteria, ParamPagination paramPagination, IdentificationContribuableEtatFilter filter,
	                                      TypeDemande... typeDemande);

	int count(IdentificationContribuableCriteria identificationContribuableCriteria, IdentificationContribuableEtatFilter filter, TypeDemande... typeDemande);
}
