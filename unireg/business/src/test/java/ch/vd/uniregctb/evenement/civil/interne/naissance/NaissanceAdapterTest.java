package ch.vd.uniregctb.evenement.civil.interne.naissance;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.Assert;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.AbstractEvenementAdapterTest;
import ch.vd.uniregctb.evenement.EvenementFiscal;
import ch.vd.uniregctb.evenement.EvenementFiscalDAO;
import ch.vd.uniregctb.evenement.EvenementFiscalNaissance;
import ch.vd.uniregctb.evenement.EvenementFiscalSituationFamille;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

@SuppressWarnings({"JavaDoc"})
public class NaissanceAdapterTest extends AbstractEvenementAdapterTest {

	private static final Logger LOGGER = Logger.getLogger(NaissanceAdapterTest.class);

	/**
	 * Le numéro d'individu du nouveau né.
	 */
	private static final long NOUVEAU_NE = 983254L;
	private static final long NOUVEAU_NE_MAJEUR = 89123L;
	private static final long NOUVEAU_NE_FIN_ANNEE = 123456L;

	private EvenementFiscalDAO evenementFiscalDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		serviceCivil.setUp(new DefaultMockServiceCivil());
		evenementFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");
	}

	@Test
	public void testHandle() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de naissance.");

		{
			List<Tiers> tierss = tiersDAO.getAll();
			assertEquals("le tiers correspondant au nouveau n'a pas été créé", 0, tierss.size());
		}

		Individu bebe = serviceCivil.getIndividu(NOUVEAU_NE, 2007);
		NaissanceAdapter naissance = createValidNaissance(bebe);
		List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		naissance.checkCompleteness(erreurs, warnings);
		naissance.validate(erreurs, warnings);
		naissance.handle(warnings);

		Assert.isTrue(erreurs.isEmpty(), "Une erreur est survenue lors du traitement de la naissance");

		List<Tiers> tierss = tiersDAO.getAll();
		assertEquals("le tiers correspondant au nouveau n'a pas été créé", 1, tierss.size());
		/*
		 * une événement doit être créé et un événement doit être publié
		 */
		assertEquals(1, eventSender.count);
		assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(tierss.get(0)).size());
	}

	@Test
	public void testHandleNaissanceFinAnnee() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de naissance en fin d'année.");

		{
			List<Tiers> tierss = tiersDAO.getAll();
			assertEquals("le tiers correspondant au nouveau n'a pas été créé", 0, tierss.size());
		}

		Individu bebe = serviceCivil.getIndividu(NOUVEAU_NE_FIN_ANNEE, 2007);
		NaissanceAdapter naissance = createValidNaissance(bebe);
		List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		naissance.checkCompleteness(erreurs, warnings);
		naissance.validate(erreurs, warnings);
		naissance.handle(warnings);

		Assert.isTrue(erreurs.isEmpty(), "Une erreur est survenue lors du traitement de la naissance");

		List<Tiers> tierss = tiersDAO.getAll();
		assertEquals("le tiers correspondant au nouveau n'a pas été créé", 1, tierss.size());
		/*
		 * une événement doit être créé et un événement doit être publié
		 */
		assertEquals(1, eventSender.count);
		assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(tierss.get(0)).size());

		// l'événement fiscal ne doit pas avoir eu sa date décalé à l'année suivante!
		final EvenementFiscal evtFiscal = getEvenementFiscalService().getEvenementsFiscaux(tierss.get(0)).iterator().next();
		assertEquals(bebe.getDateNaissance(), evtFiscal.getDateEvenement());
	}

	@Test
	public void testHandleMajeur() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de naissance.");

		Individu bebe = serviceCivil.getIndividu(NOUVEAU_NE_MAJEUR, 2007);
		NaissanceAdapter naissance = createValidNaissance(bebe);
		List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		naissance.checkCompleteness(erreurs, warnings);
		naissance.validate(erreurs, warnings);
		naissance.handle(warnings);

		Assert.isTrue(!erreurs.isEmpty(), "Une erreur aurait du survenir puisque l'individu est majeur");
	}

	private NaissanceAdapter createValidNaissance(Individu individu) {
		return new NaissanceAdapter(individu, null, individu.getDateNaissance(), 4848, null, context);
	}

	/**
	 * [UNIREG-3244] Teste que le traitement d'un événement civil de naissance provoque bien l'envoi d'un événement fiscal de naissance.
	 */
	@Test
	@NotTransactional
	public void testHandlePourEnvoiEvenementFiscalDeNaissance() throws Exception {

		final long indPere = 1;
		final long indMere = 2;
		final long indFils = 3;

		// On crée la situation de départ : une mère et un fils mineur qui possède un immeuble
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pere = addIndividu(indPere, date(1980, 1, 1), "Cognac", "Raoul", true);
				MockIndividu mere = addIndividu(indMere, date(1980, 1, 1), "Cognac", "Josette", false);
				MockIndividu fils = addIndividu(indFils, date(2010, 2, 8), "Cognac", "Yvan", true);
				fils.setMere(pere);
				fils.setMere(mere);
			}
		});

		class Ids {
			Long pere;
			Long mere;
			Long fils;
		}
		final Ids ids = new Ids();

		// On crée le père et la mère
		doInNewTransactionAndSession(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pere = addHabitant(indPere);
				ids.pere = pere.getId();
				final PersonnePhysique mere = addHabitant(indMere);
				ids.mere = mere.getId();
				return null;
			}
		});

		// On envoie l'événement de naissance
		doInNewTransactionAndSession(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Individu fils = serviceCivil.getIndividu(indFils, 2010);
				final NaissanceAdapter naissance = createValidNaissance(fils);

				List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
				List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

				naissance.checkCompleteness(erreurs, warnings);
				assertEmpty(erreurs);
				assertEmpty(warnings);

				naissance.validate(erreurs, warnings);
				assertEmpty(erreurs);
				assertEmpty(warnings);

				final Pair<PersonnePhysique, PersonnePhysique> res = naissance.handle(warnings);
				assertEmpty(erreurs);
				assertEmpty(warnings);

				ids.fils = res.getFirst().getNumero();
				return null;
			}
		});


		// On vérifie que il y a eu :
		// - un événement de changement de situation de famille
		// - un événement de naissane
		doInNewTransactionAndSession(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
				assertNotNull(events);
				assertEquals(2, events.size());

				final EvenementFiscalSituationFamille event0 = (EvenementFiscalSituationFamille) events.get(0);
				assertNotNull(event0);

				final EvenementFiscalNaissance event1 = (EvenementFiscalNaissance) events.get(1);
				assertNotNull(event1);
				assertEquals(ids.mere, event1.getTiers().getNumero());
				assertEquals(ids.fils, event1.getEnfant().getNumero());
				assertEquals(date(2010, 2, 8), event1.getDateEvenement());
				return null;
			}
		});
	}
}
