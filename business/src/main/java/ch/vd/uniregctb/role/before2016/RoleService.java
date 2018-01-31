package ch.vd.uniregctb.role.before2016;

import ch.vd.uniregctb.common.StatusManager;

/**
 * Service de production des rôles pour les communes vaudoises.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface RoleService {

	/**
	 * Produit la liste des contribuables PP de toutes les communes (et fractions de commune) vaudoise pour la période fiscale spécifiée.
	 *
	 * @param anneePeriode
	 *            l'année de la période fiscale considérée.
	 * @param nbThreads nombre de threads de traitement
	 * @return un rapport (technique) sur les rôles par commune et contribuables.
	 */
	ProduireRolesPPCommunesResults produireRolesPPPourToutesCommunes(int anneePeriode, int nbThreads, StatusManager status) throws ServiceException;

	/**
	 * Produit la liste des contribuables PP d'une commune (ou fraction de commune) vaudoise pour la période fiscale spécifiée.
	 * <p>
	 * Note: le rapport peut contenir quelques résultats pour des communes autres que la commune spécifiée, en fonction des déménagement des
	 * contribuables.
	 *
	 * @param anneePeriode
	 *            l'année de la période fiscale considérée.
	 * @param noOfsCommune
	 *            le numéro Ofs de la commune à traiter
	 * @return un rapport (technique) sur les rôles des contribuables de la commune spécifiée.
	 */
	ProduireRolesPPCommunesResults produireRolesPPPourUneCommune(int anneePeriode, int noOfsCommune, int nbThreads, StatusManager status) throws ServiceException;

	/**
	 * Produit la liste des contribuables PP d'un office d'impôt pour la période fiscale spécifiée.
	 * <p>
	 * Note: le rapport peut contenir quelques résultats pour des communes autres que celles gérées par l'office d'impôt, en fonction des
	 * déménagement des contribuables.
	 *
	 * @param anneePeriode
	 *            l'année de la période fiscale considérée.
	 * @param oid
	 *            l'id de l'office d'impôt concerné
	 * @param nbThreads nombre de threads de traitement
	 * @return un rapport (technique) sur les rôles des contribuables de l'office d'impôt spécifié
	 */
	ProduireRolesOIDsResults produireRolesPourUnOfficeImpot(int anneePeriode, int oid, int nbThreads, StatusManager status) throws ServiceException;

	/**
	 * Produit la liste des contribuables PP de tous les offices d'impôt pour la période fiscale spécifiée.
	 * <p>
	 * Note: le rapport peut contenir quelques résultats pour des communes autres que celles gérées par l'office d'impôt, en fonction des
	 * déménagement des contribuables.
	 *
	 * @param anneePeriode
	 *            l'année de la période fiscale considérée.
	 * @param nbThreads nombre de threads de traitement
	 * @return les rapports (techniques) sur les rôles des contribuables dans chaque OID
	 */
	ProduireRolesOIDsResults[] produireRolesPourTousOfficesImpot(int anneePeriode, int nbThreads, StatusManager status) throws ServiceException;

	/**
	 * Produit la liste des contribuables PM de toutes les communes (et fractions de commune) vaudoise pour la période fiscale spécifiée.
	 *
	 * @param anneePeriode
	 *            l'année de la période fiscale considérée.
	 * @param nbThreads nombre de threads de traitement
	 * @return un rapport (technique) sur les rôles par commune et contribuables.
	 */
	ProduireRolesPMCommunesResults produireRolesPMPourToutesCommunes(int anneePeriode, int nbThreads, StatusManager status) throws ServiceException;

	/**
	 * Produit la liste des contribuables PM d'une commune (ou fraction de commune) vaudoise pour la période fiscale spécifiée.
	 * <p>
	 * Note: le rapport peut contenir quelques résultats pour des communes autres que la commune spécifiée, en fonction des déménagement des
	 * contribuables.
	 *
	 * @param anneePeriode
	 *            l'année de la période fiscale considérée.
	 * @param noOfsCommune
	 *            le numéro Ofs de la commune à traiter
	 * @return un rapport (technique) sur les rôles des contribuables de la commune spécifiée.
	 */
	ProduireRolesPMCommunesResults produireRolesPMPourUneCommune(int anneePeriode, int noOfsCommune, int nbThreads, StatusManager status) throws ServiceException;

	/**
	 * Produit la liste des contribuables PM du canton pour la période fiscale considérée
	 * @param anneePeriode l'année de la période fiscale
	 * @param nbThreads le nombre de threads de traitement
	 * @param status le statusManager
	 * @return un rapport (technique) sur les rôles des contribuables PM du canton
	 * @throws ServiceException en cas de souci
	 */
	ProduireRolesOIPMResults produireRolesPourOfficePersonnesMorales(int anneePeriode, int nbThreads, StatusManager status) throws ServiceException;
}
