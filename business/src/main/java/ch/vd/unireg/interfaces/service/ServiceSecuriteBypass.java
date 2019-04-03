package ch.vd.unireg.interfaces.service;

import java.util.Set;

import ch.vd.unireg.security.IfoSecBypass;
import ch.vd.unireg.security.Role;

/**
 * Interface implémentée par le service de sécurité qui permet de bypasser certaines procédures pour le test.
 */
public interface ServiceSecuriteBypass {

	/**
	 * Ajoute une procédure de bypass
	 *
	 * @param bypass la procédure à bypasser
	 */
	void addBypass(IfoSecBypass bypass);

	/**
	 * @param visa un visa opérateur
	 * @return les rôles bypasser pour cet opérateur
	 */
	Set<Role> getBypass(String visa);

}
