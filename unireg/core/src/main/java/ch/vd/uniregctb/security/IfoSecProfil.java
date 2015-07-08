package ch.vd.uniregctb.security;

import java.util.List;

public interface IfoSecProfil {

	/**
	 * @return l'imprimant du profil opérateur.
	 */
	String getImprimante();

	/**
	 * @return le nom du profil opérateur.
	 */
	String getNom();

	/**
	 * @return le numéro de téléphone du profil opérateur.
	 */
	String getNoTelephone();

	/**
	 * @return le prénom du profil opérateur.
	 */
	String getPrenom();

	/**
	 * @return la liste des procédures du profil opérateur.
	 */
	List<IfoSecProcedure> getProcedures();

	/**
	 * @return le titre du profil opérateur.
	 */
	String getTitre();

	/**
	 * @return le visa du profil opérateur.
	 */
	String getVisaOperateur();
}
