package ch.vd.uniregctb.role;

import ch.vd.uniregctb.common.StatusManager;

/**
 * Service de production des rôles pour les communes vaudoises.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface RoleService {

	/**
	 * Produit la liste de contribuables de toutes les communes (et fractions de commune) vaudoise pour la période fiscale spécifiée.
	 *
	 * @param anneePeriode
	 *            l'année de la période fiscale considérée.
	 * @return un rapport (technique) sur les rôles par commune et contribuables.
	 */
	ProduireRolesResults produireRolesPourToutesCommunes(int anneePeriode, StatusManager status)
			throws ServiceException;

	/**
	 * Produit la liste de contribuables d'une commune (ou fraction de commune) vaudoise pour la période fiscale spécifiée.
	 * <p>
	 * Note: le rapport peut contenir quelques résultats pour des communes autres que la commune spécifiée, en fonction des déménagement des
	 * contribuables.
	 *
	 * @param anneePeriode
	 *            l'année de la période fiscale considérée.
	 * @param noOfsCommune
	 *            le numéro Ofs étendu de la commune à traiter
	 * @return un rapport (technique) sur les rôles des contribuables de la commune spécifiée.
	 */
	ProduireRolesResults produireRolesPourUneCommune(int anneePeriode, int noOfsCommune, StatusManager status)
			throws ServiceException;

	/**
	 * Produit la liste de contribuables d'un office d'impôt pour la période fiscale spécifiée.
	 * <p>
	 * Note: le rapport peut contenir quelques résultats pour des communes autres que celles gérées par l'office d'impôt, en fonction des
	 * déménagement des contribuables.
	 *
	 * @param anneePeriode
	 *            l'année de la période fiscale considérée.
	 * @param oid
	 *            l'id de l'office d'impôt concerné
	 * @return un rapport (technique) sur les rôles des contribuables de la commune spécifiée.
	 */
	ProduireRolesResults produireRolesPourUnOfficeImpot(int anneePeriode, int oid, StatusManager status)
			throws ServiceException;
}
