package ch.vd.unireg.evenement.civil.engine.ech;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.TypeEvenementCivilEch;

public class EtatCivilComparisonStrategyTest extends AbstractIndividuComparisonStrategyTest {

	private EtatCivilComparisonStrategy strategy;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		strategy = new EtatCivilComparisonStrategy();
	}

	private void setupCivil(final long noIndividu, final long noEvt1, final RegDate debut1, final TypeEtatCivil etat1,
	                        final long noEvt2, final RegDate debut2, final TypeEtatCivil etat2) {
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, null, "Leblanc", "Juste", true);
				individu.getEtatsCivils().clear();
				if (etat1 != null) {
					addEtatCivil(individu, debut1, etat1);
				}
				addIndividuAfterEvent(noEvt1, individu, RegDate.get(), TypeEvenementCivilEch.ARRIVEE);

				final MockIndividu individuCorrige = createIndividu(noIndividu, null, "Leblenc", "Justin", true);
				individuCorrige.getEtatsCivils().clear();
				if (etat2 != null) {
					addEtatCivil(individuCorrige, debut2, etat2);
				}
				addIndividuAfterEvent(noEvt2, individuCorrige, RegDate.get(), TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.CORRECTION, noEvt2);
			}
		});
	}

	@Test(timeout = 10000L)
	public void testMemeEtatCivil() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 1, 1);
		final TypeEtatCivil type = TypeEtatCivil.PACS_TERMINE;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, debut, type, noEvt2, debut, type);
		assertNeutre(strategy, noEvt1, noEvt2);
	}

	@Test(timeout = 10000L)
	public void testSansEtatCivil() throws Exception {

		final long noIndividu = 367315L;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, null, null, noEvt2, null, null);
		assertNeutre(strategy, noEvt1, noEvt2);
	}

	@Test(timeout = 10000L)
	public void testEtatCivilDisparu() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 1, 1);
		final TypeEtatCivil type = TypeEtatCivil.PACS_TERMINE;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, debut, type, noEvt2, null, null);
		assertNonNeutre(strategy, noEvt1, noEvt2, "état civil (disparition)");
	}

	@Test(timeout = 10000L)
	public void testEtatCivilApparu() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 1, 1);
		final TypeEtatCivil type = TypeEtatCivil.PACS_TERMINE;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, null, null, noEvt2, debut, type);
		assertNonNeutre(strategy, noEvt1, noEvt2, "état civil (apparition)");
	}

	@Test(timeout = 10000L)
	public void testEtatCivilTypeDifferent() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 1, 1);
		final TypeEtatCivil type1 = TypeEtatCivil.PACS_TERMINE;
		final TypeEtatCivil type2 = TypeEtatCivil.PACS_SEPARE;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, debut, type1, noEvt2, debut, type2);
		assertNonNeutre(strategy, noEvt1, noEvt2, "état civil");
	}

	@Test(timeout = 10000L)
	public void testEtatCivilDatesDebutDifferentes() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut1 = date(2000, 1, 1);
		final RegDate debut2 = date(2000, 2, 1);
		final TypeEtatCivil type = TypeEtatCivil.PACS_TERMINE;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, debut1, type, noEvt2, debut2, type);
		assertNonNeutre(strategy, noEvt1, noEvt2, "état civil (dates)");
	}
}
