package ch.vd.uniregctb.security;

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
}
