package ch.vd.uniregctb.evenement.identification.contribuable;

import java.util.List;

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
	 * @param identificationContribuableCriteria
	 * @param paramPagination
	 * @param nonTraiteOnly TODO
	 * @param archiveOnly TODO
	 * @return
	 */
	public List<IdentificationContribuable> find (IdentificationContribuableCriteria identificationContribuableCriteria, ParamPagination paramPagination, boolean nonTraiteOnly, boolean archiveOnly,boolean nonTraiteAndSuspendu) ;

	/**
	 * Nombre d'IdentificationContribuable en fonction de critères
	 * @param identificationContribuableCriteria
	 * @param nonTraiteOnly TODO
	 * @param archiveOnly TODO
	 * @return
	 */
	public int count (IdentificationContribuableCriteria identificationContribuableCriteria, boolean nonTraiteOnly, boolean archiveOnly, boolean nonTraiteAndSuspendu) ;

	/**
	 * Récupère la liste des types de message
	 * @return
	 */
	public List<String> getTypesMessage () ;

	/**
	 * Récupère la liste des émetteurs
	 * @return
	 */
	public List<String> getEmetteursId ();

}
