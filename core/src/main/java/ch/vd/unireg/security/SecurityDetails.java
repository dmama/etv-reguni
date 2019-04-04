package ch.vd.unireg.security;

public interface SecurityDetails {

	/**
	 * @return l'id de l'OID dans lequel l'utilisateur est loggé
	 */
	Integer getOID();

	/**
	 * @return le sigle de l'OID dans lequel l'utilisateur est loggé
	 */
	String getOIDSigle();

	/**
	 * @return le profil de sécurité de l'utilisateur loggé
	 */
	ProfileOperateur getProfil();

	void setProfil(ProfileOperateur profil);
}
