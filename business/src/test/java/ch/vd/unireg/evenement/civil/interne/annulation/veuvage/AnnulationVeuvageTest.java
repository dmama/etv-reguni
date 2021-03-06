package ch.vd.unireg.evenement.civil.interne.annulation.veuvage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.unireg.evenement.civil.interne.MessageCollector;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockIndividuConnector;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.type.TypePermis;
import ch.vd.unireg.type.TypeRapportEntreTiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class AnnulationVeuvageTest extends AbstractEvenementCivilInterneTest {

	private static final Log LOGGER = LogFactory.getLog(AnnulationVeuvageTest.class);

	/**
	 * Le numéro d'individu du marié seul.
	 */
	private static final long NO_INDIVIDU_MARIE_SEUL = 92647;

	/**
	 * Le numéro d'individu du marié seul.
	 */
	private static final long NO_INDIVIDU_NON_VEUF = 54321;

	/**
	 * La date de veuvage.
	 */
	private static final RegDate DATE_VEUVAGE = RegDate.get(2008, 1, 1);

	/**
	 * Le fichier de données de test.
	 */
	private static final String DB_UNIT_DATA_FILE = "AnnulationVeuvageTest.xml";

	@Override
	public void onSetUp() throws Exception {

		final RegDate dateMariageAndre = RegDate.get(1982, 12, 4);

		super.onSetUp();
		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				super.init();

				MockIndividu andre = addIndividu(NO_INDIVIDU_MARIE_SEUL, RegDate.get(1956, 2, 25), "Girard", "André", true);
				addDefaultAdressesTo(andre);
				marieIndividu(andre, dateMariageAndre);
				addNationalite(andre, MockPays.France, andre.getDateNaissance(), null);
				addPermis(andre, TypePermis.FRONTALIER, RegDate.get(2008, 9, 8), null, false);
			}
		});

		loadDatabase(DB_UNIT_DATA_FILE);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAnnulationVeuvageMarieSeul() throws EvenementCivilException {

		LOGGER.debug("Test de traitement d'un événement d'annulation de veuvage d'un marié seul.");

		Individu veuf = serviceCivil.getIndividu(NO_INDIVIDU_MARIE_SEUL, date(2008, 12, 31));
		AnnulationVeuvage annulation = createValidAnnulationVeuvage(veuf);

		final MessageCollector collector = buildMessageCollector();
		annulation.validate(collector, collector);
		assertEmpty("Une erreur est survenue lors du validate de l'annulation de veuvage", collector.getErreurs());
		annulation.handle(collector);

		PersonnePhysique andre = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_MARIE_SEUL);
		assertNotNull("Plusieurs habitants trouvés avec le même numero individu (ou aucun)", andre);

		// Vérification des fors fiscaux
		assertNull("André ne doit pas avoir de for principal actif après l'annulation de veuvage", andre.getForFiscalPrincipalAt(null));
		for (ForFiscal forFiscal : andre.getForsFiscaux()) {
			if (forFiscal.getDateFin() == null && DATE_VEUVAGE.getOneDayAfter().equals(forFiscal.getDateDebut())) {
				assertEquals("Les fors fiscaux créés lors du veuvage doivent êtres annulés", true, forFiscal.isAnnule());
			}
		}

		// Vérification de la présence d'un tiers MenageCommun
		MenageCommun menageCommun = null;
		int nbMenagesCommuns = 0;
		for ( RapportEntreTiers rapport : andre.getRapportsSujet() ) {
			if (rapport.getType() == TypeRapportEntreTiers.APPARTENANCE_MENAGE) {
				nbMenagesCommuns++;
				menageCommun = (MenageCommun) tiersDAO.get(rapport.getObjetId());
			}
		}
		assertEquals("Il aurait dû y avoir 2 rapports entre tiers: 1 annulé et 1 rouvert", 2, nbMenagesCommuns);

		// Vérification du for principal du tiers MenageCommun
		ForFiscalPrincipal forCommun = menageCommun.getForFiscalPrincipalAt(null);
		assertNotNull("Aucun for fiscal principal trouvé sur le tiers MenageCommun",
				forCommun);
		assertNull("Le for fiscal principal précédent devrait être rouvert (date null)",
				forCommun.getDateFin());
		assertNull("Le for fiscal principal précédent devrait être rouvert (motif fermeture null)",
				forCommun.getMotifFermeture());

		/*
		 * Evénements fiscaux devant être générés :
		 *  - annulation du for fermé du ménage
		 *  - ré-ouverture for fiscal principal du ménage André
		 */
		assertEquals(2, eventSender.getCount());
		assertEquals(2, getEvenementFiscalService().getEvenementsFiscaux(menageCommun).size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAnnulationVeuvageNonVeuf() throws EvenementCivilException {

		LOGGER.debug("Test de traitement d'un événement d'annulation de veuvage d'une personne non veuve.");

		Individu fauxVeuf = serviceCivil.getIndividu(NO_INDIVIDU_NON_VEUF, date(2008, 12, 31));
		AnnulationVeuvage annulation = createValidAnnulationVeuvage(fauxVeuf);

		final MessageCollector collector = buildMessageCollector();
		annulation.validate(collector, collector);
		assertEquals("Une erreur aurait dû se produire lors du validate de l'annulation.", 1, collector.getErreurs().size());
	}

	private AnnulationVeuvage createValidAnnulationVeuvage(Individu individu) {
		return new AnnulationVeuvage(individu, null, DATE_VEUVAGE, 5652, context);
	}

}
