package ch.vd.uniregctb.evenement.fiscal;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.evenement.*;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementFiscal;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class EvenementFiscalServiceTest extends BusinessTest {

	private final static String DB_UNIT_DATA_FILE = "classpath:ch/vd/uniregctb/evenement/fiscal/EvenementFiscalServiceTest.xml";
	private final static Long NUMERO_CONTRIBUABLE = 12300002L;
	private final static Long NUMERO_DEBITEUR = 12500001L;


	private EvenementFiscalService evenementFiscalService;
	private MockEvenementFiscalSender evenementFiscalSender;
	private EvenementFiscalDAO evenementFiscalDAO;


	private TiersDAO tiersDAO;

	public EvenementFiscalServiceTest() {
		super();
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		evenementFiscalService = getBean(EvenementFiscalService.class, "evenementFiscalService");
		evenementFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");
	    evenementFiscalSender = getBean(MockEvenementFiscalSender.class, "evenementFiscalSender");

		loadDatabase(DB_UNIT_DATA_FILE);
		tiersDAO = getBean( TiersDAO.class, "tiersDAO");
	}

	@Test
	public void testPublierEvenementNullArgument() throws Exception {
		try {
			evenementFiscalService.publierEvenementFiscal(null);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("evenementFiscal ne peut être null.", e.getMessage());
		}
	}
	
	@Test
	public void testPublierEvenementFor() throws Exception {

		evenementFiscalSender.count = 0;
		assertEquals(0, evenementFiscalDAO.getAll().size());

		// Crée et publie un événement
		final Tiers tiers = tiersDAO.get(NUMERO_CONTRIBUABLE);
		final EvenementFiscalFor event = new EvenementFiscalFor(tiers, RegDate.get(), TypeEvenementFiscal.OUVERTURE_FOR, MotifFor.ARRIVEE_HS, null, (long) 1);
		evenementFiscalService.publierEvenementFiscal(event);

		// Vérifie que l'événement a été envoyé
		assertEquals(1, evenementFiscalSender.count);

		// Vérifie que l'événement est dans la base
		final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
		assertEquals(1, events.size());
		assertForEvent(NUMERO_CONTRIBUABLE, RegDate.get(), TypeEvenementFiscal.OUVERTURE_FOR, null, MotifFor.ARRIVEE_HS, 1L, events.get(0));
	}

	@Test
	public void publierEvenementFiscalRetourLR() throws Exception {

		evenementFiscalSender.count = 0;
		assertEquals(0, evenementFiscalDAO.getAll().size());

		// Crée et publie un événement
		final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiersDAO.get(NUMERO_DEBITEUR);
		final EvenementFiscalLR event = new EvenementFiscalLR(debiteur, RegDate.get(), TypeEvenementFiscal.RETOUR_LR, date(2005, 1, 1), date(2005, 6, 30), (long) 1);
		evenementFiscalService.publierEvenementFiscal(event);

		// Vérifie que l'événement a été envoyé
		assertEquals(1, evenementFiscalSender.count);

		// Vérifie que l'événement est dans la base
		final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
		assertEquals(1, events.size());
		assertLREvent(NUMERO_DEBITEUR, RegDate.get(), TypeEvenementFiscal.RETOUR_LR, date(2005, 1, 1), date(2005, 6, 30), 1L, events.get(0));
	}

	@Test
	public void publierEvenementFiscalChangementSituation() throws Exception {

		evenementFiscalSender.count = 0;
		assertEquals(0, evenementFiscalDAO.getAll().size());

		// Crée et publie un événement
		final Tiers tiers = tiersDAO.get(NUMERO_CONTRIBUABLE);
		final EvenementFiscalSituationFamille event = new EvenementFiscalSituationFamille(tiers, RegDate.get(), (long) 1);
		evenementFiscalService.publierEvenementFiscal(event);

		// Vérifie que l'événement a été envoyé
		assertEquals(1, evenementFiscalSender.count);

		// Vérifie que l'événement est dans la base
		final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
		assertEquals(1, events.size());
		assertCSFEvent(NUMERO_CONTRIBUABLE, RegDate.get(), 1L, events.get(0));
	}

	private static void assertCSFEvent(Long tiersId, RegDate dateEvenement, long numeroTechnique, EvenementFiscal event) {
		assertEvent(tiersId, dateEvenement, TypeEvenementFiscal.CHANGEMENT_SITUATION_FAMILLE, numeroTechnique, event);
	}

	private static void assertForEvent(Long tiersId, RegDate dateEvenement, TypeEvenementFiscal type, ModeImposition modeImposition, MotifFor motifFor, Long numeroTechnique, EvenementFiscal event) {
		assertEvent(tiersId, dateEvenement, type, numeroTechnique, event);
		final EvenementFiscalFor forEvent = (EvenementFiscalFor) event;
		assertEquals(modeImposition, forEvent.getModeImposition());
		assertEquals(motifFor, forEvent.getMotifFor());
	}

	private static void assertLREvent(Long tiersId, RegDate dateEvenement, TypeEvenementFiscal type, RegDate dateDebutPeriode, RegDate dateFinPeriode, Long numeroTechnique, EvenementFiscal event) {
		assertEvent(tiersId, dateEvenement, type, numeroTechnique, event);
		final EvenementFiscalLR forEvent = (EvenementFiscalLR) event;
		assertEquals(dateDebutPeriode, forEvent.getDateDebutPeriode());
		assertEquals(dateFinPeriode, forEvent.getDateFinPeriode());
	}

	private static void assertEvent(Long tiersId, RegDate dateEvenement, TypeEvenementFiscal type, Long numeroTechnique, EvenementFiscal event0) {
		assertNotNull(event0);
		assertEquals(dateEvenement, event0.getDateEvenement());
		assertEquals(numeroTechnique, event0.getNumeroTechnique());
		assertEquals(type, event0.getType());
		assertEquals(tiersId, event0.getTiers().getNumero());
	}
}
