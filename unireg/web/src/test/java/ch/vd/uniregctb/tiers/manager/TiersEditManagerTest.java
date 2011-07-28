package ch.vd.uniregctb.tiers.manager;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.view.DebiteurEditView;
import ch.vd.uniregctb.tiers.view.TiersEditView;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.ModeCommunication;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SuppressWarnings({"JavaDoc"})
public class TiersEditManagerTest extends WebTest {

	private final static String DB_UNIT_FILE = "TiersEditManagerTest.xml";

	TiersEditManager tiersEditManager ;


	/**
	 * @see ch.vd.uniregctb.common.AbstractCoreDAOTest#onSetUp()
	 */
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {

				final MockIndividu individu1 = addIndividu(282315, RegDate.get(1974, 3, 22), "Bolomey", "Alain", true);
				final MockIndividu individu2 = addIndividu(282316, RegDate.get(1974, 3, 22), "Richard", "Marcel", true);
				addAdresse(individu1, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null,
						RegDate.get(1980, 1, 1), null);
				addAdresse(individu2, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null,
						RegDate.get(1980, 1, 1), null);
			}
		});

		loadDatabase(DB_UNIT_FILE);
		tiersEditManager = getBean(TiersEditManager.class, "tiersEditManager");
	}

	/**
	 * Teste la methode getView
	 */

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetView() throws Exception {

		TiersEditView view = tiersEditManager.getView(new Long(6789));
		assertEquals("Bolomey", view.getIndividu().getNom());
	}

	/**
	 * Teste la methode creePersonne
	 */

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCreePersonne() {

		TiersEditView view = tiersEditManager.creePersonne();
		Tiers tiers = view.getTiers();
		PersonnePhysique nonHab = (PersonnePhysique) tiers;
		assertNull(nonHab.getSexe());
	}

	/**
	 * Teste la methode creeOrganisation
	 */

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCreeOrganisation() {

		TiersEditView view = tiersEditManager.creeOrganisation();
		Tiers tiers = view.getTiers();
		AutreCommunaute autreCommunaute = (AutreCommunaute) tiers;
		assertNotNull(autreCommunaute);
	}

	/**
	 * Teste la methode creeDebiteur
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCreeDebiteur() throws Exception{

		TiersEditView view = tiersEditManager.creeDebiteur(new Long(6789));
		Tiers tiers = view.getTiers();
		DebiteurPrestationImposable debiteurPrestationImposable = (DebiteurPrestationImposable) tiers;
		assertNotNull(debiteurPrestationImposable);
	}

	/**
	 * Teste la methode save
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSave() {

		TiersEditView view = tiersEditManager.creePersonne();
		Tiers tiers = view.getTiers();
		PersonnePhysique nonHab = (PersonnePhysique) tiers;
		nonHab.setNom("claude");
		view.setTiers(nonHab);
		Tiers tiersSaved = tiersEditManager.save(view);
		PersonnePhysique nonHabitantSaved = (PersonnePhysique)tiersSaved;
		assertEquals("claude", nonHabitantSaved.getNom());

	}

	/**
	 * Cas jira UNIREG-3180
	 */
	@Test
	public void testChangeModeCommunicationDebiteur() throws Exception {

		final long dpiId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1980, 10, 25), Sexe.MASCULIN);
				final DebiteurPrestationImposable dpi = addDebiteur(null, pp, date(2010, 1, 1));
				dpi.setModeCommunication(ModeCommunication.PAPIER);
				dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
				dpi.addPeriodicite(new Periodicite(PeriodiciteDecompte.MENSUEL, null, date(2010, 1, 1), null));
				return dpi.getNumero();
			}
		});

		DebiteurEditView view = tiersEditManager.getDebiteurEditView(dpiId);
		assertEquals(ModeCommunication.PAPIER, view.getModeCommunication());
		{
			view.setModeCommunication(ModeCommunication.ELECTRONIQUE);
			tiersEditManager.save(view);
			view = tiersEditManager.getDebiteurEditView(dpiId);
			assertEquals(ModeCommunication.ELECTRONIQUE, view.getModeCommunication());
		}
		{
			view.setModeCommunication(ModeCommunication.SITE_WEB);
			tiersEditManager.save(view);
			view = tiersEditManager.getDebiteurEditView(dpiId);
			assertEquals(ModeCommunication.SITE_WEB, view.getModeCommunication());
		}
	}

	/**
	 * Cas jira UNIREG-3180
	 */
	@Test
	public void testChangeCategorieImpotSource() throws Exception {

		final long dpiId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1980, 10, 25), Sexe.MASCULIN);
				final DebiteurPrestationImposable dpi = addDebiteur(null, pp, date(2010, 1, 1));
				dpi.setModeCommunication(ModeCommunication.PAPIER);
				dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
				dpi.addPeriodicite(new Periodicite(PeriodiciteDecompte.MENSUEL, null, date(2010, 1, 1), null));
				return dpi.getNumero();
			}
		});

		DebiteurEditView view = tiersEditManager.getDebiteurEditView(dpiId);
		assertEquals(CategorieImpotSource.REGULIERS, view.getCategorieImpotSource());
		{
			view.setCategorieImpotSource(CategorieImpotSource.ADMINISTRATEURS);
			tiersEditManager.save(view);
			view = tiersEditManager.getDebiteurEditView(dpiId);
			assertEquals(CategorieImpotSource.ADMINISTRATEURS, view.getCategorieImpotSource());
		}
		{
			view.setCategorieImpotSource(CategorieImpotSource.LOI_TRAVAIL_AU_NOIR);
			tiersEditManager.save(view);
			view = tiersEditManager.getDebiteurEditView(dpiId);
			assertEquals(CategorieImpotSource.LOI_TRAVAIL_AU_NOIR, view.getCategorieImpotSource());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRefresh() throws AdresseException, ServiceInfrastructureException {
		TiersEditView view = tiersEditManager.getView(6789L);
		view.getTiers().setPersonneContact("toto");
		tiersEditManager.refresh(view, 6789L);
	}

	public TiersEditManager getTiersEditManager() {
		return tiersEditManager;
	}

	public void setTiersEditManager(TiersEditManager tiersEditManager) {
		this.tiersEditManager = tiersEditManager;
	}
}
