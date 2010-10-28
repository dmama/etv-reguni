package ch.vd.uniregctb.tiers.manager;

import org.junit.Test;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.view.TiersEditView;
import ch.vd.uniregctb.type.TypeAdresseCivil;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
	public void testGetView() throws Exception {

		TiersEditView view = tiersEditManager.getView(new Long(6789));
		assertEquals("Bolomey", view.getIndividu().getNom());
	}

	/**
	 * Teste la methode creePersonne
	 */

	@Test
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


	@Test
	public void testRefresh() throws AdresseException, InfrastructureException {
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
