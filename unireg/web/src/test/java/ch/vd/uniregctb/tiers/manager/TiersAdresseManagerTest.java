package ch.vd.uniregctb.tiers.manager;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseEtrangere;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.adresse.AdresseTiersDAO;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.view.AdresseView;
import ch.vd.uniregctb.type.TypeAdresseTiers;

import static junit.framework.Assert.assertEquals;

public class TiersAdresseManagerTest extends WebTest {

	private AdresseManager adresseManager;

	private final static String TYPE_LOCALITE_SUISSE = "suisse";

	private final static String TYPE_LOCALITE_PAYS = "pays";

	private final static String DB_UNIT_FILE = "TiersVisuManagerTest.xml";

	/**
	 * @see ch.vd.uniregctb.common.AbstractCoreDAOTest#onSetUp()
	 */
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		serviceCivil.setUp(new MockServiceCivil() {
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
		AdresseView adview = adresseManager.getAdresseView(new Long(5));
		assertEquals("75012 PARIS", adview.getLocaliteNpa());
		assertEquals(new Integer(8212), adview.getPaysOFS());
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
		adresseView.setNumCTB(new Long(67895));
		adresseManager.save(adresseView);

		TiersDAO tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		Tiers tiers = tiersDAO.get(new Long(67895));
		assertEquals(1, tiers.getAdressesTiers().size());

	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testModifyAdresseSuisse() throws Exception {

		AdresseView adresseView = new AdresseView();
		adresseView.setId(new Long(192));
		adresseView.setUsage(TypeAdresseTiers.COURRIER);
		adresseView.setTypeLocalite(TYPE_LOCALITE_SUISSE);
		adresseView.setNumeroOrdrePoste("1269");
		adresseView.setDateDebut(RegDate.get());
		adresseView.setLocaliteSuisse("Neuchâtel 1 Cases");
		adresseView.setNumCTB(new Long(6789));
		adresseManager.save(adresseView);

		AdresseTiersDAO adresseTiersDAO = getBean(AdresseTiersDAO.class, "adresseTiersDAO");
		AdresseTiers adresseTiers = adresseTiersDAO.get(new Long(192));
		AdresseSuisse addSuisse = (AdresseSuisse) adresseTiers;
		assertEquals(1269, addSuisse.getNumeroOrdrePoste().intValue());

	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSaveAdresseEtrangere() throws Exception {

		AdresseView adresseView = new AdresseView();
		adresseView.setNumCTB(new Long(56789));
		adresseView.setUsage(TypeAdresseTiers.COURRIER);
		adresseView.setTypeLocalite(TYPE_LOCALITE_PAYS);
		adresseView.setPaysNpa("France");
		adresseView.setDateDebut(RegDate.get());
		adresseView.setPaysOFS(8212);
		adresseView.setLocaliteNpa("Paris");
		adresseManager.save(adresseView);

		TiersDAO tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		Tiers tiers = tiersDAO.get(new Long(56789));
		assertEquals(1, tiers.getAdressesTiers().size());

	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testModifyAdresseEtrangere() throws Exception {

		AdresseView adresseView = new AdresseView();
		adresseView.setId(new Long(5));
		adresseView.setNumCTB(new Long(6789));
		adresseView.setUsage(TypeAdresseTiers.COURRIER);
		adresseView.setTypeLocalite(TYPE_LOCALITE_PAYS);
		adresseView.setPaysNpa("France");
		adresseView.setDateDebut(RegDate.get());
		adresseView.setPaysOFS(8212);
		adresseView.setLocaliteNpa("Lyon");
		adresseManager.save(adresseView);

		AdresseTiersDAO adresseTiersDAO = getBean(AdresseTiersDAO.class, "adresseTiersDAO");
		AdresseTiers adresseTiers = adresseTiersDAO.get(new Long(5));
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
