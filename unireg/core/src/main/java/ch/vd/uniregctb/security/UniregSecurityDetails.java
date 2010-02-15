package ch.vd.uniregctb.security;

import java.util.Hashtable;

import org.acegisecurity.providers.AbstractAuthenticationToken;

import ch.vd.registre.base.utils.Assert;
import ch.vd.securite.model.ProfilOperateur;

public class UniregSecurityDetails extends Hashtable<String, Object> {

	//private final static Logger LOGGER = Logger.getLogger(UniregSecurityDetails.class);

	/**
	 *
	 */
	private static final long serialVersionUID = 7427380869548372766L;

	private String[] iamRoles = null;
	private String iamFirstName = null;
	private String iamLastName = null;
	private ProfilOperateur ifoSecProfil = null;
	private Integer ifoSecOID = null;
	private String ifoSecOIDSigle = null;


	public String getIamFirstName() {
		return iamFirstName;
	}
	public void setIamFirstName(String firstName) {
		this.iamFirstName = firstName;
	}

	public String getIamLastName() {
		return iamLastName;
	}
	public void setIamLastName(String lastName) {
		this.iamLastName = lastName;
	}

	public String[] getIamRoles() {
		return iamRoles;
	}

	public void setIamRoles(String[] roles) {
		iamRoles = roles;
	}

	public ProfilOperateur getIfoSecProfil() {
		return ifoSecProfil;
	}
	public void setIfoSecProfil(ProfilOperateur ifosecProfil) {
		this.ifoSecProfil = ifosecProfil;
	}

	public Integer getIfoSecOID() {
		return ifoSecOID;
	}
	public void setIfoSecOID(Integer ifoSecOID) {
		this.ifoSecOID = ifoSecOID;
	}
	public String getIfoSecOIDSigle() {
		return ifoSecOIDSigle;
	}
	public void setIfoSecOIDSigle(String ifoSecOIDSigle) {
		this.ifoSecOIDSigle = ifoSecOIDSigle;
	}



	public void setToAuthentication(AbstractAuthenticationToken auth) {
		UniregSecurityDetails details = (UniregSecurityDetails)auth.getDetails();
		if (details == null) {
			auth.setDetails(this);
		}
		else {
			Assert.isTrue(details instanceof UniregSecurityDetails);
			Assert.isTrue(details == this);
		}
	}

}
