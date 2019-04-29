package ch.vd.unireg.evenement.civil.engine.ech;

import java.util.Set;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.unireg.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEch;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchErreur;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.unireg.evenement.civil.interne.EvenementCivilInterneComposite;
import ch.vd.unireg.evenement.civil.interne.HandleStatus;
import ch.vd.unireg.evenement.civil.interne.testing.Testing;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeEvenementCivilEch;
import ch.vd.unireg.type.TypeEvenementErreur;
import ch.vd.unireg.type.TypeRapportEntreTiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("JavaDoc")
public class EvenementCivilEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {

	@Test(timeout = 10000L)
	public void testEvenementExceptionDansHandle() throws Exception {

		traiterEvenementTesting(Testing.NoExceptionDansHandle, new AfterHandleCallback() {
			@Override
			public void checkEvent(EvenementCivilEch evt) {
				assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());

				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				assertNotNull(erreurs);
				assertEquals(1, erreurs.size());

				final EvenementCivilEchErreur erreur0 = erreurs.iterator().next();
				assertNotNull(erreur0);
				assertEquals(TypeEvenementErreur.ERROR, erreur0.getType());
				assertEquals("Exception de test", erreur0.getMessage());
			}
		});
	}

	@Test(timeout = 10000L)
	public void testEvenementTraiteAvecWarningDansHandle() throws Exception {

		traiterEvenementTesting(Testing.NoTraiteAvecWarningDansHandle, new AfterHandleCallback() {
			@Override
			public void checkEvent(EvenementCivilEch evt) {
				assertEquals(EtatEvenementCivil.A_VERIFIER, evt.getEtat());

				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				assertNotNull(erreurs);
				assertEquals(1, erreurs.size());

				final EvenementCivilEchErreur erreur0 = erreurs.iterator().next();
				assertNotNull(erreur0);
				assertEquals(TypeEvenementErreur.WARNING, erreur0.getType());
				assertEquals("Warning de test", erreur0.getMessage());
			}
		});
	}

	@Test(timeout = 10000L)
	public void testEvenementRedondantAvecWarningDansHandle() throws Exception {

		traiterEvenementTesting(Testing.NoRedondantAvecWarningDansHandle, new AfterHandleCallback() {
			@Override
			public void checkEvent(EvenementCivilEch evt) {
				assertEquals(EtatEvenementCivil.A_VERIFIER, evt.getEtat());

				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				assertNotNull(erreurs);
				assertEquals(1, erreurs.size());

				final EvenementCivilEchErreur erreur0 = erreurs.iterator().next();
				assertNotNull(erreur0);
				assertEquals(TypeEvenementErreur.WARNING, erreur0.getType());
				assertEquals("Warning de test", erreur0.getMessage());
			}
		});
	}

	@Test(timeout = 10000L)
	public void testEvenementTraiteSansWarningDansHandle() throws Exception {

		traiterEvenementTesting(Testing.NoTraiteSansWarning, new AfterHandleCallback() {
			@Override
			public void checkEvent(EvenementCivilEch evt) {
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
				assertEmpty(evt.getErreurs());
			}
		});
	}

	@Test(timeout = 10000L)
	public void testEvenementRedondantSansWarningDansHandle() throws Exception {

		traiterEvenementTesting(Testing.NoRedondantSansWarning, new AfterHandleCallback() {
			@Override
			public void checkEvent(EvenementCivilEch evt) {
				assertEquals(EtatEvenementCivil.REDONDANT, evt.getEtat());
				assertEmpty(evt.getErreurs());
			}
		});
	}
	
	@Test(timeout = 10000L)
	public void testTraitementEvenementIndexationPureSiErreur() throws Exception {

		final long noIndividu = 316547256L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, date(1976, 8, 22), "Kaderate", "Yamamoto", true);
			}
		});

		// aucune mise en place fiscale -> le souci lors du traitement du mariage, ce sera justement que le tiers n'existe pas...

		// création des événements sur cet individu : 1 événement qui part en erreur et un événement d'indexation pure à une date postérieure (ce dernier doit être traité quand-même)
		final long evtErreurId = 32657743L;
		final long evtIndexationId = 423677342L;
		doInNewTransactionAndSession(status -> {
			final RegDate dateMariage = date(2000, 1, 1);

			// événement qui partira en erreur
			final EvenementCivilEch evtErreur = new EvenementCivilEch();
			evtErreur.setId(evtErreurId);
			evtErreur.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evtErreur.setDateEvenement(dateMariage);
			evtErreur.setEtat(EtatEvenementCivil.A_TRAITER);
			evtErreur.setNumeroIndividu(noIndividu);
			evtErreur.setType(TypeEvenementCivilEch.MARIAGE);
			hibernateTemplate.merge(evtErreur);

			// événement d'indexation pure (obtention d'origine)
			final EvenementCivilEch evtIndexation = new EvenementCivilEch();
			evtIndexation.setId(evtIndexationId);
			evtIndexation.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evtIndexation.setDateEvenement(dateMariage.addYears(1));              // après celui qui partira en erreur, donc
			evtIndexation.setEtat(EtatEvenementCivil.A_TRAITER);
			evtIndexation.setNumeroIndividu(noIndividu);
			evtIndexation.setType(TypeEvenementCivilEch.OBENTION_DROIT_CITE);
			hibernateTemplate.merge(evtIndexation);
			return null;
		});

		// traitement de l'événement, passage en erreur et traitement de l'indexation pure quand-même (si tout va bien...)
		traiterEvenements(noIndividu);

		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evtErreur = evtCivilDAO.get(evtErreurId);
			assertNotNull(evtErreur);
			assertEquals(EtatEvenementCivil.EN_ERREUR, evtErreur.getEtat());

			final EvenementCivilEch evtIndexation = evtCivilDAO.get(evtIndexationId);
			assertNotNull(evtIndexation);
			assertEquals(EtatEvenementCivil.TRAITE, evtIndexation.getEtat());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testMiseEnAttente() throws Exception {

		final long noIndividu = 316547256L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, date(1976, 8, 22), "Kaderate", "Yamamoto", true);
			}
		});

		// aucune mise en place fiscale -> le souci lors du traitement du mariage, ce sera justement que le tiers n'existe pas...

		// création des événements sur cet individu : 1 événement qui part en erreur et un événement d'indexation pure à une date postérieure (ce dernier doit être traité quand-même)
		final long evtErreurId = 32657743L;
		final long evtAttenteId = 423677342L;
		doInNewTransactionAndSession(status -> {
			final RegDate dateMariage = date(2000, 1, 1);

			// événement qui partira en erreur
			final EvenementCivilEch evtErreur = new EvenementCivilEch();
			evtErreur.setId(evtErreurId);
			evtErreur.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evtErreur.setDateEvenement(dateMariage);
			evtErreur.setEtat(EtatEvenementCivil.A_TRAITER);
			evtErreur.setNumeroIndividu(noIndividu);
			evtErreur.setType(TypeEvenementCivilEch.MARIAGE);
			hibernateTemplate.merge(evtErreur);

			// événement qui sera mis en attente
			final EvenementCivilEch evtAttente = new EvenementCivilEch();
			evtAttente.setId(evtAttenteId);
			evtAttente.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evtAttente.setDateEvenement(dateMariage.addYears(1));              // après celui qui partira en erreur, donc
			evtAttente.setEtat(EtatEvenementCivil.A_TRAITER);
			evtAttente.setNumeroIndividu(noIndividu);
			evtAttente.setType(TypeEvenementCivilEch.DIVORCE);
			hibernateTemplate.merge(evtAttente);
			return null;
		});

		// traitement de l'événement, passage en erreur et traitement de l'indexation pure quand-même (si tout va bien...)
		traiterEvenements(noIndividu);

		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evtErreur = evtCivilDAO.get(evtErreurId);
			assertNotNull(evtErreur);
			assertEquals(EtatEvenementCivil.EN_ERREUR, evtErreur.getEtat());

			final EvenementCivilEch evtIndexation = evtCivilDAO.get(evtAttenteId);
			assertNotNull(evtIndexation);
			assertEquals(EtatEvenementCivil.EN_ATTENTE, evtIndexation.getEtat());
			return null;
		});
	}

	/**
	 * Cas du SIFISC-9031 : une annulation qui n'a pas de numéro d'individu est passée en attente sans se voir assigner de numéro d'individu
	 */
	@Test
	public void testAffectationNumeroIndividuSurMiseEnAttente() throws Exception {

		final long noIndividu = 544515L;
		final RegDate dateFinAdresse = date(2010, 5, 12);
		final RegDate dateEvtDepart = dateFinAdresse.addMonths(1);      // pour être sûr que l'événement de départ part en erreur
		final long evtErreurId = 78541L;
		final long evtAttenteId = 484212L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, null, "Tartempion", "Pê", Sexe.MASCULIN);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, null, dateFinAdresse);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateFinAdresse.getOneDayAfter(), null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, date(2000, 1, 1), MotifFor.INDETERMINE, MockCommune.Cossonay);
			return pp.getNumero();
		});

		// création des événements civils
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evtErreur = new EvenementCivilEch();
			evtErreur.setId(evtErreurId);
			evtErreur.setAction(ActionEvenementCivilEch.CORRECTION);
			evtErreur.setDateEvenement(dateEvtDepart);
			evtErreur.setEtat(EtatEvenementCivil.A_TRAITER);
			evtErreur.setNumeroIndividu(noIndividu);
			evtErreur.setType(TypeEvenementCivilEch.DEPART);
			hibernateTemplate.merge(evtErreur);

			final EvenementCivilEch evtEnAttente = new EvenementCivilEch();
			evtEnAttente.setId(evtAttenteId);
			evtEnAttente.setAction(ActionEvenementCivilEch.ANNULATION);
			evtEnAttente.setDateEvenement(dateEvtDepart);
			evtEnAttente.setEtat(EtatEvenementCivil.A_TRAITER);
			evtEnAttente.setNumeroIndividu(null);               // pas encore assigné
			evtEnAttente.setType(TypeEvenementCivilEch.DEPART);
			evtEnAttente.setRefMessageId(evtErreurId);          // pour faire le lien quand-même
			hibernateTemplate.merge(evtEnAttente);
			return null;
		});

		// traitement des événements civils
		traiterEvenements(noIndividu);

		// vérification des résultats
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch enErreur = evtCivilDAO.get(evtErreurId);
			assertNotNull(enErreur);
			assertEquals(EtatEvenementCivil.EN_ERREUR, enErreur.getEtat());

			final EvenementCivilEch enAttente = evtCivilDAO.get(evtAttenteId);
			assertNotNull(enAttente);
			assertEquals(EtatEvenementCivil.EN_ATTENTE, enAttente.getEtat());
			assertEquals((Long) noIndividu, enAttente.getNumeroIndividu());
			return null;
		});
	}

	/**
	 * Cas du SIFISC-9031 : une annulation qui n'a pas de numéro d'individu est passée en attente sans se voir assigner de numéro d'individu
	 */
	@Test
	public void testAffectationNumeroIndividuSurEvenementDejaMisEnAttente() throws Exception {

		final long noIndividu = 544515L;
		final RegDate dateFinAdresse = date(2010, 5, 12);
		final RegDate dateEvtDepart = dateFinAdresse.addMonths(1);      // pour être sûr que l'événement de départ part en erreur
		final long evtErreurId = 78541L;
		final long evtAttenteId = 484212L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, null, "Tartempion", "Pê", Sexe.MASCULIN);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, null, dateFinAdresse);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateFinAdresse.getOneDayAfter(), null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, date(2000, 1, 1), MotifFor.INDETERMINE, MockCommune.Cossonay);
			return pp.getNumero();
		});

		// création des événements civils
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evtErreur = new EvenementCivilEch();
			evtErreur.setId(evtErreurId);
			evtErreur.setAction(ActionEvenementCivilEch.CORRECTION);
			evtErreur.setDateEvenement(dateEvtDepart);
			evtErreur.setEtat(EtatEvenementCivil.EN_ERREUR);
			evtErreur.setNumeroIndividu(noIndividu);
			evtErreur.setType(TypeEvenementCivilEch.DEPART);
			hibernateTemplate.merge(evtErreur);

			final EvenementCivilEch evtEnAttente = new EvenementCivilEch();
			evtEnAttente.setId(evtAttenteId);
			evtEnAttente.setAction(ActionEvenementCivilEch.ANNULATION);
			evtEnAttente.setDateEvenement(dateEvtDepart);
			evtEnAttente.setEtat(EtatEvenementCivil.EN_ATTENTE);
			evtEnAttente.setNumeroIndividu(null);               // pas encore assigné
			evtEnAttente.setType(TypeEvenementCivilEch.DEPART);
			evtEnAttente.setRefMessageId(evtErreurId);          // pour faire le lien quand-même
			hibernateTemplate.merge(evtEnAttente);
			return null;
		});

		// traitement des événements civils
		traiterEvenements(noIndividu);

		// vérification des résultats
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch enErreur = evtCivilDAO.get(evtErreurId);
			assertNotNull(enErreur);
			assertEquals(EtatEvenementCivil.EN_ERREUR, enErreur.getEtat());

			final EvenementCivilEch enAttente = evtCivilDAO.get(evtAttenteId);
			assertNotNull(enAttente);
			assertEquals(EtatEvenementCivil.EN_ATTENTE, enAttente.getEtat());
			assertEquals((Long) noIndividu, enAttente.getNumeroIndividu());
			return null;
		});
	}

	private interface AfterHandleCallback {
		void checkEvent(EvenementCivilEch evt);
	}

	private void traiterEvenementTesting(final long noEventTesting, final AfterHandleCallback afterHandleCallback) throws Exception {
		final long noIndividu = 12345L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, date(1982, 1, 2), "Anoa", "Yamomoto", true);
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				addHabitant(noIndividu);
				return null;
			}
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long testingId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(noEventTesting);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(date(2000, 1, 1));
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.TESTING);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement synchrone de l'événement
		traiterEvenements(noIndividu);

		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(testingId);
			assertNotNull(evt);
			afterHandleCallback.checkEvent(evt);
			return null;
		});
	}
	
	@Test(timeout = 10000L)
	public void testEvenementCompositeEtTransactionAvecRuntimeException() throws Exception {
		doTestEvenementCompositeEtTransaction(new Handler() {
			@Override
			public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
				throw new RuntimeException("Boom!");
			}
		});
	}
	
	@Test(timeout = 10000L)
	public void testEvenementCompositeEtTransactionAvecCheckedException() throws Exception {
		doTestEvenementCompositeEtTransaction(new Handler() {
			@Override
			public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
				throw new EvenementCivilException("Boom!");
			}
		});
	}
	
	private interface Handler {
		HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException;
	}

	/**
	 * Le handler sera appelé dans l'événement interne suivant celui qui a créé un nouveau contribuable
	 * @param handler handler
	 */
	private void doTestEvenementCompositeEtTransaction(final Handler handler) throws Exception {

		final long noIndividu = 25614312L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, date(1982, 4, 12), "Lara", "Clette", false);
			}
		});

		// stratégie qui génère un événement composite qui appelle le handler sur le deuxième traitement
		// après avoir créé un nouveau contribuable sur le premier
		final EvenementCivilEchTranslationStrategy strategy = new EvenementCivilEchTranslationStrategy() {
			@Override
			public EvenementCivilInterne create(EvenementCivilEchFacade event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {

				// un événement qui crée un truc en base
				final EvenementCivilInterne naissance = new EvenementCivilInterne(event, context, options) {
					@NotNull
					@Override
					public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
						final PersonnePhysique pp = new PersonnePhysique(true);
						pp.setNumeroIndividu(getNoIndividu());
						context.getTiersDAO().save(pp);
						return HandleStatus.TRAITE;
					}

					@Override
					protected void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
					}

					@Override
					protected boolean isContribuableObligatoirementConnuAvantTraitement() {
						return false;
					}
				};

				// un événement qui explose avec une erreur métier
				final EvenementCivilInterne boom = new EvenementCivilInterne(event, context, options) {
					@NotNull
					@Override
					public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
						return handler.handle(warnings);
					}

					@Override
					protected void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
					}

					@Override
					protected boolean isContribuableObligatoirementConnuAvantTraitement() {
						return false;
					}
				};

				return new EvenementCivilInterneComposite(event, context, options, naissance, boom);
			}

			@Override
			public boolean isPrincipalementIndexation(EvenementCivilEchFacade event, EvenementCivilContext context) throws EvenementCivilException {
				return false;
			}
		};

		buildStrategyOverridingTranslatorAndProcessor(true, new StrategyOverridingCallback() {
			@Override
			public void overrideStrategies(EvenementCivilEchTranslatorImplOverride translator) {
				translator.overrideStrategy(TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.PREMIERE_LIVRAISON, strategy);
			}
		});

		// construction de l'événement en base
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(1367813456723L);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.TESTING);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setDateEvenement(RegDate.get());
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification que rien n'a été committé en base (autre que les messages d'erreur, bien-sûr)
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			assertNotNull(evt);
			assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());

			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			assertNotNull(erreurs);
			assertEquals(1, erreurs.size());

			final EvenementCivilEchErreur erreur = erreurs.iterator().next();
			assertNotNull(erreur);
			assertEquals("Boom!", erreur.getMessage());

			// la personne physique ne doit pas avoir été créée
			final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
			assertNull("La personne physique n'aurait pas dû survivre à la transaction", pp);
			return null;
		});
	}
	
	@Test(timeout = 10000L)
	public void testListenerSansEvenement() throws Exception {

		final long noIndividu = 346783L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, date(1982, 4, 12), "Lara", "Clette", false);
			}
		});

		// lancement du traitement des événements en queue pour cet individu
		traiterEvenements(noIndividu);
		
		// mais l'individu n'en a pas, justement -> si la mécanique de listener ne fonctionne pas dans ce cas là
		// alors la méthode traiterEvenements() ne reviendra jamais et c'est le timeout du test qui va sauter
	}
	
	@Test(timeout = 10000L)
	public void testListenerAvecEvenementTousTraites() throws Exception {

		final long noIndividu = 423782690773L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, date(1982, 4, 12), "Lara", "Clette", false);
			}
		});

		// création de quelques événements sur cet individu, déjà tous traités
		doInNewTransactionAndSession(status -> {
			// événement déja traité
			final EvenementCivilEch evtTraite = new EvenementCivilEch();
			evtTraite.setId(3523732L);
			evtTraite.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evtTraite.setDateEvenement(RegDate.get());
			evtTraite.setEtat(EtatEvenementCivil.TRAITE);
			evtTraite.setNumeroIndividu(noIndividu);
			evtTraite.setType(TypeEvenementCivilEch.TESTING);
			hibernateTemplate.merge(evtTraite);

			// événement forcé (= déjà traité aussi)
			final EvenementCivilEch evtForce = new EvenementCivilEch();
			evtForce.setId(3427842L);
			evtForce.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evtForce.setDateEvenement(RegDate.get());
			evtForce.setEtat(EtatEvenementCivil.FORCE);
			evtForce.setNumeroIndividu(noIndividu);
			evtForce.setType(TypeEvenementCivilEch.TESTING);
			hibernateTemplate.merge(evtForce);
			return null;
		});

		// lancement du traitement des événements en queue pour cet individu
		traiterEvenements(noIndividu);

		// mais l'individu n'en a pas à traiter, justement -> si la mécanique de listener ne fonctionne pas dans ce cas là
		// alors la méthode traiterEvenements() ne reviendra jamais et c'est le timeout du test qui va sauter
	}
	
	@Test(timeout = 10000L)
	public void testListenerHandleUsage() throws Exception {
		// handle obtenu normalement
		final EvenementCivilEchProcessor.Listener listener = new EvenementCivilEchProcessor.Listener() {
			@Override
			public void onIndividuTraite(long noIndividu) {
			}

			@Override
			public void onStop() {
			}
		};
		final EvenementCivilEchProcessor.ListenerHandle handle = processor.registerListener(listener);
		handle.unregister();

		try {
			// deuxième appel -> boom !
			handle.unregister();
			Assert.fail();
		}
		catch (IllegalStateException e) {
			// ok, tout va bien
		}

	}

	@Test(timeout = 10000L)
	public void testListenerRegistration() throws Exception {

		final long noIndividu = 467278456783L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, date(1982, 4, 12), "Lara", "Clette", false);
			}
		});

		final MutableBoolean recu = new MutableBoolean(false);
		final EvenementCivilEchProcessor.Listener listener = new EvenementCivilEchProcessor.Listener() {
			@Override
			public void onIndividuTraite(long noIndividu) {
				recu.setValue(true);
			}

			@Override
			public void onStop() {
			}
		};

		// traitement des événements (= aucun) de l'individu
		traiterEvenements(noIndividu);
		
		// notre listener n'a rien reçu
		assertFalse(recu.booleanValue());

		// après insertion du listener, il devrait recevoir les nouveaux événements
		final EvenementCivilEchProcessor.ListenerHandle handle = processor.registerListener(listener);
		assertFalse(recu.booleanValue());
		traiterEvenements(noIndividu);
		assertTrue(recu.booleanValue());

		// après désactivation du listener, il ne devrait plus rien recevoir
		recu.setValue(false);
		handle.unregister();
		assertFalse(recu.booleanValue());
		traiterEvenements(noIndividu);
		assertFalse(recu.booleanValue());
	}

	/**
	 * [SIFISC-6908] lors d'un forçage d'événement civil, le flag 'habitant' doit être mis-à-jour
	 */
	@Test
	public void testUpdateFlagHabitantSurForcage() throws Exception {

		final long noIndividu = 14563435356783512L;
		final long evtId = 12456234125L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, date(1940, 10, 31), "Hitchcock", "Alfred", true);
			}
		});

		// mise en place fiscale, remplissage du cache du service civil sur l'individu
		final long ppid = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			Assert.assertEquals("Alfred Hitchcock", tiersService.getNomPrenom(pp));
			return pp.getNumero();
		});

		// création d'un événement en erreur
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(evtId);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setType(TypeEvenementCivilEch.CHGT_NOM);
			evt.setDateEvenement(date(2009, 1, 1));
			evt.setEtat(EtatEvenementCivil.EN_ERREUR);
			evt.setNumeroIndividu(noIndividu);
			evtCivilDAO.save(evt);
			return null;
		});

		// vérification que le flag habitant est toujours setté
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppid);
			Assert.assertTrue(pp.isHabitantVD());
			return null;
		});

		// forçage de l'événement en erreur
		doInNewTransactionAndSession(status -> {
			evtCivilService.forceEvenement(evtId);
			return null;
		});

		// vérification que le flag habitant a bien été resetté
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppid);
			Assert.assertFalse(pp.isHabitantVD()); // [SIFISC-6908] le forçage de l'événement doit recalculer le flag 'habitant';
			return null;
		});
	}

	@Test
	public void testRefreshSystematiqueParentes() throws Exception {

		final long noIndParent = 467832457L;
		final long noIndEnfant = 4367453762L;
		final RegDate dateNaissanceEnfant = date(2010, 4, 2);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu parent = addIndividu(noIndParent, null, "Dumas", "Alexandre, père", Sexe.MASCULIN);
				final MockIndividu enfant = addIndividu(noIndEnfant, null, "Dumas", "Alexandre, fils", Sexe.MASCULIN);
				addLiensFiliation(enfant, parent, null, dateNaissanceEnfant, null);
			}
		});

		final class Ids {
			long idParent;
			long idEnfant;
		}

		// mise en place fiscale des deux contribuable (sans relation de parenté pour le moment)
		final Ids ids = doInNewTransactionAndSessionUnderSwitch(parentesSynchronizer, false, status -> {
			final PersonnePhysique parent = addHabitant(noIndParent);
			final PersonnePhysique enfant = addHabitant(noIndEnfant);
			final Ids ids1 = new Ids();
			ids1.idEnfant = enfant.getNumero();
			ids1.idParent = parent.getNumero();
			return ids1;
		});

		// construction d'un événement civil bidon sur l'enfant qui ne fait rien
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(1367813456723L);
			evt.setNumeroIndividu(noIndEnfant);
			evt.setType(TypeEvenementCivilEch.TESTING);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setDateEvenement(RegDate.get());
			return hibernateTemplate.merge(evt).getId();
		});

		// on vérifie qu'il n'y a pas de relation de parenté
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique enfant = (PersonnePhysique) tiersDAO.get(ids.idEnfant);
			assertNotNull(enfant);
			assertEmpty(enfant.getRapportsObjet());
			assertEmpty(enfant.getRapportsSujet());
			return null;
		});

		// traitement de l'événement
		traiterEvenements(noIndEnfant);

		// on vérifie que la relation de parenté est maintenant présente
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique enfant = (PersonnePhysique) tiersDAO.get(ids.idEnfant);
			assertNotNull(enfant);
			assertEmpty(enfant.getRapportsObjet());

			final Set<RapportEntreTiers> relSujets = enfant.getRapportsSujet();
			assertNotNull(relSujets);
			assertEquals(1, relSujets.size());

			final RapportEntreTiers parente = relSujets.iterator().next();
			assertNotNull(parente);
			assertEquals(TypeRapportEntreTiers.PARENTE, parente.getType());
			assertEquals((Long) ids.idParent, parente.getObjetId());
			assertEquals((Long) ids.idEnfant, parente.getSujetId());
			assertFalse(parente.isAnnule());
			assertEquals(dateNaissanceEnfant, parente.getDateDebut());
			assertNull(parente.getDateFin());
			return null;
		});
	}
}
