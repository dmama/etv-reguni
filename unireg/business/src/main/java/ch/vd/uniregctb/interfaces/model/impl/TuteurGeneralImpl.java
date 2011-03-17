package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;

import ch.vd.uniregctb.interfaces.model.TuteurGeneral;

public class TuteurGeneralImpl implements TuteurGeneral, Serializable {

	private static final long serialVersionUID = -5079133760089123152L;

	private final String noTelephoneContact;
	private final String nomContact;
	private final String nomOffice;
	private final String prenomContact;

	public static TuteurGeneralImpl get(ch.vd.registre.civil.model.TuteurGeneral target) {
		if (target == null) {
			return null;
		}
		return new TuteurGeneralImpl(target);
	}

	private TuteurGeneralImpl(ch.vd.registre.civil.model.TuteurGeneral target) {
		this.noTelephoneContact = target.getNoTelephoneContact();
		this.nomContact = target.getNomContact();
		this.nomOffice = target.getNomOffice();
		this.prenomContact = target.getPrenomContact();
	}

	public String getNoTelephoneContact() {
		return noTelephoneContact;
	}

	public String getNomContact() {
		return nomContact;
	}

	public String getNomOffice() {
		return nomOffice;
	}

	public String getPrenomContact() {
		return prenomContact;
	}

}
