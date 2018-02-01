package ch.vd.unireg.registrefoncier;

import java.util.List;

import org.junit.Test;

import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.evenement.fiscal.EvenementFiscal;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.unireg.evenement.fiscal.registrefoncier.EvenementFiscalImmeuble;
import ch.vd.unireg.registrefoncier.dao.CommuneRFDAO;
import ch.vd.unireg.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.unireg.registrefoncier.dao.SituationRFDAO;
import ch.vd.unireg.registrefoncier.dataimport.ImportRFTestClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RegistreFoncierServiceTest extends ImportRFTestClass {

	private CommuneRFDAO communeRFDAO;
	private SituationRFDAO situationRFDAO;
	private ImmeubleRFDAO immeubleRFDAO;
	private EvenementFiscalDAO evenementFiscalDAO;
	private RegistreFoncierService registreFoncierService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		communeRFDAO = getBean(CommuneRFDAO.class, "communeRFDAO");
		situationRFDAO = getBean(SituationRFDAO.class, "situationRFDAO");
		immeubleRFDAO = getBean(ImmeubleRFDAO.class, "immeubleRFDAO");
		evenementFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");
		registreFoncierService = getBean(RegistreFoncierService.class, "serviceRF");
	}

	/**
	 * [SIFISC-24367] vérifie que la surcharge d'une commune sur une situation envoie bien un événement fiscal de modification de situation.
	 */
	@Test
	public void testSurchargerCommuneFiscaleSituation() throws Exception {

		final Long situationId = doInNewTransaction(status -> {

			// la commune faîtière de l'Abbaye
			final CommuneRF abbaye = communeRFDAO.save(newCommuneRF(139, "L'Abbaye", 5871));

			SituationRF situation = new SituationRF();
			situation.setCommune(abbaye);
			situation.setNoParcelle(1221);
			situation.setDateDebut(null);

			BienFondsRF immeuble = new BienFondsRF();
			immeuble.setIdRF("_1f109152381026b501381028a73d1852");
			immeuble.setCfa(false);
			immeuble.setEgrid("CHE2389838");
			immeuble.addSituation(situation);
			immeuble = (BienFondsRF) immeubleRFDAO.save(immeuble);

			situation = immeuble.getSituations().iterator().next();
			return situation.getId();
		});

		// on demande la surcharge de la situation
		doInNewTransaction(status -> {
			registreFoncierService.surchargerCommuneFiscaleSituation(situationId, MockCommune.Fraction.LesBioux.getNoOFS());
			return null;
		});

		// on vérifie que la situation est modifiée
		doInNewTransaction(status -> {
			final SituationRF situation = situationRFDAO.get(situationId);
			assertNotNull(situation);
			assertEquals(Integer.valueOf(MockCommune.Fraction.LesBioux.getNoOFS()), situation.getNoOfsCommuneSurchargee());
			assertEquals(MockCommune.Fraction.LesBioux.getNoOFS(), situation.getNoOfsCommune());
			return null;
		});

		// on vérifie que l'événement fiscal correspondant est parti
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(1, events.size());

			final EvenementFiscalImmeuble event0 = (EvenementFiscalImmeuble) events.get(0);
			assertEquals(EvenementFiscalImmeuble.TypeEvenementFiscalImmeuble.MODIFICATION_SITUATION, event0.getType());
			assertEquals(null, event0.getDateValeur());
			assertEquals("_1f109152381026b501381028a73d1852", event0.getImmeuble().getIdRF());
			return null;
		});
	}
}
