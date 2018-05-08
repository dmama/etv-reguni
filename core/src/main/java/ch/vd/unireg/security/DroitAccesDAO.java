package ch.vd.unireg.security;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.tiers.DroitAcces;

/**
 *
 *
 * @author xcifde
 *
 */
public interface DroitAccesDAO extends GenericDAO<DroitAcces, Long> {

	/**
	 * @return le droit d'accès courant entre l'opérateur et le tiers spécifié; ou <b>null</b> si aucun n'accès n'est défini.
	 */
	DroitAcces getDroitAcces(@NotNull String visaOperateur, long tiersId, RegDate date);

	/**
	 * Renvoie la liste des droits d'acces d'un utilisateur
	 */
	List<DroitAcces> getDroitsAcces(@NotNull String visaOperateur);

	/**
	 * Renvoie la liste des droits d'acces d'un utilisateur paginée
	 */
	List<DroitAcces> getDroitsAcces(@NotNull String visaOperateur, ParamPagination paramPagination);

	/**
	 * Renvoie la liste des ids des  droits d'acces d'un utilisateur
	 *
	 * @return la liste des ids des droits d'accès
	 */
	List<Long> getIdsDroitsAcces(@NotNull String visaOperateur);

	/**
	 * @return la liste de tous les droits d'accès existant sur le tiers spécifié.
	 */
	List<DroitAcces> getDroitsAccessTiers(long tiersId);

	/**
	 * @param date
	 *            date de validité des droits d'accès. Cette date est obligatoire.
	 * @return la liste de tous les droits d'accès définis sur le tiers spécifié.
	 */
	List<DroitAcces> getDroitsAccessTiers(long tiersId, RegDate date) ;

	/**
	 * @param date
	 *            date de validité des droits d'accès. Cette date est obligatoire.
	 * @return la liste de tous les droits d'accès définis sur les tiers spécifiés.
	 */
	List<DroitAcces> getDroitsAccessTiers(Set<Long> ids, RegDate date) ;

	/**
	 * @return les ids des contribuables sur lesquels des autorisations ou des restrictions sont actives actuellement.
	 */
	Set<Long> getContribuablesControles();

	Integer getDroitAccesCount(@NotNull String visaOperateur);

	/**
	 * @return la liste des ids des opérateurs qui n'ont pas de visa associé.
	 */
	List<Long> getOperatorsIdsToMigrate();

	/**
	 * Renseigne le visa de l'opérateur spécifié sur tous les droits d'accès qui correspondent.
	 *
	 * @param noOperateur   le numéro technique de l'opérateur
	 * @param visaOperateur le visa de l'opérateur
	 */
	void updateVisa(long noOperateur, @NotNull String visaOperateur);

	/**
	 * Annule tous les droits d'accès de l'opérateur spécifié.
	 * @param noOperateur l'id d'un opérateur
	 */
	void cancelOperateur(Long noOperateur);
}