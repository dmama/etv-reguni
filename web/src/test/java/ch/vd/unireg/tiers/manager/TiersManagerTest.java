package ch.vd.unireg.tiers.manager;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.unireg.common.WebTest;
import ch.vd.unireg.common.pagination.WebParamPagination;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.rt.view.RapportPrestationView;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.type.PeriodiciteDecompte;
import ch.vd.unireg.type.Sexe;

public class TiersManagerTest extends WebTest {

	private TiersManager tiersManager;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		tiersManager = getBean(TiersManager.class, "tiersManager");
	}

	@Test
	public void testGetRapportsPrestationAvecHabitantDontNumeroIndividuEstInconnuOuSensibleAuCivil() throws Exception {

		final long noIndividu = 4674235L;
		final long noIndividuMine = 326723157123L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, null, "Sorel", "Julien", Sexe.MASCULIN);
				addIndividuMine(noIndividuMine);
			}
		});

		final class Ids {
			long dpi;
			long habitantOk;
			long habitantNotOk;
			long habitantMine;
		}

		// mise en place fiscale : un débiteur et plusieurs relations de prestations imposables
		final Ids ids = doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2010, 1, 1));
			final PersonnePhysique habitantOk = addHabitant(noIndividu);
			final PersonnePhysique habitantNotOk = addHabitant(noIndividu + 1);     // ce numéro n'existe pas dans le registre civil !
			final PersonnePhysique habitantMine = addHabitant(noIndividuMine);

			// petite construction pour que l'on puisse vérifier que l'on retrouve également les rapports APRES celui ou ceux qui posent problème...
			addRapportPrestationImposable(dpi, habitantOk, date(2010, 1, 1), date(2011, 12, 31), false);
			addRapportPrestationImposable(dpi, habitantNotOk, date(2011, 1, 1), null, false);
			addRapportPrestationImposable(dpi, habitantMine, date(2012, 1, 1), date(2012, 6, 30), false);
			addRapportPrestationImposable(dpi, habitantOk, date(2013, 1, 1), null, false);

			final Ids ids1 = new Ids();
			ids1.dpi = dpi.getNumero();
			ids1.habitantOk = habitantOk.getNumero();
			ids1.habitantNotOk = habitantNotOk.getNumero();
			ids1.habitantMine = habitantMine.getNumero();
			return ids1;
		});

		// allons maintenant chercher les rapports prestations de ce débiteur
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(ids.dpi);
				final WebParamPagination pagination = new WebParamPagination(1, 10, "id", true);
				final List<RapportPrestationView> rapports = tiersManager.getRapportsPrestation(dpi, pagination, true);
				Assert.assertEquals(4, rapports.size());
				{
					final RapportPrestationView view = rapports.get(0);
					Assert.assertNotNull(view);
					Assert.assertEquals((Long) ids.habitantOk, view.getNumero());
					Assert.assertEquals(Collections.singletonList("Julien Sorel"), view.getNomCourrier());
					Assert.assertEquals(date(2010, 1, 1), view.getDateDebut());
					Assert.assertEquals(date(2011, 12, 31), view.getDateFin());
				}
				{
					final RapportPrestationView view = rapports.get(1);
					Assert.assertNotNull(view);
					Assert.assertEquals((Long) ids.habitantNotOk, view.getNumero());
					Assert.assertNull(view.getNomCourrier());
					Assert.assertEquals(date(2011, 1, 1), view.getDateDebut());
					Assert.assertNull(view.getDateFin());
				}
				{
					final RapportPrestationView view = rapports.get(2);
					Assert.assertNotNull(view);
					Assert.assertEquals((Long) ids.habitantMine, view.getNumero());
					Assert.assertNull(view.getNomCourrier());
					Assert.assertEquals(date(2012, 1, 1), view.getDateDebut());
					Assert.assertEquals(date(2012, 6, 30), view.getDateFin());
				}
				{
					final RapportPrestationView view = rapports.get(3);
					Assert.assertNotNull(view);
					Assert.assertEquals((Long) ids.habitantOk, view.getNumero());
					Assert.assertEquals(Collections.singletonList("Julien Sorel"), view.getNomCourrier());
					Assert.assertEquals(date(2013, 1, 1), view.getDateDebut());
					Assert.assertNull(view.getDateFin());
				}
				return null;
			}
		});
	}
}
