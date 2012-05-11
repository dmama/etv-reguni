package ch.vd.unireg.interfaces.civil.data;

import java.io.Serializable;

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

	@Override
	public String getNoTelephoneContact() {
		return noTelephoneContact;
	}

	@Override
	public String getNomContact() {
		return nomContact;
	}

	@Override
	public String getNomOffice() {
		return nomOffice;
	}

	@Override
	public String getPrenomContact() {
		return prenomContact;
	}

}
