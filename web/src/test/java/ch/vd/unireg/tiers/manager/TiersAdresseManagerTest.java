package ch.vd.unireg.tiers.manager;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseEtrangere;
import ch.vd.unireg.adresse.AdresseSuisse;
import ch.vd.unireg.adresse.AdresseTiers;
import ch.vd.unireg.adresse.AdresseTiersDAO;
import ch.vd.unireg.common.WebTest;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.view.AdresseView;
import ch.vd.unireg.type.TypeAdresseTiers;

import static org.junit.Assert.assertEquals;

public class TiersAdresseManagerTest extends WebTest {

	private AdresseManager adresseManager;

	private static final String TYPE_LOCALITE_SUISSE = "suisse";

	private static final String TYPE_LOCALITE_PAYS = "pays";

	private static final String DB_UNIT_FILE = "TiersVisuManagerTest.xml";

	/**
	 * @see ch.vd.unireg.common.AbstractCoreDAOTest#onSetUp()
	 */
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				addIndividu(282315, RegDate.get(1974, 3, 22), "Bolomey", "Alain", true);
				addIndividu(282316, RegDate.get(1974, 3, 22), "Richard", "Marcel", true);
				addIndividu(282312, RegDate.get(1974, 3, 22), "Paul", "Marcel", true);

			}
		});

		loadDatabase(DB_UNIT_FILE);
		adresseManager = getBean(AdresseManager.class, "adresseManager");

	}

	/**
	 * Teste la methode getView
	 */

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetAdresseView() throws Exception {
		AdresseView adview = adresseManager.getAdresseView(5L);
		assertEquals("75012 PARIS", adview.getLocaliteNpa());
		assertEquals(Integer.valueOf(8212), adview.getPaysOFS());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSaveAdresseSuisse() throws Exception {

		AdresseView adresseView = new AdresseView();
		adresseView.setUsage(TypeAdresseTiers.COURRIER);
		adresseView.setTypeLocalite(TYPE_LOCALITE_SUISSE);
		adresseView.setNumeroOrdrePoste("1269");
		adresseView.setDateDebut(RegDate.get());
		adresseView.setLocaliteSuisse("Neuchâtel 1 Cases");
		adresseView.setNumCTB(67895L);
		adresseManager.save(adresseView);

		TiersDAO tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		Tiers tiers = tiersDAO.get(67895L);
		assertEquals(1, tiers.getAdressesTiers().size());

	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testModifyAdresseSuisse() throws Exception {

		AdresseView adresseView = new AdresseView();
		adresseView.setId(192L);
		adresseView.setUsage(TypeAdresseTiers.COURRIER);
		adresseView.setTypeLocalite(TYPE_LOCALITE_SUISSE);
		adresseView.setNumeroOrdrePoste("1269");
		adresseView.setDateDebut(RegDate.get());
		adresseView.setLocaliteSuisse("Neuchâtel 1 Cases");
		adresseView.setNumCTB(6789L);
		adresseManager.save(adresseView);

		AdresseTiersDAO adresseTiersDAO = getBean(AdresseTiersDAO.class, "adresseTiersDAO");
		AdresseTiers adresseTiers = adresseTiersDAO.get(192L);
		AdresseSuisse addSuisse = (AdresseSuisse) adresseTiers;
		assertEquals(1269, addSuisse.getNumeroOrdrePoste().intValue());

	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSaveAdresseEtrangere() throws Exception {

		AdresseView adresseView = new AdresseView();
		adresseView.setNumCTB(56789L);
		adresseView.setUsage(TypeAdresseTiers.COURRIER);
		adresseView.setTypeLocalite(TYPE_LOCALITE_PAYS);
		adresseView.setPaysNpa("France");
		adresseView.setDateDebut(RegDate.get());
		adresseView.setPaysOFS(8212);
		adresseView.setLocaliteNpa("Paris");
		adresseManager.save(adresseView);

		TiersDAO tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		Tiers tiers = tiersDAO.get(56789L);
		assertEquals(1, tiers.getAdressesTiers().size());

	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testModifyAdresseEtrangere() throws Exception {

		AdresseView adresseView = new AdresseView();
		adresseView.setId(5L);
		adresseView.setNumCTB(6789L);
		adresseView.setUsage(TypeAdresseTiers.COURRIER);
		adresseView.setTypeLocalite(TYPE_LOCALITE_PAYS);
		adresseView.setPaysNpa("France");
		adresseView.setDateDebut(RegDate.get());
		adresseView.setPaysOFS(8212);
		adresseView.setLocaliteNpa("Lyon");
		adresseManager.save(adresseView);

		AdresseTiersDAO adresseTiersDAO = getBean(AdresseTiersDAO.class, "adresseTiersDAO");
		AdresseTiers adresseTiers = adresseTiersDAO.get(5L);
		AdresseEtrangere addEtrangere = (AdresseEtrangere) adresseTiers;
		assertEquals("Lyon", addEtrangere.getNumeroPostalLocalite());

	}

	public AdresseManager getAdresseManager() {
		return adresseManager;
	}

	public void setAdresseManager(AdresseManager adresseManager) {
		this.adresseManager = adresseManager;
	}

}
