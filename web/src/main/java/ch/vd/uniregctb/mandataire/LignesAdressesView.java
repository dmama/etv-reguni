package ch.vd.uniregctb.mandataire;

import java.util.List;

public class LignesAdressesView {

	private final List<String> adresse;

	public LignesAdressesView() {
		this.adresse = null;
	}

	public LignesAdressesView(List<String> adresse) {
		this.adresse = adresse;
	}

	public List<String> getAdresse() {
		return adresse;
	}
}
