package ch.vd.unireg.evenement.civil.engine.ech;

import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeEvenementCivilEch;

public class SexeComparisonStrategyTest extends AbstractIndividuComparisonStrategyTest {

	private SexeComparisonStrategy strategy;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		strategy = new SexeComparisonStrategy();
	}

	private void setupCivil(final long noIndividu, final long noEvt1, @Nullable final Sexe sexe1, final long noEvt2, @Nullable final Sexe sexe2) {
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, null, "Leblanc", "Claude", sexe1);
				addIndividuAfterEvent(noEvt1, individu, RegDate.get(), TypeEvenementCivilEch.ARRIVEE);

				final MockIndividu individuCorrige = createIndividu(noIndividu, null, "Leblanc", "Claude", sexe2);
				addIndividuAfterEvent(noEvt2, individuCorrige, RegDate.get(), TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.CORRECTION, noEvt2);
			}
		});
	}

	@Test(timeout = 10000L)
	public void testSansChangementInfoSexeConnue() throws Exception {

		final long noIndividu = 367423526L;
		final long noEvt1 = 46735562L;
		final long noEvt2 = 256432652L;

		setupCivil(noIndividu, noEvt1, Sexe.MASCULIN, noEvt2, Sexe.MASCULIN);
		assertNeutre(strategy, noEvt1, noEvt2);
	}

	@Test(timeout = 10000L)
	public void testChangementInfoSexeConnue() throws Exception {

		final long noIndividu = 367423526L;
		final long noEvt1 = 46735562L;
		final long noEvt2 = 256432652L;

		setupCivil(noIndividu, noEvt1, Sexe.MASCULIN, noEvt2, Sexe.FEMININ);
		assertNonNeutre(strategy, noEvt1, noEvt2, "sexe");
	}

	@Test(timeout = 10000L)
	public void testSansChangementInfoSexeInconnue() throws Exception {

		final long noIndividu = 367423526L;
		final long noEvt1 = 46735562L;
		final long noEvt2 = 256432652L;

		setupCivil(noIndividu, noEvt1, null, noEvt2, null);
		assertNeutre(strategy, noEvt1, noEvt2);
	}

	@Test(timeout = 10000L)
	public void testApparitionInfoSexe() throws Exception {

		final long noIndividu = 367423526L;
		final long noEvt1 = 46735562L;
		final long noEvt2 = 256432652L;

		setupCivil(noIndividu, noEvt1, null, noEvt2, Sexe.FEMININ);
		assertNonNeutre(strategy, noEvt1, noEvt2, "sexe");
	}

	@Test(timeout = 10000L)
	public void testDisparitionInfoSexe() throws Exception {

		final long noIndividu = 367423526L;
		final long noEvt1 = 46735562L;
		final long noEvt2 = 256432652L;

		setupCivil(noIndividu, noEvt1, Sexe.MASCULIN, noEvt2, null);
		assertNonNeutre(strategy, noEvt1, noEvt2, "sexe");
	}
}
