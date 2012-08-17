package ch.vd.unireg.interfaces;

import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.civil.data.LocalisationType;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;

public class InterfacesTestHelper {
	public static Localisation newLocalisation(MockCommune commune) {
		Localisation l = new Localisation();
		l.setNoOfs(commune.getNoOFS());
		l.setType(commune.isVaudoise() ? LocalisationType.CANTON_VD : LocalisationType.HORS_CANTON);
		return l;
	}

	public static Localisation newLocalisation(MockPays pays) {
		Localisation l = new Localisation();
		l.setNoOfs(pays.getNoOFS());
		l.setType(LocalisationType.HORS_SUISSE);
		return l;
	}
}
