package ch.vd.uniregctb.iban;

/**
 * DAO pour tester la validité d'un code d'un établissement bancaire. 
 * @author Ludovic Bertin(OOSphere)
 *
 */
public interface ClearingDao {
	
	/**
	 * Teste la validité du numéro de clearing bancaire.
	 * @param numeroClearing	le numéro de clearing bancaire à tester
	 * @return true si le numéro de clearing bancaire est valide
	 */
	boolean isNumeroClearingValid(String numeroClearing);
}
