package ch.vd.unireg.security;

public interface IFOSecDetails {

	Integer getIfoSecOID();

	void setIfoSecOID(Integer ifoSecOID);

	String getIfoSecOIDSigle();

	void setIfoSecOIDSigle(String ifoSecOIDSigle);

	IfoSecProfil getIfoSecProfil();

	void setIfoSecProfil(IfoSecProfil ifosecProfil);
}
