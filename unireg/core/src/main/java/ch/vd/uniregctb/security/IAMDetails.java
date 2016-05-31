package ch.vd.uniregctb.security;

public interface IAMDetails {

	String getIamFirstName();
	void setIamFirstName(String firstName);

	String getIamLastName();
	void setIamLastName(String lastName);

	String[] getIamRoles();
	void setIamRoles(String[] roles);
}
