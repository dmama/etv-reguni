package ch.vd.unireg.interfaces.service;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.TypeCollectivite;
import ch.vd.unireg.interfaces.service.host.Operateur;
import ch.vd.unireg.security.ProfileOperateur;

public interface ServiceSecuriteService {
	String SERVICE_NAME = "ServiceSecurite";


	/**
	 * Retourne la liste des collectivités administrative d'un opérateur.
	 *
	 * @param visaOperateur le visa de l'opérateur.
	 * @return la liste des collectivités administrative de l'opérateur.
	 */
	// TODO : modifier cette méthode pour ne retourner que les liste des numéros de collectivités administratives (car les collectivités administratives
	//        sont des données d'infrastructure, pas de sécurité. Seule la liste des numéros de collectivités administratives associée avec chaque opérateur
	//        est une information de sécurité).
	List<CollectiviteAdministrative> getCollectivitesUtilisateur(String visaOperateur) throws ServiceSecuriteException;

	/**
	 * @param visaOperateur le visa d'un opérateur.
	 * @return l'id de la collectivité administrative par défaut de l'opérateur spécifié ; ou <i>null</i> si l'opérateur est inconnu ou ne possède pas de collectivité administrative par défaut.
	 */
	Integer getCollectiviteParDefaut(@NotNull String visaOperateur) throws ServiceSecuriteException;

	/**
	 * Retourne le profil que possède un opérateur pour une collectivité administrative.
	 *
	 * @param visaOperateur    le visa de l'operateur.
	 * @param codeCollectivite le code de la collectivité administrative.
	 * @return le profil que possède un opérateur pour une collectivité administrative.
	 */
	ProfileOperateur getProfileUtilisateur(String visaOperateur, int codeCollectivite) throws ServiceSecuriteException;


	/**
	 * Retourne tous les utilisateurs
	 *
	 * @param typesCollectivite
	 * @return la liste des utilisateurs
	 */
	List<Operateur> getUtilisateurs(List<TypeCollectivite> typesCollectivite) throws ServiceSecuriteException;

	/**
	 * Retourne l'operateur pour l'indivu passé en paramètre.
	 *
	 * @param individuNoTechnique
	 * @return l'operateur pour l'indivu passé en paramètre (NOTE: Les champs de CollectiviteOperateur dans le résultat ne sont pas renseignés).
	 */
	Operateur getOperateur(long individuNoTechnique);

	/**
	 * Recherche un opérateur à partir de son visa.
	 *
	 * @param visa le visa de l'opérateur (zaixxx, ...)
	 * @return un opérateur ou <b>null</b> si l'opérateur n'est pas trouvé.
	 */
	Operateur getOperateur(@NotNull String visa) throws ServiceSecuriteException;

}
