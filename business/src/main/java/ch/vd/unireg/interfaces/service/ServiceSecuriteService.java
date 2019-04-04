package ch.vd.unireg.interfaces.service;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.TypeCollectivite;
import ch.vd.unireg.interfaces.service.host.Operateur;
import ch.vd.unireg.security.ProcedureSecurite;
import ch.vd.unireg.security.ProfileOperateur;
import ch.vd.unireg.security.Role;

public interface ServiceSecuriteService {

	String SERVICE_NAME = "ServiceSecurite";

	/**
	 * Vérifie que l'opérateur spécifié possède le rôle spécifié.
	 *
	 * @param role
	 *            le rôle dont on veut vérifier l'allocation.
	 * @param visaOperateur
	 *            le visa de l'opérateur
	 * @param codeCollectivite
	 *            le code de la collectivité de l'opérateur
	 *
	 * @return <b>vrai</b> si le rôle spécifié est alloué; <b>faux</b> autrement.
	 */
	default boolean isGranted(@NotNull Role role, @NotNull String visaOperateur, int codeCollectivite) {

		final ProfileOperateur profile = getProfileUtilisateur(visaOperateur, codeCollectivite);
		if (profile == null) {
			// pas de profile, pas de droit
			return false;
		}

		final List<ProcedureSecurite> procedures = profile.getProcedures();
		if (procedures == null) {
			// pas de procédure, pas de droit
			return false;
		}

		for (ProcedureSecurite p : procedures) {
			final String code = p.getCode();
			final Role r = Role.fromCodeProcedure(code);
			if (r == role) {
				// c'est bon, la procédure est trouvée
				return true;
			}
		}

		// pas de procédure trouvée, pas de droit
		return false;
	}

	/**
	 * Retourne la liste des collectivités administrative d'un opérateur.
	 *
	 * @param visaOperateur le visa de l'opérateur.
	 * @return la liste des collectivités administrative de l'opérateur.
	 */
	// TODO : modifier cette méthode pour ne retourner que les liste des numéros de collectivités administratives (car les collectivités administratives
	//        sont des données d'infrastructure, pas de sécurité. Seule la liste des numéros de collectivités administratives associée avec chaque opérateur
	//        est une information de sécurité).
	@NotNull
	List<CollectiviteAdministrative> getCollectivitesUtilisateur(String visaOperateur) throws ServiceSecuriteException;

	/**
	 * @param visaOperateur le visa d'un opérateur.
	 * @return l'id de la collectivité administrative par défaut de l'opérateur spécifié ; ou <i>null</i> si l'opérateur est inconnu ou ne possède pas de collectivité administrative par défaut.
	 */
	@Nullable
	Integer getCollectiviteParDefaut(@NotNull String visaOperateur) throws ServiceSecuriteException;

	/**
	 * Retourne le profil que possède un opérateur pour une collectivité administrative.
	 *
	 * @param visaOperateur    le visa de l'operateur.
	 * @param codeCollectivite le code de la collectivité administrative.
	 * @return le profil que possède un opérateur pour une collectivité administrative.
	 */
	@Nullable
	ProfileOperateur getProfileUtilisateur(String visaOperateur, int codeCollectivite) throws ServiceSecuriteException;

	/**
	 * Retourne les utilisateurs qui sont définis pour certains types de collectivités.
	 *
	 * @param typesCollectivite les types de collectivités
	 * @return la liste des utilisateurs correspondants
	 */
	@NotNull
	List<Operateur> getUtilisateurs(List<TypeCollectivite> typesCollectivite) throws ServiceSecuriteException;

	/**
	 * Recherche un opérateur à partir de son visa.
	 *
	 * @param visa le visa de l'opérateur (zaixxx, ...)
	 * @return un opérateur ou <b>null</b> si l'opérateur n'est pas trouvé.
	 */
	@Nullable
	Operateur getOperateur(@NotNull String visa) throws ServiceSecuriteException;

	/**
	 * Méthode qui permet de tester que le service de sécurité répond bien. Cette méthode est insensible aux caches.
	 *
	 * @throws ServiceSecuriteException en cas de non-fonctionnement du service de sécurité
	 */
	void ping() throws ServiceSecuriteException;
}
