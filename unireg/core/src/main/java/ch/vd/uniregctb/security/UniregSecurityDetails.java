package ch.vd.uniregctb.security;

import java.util.Hashtable;

import ch.vd.securite.model.ProfilOperateur;

public class UniregSecurityDetails extends Hashtable<String, Object> implements IAMDetails, IFOSecDetails {

	private static final long serialVersionUID = 7427380869548372766L;

	private String[] iamRoles = null;
	private String iamFirstName = null;
	private String iamLastName = null;
	private ProfilOperateur ifoSecProfil = null;
	private Integer ifoSecOID = null;
	private String ifoSecOIDSigle = null;

	public UniregSecurityDetails() {
		int z = 0;
	}

	@Override
	public String getIamFirstName() {
		return iamFirstName;
	}

	@Override
	public void setIamFirstName(String firstName) {
		this.iamFirstName = firstName;
	}

	@Override
	public String getIamLastName() {
		return iamLastName;
	}

	@Override
	public void setIamLastName(String lastName) {
		this.iamLastName = lastName;
	}

	@Override
	public String[] getIamRoles() {
		return iamRoles;
	}

	@Override
	public void setIamRoles(String[] roles) {
		iamRoles = roles;
	}

	@Override
	public ProfilOperateur getIfoSecProfil() {
		return ifoSecProfil;
	}

	@Override
	public void setIfoSecProfil(ProfilOperateur ifosecProfil) {
		this.ifoSecProfil = ifosecProfil;
	}

	@Override
	public Integer getIfoSecOID() {
		return ifoSecOID;
	}

	@Override
	public void setIfoSecOID(Integer ifoSecOID) {
		this.ifoSecOID = ifoSecOID;
	}

	@Override
	public String getIfoSecOIDSigle() {
		return ifoSecOIDSigle;
	}

	@Override
	public void setIfoSecOIDSigle(String ifoSecOIDSigle) {
		this.ifoSecOIDSigle = ifoSecOIDSigle;
	}
}
