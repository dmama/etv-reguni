package ch.vd.uniregctb.tiers.view;

import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.civil.data.LocalisationType;
import ch.vd.uniregctb.type.TypeAdresseCivil;

public class LocalisationView {

	private final LocalisationType type;
	private final Integer noOfs;
	private final AdresseCivilView adresseCourrier;

	public LocalisationView(Localisation localisation) {
		this.type = localisation.getType();
		this.noOfs = localisation.getNoOfs();
		this.adresseCourrier = localisation.getAdresseCourrier() == null ? null : new AdresseCivilView(localisation.getAdresseCourrier(), TypeAdresseCivil.COURRIER);
	}

	public LocalisationType getType() {
		return type;
	}

	public Integer getNoOfs() {
		return noOfs;
	}

	public AdresseCivilView getAdresseCourrier() {
		return adresseCourrier;
	}
}
