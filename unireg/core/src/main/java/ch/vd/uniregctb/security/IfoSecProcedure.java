package ch.vd.uniregctb.security;

public interface IfoSecProcedure {

	/**
	 * @return le code de la procédure.
	 */
	String getCode();

	/**
	 * @return le code d'activité de la procédure.
	 */
	String getCodeActivite();

	/**
	 * @return la désignation de la procédure.
	 */
	String getDesignation();

	/**
	 * @return le numéro de la procédure.
	 */
	int getNumero();
}
