package ch.vd.unireg.evenement.civil.engine.ech;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.TypeEvenementCivilEch;

public abstract class AbstractAdresseComparisonStrategyTest extends AbstractIndividuComparisonStrategyTest {

	protected interface AddressBuilder {
		void buildAdresses(MockIndividu individu);
	}

	protected void setupCivil(final long noIndividu, final long noEvt1, final AddressBuilder b1, final long noEvt2, final AddressBuilder b2) {
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Fraise", "Tartala", false);
				if (b1 != null) {
					b1.buildAdresses(ind);
				}
				addIndividuAfterEvent(noEvt1, ind, RegDate.get(), TypeEvenementCivilEch.ARRIVEE);

				final MockIndividu indCorrige = createIndividu(noIndividu, null, "Frèze", "Tart'ala", false);
				if (b2 != null) {
					b2.buildAdresses(indCorrige);
				}
				addIndividuAfterEvent(noEvt2, indCorrige, RegDate.get(), TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.CORRECTION, noEvt1);
			}
		});
	}
}
