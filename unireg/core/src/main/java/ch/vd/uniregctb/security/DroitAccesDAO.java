package ch.vd.uniregctb.security;

import java.util.List;
import java.util.Set;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.DroitAcces;

/**
 *
 *
 * @author xcifde
 *
 */
public interface DroitAccesDAO extends GenericDAO<DroitAcces, Long> {

	/**
	 * @param date TODO
	 * @return le droit d'accès courant entre l'opérateur et le tiers spécifié; ou <b>null</b> si aucun n'accès n'est défini.
	 */
	DroitAcces getDroitAcces(long operateurId, long tiersId, RegDate date);

	/**
	 * Renvoie la liste des droits d'acces d'un utilisateur
	 * @param noIndividuOperateur
	 * @return
	 */
	List<DroitAcces> getDroitsAcces(long noIndividuOperateur);

		/**
	 * Renvoie la liste des ids des  droits d'acces d'un utilisateur
	 * @param noIndividuOperateur
	 * @return  la liste des ids des droits d'accès
	 */
	List<Long> getIdsDroitsAcces(long noIndividuOperateur);

	/**
	 * @return la liste de tous les droits d'accès existant sur le tiers spécifié.
	 */
	public List<DroitAcces> getDroitsAccessTiers(long tiersId);

	/**
	 * @param date
	 *            date de validité des droits d'accès. Cette date est obligatoire.
	 * @return la liste de tous les droits d'accès définis sur le tiers spécifié.
	 */
	public List<DroitAcces> getDroitsAccessTiers(long tiersId, RegDate date) ;

	/**
	 * @param date
	 *            date de validité des droits d'accès. Cette date est obligatoire.
	 * @return la liste de tous les droits d'accès définis sur les tiers spécifiés.
	 */
	public List<DroitAcces> getDroitsAccessTiers(List<Long> ids, RegDate date) ;

	/**
	 * @return les ids des contribuables sur lesquels des autorisations ou des restrictions sont actives actuellement.
	 */
	Set<Long> getContribuablesControles();
}