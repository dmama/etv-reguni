package ch.vd.uniregctb.activation;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.tiers.AnnuleEtRemplace;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.Tache;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class ActivationServiceTest extends BusinessTest{

	private final static String DB_UNIT_DATA_FILE = "classpath:ch/vd/uniregctb/activation/ActivationServiceTest.xml";

	private ActivationService activationService;
	private TiersService tiersService;
	private TacheDAO tacheDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		activationService = getBean(ActivationService.class, "activationService");
		tiersService = getBean(TiersService.class, "tiersService");
		tacheDAO = getBean(TacheDAO.class, "tacheDAO");
	}

	@Test
	public void testAnnuleTiers() throws Exception {
		loadDatabase(DB_UNIT_DATA_FILE);
		Tiers tiers = tiersService.getTiers(12600003);
		RegDate dateAnnulation = RegDate.get(2010, 1, 1);
		activationService.annuleTiers(tiers, dateAnnulation);
		assertEquals(dateAnnulation, RegDate.get(tiers.getAnnulationDate()));
		ForFiscalPrincipal forFiscalPrincipal = tiers.getForFiscalPrincipalAt(dateAnnulation);
		assertEquals(MotifFor.ANNULATION, forFiscalPrincipal.getMotifFermeture());
		Tache tache = tacheDAO.get(Long.valueOf(1));
		assertEquals(dateAnnulation, RegDate.get(tache.getAnnulationDate()));
	}

	@Test
	public void testRemplaceTiers() throws Exception {
		loadDatabase(DB_UNIT_DATA_FILE);
		Tiers tiersRemplace = tiersService.getTiers(12600003);
		Tiers tiersRemplacant = tiersService.getTiers(12600009);
		RegDate dateRemplacement = RegDate.get(2010, 1, 1);
		activationService.remplaceTiers(tiersRemplace, tiersRemplacant, dateRemplacement);
		assertEquals(dateRemplacement, RegDate.get(tiersRemplace.getAnnulationDate()));
		ForFiscalPrincipal forFiscalPrincipal = tiersRemplace.getForFiscalPrincipalAt(dateRemplacement);
		assertEquals(MotifFor.ANNULATION, forFiscalPrincipal.getMotifFermeture());
		AnnuleEtRemplace annuleEtRemplace = (AnnuleEtRemplace) tiersRemplacant.getRapportObjetValidAt(dateRemplacement, TypeRapportEntreTiers.ANNULE_ET_REMPLACE);
		assertNotNull(annuleEtRemplace);
		annuleEtRemplace = (AnnuleEtRemplace) tiersRemplace.getRapportSujetValidAt(dateRemplacement, TypeRapportEntreTiers.ANNULE_ET_REMPLACE);
		assertNotNull(annuleEtRemplace);
		Tache tache = tacheDAO.get(Long.valueOf(1));
		assertEquals(dateRemplacement, RegDate.get(tache.getAnnulationDate()));
	}

	@Test
	public void testReactiveTiers() throws Exception {
		loadDatabase(DB_UNIT_DATA_FILE);
		Tiers tiersAnnule = tiersService.getTiers(12600004);
		RegDate dateReactivation = RegDate.get(2010, 1, 1);
		activationService.reactiveTiers(tiersAnnule, dateReactivation);
		assertNull(tiersAnnule.getAnnulationDate());
		assertNull(tiersAnnule.getAnnulationUser());
		ForFiscalPrincipal forFiscalPrincipal = tiersAnnule.getForFiscalPrincipalAt(dateReactivation);
		assertEquals(MotifFor.REACTIVATION, forFiscalPrincipal.getMotifOuverture());
	}

}
