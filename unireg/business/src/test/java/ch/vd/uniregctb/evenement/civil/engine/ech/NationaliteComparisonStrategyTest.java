package ch.vd.uniregctb.evenement.civil.engine.ech;

import org.junit.Test;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Pays;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class NationaliteComparisonStrategyTest extends AbstractIndividuComparisonStrategyTest {

	private NationaliteComparisonStrategy strategy;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		strategy = new NationaliteComparisonStrategy();
	}

	private void setupCivil(final long noIndividu, final long noEvt1, final DateRange range1, final Pays pays1,
	                        final long noEvt2, final DateRange range2, final Pays pays2) {
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, null, "Leblanc", "Juste", true);
				if (pays1 != null) {
					addNationalite(individu, pays1, range1.getDateDebut(), range1.getDateFin());
				}
				addIndividuFromEvent(noEvt1, individu, RegDate.get(), TypeEvenementCivilEch.ARRIVEE);

				final MockIndividu individuCorrige = createIndividu(noIndividu, null, "Leblenc", "Justin", true);
				if (pays2 != null) {
					addNationalite(individuCorrige, pays2, range2.getDateDebut(), range2.getDateFin());
				}
				addIndividuFromEvent(noEvt2, individuCorrige, RegDate.get(), TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.CORRECTION, noEvt2);
			}
		});
	}

	@Test
	public void testMemeNationaliteEtrangere() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 1, 1);
		final RegDate fin = date(2012, 8, 3);
		final Pays pays = MockPays.Liechtenstein;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut, fin), pays, noEvt2, new DateRangeHelper.Range(debut, fin), pays);
		assertNeutre(strategy, noEvt1, noEvt2);
	}

	@Test
	public void testMemeNationaliteSuisse() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 1, 1);
		final RegDate fin = date(2012, 8, 3);
		final Pays pays = MockPays.Suisse;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut, fin), pays, noEvt2, new DateRangeHelper.Range(debut, fin), pays);
		assertNeutre(strategy, noEvt1, noEvt2);
	}

	@Test
	public void testMemeNationaliteApatride() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 1, 1);
		final RegDate fin = date(2012, 8, 3);
		final Pays pays = MockPays.Apatridie;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut, fin), pays, noEvt2, new DateRangeHelper.Range(debut, fin), pays);
		assertNeutre(strategy, noEvt1, noEvt2);
	}

	@Test
	public void testMemeNationalitePaysInconnu() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 1, 1);
		final RegDate fin = date(2012, 8, 3);
		final Pays pays = MockPays.PaysInconnu;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut, fin), pays, noEvt2, new DateRangeHelper.Range(debut, fin), pays);
		assertNeutre(strategy, noEvt1, noEvt2);
	}

	@Test
	public void testSansNationalite() throws Exception {

		final long noIndividu = 367315L;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, null, null, noEvt2, null, null);
		assertNeutre(strategy, noEvt1, noEvt2);
	}

	@Test
	public void testPaysEtrangersDifferents() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 1, 1);
		final RegDate fin = date(2012, 8, 3);
		final Pays pays1 = MockPays.France;
		final Pays pays2 = MockPays.Espagne;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut, fin), pays1, noEvt2, new DateRangeHelper.Range(debut, fin), pays2);
		assertNeutre(strategy, noEvt1, noEvt2);
	}

	@Test
	public void testApatrideVersPaysEtranger() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 1, 1);
		final RegDate fin = date(2012, 8, 3);
		final Pays pays1 = MockPays.Apatridie;
		final Pays pays2 = MockPays.Espagne;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut, fin), pays1, noEvt2, new DateRangeHelper.Range(debut, fin), pays2);
		assertNonNeutre(strategy, noEvt1, noEvt2, "nationalité");
	}

	@Test
	public void testApatrideVersPaysInconnu() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 1, 1);
		final RegDate fin = date(2012, 8, 3);
		final Pays pays1 = MockPays.Apatridie;
		final Pays pays2 = MockPays.PaysInconnu;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut, fin), pays1, noEvt2, new DateRangeHelper.Range(debut, fin), pays2);
		assertNonNeutre(strategy, noEvt1, noEvt2, "nationalité");
	}

	@Test
	public void testApatrideVersSuisse() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 1, 1);
		final RegDate fin = date(2012, 8, 3);
		final Pays pays1 = MockPays.Apatridie;
		final Pays pays2 = MockPays.Suisse;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut, fin), pays1, noEvt2, new DateRangeHelper.Range(debut, fin), pays2);
		assertNonNeutre(strategy, noEvt1, noEvt2, "nationalité");
	}

	@Test
	public void testPaysInconnuVersPaysEtranger() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 1, 1);
		final RegDate fin = date(2012, 8, 3);
		final Pays pays1 = MockPays.PaysInconnu;
		final Pays pays2 = MockPays.Espagne;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut, fin), pays1, noEvt2, new DateRangeHelper.Range(debut, fin), pays2);
		assertNonNeutre(strategy, noEvt1, noEvt2, "nationalité");
	}

	@Test
	public void testPaysInconnuVersApatride() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 1, 1);
		final RegDate fin = date(2012, 8, 3);
		final Pays pays1 = MockPays.PaysInconnu;
		final Pays pays2 = MockPays.Apatridie;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut, fin), pays1, noEvt2, new DateRangeHelper.Range(debut, fin), pays2);
		assertNonNeutre(strategy, noEvt1, noEvt2, "nationalité");
	}

	@Test
	public void testPaysInconnuVersSuisse() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 1, 1);
		final RegDate fin = date(2012, 8, 3);
		final Pays pays1 = MockPays.PaysInconnu;
		final Pays pays2 = MockPays.Suisse;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut, fin), pays1, noEvt2, new DateRangeHelper.Range(debut, fin), pays2);
		assertNonNeutre(strategy, noEvt1, noEvt2, "nationalité");
	}

	@Test
	public void testPaysEtrangerVersSuisse() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 1, 1);
		final RegDate fin = date(2012, 8, 3);
		final Pays pays1 = MockPays.France;
		final Pays pays2 = MockPays.Suisse;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut, fin), pays1, noEvt2, new DateRangeHelper.Range(debut, fin), pays2);
		assertNonNeutre(strategy, noEvt1, noEvt2, "nationalité");
	}

	@Test
	public void testSuisseVersPaysEtranger() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 1, 1);
		final RegDate fin = date(2012, 8, 3);
		final Pays pays1 = MockPays.Suisse;
		final Pays pays2 = MockPays.Allemagne;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut, fin), pays1, noEvt2, new DateRangeHelper.Range(debut, fin), pays2);
		assertNonNeutre(strategy, noEvt1, noEvt2, "nationalité");
	}

	@Test
	public void testDatesDifferentesSurPaysEtranger() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut1 = date(2000, 1, 1);
		final RegDate debut2 = date(2000, 2, 1);
		final RegDate fin = date(2012, 8, 3);
		final Pays pays = MockPays.France;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut1, fin), pays, noEvt2, new DateRangeHelper.Range(debut2, null), pays);
		assertNeutre(strategy, noEvt1, noEvt2);
	}

	@Test
	public void testDatesDebutDifferentesSuisse() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut1 = date(2000, 1, 1);
		final RegDate debut2 = date(2000, 2, 1);
		final RegDate fin = date(2012, 8, 3);
		final Pays pays = MockPays.Suisse;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut1, fin), pays, noEvt2, new DateRangeHelper.Range(debut2, fin), pays);
		assertNonNeutre(strategy, noEvt1, noEvt2, "nationalité");
	}

	@Test
	public void testDatesFinDifferentesSuisse() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 2, 1);
		final RegDate fin1 = date(2012, 8, 3);
		final RegDate fin2 = null;
		final Pays pays = MockPays.Suisse;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut, fin1), pays, noEvt2, new DateRangeHelper.Range(debut, fin2), pays);
		assertNonNeutre(strategy, noEvt1, noEvt2, "nationalité");
	}

	@Test
	public void testApparitionSeuleNationaliteEtrangere() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 2, 1);
		final RegDate fin = date(2012, 8, 3);
		final Pays pays = MockPays.Danemark;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, null, null, noEvt2, new DateRangeHelper.Range(debut, fin), pays);
		assertNonNeutre(strategy, noEvt1, noEvt2, "nationalité");
	}

	@Test
	public void testApparitionSeuleNationaliteSuisse() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 2, 1);
		final RegDate fin = date(2012, 8, 3);
		final Pays pays = MockPays.Suisse;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, null, null, noEvt2, new DateRangeHelper.Range(debut, fin), pays);
		assertNonNeutre(strategy, noEvt1, noEvt2, "nationalité");
	}

	@Test
	public void testDisparitionSeuleNationaliteEtrangere() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 2, 1);
		final RegDate fin = date(2012, 8, 3);
		final Pays pays = MockPays.Danemark;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut, fin), pays, noEvt2, null, null);
		assertNonNeutre(strategy, noEvt1, noEvt2, "nationalité");
	}

	@Test
	public void testDisparitionSeuleNationaliteSuisse() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 2, 1);
		final RegDate fin = date(2012, 8, 3);
		final Pays pays = MockPays.Suisse;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut, fin), pays, noEvt2, null, null);
		assertNonNeutre(strategy, noEvt1, noEvt2, "nationalité");
	}
}
