package ch.vd.unireg.evenement.civil.engine.ech;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.TypeEvenementCivilEch;

public class DateNaissanceComparisonStrategyTest extends AbstractIndividuComparisonStrategyTest {

	private DateNaissanceComparisonStrategy strategy;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		strategy = new DateNaissanceComparisonStrategy();
	}

	private void setupCivil(final long noIndividu, final long noEvt1, final long noEvt2, final RegDate dateNaissance1, final RegDate dateNaissance2) {
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, dateNaissance1, "D'empoigne", "Foire", false);
				addIndividuAfterEvent(noEvt1, individu, dateNaissance1, TypeEvenementCivilEch.NAISSANCE);

				final MockIndividu individuCorrige = createIndividu(noIndividu, dateNaissance2, "D'ampouagne", "Fouare", false);
				addIndividuAfterEvent(noEvt2, individuCorrige, dateNaissance2, TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.CORRECTION, noEvt1);
			}
		});
	}

	@Test(timeout = 10000L)
	public void testMemesDates() throws Exception {

		final RegDate date = date(2012, 4, 12);
		final long noIndividu = 2367345623L;
		final long noEvtOriginel = 1536342L;
		final long noEvtCorrection = 4367823452374L;

		setupCivil(noIndividu, noEvtOriginel, noEvtCorrection, date, date);
		assertNeutre(strategy, noEvtOriginel, noEvtCorrection);
	}

	@Test(timeout = 10000L)
	public void testSansDate() throws Exception {

		final long noIndividu = 2367345623L;
		final long noEvtOriginel = 1536342L;
		final long noEvtCorrection = 4367823452374L;

		setupCivil(noIndividu, noEvtOriginel, noEvtCorrection, null, null);
		assertNeutre(strategy, noEvtOriginel, noEvtCorrection);
	}

	@Test(timeout = 10000L)
	public void testUnJourDifference() throws Exception {

		final RegDate date1 = date(2012, 4, 12);
		final RegDate date2 = date1.addDays(1);
		final long noIndividu = 2367345623L;
		final long noEvtOriginel = 1536342L;
		final long noEvtCorrection = 4367823452374L;

		setupCivil(noIndividu, noEvtOriginel, noEvtCorrection, date1, date2);
		assertNonNeutre(strategy, noEvtOriginel, noEvtCorrection, "date de naissance");
	}

	@Test(timeout = 10000L)
	public void testUnAnDifference() throws Exception {

		final RegDate date1 = date(2012, 4, 12);
		final RegDate date2 = date1.addYears(1);
		final long noIndividu = 2367345623L;
		final long noEvtOriginel = 1536342L;
		final long noEvtCorrection = 4367823452374L;

		setupCivil(noIndividu, noEvtOriginel, noEvtCorrection, date1, date2);
		assertNonNeutre(strategy, noEvtOriginel, noEvtCorrection, "date de naissance");
	}

	@Test(timeout = 10000L)
	public void testDisparition() throws Exception {

		final RegDate date = date(2012, 4, 12);
		final long noIndividu = 2367345623L;
		final long noEvtOriginel = 1536342L;
		final long noEvtCorrection = 4367823452374L;

		setupCivil(noIndividu, noEvtOriginel, noEvtCorrection, date, null);
		assertNonNeutre(strategy, noEvtOriginel, noEvtCorrection, "date de naissance (disparition)");
	}

	@Test(timeout = 10000L)
	public void testApparition() throws Exception {

		final RegDate date = date(2012, 4, 12);
		final long noIndividu = 2367345623L;
		final long noEvtOriginel = 1536342L;
		final long noEvtCorrection = 4367823452374L;

		setupCivil(noIndividu, noEvtOriginel, noEvtCorrection, null, date);
		assertNonNeutre(strategy, noEvtOriginel, noEvtCorrection, "date de naissance (apparition)");
	}
}
