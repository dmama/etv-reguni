package ch.vd.uniregctb.mandataire;

import java.util.List;

public class AdresseMandatView extends LignesAdressesView {

	private final String personneContact;
	private final String noTelContact;
	private final String erreur;

	public AdresseMandatView(String personneContact, String noTelContact, List<String> adresse) {
		super(adresse);
		this.personneContact = personneContact;
		this.noTelContact = noTelContact;
		this.erreur = null;
	}

	public AdresseMandatView(String personneContact, String noTelContact, String erreur) {
		this.personneContact = personneContact;
		this.noTelContact = noTelContact;
		this.erreur = erreur;
	}

	public String getPersonneContact() {
		return personneContact;
	}

	public String getNoTelContact() {
		return noTelContact;
	}

	public String getErreur() {
		return erreur;
	}
}
