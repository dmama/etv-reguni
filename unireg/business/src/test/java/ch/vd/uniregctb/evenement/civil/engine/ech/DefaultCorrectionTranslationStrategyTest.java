package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.Arrays;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;
import ch.vd.uniregctb.type.TypePermis;

public class DefaultCorrectionTranslationStrategyTest extends AbstractEvenementCivilEchProcessorTest {

	private DefaultCorrectionTranslationStrategy strategy;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();

		final ServiceCivilService serviceCivil = getBean(ServiceCivilService.class, "serviceCivilService");
		strategy = new DefaultCorrectionTranslationStrategy(serviceCivil);
	}

	@Override
	protected boolean buildProcessorOnSetup() {
		return false;
	}

	@Test
	public void testSeulementCorrectionsAcceptees() throws Exception {
		for (TypeEvenementCivilEch type : TypeEvenementCivilEch.values()) {
			for (ActionEvenementCivilEch action : Arrays.asList(ActionEvenementCivilEch.PREMIERE_LIVRAISON, ActionEvenementCivilEch.ANNULATION)) {
				try {
					final EvenementCivilEch evt = new EvenementCivilEch();
					evt.setType(type);
					evt.setAction(action);
					final EvenementCivilInterne interne = strategy.create(evt, null, null);
					Assert.fail("La stratégie aurait dû refuser la création pour une " + action);
				}
				catch (IllegalArgumentException e) {
					Assert.assertEquals("Stratégie applicable aux seuls événements civils de correction.", e.getMessage());
				}
			}
		}
	}

	@Test(timeout = 10000L)
	public void testSansModificationFiscale() throws Exception {

		buildStrategyOverridingTranslatorAndProcessor(true, new StrategyOverridingCallback() {
			@Override
			public void overrideStrategies(EvenementCivilEchTranslatorImplOverride translator) {
				translator.overrideStrategy(TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, strategy);
			}
		});

		final long noIndividu = 4684263L;
		final long idEvtCorrige = 464735292L;
		final long idEvtCorrection = 4326478256242L;
		final RegDate dateEvtOrig = RegDate.get();
		final RegDate dateEvtCorrection = dateEvtOrig.addDays(-2);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Zweisteinen", "Robert", true);
				addIndividuFromEvent(idEvtCorrige, ind, dateEvtOrig, TypeEvenementCivilEch.TESTING);

				final MockIndividu ind2 = createIndividu(noIndividu, null, "Dreisteinen", "Albertine", false);
				addIndividuFromEvent(idEvtCorrection, ind2, dateEvtCorrection, TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, idEvtCorrige);
			}
		});

		// construction de l'événement de correction
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(idEvtCorrection);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.TESTING);
				evt.setAction(ActionEvenementCivilEch.CORRECTION);
				evt.setDateEvenement(dateEvtCorrection);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setRefMessageId(idEvtCorrige);
				hibernateTemplate.merge(evt);
				return null;
			}
		});

		// traitement de l'événement de correction
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(idEvtCorrection);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
				Assert.assertEquals("Evénement ignoré car sans impact fiscal.", evt.getCommentaireTraitement());
				return null;
			}
		});
	}

	@Test(timeout = 10000L)
	public void testModificationDateNaissance() throws Exception {

		buildStrategyOverridingTranslatorAndProcessor(true, new StrategyOverridingCallback() {
			@Override
			public void overrideStrategies(EvenementCivilEchTranslatorImplOverride translator) {
				translator.overrideStrategy(TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, strategy);
			}
		});

		final long noIndividu = 4684263L;
		final long idEvtCorrige = 464735292L;
		final long idEvtCorrection = 4326478256242L;
		final RegDate dateEvtOrig = RegDate.get();
		final RegDate dateEvtCorrection = dateEvtOrig.addDays(-2);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Zweisteinen", "Robert", true);
				marieIndividu(ind, date(1955, 12, 1));
				addIndividuFromEvent(idEvtCorrige, ind, dateEvtOrig, TypeEvenementCivilEch.TESTING);

				final MockIndividu ind2 = createIndividu(noIndividu, date(1934, 2, 12), "Dreisteinen", "Albertine", false);
				marieIndividu(ind2, date(1955, 12, 1));
				addIndividuFromEvent(idEvtCorrection, ind2, dateEvtCorrection, TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, idEvtCorrige);
			}
		});

		// construction de l'événement de correction
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(idEvtCorrection);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.TESTING);
				evt.setAction(ActionEvenementCivilEch.CORRECTION);
				evt.setDateEvenement(dateEvtCorrection);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setRefMessageId(idEvtCorrige);
				hibernateTemplate.merge(evt);
				return null;
			}
		});

		// traitement de l'événement de correction
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(idEvtCorrection);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
				Assert.assertEquals("L'élément suivant a été modifié par la correction : date de naissance.", evt.getCommentaireTraitement());

				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				Assert.assertNotNull(erreurs);
				Assert.assertEquals(1, erreurs.size());

				final EvenementCivilEchErreur erreur = erreurs.iterator().next();
				Assert.assertEquals("Traitement automatique non implémenté. Veuillez effectuer cette opération manuellement.", erreur.getMessage());
				return null;
			}
		});
	}

	@Test(timeout = 10000L)
	public void testModificationDateDeces() throws Exception {

		buildStrategyOverridingTranslatorAndProcessor(true, new StrategyOverridingCallback() {
			@Override
			public void overrideStrategies(EvenementCivilEchTranslatorImplOverride translator) {
				translator.overrideStrategy(TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, strategy);
			}
		});

		final long noIndividu = 4684263L;
		final long idEvtCorrige = 464735292L;
		final long idEvtCorrection = 4326478256242L;
		final RegDate dateEvtOrig = RegDate.get();
		final RegDate dateEvtCorrection = dateEvtOrig.addDays(-2);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Zweisteinen", "Robert", true);
				marieIndividu(ind, date(1955, 12, 1));
				addIndividuFromEvent(idEvtCorrige, ind, dateEvtOrig, TypeEvenementCivilEch.TESTING);

				final MockIndividu ind2 = createIndividu(noIndividu, null, "Dreisteinen", "Albertine", false);
				marieIndividu(ind2, date(1955, 12, 1));
				ind2.setDateDeces(RegDate.get().addMonths(-1));
				addIndividuFromEvent(idEvtCorrection, ind2, dateEvtCorrection, TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, idEvtCorrige);
			}
		});

		// construction de l'événement de correction
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(idEvtCorrection);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.TESTING);
				evt.setAction(ActionEvenementCivilEch.CORRECTION);
				evt.setDateEvenement(dateEvtCorrection);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setRefMessageId(idEvtCorrige);
				hibernateTemplate.merge(evt);
				return null;
			}
		});

		// traitement de l'événement de correction
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(idEvtCorrection);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
				Assert.assertEquals("L'élément suivant a été modifié par la correction : date de décès.", evt.getCommentaireTraitement());

				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				Assert.assertNotNull(erreurs);
				Assert.assertEquals(1, erreurs.size());

				final EvenementCivilEchErreur erreur = erreurs.iterator().next();
				Assert.assertEquals("Traitement automatique non implémenté. Veuillez effectuer cette opération manuellement.", erreur.getMessage());
				return null;
			}
		});
	}

	@Test(timeout = 10000L)
	public void testModificationNationalite() throws Exception {

		buildStrategyOverridingTranslatorAndProcessor(true, new StrategyOverridingCallback() {
			@Override
			public void overrideStrategies(EvenementCivilEchTranslatorImplOverride translator) {
				translator.overrideStrategy(TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, strategy);
			}
		});

		final long noIndividu = 4684263L;
		final long idEvtCorrige = 464735292L;
		final long idEvtCorrection = 4326478256242L;
		final RegDate dateEvtOrig = RegDate.get();
		final RegDate dateEvtCorrection = dateEvtOrig.addDays(-2);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Zweisteinen", "Robert", true);
				marieIndividu(ind, date(1955, 12, 1));
				addIndividuFromEvent(idEvtCorrige, ind, dateEvtOrig, TypeEvenementCivilEch.TESTING);

				final MockIndividu ind2 = createIndividu(noIndividu, null, "Dreisteinen", "Albertine", false);
				marieIndividu(ind2, date(1955, 12, 1));
				addNationalite(ind2, MockPays.Suisse, date(2000, 1, 1), null);
				addIndividuFromEvent(idEvtCorrection, ind2, dateEvtCorrection, TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, idEvtCorrige);
			}
		});

		// construction de l'événement de correction
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(idEvtCorrection);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.TESTING);
				evt.setAction(ActionEvenementCivilEch.CORRECTION);
				evt.setDateEvenement(dateEvtCorrection);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setRefMessageId(idEvtCorrige);
				hibernateTemplate.merge(evt);
				return null;
			}
		});

		// traitement de l'événement de correction
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(idEvtCorrection);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
				Assert.assertEquals("L'élément suivant a été modifié par la correction : nationalité.", evt.getCommentaireTraitement());

				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				Assert.assertNotNull(erreurs);
				Assert.assertEquals(1, erreurs.size());

				final EvenementCivilEchErreur erreur = erreurs.iterator().next();
				Assert.assertEquals("Traitement automatique non implémenté. Veuillez effectuer cette opération manuellement.", erreur.getMessage());
				return null;
			}
		});
	}

	@Test(timeout = 10000L)
	public void testModificationPermis() throws Exception {

		buildStrategyOverridingTranslatorAndProcessor(true, new StrategyOverridingCallback() {
			@Override
			public void overrideStrategies(EvenementCivilEchTranslatorImplOverride translator) {
				translator.overrideStrategy(TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, strategy);
			}
		});

		final long noIndividu = 4684263L;
		final long idEvtCorrige = 464735292L;
		final long idEvtCorrection = 4326478256242L;
		final RegDate dateEvtOrig = RegDate.get();
		final RegDate dateEvtCorrection = dateEvtOrig.addDays(-2);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Zweisteinen", "Robert", true);
				marieIndividu(ind, date(1955, 12, 1));
				addIndividuFromEvent(idEvtCorrige, ind, dateEvtOrig, TypeEvenementCivilEch.TESTING);

				final MockIndividu ind2 = createIndividu(noIndividu, null, "Dreisteinen", "Albertine", false);
				marieIndividu(ind2, date(1955, 12, 1));
				addPermis(ind2, TypePermis.ETABLISSEMENT, date(2008, 2, 20), null, false);
				addIndividuFromEvent(idEvtCorrection, ind2, dateEvtCorrection, TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, idEvtCorrige);
			}
		});

		// construction de l'événement de correction
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(idEvtCorrection);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.TESTING);
				evt.setAction(ActionEvenementCivilEch.CORRECTION);
				evt.setDateEvenement(dateEvtCorrection);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setRefMessageId(idEvtCorrige);
				hibernateTemplate.merge(evt);
				return null;
			}
		});

		// traitement de l'événement de correction
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(idEvtCorrection);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
				Assert.assertEquals("L'élément suivant a été modifié par la correction : permis.", evt.getCommentaireTraitement());

				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				Assert.assertNotNull(erreurs);
				Assert.assertEquals(1, erreurs.size());

				final EvenementCivilEchErreur erreur = erreurs.iterator().next();
				Assert.assertEquals("Traitement automatique non implémenté. Veuillez effectuer cette opération manuellement.", erreur.getMessage());
				return null;
			}
		});
	}

	@Test(timeout = 10000L)
	public void testModificationAdresseDeResidence() throws Exception {

		buildStrategyOverridingTranslatorAndProcessor(true, new StrategyOverridingCallback() {
			@Override
			public void overrideStrategies(EvenementCivilEchTranslatorImplOverride translator) {
				translator.overrideStrategy(TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, strategy);
			}
		});

		final long noIndividu = 4684263L;
		final long idEvtCorrige = 464735292L;
		final long idEvtCorrection = 4326478256242L;
		final RegDate dateEvtOrig = RegDate.get();
		final RegDate dateEvtCorrection = dateEvtOrig.addDays(-2);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Zweisteinen", "Robert", true);
				marieIndividu(ind, date(1955, 12, 1));
				addIndividuFromEvent(idEvtCorrige, ind, dateEvtOrig, TypeEvenementCivilEch.TESTING);

				final MockIndividu ind2 = createIndividu(noIndividu, null, "Dreisteinen", "Albertine", false);
				marieIndividu(ind2, date(1955, 12, 1));
				addAdresse(ind2, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, null, null);
				addIndividuFromEvent(idEvtCorrection, ind2, dateEvtCorrection, TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, idEvtCorrige);
			}
		});

		// construction de l'événement de correction
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(idEvtCorrection);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.TESTING);
				evt.setAction(ActionEvenementCivilEch.CORRECTION);
				evt.setDateEvenement(dateEvtCorrection);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setRefMessageId(idEvtCorrige);
				hibernateTemplate.merge(evt);
				return null;
			}
		});

		// traitement de l'événement de correction
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(idEvtCorrection);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
				Assert.assertEquals("L'élément suivant a été modifié par la correction : adresse de résidence.", evt.getCommentaireTraitement());

				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				Assert.assertNotNull(erreurs);
				Assert.assertEquals(1, erreurs.size());

				final EvenementCivilEchErreur erreur = erreurs.iterator().next();
				Assert.assertEquals("Traitement automatique non implémenté. Veuillez effectuer cette opération manuellement.", erreur.getMessage());
				return null;
			}
		});
	}

	@Test(timeout = 10000L)
	public void testModificationRelations() throws Exception {

		buildStrategyOverridingTranslatorAndProcessor(true, new StrategyOverridingCallback() {
			@Override
			public void overrideStrategies(EvenementCivilEchTranslatorImplOverride translator) {
				translator.overrideStrategy(TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, strategy);
			}
		});

		final long noIndividu = 4684263L;
		final long noIndividuConjoint = 2567315623L;
		final long idEvtCorrige = 464735292L;
		final long idEvtCorrection = 4326478256242L;
		final RegDate dateEvtOrig = RegDate.get();
		final RegDate dateEvtCorrection = dateEvtOrig.addDays(-2);
		final RegDate dateMariage = date(1955, 12, 1);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Zweisteinen", "Robert", true);
				marieIndividu(ind, dateMariage);
				addIndividuFromEvent(idEvtCorrige, ind, dateEvtOrig, TypeEvenementCivilEch.TESTING);

				final MockIndividu conjoint = addIndividu(noIndividuConjoint, null, "Viersteinen", "Helmut", true);
				final MockIndividu ind2 = createIndividu(noIndividu, null, "Dreisteinen", "Albertine", false);
				marieIndividus(ind2, conjoint, dateMariage);
				addIndividuFromEvent(idEvtCorrection, ind2, dateEvtCorrection, TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, idEvtCorrige);
			}
		});

		// construction de l'événement de correction
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(idEvtCorrection);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.TESTING);
				evt.setAction(ActionEvenementCivilEch.CORRECTION);
				evt.setDateEvenement(dateEvtCorrection);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setRefMessageId(idEvtCorrige);
				hibernateTemplate.merge(evt);
				return null;
			}
		});

		// traitement de l'événement de correction
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(idEvtCorrection);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
				Assert.assertEquals("L'élément suivant a été modifié par la correction : relations.", evt.getCommentaireTraitement());

				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				Assert.assertNotNull(erreurs);
				Assert.assertEquals(1, erreurs.size());

				final EvenementCivilEchErreur erreur = erreurs.iterator().next();
				Assert.assertEquals("Traitement automatique non implémenté. Veuillez effectuer cette opération manuellement.", erreur.getMessage());
				return null;
			}
		});
	}

	@Test(timeout = 10000L)
	public void testModificationPlusieursAttributs() throws Exception {

		buildStrategyOverridingTranslatorAndProcessor(true, new StrategyOverridingCallback() {
			@Override
			public void overrideStrategies(EvenementCivilEchTranslatorImplOverride translator) {
				translator.overrideStrategy(TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, strategy);
			}
		});

		final long noIndividu = 4684263L;
		final long noIndividuConjoint = 2567315623L;
		final long idEvtCorrige = 464735292L;
		final long idEvtCorrection = 4326478256242L;
		final RegDate dateEvtOrig = RegDate.get();
		final RegDate dateEvtCorrection = dateEvtOrig.addDays(-2);
		final RegDate dateMariage = date(1955, 12, 1);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Zweisteinen", "Robert", true);
				marieIndividu(ind, dateMariage.addDays(1));     // <-- état civil différent par la date
				addIndividuFromEvent(idEvtCorrige, ind, dateEvtOrig, TypeEvenementCivilEch.TESTING);

				final MockIndividu conjoint = addIndividu(noIndividuConjoint, null, "Viersteinen", "Helmut", true);
				final MockIndividu ind2 = createIndividu(noIndividu, date(1930, 5, 12), "Dreisteinen", "Albertine", false); // <-- date de naissance différente
				marieIndividus(ind2, conjoint, dateMariage);    // <-- conjoint différent
				addIndividuFromEvent(idEvtCorrection, ind2, dateEvtCorrection, TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, idEvtCorrige);
			}
		});

		// construction de l'événement de correction
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(idEvtCorrection);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.TESTING);
				evt.setAction(ActionEvenementCivilEch.CORRECTION);
				evt.setDateEvenement(dateEvtCorrection);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setRefMessageId(idEvtCorrige);
				hibernateTemplate.merge(evt);
				return null;
			}
		});

		// traitement de l'événement de correction
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(idEvtCorrection);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
				Assert.assertEquals("Les éléments suivants ont été modifiés par la correction : date de naissance, relations, état civil.", evt.getCommentaireTraitement());

				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				Assert.assertNotNull(erreurs);
				Assert.assertEquals(1, erreurs.size());

				final EvenementCivilEchErreur erreur = erreurs.iterator().next();
				Assert.assertEquals("Traitement automatique non implémenté. Veuillez effectuer cette opération manuellement.", erreur.getMessage());
				return null;
			}
		});
	}

	@Test(timeout = 10000L)
	public void testContournementBugSiref2590() throws Exception {
		buildStrategyOverridingTranslatorAndProcessor(true, new StrategyOverridingCallback() {
			@Override
			public void overrideStrategies(EvenementCivilEchTranslatorImplOverride translator) {
				translator.overrideStrategy(TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, strategy);
			}
		});

		final long noIndividu = 4684263L;
		final long idEvtCorrige = 464735292L;
		final long idEvtCorrection = 4326478256242L;
		final RegDate dateEvtOrig = RegDate.get();
		final RegDate dateEvtCorrection = dateEvtOrig.addDays(-2);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Zweisteinen", "Robert", true);
				addIndividuFromEvent(idEvtCorrige, ind, dateEvtOrig, TypeEvenementCivilEch.TESTING);

				final MockIndividu ind2 = createIndividu(noIndividu, null, "Dreisteinen", "Albertine", false);
				addIndividuFromEvent(idEvtCorrection, ind2, dateEvtCorrection, TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, idEvtCorrige);
			}
		});

		// construction de l'événement de correction
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(idEvtCorrection);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.TESTING);
				evt.setAction(ActionEvenementCivilEch.CORRECTION);
				evt.setDateEvenement(dateEvtCorrection);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setRefMessageId(null);      // <-- c'est bien le problème...
				hibernateTemplate.merge(evt);
				return null;
			}
		});

		// traitement de l'événement de correction
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(idEvtCorrection);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
				Assert.assertEquals("Evénement ignoré car sans impact fiscal.", evt.getCommentaireTraitement());
				return null;
			}
		});
	}
}
