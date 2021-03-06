package ch.vd.unireg.evenement.civil.engine.ech;

import org.junit.Test;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.TypeEvenementCivilEch;
import ch.vd.unireg.type.TypePermis;

public class PermisComparisonStrategyTest extends AbstractIndividuComparisonStrategyTest {

	private PermisComparisonStrategy strategy;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		strategy = new PermisComparisonStrategy();
	}

	private void setupCivil(final long noIndividu, final long noEvt1, final DateRange range1, final TypePermis type1,
	                        final long noEvt2, final DateRange range2, final TypePermis type2) {
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, null, "Leblanc", "Juste", true);
				if (type1 != null) {
					addPermis(individu, type1, range1.getDateDebut(), range1.getDateFin(), false);
				}
				addIndividuAfterEvent(noEvt1, individu, RegDate.get(), TypeEvenementCivilEch.ARRIVEE);

				final MockIndividu individuCorrige = createIndividu(noIndividu, null, "Leblenc", "Justin", true);
				if (type2!= null) {
					addPermis(individuCorrige, type2, range2.getDateDebut(), range2.getDateFin(), false);
				}
				addIndividuAfterEvent(noEvt2, individuCorrige, RegDate.get(), TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.CORRECTION, noEvt2);
			}
		});
	}

	@Test(timeout = 10000L)
	public void testMemePermis() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 1, 1);
		final RegDate fin = date(2012, 8, 3);
		final TypePermis type = TypePermis.SEJOUR;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut, fin), type, noEvt2, new DateRangeHelper.Range(debut, fin), type);
		assertNeutre(strategy, noEvt1, noEvt2);
	}

	@Test(timeout = 10000L)
	public void testSansPermis() throws Exception {

		final long noIndividu = 367315L;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, null, null, noEvt2, null, null);
		assertNeutre(strategy, noEvt1, noEvt2);
	}

	@Test(timeout = 10000L)
	public void testTypesDifferentsDontC() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 1, 1);
		final RegDate fin = date(2012, 8, 3);
		final TypePermis type1 = TypePermis.DIPLOMATE_OU_FONCT_INTER_AVEC_IMMUNITE;
		final TypePermis type2 = TypePermis.ETABLISSEMENT;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut, fin), type1, noEvt2, new DateRangeHelper.Range(debut, fin), type2);
		assertNonNeutre(strategy, noEvt1, noEvt2, "permis");
	}

	@Test(timeout = 10000L)
	public void testTypesDifferentsSansC() throws Exception {
		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 1, 1);
		final RegDate fin = date(2012, 8, 3);
		final TypePermis type1 = TypePermis.COURTE_DUREE;
		final TypePermis type2 = TypePermis.SEJOUR;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut, fin), type1, noEvt2, new DateRangeHelper.Range(debut, fin), type2);
		assertNeutre(strategy, noEvt1, noEvt2);
	}

	@Test(timeout = 10000L)
	public void testDatesDebutDifferentesPermisC() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut1 = date(2000, 1, 1);
		final RegDate debut2 = date(2000, 2, 1);
		final RegDate fin = date(2012, 8, 3);
		final TypePermis type = TypePermis.ETABLISSEMENT;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut1, fin), type, noEvt2, new DateRangeHelper.Range(debut2, fin), type);
		assertNonNeutre(strategy, noEvt1, noEvt2, "permis (dates)");
	}

	@Test(timeout = 10000L)
	public void testDatesDebutDifferentesPermisNonC() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut1 = date(2000, 1, 1);
		final RegDate debut2 = date(2000, 2, 1);
		final RegDate fin = date(2012, 8, 3);
		final TypePermis type = TypePermis.SAISONNIER;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut1, fin), type, noEvt2, new DateRangeHelper.Range(debut2, fin), type);
		assertNeutre(strategy, noEvt1, noEvt2);
	}

	@Test(timeout = 10000L)
	public void testDatesFinDifferentesPermisC() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 2, 1);
		final RegDate fin1 = date(2012, 8, 3);
		final RegDate fin2 = null;
		final TypePermis type = TypePermis.ETABLISSEMENT;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut, fin1), type, noEvt2, new DateRangeHelper.Range(debut, fin2), type);
		assertNonNeutre(strategy, noEvt1, noEvt2, "permis (dates)");
	}

	@Test(timeout = 10000L)
	public void testDatesFinDifferentesPermisNonC() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 2, 1);
		final RegDate fin1 = date(2012, 8, 3);
		final RegDate fin2 = null;
		final TypePermis type = TypePermis.ETRANGER_ADMIS_PROVISOIREMENT;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut, fin1), type, noEvt2, new DateRangeHelper.Range(debut, fin2), type);
		assertNeutre(strategy, noEvt1, noEvt2);
	}

	@Test(timeout = 10000L)
	public void testApparition() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 2, 1);
		final RegDate fin = date(2012, 8, 3);
		final TypePermis type = TypePermis.DIPLOMATE_OU_FONCT_INTER_AVEC_IMMUNITE;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, null, null, noEvt2, new DateRangeHelper.Range(debut, fin), type);
		assertNonNeutre(strategy, noEvt1, noEvt2, "permis (apparition)");
	}

	@Test(timeout = 10000L)
	public void testDisparition() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 2, 1);
		final RegDate fin = date(2012, 8, 3);
		final TypePermis type = TypePermis.COURTE_DUREE;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut, fin), type, noEvt2, null, null);
		assertNonNeutre(strategy, noEvt1, noEvt2, "permis (disparition)");
	}
}
