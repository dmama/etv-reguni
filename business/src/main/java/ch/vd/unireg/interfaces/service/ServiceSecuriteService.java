package ch.vd.unireg.interfaces.service;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrativeUtilisateur;
import ch.vd.unireg.interfaces.infra.data.TypeCollectivite;
import ch.vd.unireg.interfaces.service.host.Operateur;
import ch.vd.unireg.security.IfoSecProfil;

public interface ServiceSecuriteService {

	String SERVICE_NAME = "ServiceSecurite";

	/**
	 * Retourne la liste des collectivités administrative d'un opérateur.
	 *
	 * @param visaOperateur
	 *            le visa de l'opérateur.
	 * @return la liste des collectivités administrative de l'opérateur.
	 */
	List<CollectiviteAdministrativeUtilisateur> getCollectivitesUtilisateur(String visaOperateur);

	/**
	 * Retourne le profil que possède un opérateur pour une collectivité administrative.
	 *
	 * @param visaOperateur
	 *            le visa de l'operateur.
	 * @param codeCollectivite
	 *            le code de la collectivité administrative.
	 * @return le profil que possède un opérateur pour une collectivité administrative.
	 */
	IfoSecProfil getProfileUtilisateur(String visaOperateur, int codeCollectivite);


	/**
	 * Retourne tous les utilisateurs
	 *
	 * @param typesCollectivite
	 * @return la liste des utilisateurs
	 */
	List<Operateur> getUtilisateurs(List<TypeCollectivite> typesCollectivite) ;

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
	Operateur getOperateur(@NotNull String visa);
}
