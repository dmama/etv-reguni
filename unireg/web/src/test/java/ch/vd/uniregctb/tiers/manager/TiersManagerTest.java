package ch.vd.uniregctb.tiers.manager;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.rt.view.RapportPrestationView;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.Sexe;

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
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2010, 1, 1));
				final PersonnePhysique habitantOk = addHabitant(noIndividu);
				final PersonnePhysique habitantNotOk = addHabitant(noIndividu + 1);     // ce numéro n'existe pas dans le registre civil !
				final PersonnePhysique habitantMine = addHabitant(noIndividuMine);

				// petite construction pour que l'on puisse vérifier que l'on retrouve également les rapports APRES celui ou ceux qui posent problème...
				addRapportPrestationImposable(dpi, habitantOk, date(2010, 1, 1), date(2011, 12, 31), false);
				addRapportPrestationImposable(dpi, habitantNotOk, date(2011, 1, 1), null, false);
				addRapportPrestationImposable(dpi, habitantMine, date(2012, 1, 1), date(2012, 6, 30), false);
				addRapportPrestationImposable(dpi, habitantOk, date(2013, 1, 1), null, false);

				final Ids ids = new Ids();
				ids.dpi = dpi.getNumero();
				ids.habitantOk = habitantOk.getNumero();
				ids.habitantNotOk = habitantNotOk.getNumero();
				ids.habitantMine = habitantMine.getNumero();
				return ids;
			}
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
					Assert.assertEquals(date(2010, 1, 1), view.getRegDateDebut());
					Assert.assertEquals(date(2011, 12, 31), view.getRegDateFin());
				}
				{
					final RapportPrestationView view = rapports.get(1);
					Assert.assertNotNull(view);
					Assert.assertEquals((Long) ids.habitantNotOk, view.getNumero());
					Assert.assertNull(view.getNomCourrier());
					Assert.assertEquals(date(2011, 1, 1), view.getRegDateDebut());
					Assert.assertNull(view.getRegDateFin());
				}
				{
					final RapportPrestationView view = rapports.get(2);
					Assert.assertNotNull(view);
					Assert.assertEquals((Long) ids.habitantMine, view.getNumero());
					Assert.assertNull(view.getNomCourrier());
					Assert.assertEquals(date(2012, 1, 1), view.getRegDateDebut());
					Assert.assertEquals(date(2012, 6, 30), view.getRegDateFin());
				}
				{
					final RapportPrestationView view = rapports.get(3);
					Assert.assertNotNull(view);
					Assert.assertEquals((Long) ids.habitantOk, view.getNumero());
					Assert.assertEquals(Collections.singletonList("Julien Sorel"), view.getNomCourrier());
					Assert.assertEquals(date(2013, 1, 1), view.getRegDateDebut());
					Assert.assertNull(view.getRegDateFin());
				}
				return null;
			}
		});
	}
}
