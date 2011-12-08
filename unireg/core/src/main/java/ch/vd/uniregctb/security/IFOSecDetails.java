package ch.vd.uniregctb.security;

import ch.vd.securite.model.ProfilOperateur;

public interface IFOSecDetails {

	Integer getIfoSecOID();

	void setIfoSecOID(Integer ifoSecOID);

	String getIfoSecOIDSigle();

	void setIfoSecOIDSigle(String ifoSecOIDSigle);

	ProfilOperateur getIfoSecProfil();

	void setIfoSecProfil(ProfilOperateur ifosecProfil);
}
