package ch.vd.uniregctb.evenement.civil.engine.ech;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class DateDecesComparisonStrategyTest extends AbstractIndividuComparisonStrategyTest {

	private DateDecesComparisonStrategy strategy;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		strategy = new DateDecesComparisonStrategy();
	}

	private void setupCivil(final long noIndividu, final long noEvt1, final long noEvt2, final RegDate dateDeces1, final RegDate dateDeces2) {
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, null, "D'empoigne", "Foire", false);
				individu.setDateDeces(dateDeces1);
				addIndividuFromEvent(noEvt1, individu, dateDeces1, TypeEvenementCivilEch.DECES);

				final MockIndividu individuCorrige = createIndividu(noIndividu, null, "D'ampouagne", "Fouare", false);
				individuCorrige.setDateDeces(dateDeces2);
				addIndividuFromEvent(noEvt2, individuCorrige, dateDeces2, TypeEvenementCivilEch.DECES, ActionEvenementCivilEch.CORRECTION, noEvt1);
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
		assertNonNeutre(strategy, noEvtOriginel, noEvtCorrection, "date de décès");
	}

	@Test
	public void testUnAnDifference() throws Exception {

		final RegDate date1 = date(2012, 4, 12);
		final RegDate date2 = date1.addYears(1);
		final long noIndividu = 2367345623L;
		final long noEvtOriginel = 1536342L;
		final long noEvtCorrection = 4367823452374L;

		setupCivil(noIndividu, noEvtOriginel, noEvtCorrection, date1, date2);
		assertNonNeutre(strategy, noEvtOriginel, noEvtCorrection, "date de décès");
	}

	@Test
	public void testDisparition() throws Exception {

		final RegDate date = date(2012, 4, 12);
		final long noIndividu = 2367345623L;
		final long noEvtOriginel = 1536342L;
		final long noEvtCorrection = 4367823452374L;

		setupCivil(noIndividu, noEvtOriginel, noEvtCorrection, date, null);
		assertNonNeutre(strategy, noEvtOriginel, noEvtCorrection, "date de décès");
	}

	@Test
	public void testApparition() throws Exception {

		final RegDate date = date(2012, 4, 12);
		final long noIndividu = 2367345623L;
		final long noEvtOriginel = 1536342L;
		final long noEvtCorrection = 4367823452374L;

		setupCivil(noIndividu, noEvtOriginel, noEvtCorrection, null, date);
		assertNonNeutre(strategy, noEvtOriginel, noEvtCorrection, "date de décès");
	}
}
