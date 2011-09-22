package ch.vd.uniregctb.security;

import java.util.Set;

public interface IfoSecService {

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
	public boolean isGranted(Role role, String visaOperateur, int codeCollectivite);

	/**
	 * Défini un bypass des procédures IfoSec (pour les environnements de développement et d'intégration uniquement).
	 */
	public void addBypass(IfoSecBypass bypass);

	/**
	 * Retourne la liste des rôles que l'utilisateur spécifié possède pour des raisons de bypass IFOSec (normalement uniquement pour le développement).
	 *
	 *
	 * @param visa un visa d'opérateur
	 * @return une liste de rôles bypassé. Cette liste pour être vide.
	 */
	public Set<Role> getBypass(String visa);
}
