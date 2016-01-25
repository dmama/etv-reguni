package ch.vd.uniregctb.migration.pm.regpm;

/**
 * Interface implémentée par les entités qui exposent des coordonnées financières
 */
public interface CoordonneesFinancieresContainer {

	/**
	 * @return un IBAN
	 */
	String getIban();

	/**
	 * @return un numéro de compte CCP
	 */
	String getNoCCP();

	/**
	 * @return un numéro de compte bancaire (la banque est l'institution financière)
	 */
	String getNoCompteBancaire();

	/**
	 * @return l'institution financière liée au compte bancaire
	 */
	RegpmInstitutionFinanciere getInstitutionFinanciere();
}
