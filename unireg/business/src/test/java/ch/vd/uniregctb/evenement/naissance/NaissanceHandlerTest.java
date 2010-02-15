package ch.vd.uniregctb.evenement.naissance;

import static junit.framework.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ch.vd.uniregctb.evenement.EvenementFiscal;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.util.Assert;

import ch.vd.uniregctb.evenement.AbstractEvenementHandlerTest;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.Tiers;

public class NaissanceHandlerTest extends AbstractEvenementHandlerTest {

	private static final Logger LOGGER = Logger.getLogger(NaissanceHandlerTest.class);

	/**
	 * Le numero d'individu du nouveau né.
	 */
	private static final long NOUVEAU_NE = 983254L;
	private static final long NOUVEAU_NE_MAJEUR = 89123L;
	private static final long NOUVEAU_NE_FIN_ANNEE = 123456L;

	/**
	 * La date de naissance.
	 */
	//private static final RegDate DATE_NAISSANCE = RegDate.get(1964, 4, 8);



	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		serviceCivil.setUp(new DefaultMockServiceCivil());
	}

	@Test
	public void testHandle() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de naissance.");

		{
			List<Tiers> tierss = tiersDAO.getAll();
			assertEquals("le tiers correspondant au nouveau n'a pas été créé", 0, tierss.size());
		}

		Individu bebe = serviceCivil.getIndividu(NOUVEAU_NE, 2007);
		Naissance naissance = createValidNaissance(bebe);
		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		evenementCivilHandler.checkCompleteness(naissance, erreurs, warnings);
		evenementCivilHandler.validate(naissance, erreurs, warnings);
		evenementCivilHandler.handle(naissance, warnings);

		Assert.isTrue(erreurs.isEmpty(), "Une erreur est survenue lors du traitement de la naissance");

		List<Tiers> tierss = tiersDAO.getAll();
		assertEquals("le tiers correspondant au nouveau n'a pas été créé", 1, tierss.size());
		/*
		 * une événement doit être créé et un événement doit être publié
		 */
		assertEquals(1, getMockEvenementFiscalFacade().count);
		assertEquals(1, getEvenementFiscalService().getEvenementFiscals(tierss.get(0)).size());
	}

	@Test
	public void testHandleNaissanceFinAnnee() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de naissance en fin d'année.");

		{
			List<Tiers> tierss = tiersDAO.getAll();
			assertEquals("le tiers correspondant au nouveau n'a pas été créé", 0, tierss.size());
		}

		Individu bebe = serviceCivil.getIndividu(NOUVEAU_NE_FIN_ANNEE, 2007);
		Naissance naissance = createValidNaissance(bebe);
		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		evenementCivilHandler.checkCompleteness(naissance, erreurs, warnings);
		evenementCivilHandler.validate(naissance, erreurs, warnings);
		evenementCivilHandler.handle(naissance, warnings);

		Assert.isTrue(erreurs.isEmpty(), "Une erreur est survenue lors du traitement de la naissance");

		List<Tiers> tierss = tiersDAO.getAll();
		assertEquals("le tiers correspondant au nouveau n'a pas été créé", 1, tierss.size());
		/*
		 * une événement doit être créé et un événement doit être publié
		 */
		assertEquals(1, getMockEvenementFiscalFacade().count);
		assertEquals(1, getEvenementFiscalService().getEvenementFiscals(tierss.get(0)).size());

		// l'événement fiscal ne doit pas avoir eu sa date décalé à l'année suivante!
		final EvenementFiscal evtFiscal = getEvenementFiscalService().getEvenementFiscals(tierss.get(0)).iterator().next();
		assertEquals(bebe.getDateNaissance(), evtFiscal.getDateEvenement());
	}

	@Test
	public void testHandleMajeur() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de naissance.");

		Individu bebe = serviceCivil.getIndividu(NOUVEAU_NE_MAJEUR, 2007);
		Naissance naissance = createValidNaissance(bebe);
		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		evenementCivilHandler.checkCompleteness(naissance, erreurs, warnings);
		evenementCivilHandler.validate(naissance, erreurs, warnings);
		evenementCivilHandler.handle(naissance, warnings);

		Assert.isTrue(!erreurs.isEmpty(), "Une erreur aurait du survenir puisque l'individu est majeur");
	}

	private MockNaissance createValidNaissance(Individu individu) {
		MockNaissance naissance = new MockNaissance();
		naissance.setIndividu(individu);

		naissance.setNumeroOfsCommuneAnnonce(4848);
		naissance.setDate(individu.getDateNaissance());

		return naissance;
	}

}
