package ch.vd.unireg.interfaces;

import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.civil.data.LocalisationType;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;

public class InterfacesTestHelper {

	public static Localisation newLocalisation(MockCommune commune) {
		return new Localisation(commune.isVaudoise() ? LocalisationType.CANTON_VD : LocalisationType.HORS_CANTON, commune.getNoOFS(), null);
	}

	public static Localisation newLocalisation(MockPays pays) {
		return new Localisation(LocalisationType.HORS_SUISSE, pays.getNoOFS(), null);
	}
}
