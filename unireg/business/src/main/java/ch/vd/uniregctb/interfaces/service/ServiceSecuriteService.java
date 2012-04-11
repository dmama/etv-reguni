package ch.vd.uniregctb.interfaces.service;

import java.rmi.RemoteException;
import java.util.List;

import ch.vd.infrastructure.model.CollectiviteAdministrative;
import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.securite.model.Operateur;
import ch.vd.securite.model.ProfilOperateur;
import ch.vd.securite.service.SecuriteException;

public interface ServiceSecuriteService {

	public static final String SERVICE_NAME = "ServiceSecurite";

	/**
	 * Retourne la liste des collectivités administrative d'un opérateur.
	 *
	 * @param visaOperateur
	 *            le visa de l'opérateur.
	 * @return la liste des collectivités administrative de l'opérateur.
	 * @throws SecuriteException
	 *             si un problème métier survient lors de l'invocation du service.
	 * @throws RemoteException
	 *             si un problème technique survient durant l'invocation du service.
	 */
	List<CollectiviteAdministrative> getCollectivitesUtilisateur(String visaOperateur);

	/**
	 * Retourne la liste des operateurs valides exercant une fonction dans une collectivité données administrative.
	 *
	 * @param codeFonction
	 *            le code de la fonction.
	 * @param noCollectivite
	 *            l'identifiant de la collectivité administrative.
	 * @return la liste des operateurs valides.
	 * @throws SecuriteException
	 *             si un problème métier survient lors de l'invocation du service.
	 * @throws RemoteException
	 *             si un problème technique survient durant l'invocation du service.
	 */
	List<ProfilOperateur> getListeOperateursPourFonctionCollectivite(String codeFonction, int noCollectivite);

	/**
	 * Retourne le profil que possède un opérateur pour une collectivité administrative.
	 *
	 * @param visaOperateur
	 *            le visa de l'operateur.
	 * @param codeCollectivite
	 *            le code de la collectivité administrative.
	 * @return le profil que possède un opérateur pour une collectivité administrative.
	 * @throws SecuriteException
	 *             si un problème métier survient lors de l'invocation du service.
	 * @throws RemoteException
	 *             si un problème technique survient durant l'invocation du service.
	 */
	ProfilOperateur getProfileUtilisateur(String  visaOperateur, int codeCollectivite);


	/**
	 * Retourne tous les utilisateurs
	 *
	 * @param typesCollectivite
	 * @return la liste des utilisateurs
	 */
	public List<Operateur> getUtilisateurs(List<EnumTypeCollectivite> typesCollectivite) ;

    /**
	 * Retourne l'operateur pour l'indivu passé en paramètre.
	 *
	 * @param individuNoTechnique
	 * @return l'operateur pour l'indivu passé en paramètre (NOTE: Les champs de CollectiviteOperateur dans le résultat ne sont pas
	 *         renseignés).
	 */
	Operateur getOperateur(long individuNoTechnique);

	/**
	 * Recherche un opérateur à partir de son visa.
	 *
	 * @param visa
	 *            le visa de l'opérateur (zaixxx, ...)
	 * @return un opérateur ou <b>null</b> si l'opérateur n'est pas trouvé.
	 */
	Operateur getOperateur(String visa);
}
