package ch.vd.unireg.interfaces.securite;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.security.Operateur;
import ch.vd.unireg.security.ProfileOperateur;

public interface SecuriteConnector {

	String SERVICE_NAME = "SecuriteConnector";

	/**
	 * Recherche un opérateur à partir de son visa.
	 *
	 * @param visa le visa de l'opérateur (zaixxx, ...)
	 * @return un opérateur ou <b>null</b> si l'opérateur n'est pas trouvé.
	 */
	@Nullable
	Operateur getOperateur(@NotNull String visa) throws SecuriteConnectorException;

	/**
	 * Retourne le profil que possède un opérateur pour une collectivité administrative.
	 *
	 * @param visaOperateur    le visa de l'operateur.
	 * @param codeCollectivite le code de la collectivité administrative.
	 * @return le profil que possède un opérateur pour une collectivité administrative.
	 */
	@Nullable
	ProfileOperateur getProfileUtilisateur(String visaOperateur, int codeCollectivite) throws SecuriteConnectorException;

	/**
	 * Recherche les utilisateurs qui sont définis pour une collectivité administrative.
	 *
	 * @param noCollAdmin le numéro de collectivité administrative
	 * @return les visas des utilisateurs correspondants
	 */
	@NotNull
	List<String> getUtilisateurs(int noCollAdmin) throws SecuriteConnectorException;

	/**
	 * Retourne les collectivités administratives d'un opérateur.
	 *
	 * @param visaOperateur le visa de l'opérateur.
	 * @return les ids des collectivités administratives de l'opérateur.
	 */
	@NotNull
	Set<Integer> getCollectivitesOperateur(@NotNull String visaOperateur) throws SecuriteConnectorException;

	/**
	 * Méthode qui permet de tester que le connecteur de sécurité répond bien. Cette méthode est insensible aux caches.
	 *
	 * @throws SecuriteConnectorException en cas de non-fonctionnement du connecteur de sécurité
	 */
	void ping() throws SecuriteConnectorException;
}
