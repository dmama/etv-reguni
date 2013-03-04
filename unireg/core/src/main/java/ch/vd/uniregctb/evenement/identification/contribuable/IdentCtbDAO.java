package ch.vd.uniregctb.evenement.identification.contribuable;

import java.util.List;
import java.util.Map;

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
	 */
	public List<IdentificationContribuable> find(IdentificationContribuableCriteria identificationContribuableCriteria, ParamPagination paramPagination, boolean nonTraiteOnly, boolean archiveOnly,
	                                             boolean suspenduOnly, TypeDemande... typeDemande);

	/**
	 * Nombre d'IdentificationContribuable en fonction de critères
	 */
	public int count(IdentificationContribuableCriteria identificationContribuableCriteria, boolean nonTraiteOnly, boolean archiveOnly,
	                 boolean suspenduOnly, TypeDemande... typeDemande);


	/**
	 * @return une structure de données qui permet de savoir quels sont les types de messages utilisés par type de demande et par etat
	 */
	public Map<TypeDemande, Map<IdentificationContribuable.Etat, List<String>>> getTypesMessages();

	/**
	 * @return la liste des émetteurs par etat de la demande d'identification
	 */
	public Map<IdentificationContribuable.Etat, List<String>> getEmetteursIds();

	/**
	 * @return la liste des émetteurs par état de la demande d'identification
	 */
	public Map<IdentificationContribuable.Etat, List<Integer>> getPeriodesFiscales();

	/**
	 * @return la liste des utilisateurs ayant effectué un traitement
	 */
	public List<String> getTraitementUser();

	/**
	 * @return  la liste des états des messages non traités
	 */
	public Map<IdentificationContribuable.Etat, List<IdentificationContribuable.Etat>> getEtats();

	/**
	 * @return la liste des types de priorité des messages non traitées
	 */
	public Map<IdentificationContribuable.Etat, List<Demande.PrioriteEmetteur>> getPriorites();

	List<IdentificationContribuable> find(IdentificationContribuableCriteria identificationContribuableCriteria, ParamPagination paramPagination, IdentificationContribuableEtatFilter filter,
	                                      TypeDemande... typeDemande);

	int count(IdentificationContribuableCriteria identificationContribuableCriteria, IdentificationContribuableEtatFilter filter, TypeDemande... typeDemande);
}
