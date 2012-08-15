package ch.vd.uniregctb.evenement.civil.engine.ech;

import org.junit.Test;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class EtatCivilComparisonStrategyTest extends AbstractIndividuComparisonStrategyTest {

	private EtatCivilComparisonStrategy strategy;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		strategy = new EtatCivilComparisonStrategy();
	}

	private void setupCivil(final long noIndividu, final long noEvt1, final DateRange range1, final TypeEtatCivil etat1,
	                        final long noEvt2, final DateRange range2, final TypeEtatCivil etat2) {
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, null, "Leblanc", "Juste", true);
				if (etat1 != null) {
					addEtatCivil(individu, range1.getDateDebut(), range1.getDateFin(), etat1);
				}
				addIndividuFromEvent(noEvt1, individu, RegDate.get(), TypeEvenementCivilEch.ARRIVEE);

				final MockIndividu individuCorrige = createIndividu(noIndividu, null, "Leblenc", "Justin", true);
				if (etat2 != null) {
					addEtatCivil(individuCorrige, range2.getDateDebut(), range2.getDateFin(), etat2);
				}
				addIndividuFromEvent(noEvt2, individuCorrige, RegDate.get(), TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.CORRECTION, noEvt2);
			}
		});
	}

	@Test
	public void testMemeEtatCivil() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 1, 1);
		final RegDate fin = date(2012, 8, 3);
		final TypeEtatCivil type = TypeEtatCivil.PACS_TERMINE;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut, fin), type, noEvt2, new DateRangeHelper.Range(debut, fin), type);
		assertNeutre(strategy, noEvt1, noEvt2);
	}

	@Test
	public void testSansEtatCivil() throws Exception {

		final long noIndividu = 367315L;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, null, null, noEvt2, null, null);
		assertNeutre(strategy, noEvt1, noEvt2);
	}

	@Test
	public void testEtatCivilDisparu() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 1, 1);
		final RegDate fin = date(2012, 8, 3);
		final TypeEtatCivil type = TypeEtatCivil.PACS_TERMINE;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut, fin), type, noEvt2, null, null);
		assertNonNeutre(strategy, noEvt1, noEvt2, "état civil");
	}

	@Test
	public void testEtatCivilApparu() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 1, 1);
		final RegDate fin = date(2012, 8, 3);
		final TypeEtatCivil type = TypeEtatCivil.PACS_TERMINE;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, null, null, noEvt2, new DateRangeHelper.Range(debut, fin), type);
		assertNonNeutre(strategy, noEvt1, noEvt2, "état civil");
	}

	@Test
	public void testEtatCivilTypeDifferent() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 1, 1);
		final RegDate fin = date(2012, 8, 3);
		final TypeEtatCivil type1 = TypeEtatCivil.PACS_TERMINE;
		final TypeEtatCivil type2 = TypeEtatCivil.PACS_INTERROMPU;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut, fin), type1, noEvt2, new DateRangeHelper.Range(debut, fin), type2);
		assertNonNeutre(strategy, noEvt1, noEvt2, "état civil");
	}

	@Test
	public void testEtatCivilDatesDebutDifferentes() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut1 = date(2000, 1, 1);
		final RegDate debut2 = date(2000, 2, 1);
		final RegDate fin = date(2012, 8, 3);
		final TypeEtatCivil type = TypeEtatCivil.PACS_TERMINE;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut1, fin), type, noEvt2, new DateRangeHelper.Range(debut2, fin), type);
		assertNonNeutre(strategy, noEvt1, noEvt2, "état civil");
	}

	@Test
	public void testEtatCivilDatesFinDifferentes() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 2, 1);
		final RegDate fin1 = date(2012, 8, 3);
		final RegDate fin2 = null;
		final TypeEtatCivil type = TypeEtatCivil.PACS_TERMINE;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut, fin1), type, noEvt2, new DateRangeHelper.Range(debut, fin2), type);
		assertNonNeutre(strategy, noEvt1, noEvt2, "état civil");
	}

	@Test
	public void testSansAucunEtatCivil() throws Exception {
		final long noIndividu = 367315L;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, null, "Leblanc", "Juste", true);
				individu.getEtatsCivils().clear();
				addIndividuFromEvent(noEvt1, individu, RegDate.get(), TypeEvenementCivilEch.ARRIVEE);

				final MockIndividu individuCorrige = createIndividu(noIndividu, null, "Leblenc", "Justin", true);
				individuCorrige.getEtatsCivils().clear();
				addIndividuFromEvent(noEvt2, individuCorrige, RegDate.get(), TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.CORRECTION, noEvt2);
			}
		});

		assertNeutre(strategy, noEvt1, noEvt2);
	}
}
