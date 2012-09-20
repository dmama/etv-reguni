package ch.vd.uniregctb.evenement.civil.engine.ech;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class DateNaissanceComparisonStrategyTest extends AbstractIndividuComparisonStrategyTest {

	private DateNaissanceComparisonStrategy strategy;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		strategy = new DateNaissanceComparisonStrategy();
	}

	private void setupCivil(final long noIndividu, final long noEvt1, final long noEvt2, final RegDate dateNaissance1, final RegDate dateNaissance2) {
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, dateNaissance1, "D'empoigne", "Foire", false);
				addIndividuFromEvent(noEvt1, individu, dateNaissance1, TypeEvenementCivilEch.NAISSANCE);

				final MockIndividu individuCorrige = createIndividu(noIndividu, dateNaissance2, "D'ampouagne", "Fouare", false);
				addIndividuFromEvent(noEvt2, individuCorrige, dateNaissance2, TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.CORRECTION, noEvt1);
			}
		});
	}

	@Test
	public void testMemesDates() throws Exception {

		final RegDate date = date(2012, 4, 12);
		final long noIndividu = 2367345623L;
		final long noEvtOriginel = 1536342L;
		final long noEvtCorrection = 4367823452374L;

		setupCivil(noIndividu, noEvtOriginel, noEvtCorrection, date, date);
		assertNeutre(strategy, noEvtOriginel, noEvtCorrection);
	}

	@Test
	public void testSansDate() throws Exception {

		final long noIndividu = 2367345623L;
		final long noEvtOriginel = 1536342L;
		final long noEvtCorrection = 4367823452374L;

		setupCivil(noIndividu, noEvtOriginel, noEvtCorrection, null, null);
		assertNeutre(strategy, noEvtOriginel, noEvtCorrection);
	}

	@Test
	public void testUnJourDifference() throws Exception {

		final RegDate date1 = date(2012, 4, 12);
		final RegDate date2 = date1.addDays(1);
		final long noIndividu = 2367345623L;
		final long noEvtOriginel = 1536342L;
		final long noEvtCorrection = 4367823452374L;

		setupCivil(noIndividu, noEvtOriginel, noEvtCorrection, date1, date2);
		assertNonNeutre(strategy, noEvtOriginel, noEvtCorrection, "date de naissance");
	}

	@Test
	public void testUnAnDifference() throws Exception {

		final RegDate date1 = date(2012, 4, 12);
		final RegDate date2 = date1.addYears(1);
		final long noIndividu = 2367345623L;
		final long noEvtOriginel = 1536342L;
		final long noEvtCorrection = 4367823452374L;

		setupCivil(noIndividu, noEvtOriginel, noEvtCorrection, date1, date2);
		assertNonNeutre(strategy, noEvtOriginel, noEvtCorrection, "date de naissance");
	}

	@Test
	public void testDisparition() throws Exception {

		final RegDate date = date(2012, 4, 12);
		final long noIndividu = 2367345623L;
		final long noEvtOriginel = 1536342L;
		final long noEvtCorrection = 4367823452374L;

		setupCivil(noIndividu, noEvtOriginel, noEvtCorrection, date, null);
		assertNonNeutre(strategy, noEvtOriginel, noEvtCorrection, "date de naissance");
	}

	@Test
	public void testApparition() throws Exception {

		final RegDate date = date(2012, 4, 12);
		final long noIndividu = 2367345623L;
		final long noEvtOriginel = 1536342L;
		final long noEvtCorrection = 4367823452374L;

		setupCivil(noIndividu, noEvtOriginel, noEvtCorrection, null, date);
		assertNonNeutre(strategy, noEvtOriginel, noEvtCorrection, "date de naissance");
	}
}
