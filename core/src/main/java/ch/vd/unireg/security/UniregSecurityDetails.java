package ch.vd.unireg.security;

import java.util.Hashtable;

import org.jetbrains.annotations.Nullable;

public class UniregSecurityDetails extends Hashtable<String, Object> implements IAMDetails, SecurityDetails {

	private static final long serialVersionUID = -6842871549412020652L;

	private String[] iamRoles = null;
	private String iamFirstName = null;
	private String iamLastName = null;
	private ProfileOperateur profil = null;
	private Integer oid = null;
	private String sigleOID = null;

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
	public ProfileOperateur getProfil() {
		return profil;
	}

	public void setProfil(@Nullable ProfileOperateur profil) {
		this.profil = profil;
	}

	@Override
	public Integer getOID() {
		return oid;
	}

	public void setOID(@Nullable Integer oid) {
		this.oid = oid;
	}

	@Override
	public String getOIDSigle() {
		return sigleOID;
	}

	public void setOIDSigle(@Nullable String sigle) {
		this.sigleOID = sigle;
	}
}
