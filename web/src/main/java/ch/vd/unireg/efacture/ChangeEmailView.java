package ch.vd.unireg.efacture;

import java.io.Serializable;

public class ChangeEmailView implements Serializable {

	private long noCtb;
	private String previousEmail;
	private String email;

	public ChangeEmailView() {
	}

	public ChangeEmailView(long noCtb, String email) {
		this.noCtb = noCtb;
		this.email = email;
		this.previousEmail = email;
	}

	public long getNoCtb() {
		return noCtb;
	}

	public void setNoCtb(long noCtb) {
		this.noCtb = noCtb;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPreviousEmail() {
		return previousEmail;
	}

	public void setPreviousEmail(String previousEmail) {
		this.previousEmail = previousEmail;
	}
}
