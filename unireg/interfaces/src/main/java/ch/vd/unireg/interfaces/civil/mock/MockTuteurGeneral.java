package ch.vd.unireg.interfaces.civil.mock;

import ch.vd.unireg.interfaces.civil.data.TuteurGeneral;

public class MockTuteurGeneral implements TuteurGeneral {

	private String noTelephoneContact;
	private String nomContact;
	private String nomOffice;
	private String prenomContact;

	@Override
	public String getNoTelephoneContact() {
		return noTelephoneContact;
	}

	public void setNoTelephoneContact(String noTelephoneContact) {
		this.noTelephoneContact = noTelephoneContact;
	}

	@Override
	public String getNomContact() {
		return nomContact;
	}

	public void setNomContact(String nomContact) {
		this.nomContact = nomContact;
	}

	@Override
	public String getNomOffice() {
		return nomOffice;
	}

	public void setNomOffice(String nomOffice) {
		this.nomOffice = nomOffice;
	}

	@Override
	public String getPrenomContact() {
		return prenomContact;
	}

	public void setPrenomContact(String prenomContact) {
		this.prenomContact = prenomContact;
	}
}
