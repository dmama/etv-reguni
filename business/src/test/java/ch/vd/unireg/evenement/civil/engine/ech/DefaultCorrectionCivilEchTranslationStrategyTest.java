package ch.vd.unireg.evenement.civil.engine.ech;

import java.util.Arrays;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEch;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchErreur;
import ch.vd.unireg.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeEvenementCivilEch;
import ch.vd.unireg.type.TypePermis;

public class DefaultCorrectionCivilEchTranslationStrategyTest extends AbstractEvenementCivilEchProcessorTest {

	private DefaultCorrectionCivilEchTranslationStrategy strategy;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		strategy = new DefaultCorrectionCivilEchTranslationStrategy(serviceCivil, serviceInfra, tiersService);
	}

	@Override
	protected boolean buildProcessorOnSetup() {
		return false;
	}

	@Test(timeout = 10000L)
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
		final RegDate dateEvt = RegDate.get();

		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Zweisteinen", "Robert", true);
				addIndividuAfterEvent(idEvtCorrige, ind, dateEvt, TypeEvenementCivilEch.TESTING);

				final MockIndividu ind2 = createIndividu(noIndividu, null, "Dreisteinen", "Albert", true);
				addIndividuAfterEvent(idEvtCorrection, ind2, dateEvt, TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, idEvtCorrige);
			}
		});

		// construction de l'événement de correction
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(idEvtCorrection);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.TESTING);
			evt.setAction(ActionEvenementCivilEch.CORRECTION);
			evt.setDateEvenement(dateEvt);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setRefMessageId(idEvtCorrige);
			hibernateTemplate.merge(evt);
			return null;
		});

		// traitement de l'événement de correction
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(idEvtCorrection);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
			Assert.assertEquals("Événement traité sans modification Unireg.", evt.getCommentaireTraitement());
			return null;
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
		final RegDate dateEvt = RegDate.get();

		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Zweisteinen", "Robert", true);
				marieIndividu(ind, date(1955, 12, 1));
				addIndividuAfterEvent(idEvtCorrige, ind, dateEvt, TypeEvenementCivilEch.TESTING);

				final MockIndividu ind2 = createIndividu(noIndividu, date(1934, 2, 12), "Dreisteinen", "Albert", true);
				marieIndividu(ind2, date(1955, 12, 1));
				addIndividuAfterEvent(idEvtCorrection, ind2, dateEvt, TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, idEvtCorrige);
			}
		});

		// construction de l'événement de correction
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(idEvtCorrection);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.TESTING);
			evt.setAction(ActionEvenementCivilEch.CORRECTION);
			evt.setDateEvenement(dateEvt);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setRefMessageId(idEvtCorrige);
			hibernateTemplate.merge(evt);
			return null;
		});

		// traitement de l'événement de correction
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(idEvtCorrection);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
			Assert.assertEquals("L'élément suivant a été modifié par la correction : date de naissance (apparition).", evt.getCommentaireTraitement());

			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			Assert.assertNotNull(erreurs);
			Assert.assertEquals(1, erreurs.size());

			final EvenementCivilEchErreur erreur = erreurs.iterator().next();
			Assert.assertEquals("Traitement automatique non implémenté. Veuillez effectuer cette opération manuellement.", erreur.getMessage());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testModificationSexe() throws Exception {

		buildStrategyOverridingTranslatorAndProcessor(true, new StrategyOverridingCallback() {
			@Override
			public void overrideStrategies(EvenementCivilEchTranslatorImplOverride translator) {
				translator.overrideStrategy(TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, strategy);
			}
		});

		final long noIndividu = 4684263L;
		final long idEvtCorrige = 464735292L;
		final long idEvtCorrection = 4326478256242L;
		final RegDate dateEvt = RegDate.get();

		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Zweisteinen", "Michel", true);
				marieIndividu(ind, date(1955, 12, 1));
				addIndividuAfterEvent(idEvtCorrige, ind, dateEvt, TypeEvenementCivilEch.TESTING);

				final MockIndividu ind2 = createIndividu(noIndividu, null, "Zweisteinen", "Michèle", false);
				marieIndividu(ind2, date(1955, 12, 1));
				addIndividuAfterEvent(idEvtCorrection, ind2, dateEvt, TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, idEvtCorrige);
			}
		});

		// construction de l'événement de correction
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(idEvtCorrection);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.TESTING);
			evt.setAction(ActionEvenementCivilEch.CORRECTION);
			evt.setDateEvenement(dateEvt);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setRefMessageId(idEvtCorrige);
			hibernateTemplate.merge(evt);
			return null;
		});

		// traitement de l'événement de correction
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(idEvtCorrection);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
			Assert.assertEquals("Événement traité sans modification Unireg.", evt.getCommentaireTraitement());
			return null;
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
		final RegDate dateEvt = RegDate.get();

		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Zweisteinen", "Robert", true);
				marieIndividu(ind, date(1955, 12, 1));
				addIndividuAfterEvent(idEvtCorrige, ind, dateEvt, TypeEvenementCivilEch.TESTING);

				final MockIndividu ind2 = createIndividu(noIndividu, null, "Dreisteinen", "Albert", true);
				marieIndividu(ind2, date(1955, 12, 1));
				ind2.setDateDeces(RegDate.get().addMonths(-1));
				addIndividuAfterEvent(idEvtCorrection, ind2, dateEvt, TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, idEvtCorrige);
			}
		});

		// construction de l'événement de correction
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(idEvtCorrection);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.TESTING);
			evt.setAction(ActionEvenementCivilEch.CORRECTION);
			evt.setDateEvenement(dateEvt);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setRefMessageId(idEvtCorrige);
			hibernateTemplate.merge(evt);
			return null;
		});

		// traitement de l'événement de correction
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(idEvtCorrection);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
			Assert.assertEquals("L'élément suivant a été modifié par la correction : date de décès (apparition).", evt.getCommentaireTraitement());

			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			Assert.assertNotNull(erreurs);
			Assert.assertEquals(1, erreurs.size());

			final EvenementCivilEchErreur erreur = erreurs.iterator().next();
			Assert.assertEquals("Traitement automatique non implémenté. Veuillez effectuer cette opération manuellement.", erreur.getMessage());
			return null;
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
		final RegDate dateEvt = RegDate.get();

		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Zweisteinen", "Robert", true);
				marieIndividu(ind, date(1955, 12, 1));
				addIndividuAfterEvent(idEvtCorrige, ind, dateEvt, TypeEvenementCivilEch.TESTING);

				final MockIndividu ind2 = createIndividu(noIndividu, null, "Dreisteinen", "Albert", true);
				marieIndividu(ind2, date(1955, 12, 1));
				addNationalite(ind2, MockPays.Suisse, date(2000, 1, 1), null);
				addIndividuAfterEvent(idEvtCorrection, ind2, dateEvt, TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, idEvtCorrige);
			}
		});

		// construction de l'événement de correction
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(idEvtCorrection);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.TESTING);
			evt.setAction(ActionEvenementCivilEch.CORRECTION);
			evt.setDateEvenement(dateEvt);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setRefMessageId(idEvtCorrige);
			hibernateTemplate.merge(evt);
			return null;
		});

		// traitement de l'événement de correction
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(idEvtCorrection);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
			Assert.assertEquals("L'élément suivant a été modifié par la correction : nationalité (apparition).", evt.getCommentaireTraitement());

			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			Assert.assertNotNull(erreurs);
			Assert.assertEquals(1, erreurs.size());

			final EvenementCivilEchErreur erreur = erreurs.iterator().next();
			Assert.assertEquals("Traitement automatique non implémenté. Veuillez effectuer cette opération manuellement.", erreur.getMessage());
			return null;
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
		final RegDate dateEvt = RegDate.get();

		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Zweisteinen", "Robert", true);
				marieIndividu(ind, date(1955, 12, 1));
				addIndividuAfterEvent(idEvtCorrige, ind, dateEvt, TypeEvenementCivilEch.TESTING);

				final MockIndividu ind2 = createIndividu(noIndividu, null, "Dreisteinen", "Albert", true);
				marieIndividu(ind2, date(1955, 12, 1));
				addPermis(ind2, TypePermis.ETABLISSEMENT, date(2008, 2, 20), null, false);
				addIndividuAfterEvent(idEvtCorrection, ind2, dateEvt, TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, idEvtCorrige);
			}
		});

		// construction de l'événement de correction
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(idEvtCorrection);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.TESTING);
			evt.setAction(ActionEvenementCivilEch.CORRECTION);
			evt.setDateEvenement(dateEvt);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setRefMessageId(idEvtCorrige);
			hibernateTemplate.merge(evt);
			return null;
		});

		// traitement de l'événement de correction
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(idEvtCorrection);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
			Assert.assertEquals("L'élément suivant a été modifié par la correction : permis (apparition).", evt.getCommentaireTraitement());

			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			Assert.assertNotNull(erreurs);
			Assert.assertEquals(1, erreurs.size());

			final EvenementCivilEchErreur erreur = erreurs.iterator().next();
			Assert.assertEquals("Traitement automatique non implémenté. Veuillez effectuer cette opération manuellement.", erreur.getMessage());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testModificationAdresseDeResidencePrincipale() throws Exception {

		buildStrategyOverridingTranslatorAndProcessor(true, new StrategyOverridingCallback() {
			@Override
			public void overrideStrategies(EvenementCivilEchTranslatorImplOverride translator) {
				translator.overrideStrategy(TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, strategy);
			}
		});

		final long noIndividu = 4684263L;
		final long idEvtCorrige = 464735292L;
		final long idEvtCorrection = 4326478256242L;
		final RegDate dateEvt = RegDate.get();

		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Zweisteinen", "Robert", true);
				marieIndividu(ind, date(1955, 12, 1));
				addIndividuAfterEvent(idEvtCorrige, ind, dateEvt, TypeEvenementCivilEch.TESTING);

				final MockIndividu ind2 = createIndividu(noIndividu, null, "Dreisteinen", "Albert", true);
				marieIndividu(ind2, date(1955, 12, 1));
				addAdresse(ind2, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, null, null);
				addIndividuAfterEvent(idEvtCorrection, ind2, dateEvt, TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, idEvtCorrige);
			}
		});

		// construction de l'événement de correction
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(idEvtCorrection);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.TESTING);
			evt.setAction(ActionEvenementCivilEch.CORRECTION);
			evt.setDateEvenement(dateEvt);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setRefMessageId(idEvtCorrige);
			hibernateTemplate.merge(evt);
			return null;
		});

		// traitement de l'événement de correction
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(idEvtCorrection);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
			Assert.assertEquals("L'élément suivant a été modifié par la correction : adresse de résidence principale (apparition).", evt.getCommentaireTraitement());

			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			Assert.assertNotNull(erreurs);
			Assert.assertEquals(1, erreurs.size());

			final EvenementCivilEchErreur erreur = erreurs.iterator().next();
			Assert.assertEquals("Traitement automatique non implémenté. Veuillez effectuer cette opération manuellement.", erreur.getMessage());
			return null;
		});
	}


	@Test(timeout = 10000L)
	public void testModificationAdresseDeResidenceSecondaire() throws Exception {

		buildStrategyOverridingTranslatorAndProcessor(true, new StrategyOverridingCallback() {
			@Override
			public void overrideStrategies(EvenementCivilEchTranslatorImplOverride translator) {
				translator.overrideStrategy(TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, strategy);
			}
		});

		final long noIndividu = 4684263L;
		final long idEvtCorrige = 464735292L;
		final long idEvtCorrection = 4326478256242L;
		final RegDate dateEvt = RegDate.get();

		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Zweisteinen", "Robert", true);
				marieIndividu(ind, date(1955, 12, 1));
				addIndividuAfterEvent(idEvtCorrige, ind, dateEvt, TypeEvenementCivilEch.TESTING);

				final MockIndividu ind2 = createIndividu(noIndividu, null, "Dreisteinen", "Albert", true);
				marieIndividu(ind2, date(1955, 12, 1));
				addAdresse(ind2, TypeAdresseCivil.SECONDAIRE, MockRue.CossonayVille.AvenueDuFuniculaire, null, null, null);
				addIndividuAfterEvent(idEvtCorrection, ind2, dateEvt, TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, idEvtCorrige);
			}
		});

		// construction de l'événement de correction
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(idEvtCorrection);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.TESTING);
			evt.setAction(ActionEvenementCivilEch.CORRECTION);
			evt.setDateEvenement(dateEvt);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setRefMessageId(idEvtCorrige);
			hibernateTemplate.merge(evt);
			return null;
		});

		// traitement de l'événement de correction
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(idEvtCorrection);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
			Assert.assertEquals("L'élément suivant a été modifié par la correction : adresse de résidence secondaire (apparition).", evt.getCommentaireTraitement());

			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			Assert.assertNotNull(erreurs);
			Assert.assertEquals(1, erreurs.size());

			final EvenementCivilEchErreur erreur = erreurs.iterator().next();
			Assert.assertEquals("Traitement automatique non implémenté. Veuillez effectuer cette opération manuellement.", erreur.getMessage());
			return null;
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
		final RegDate dateEvt = RegDate.get();
		final RegDate dateMariage = date(1955, 12, 1);

		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Zweisteinen", "Robert", true);
				marieIndividu(ind, dateMariage);
				addIndividuAfterEvent(idEvtCorrige, ind, dateEvt, TypeEvenementCivilEch.TESTING);

				final MockIndividu conjoint = addIndividu(noIndividuConjoint, null, "Viersteinen", "Beate", false);
				final MockIndividu ind2 = createIndividu(noIndividu, null, "Dreisteinen", "Albert", true);
				marieIndividus(ind2, conjoint, dateMariage);
				addIndividuAfterEvent(idEvtCorrection, ind2, dateEvt, TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, idEvtCorrige);
			}
		});

		// construction de l'événement de correction
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(idEvtCorrection);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.TESTING);
			evt.setAction(ActionEvenementCivilEch.CORRECTION);
			evt.setDateEvenement(dateEvt);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setRefMessageId(idEvtCorrige);
			hibernateTemplate.merge(evt);
			return null;
		});

		// traitement de l'événement de correction
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(idEvtCorrection);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
			Assert.assertEquals("L'élément suivant a été modifié par la correction : relations (conjoints (apparition)).", evt.getCommentaireTraitement());

			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			Assert.assertNotNull(erreurs);
			Assert.assertEquals(1, erreurs.size());

			final EvenementCivilEchErreur erreur = erreurs.iterator().next();
			Assert.assertEquals("Traitement automatique non implémenté. Veuillez effectuer cette opération manuellement.", erreur.getMessage());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void  testModificationPlusieursAttributs() throws Exception {

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
		final RegDate dateEvt = RegDate.get();
		final RegDate dateMariage = date(1955, 12, 1);

		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Zweisteinen", "Robert", true);
				marieIndividu(ind, dateMariage.addDays(1));     // <-- état civil différent par la date
				addIndividuAfterEvent(idEvtCorrige, ind, dateEvt, TypeEvenementCivilEch.TESTING);

				final MockIndividu conjoint = addIndividu(noIndividuConjoint, null, "Viersteinen", "Beate", false);
				final MockIndividu ind2 = createIndividu(noIndividu, date(1930, 5, 12), "Dreisteinen", "Albert", true); // <-- date de naissance différente
				marieIndividus(ind2, conjoint, dateMariage);    // <-- conjoint différent
				addIndividuAfterEvent(idEvtCorrection, ind2, dateEvt, TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, idEvtCorrige);
			}
		});

		// construction de l'événement de correction
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(idEvtCorrection);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.TESTING);
			evt.setAction(ActionEvenementCivilEch.CORRECTION);
			evt.setDateEvenement(dateEvt);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setRefMessageId(idEvtCorrige);
			hibernateTemplate.merge(evt);
			return null;
		});

		// traitement de l'événement de correction
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(idEvtCorrection);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
			Assert.assertEquals("Les éléments suivants ont été modifiés par la correction : date de naissance (apparition), état civil (dates), relations (conjoints (apparition)).", evt.getCommentaireTraitement());

			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			Assert.assertNotNull(erreurs);
			Assert.assertEquals(1, erreurs.size());

			final EvenementCivilEchErreur erreur = erreurs.iterator().next();
			Assert.assertEquals("Traitement automatique non implémenté. Veuillez effectuer cette opération manuellement.", erreur.getMessage());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testHabitant() throws Exception {

		// Tous les tests précédents ont été faits sans personne physique liée au numéro d'individu de l'événement civil
		// Ici, on vérifie que tout va bien également avec une personne physique connectée

		buildStrategyOverridingTranslatorAndProcessor(true, new StrategyOverridingCallback() {
			@Override
			public void overrideStrategies(EvenementCivilEchTranslatorImplOverride translator) {
				translator.overrideStrategy(TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, strategy);
			}
		});

		final long noIndividu = 4684263L;
		final long idEvtCorrige = 464735292L;
		final long idEvtCorrection = 4326478256242L;
		final RegDate dateEvt = RegDate.get();

		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Zweisteinen", "Robert", true);
				addIndividuAfterEvent(idEvtCorrige, ind, dateEvt, TypeEvenementCivilEch.TESTING);

				final MockIndividu ind2 = createIndividu(noIndividu, null, "Dreisteinen", "Albert", true);
				addIndividuAfterEvent(idEvtCorrection, ind2, dateEvt, TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, idEvtCorrige);
			}
		});

		// mise en place fiscale
		final long pp = doInNewTransactionAndSession(status -> {
			final PersonnePhysique hab = addHabitant(noIndividu);
			return hab.getNumero();
		});

		// construction de l'événement de correction
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(idEvtCorrection);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.TESTING);
			evt.setAction(ActionEvenementCivilEch.CORRECTION);
			evt.setDateEvenement(dateEvt);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setRefMessageId(idEvtCorrige);
			hibernateTemplate.merge(evt);
			return null;
		});

		// traitement de l'événement de correction
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(idEvtCorrection);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
			Assert.assertEquals("Événement traité sans modification Unireg.", evt.getCommentaireTraitement());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testAncienHabitant() throws Exception {

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

		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Zweisteinen", "Robert", true);
				addIndividuAfterEvent(idEvtCorrige, ind, dateEvtOrig, TypeEvenementCivilEch.TESTING);

				final MockIndividu ind2 = createIndividu(noIndividu, null, "Dreisteinen", "Albert", true);
				addIndividuAfterEvent(idEvtCorrection, ind2, dateEvtCorrection, TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, idEvtCorrige);
			}
		});

		// mise en place fiscale
		final long pp = doInNewTransactionAndSession(status -> {
			final PersonnePhysique nh = tiersService.createNonHabitantFromIndividu(noIndividu);
			return nh.getNumero();
		});

		// construction de l'événement de correction
		doInNewTransactionAndSession(status -> {
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
		});

		// traitement de l'événement de correction
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(idEvtCorrection);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
			Assert.assertEquals("Evénement civil de correction sur un ancien habitant du canton.", evt.getCommentaireTraitement());

			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			Assert.assertNotNull(erreurs);
			Assert.assertEquals(1, erreurs.size());

			final EvenementCivilEchErreur erreur = erreurs.iterator().next();
			Assert.assertEquals("Traitement automatique non implémenté. Veuillez effectuer cette opération manuellement.", erreur.getMessage());
			return null;
		});
	}

	/**
	 * [SIFISC-18231] les modifications dans l'adresse de contact ne sont plus considérées comme problématique fiscalement
	 */
	@Test(timeout = 10000L)
	public void testModificationAdresseContact() throws Exception {

		buildStrategyOverridingTranslatorAndProcessor(true, new StrategyOverridingCallback() {
			@Override
			public void overrideStrategies(EvenementCivilEchTranslatorImplOverride translator) {
				translator.overrideStrategy(TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, strategy);
			}
		});

		final long noIndividu = 4684263L;
		final long idEvtCorrige = 464735292L;
		final long idEvtCorrection = 4326478256242L;
		final RegDate dateEvt = RegDate.get();

		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Zweisteinen", "Robert", true);
				marieIndividu(ind, date(1955, 12, 1));
				addIndividuAfterEvent(idEvtCorrige, ind, dateEvt, TypeEvenementCivilEch.TESTING);

				final MockIndividu ind2 = createIndividu(noIndividu, null, "Dreisteinen", "Albert", true);
				marieIndividu(ind2, date(1955, 12, 1));
				addAdresse(ind2, TypeAdresseCivil.COURRIER, MockRue.CossonayVille.AvenueDuFuniculaire, null, null, null);
				addIndividuAfterEvent(idEvtCorrection, ind2, dateEvt, TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, idEvtCorrige);
			}
		});

		// construction de l'événement de correction
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(idEvtCorrection);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.TESTING);
			evt.setAction(ActionEvenementCivilEch.CORRECTION);
			evt.setDateEvenement(dateEvt);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setRefMessageId(idEvtCorrige);
			hibernateTemplate.merge(evt);
			return null;
		});

		// traitement de l'événement de correction
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(idEvtCorrection);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
			Assert.assertEquals("Événement traité sans modification Unireg.", evt.getCommentaireTraitement());

			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			Assert.assertNotNull(erreurs);
			Assert.assertEquals(0, erreurs.size());
			return null;
		});
	}

	@Test(timeout = 10000)
	public void testRattrapageIdEvenementCorrige() throws Exception {

		buildStrategyOverridingTranslatorAndProcessor(true, new StrategyOverridingCallback() {
			@Override
			public void overrideStrategies(EvenementCivilEchTranslatorImplOverride translator) {
				translator.overrideStrategy(TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, strategy);
			}
		});

		final long noIndividu = 4684263L;
		final long idEvtCorrige = 464735292L;
		final long idEvtCorrection = 4326478256242L;
		final RegDate dateEvt = RegDate.get();

		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Zweisteinen", "Robert", true);
				addIndividuAfterEvent(idEvtCorrige, ind, dateEvt, TypeEvenementCivilEch.TESTING);

				final MockIndividu ind2 = createIndividu(noIndividu, null, "Dreisteinen", "Albert", true);
				addIndividuAfterEvent(idEvtCorrection, ind2, dateEvt, TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, idEvtCorrige);
			}
		});

		// construction de l'événement de correction
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(idEvtCorrection);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.TESTING);
			evt.setAction(ActionEvenementCivilEch.CORRECTION);
			evt.setDateEvenement(dateEvt);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setRefMessageId(null);      // <<-- c'est ici : aucune donnée reçue par le canal JMS
			hibernateTemplate.merge(evt);
			return null;
		});

		// traitement de l'événement de correction
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(idEvtCorrection);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
			Assert.assertEquals("Événement traité sans modification Unireg.", evt.getCommentaireTraitement());
			Assert.assertEquals((Long) idEvtCorrige, evt.getRefMessageId());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testDoubleCorrectionQuiSAnnulent() throws Exception {

		buildStrategyOverridingTranslatorAndProcessor(true, new StrategyOverridingCallback() {
			@Override
			public void overrideStrategies(EvenementCivilEchTranslatorImplOverride translator) {
				translator.overrideStrategy(TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, strategy);
			}
		});

		final long noIndividu = 4684263L;
		final long idEvtCorrige = 464735292L;
		final long idEvtCorrection = 4326478256242L;
		final long idEvtDoubleCorrection = 45454548545L;
		final RegDate dateEvt = RegDate.get();

		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Zweisteinen", "Robert", true);
				marieIndividu(ind, date(1955, 12, 1));
				addIndividuAfterEvent(idEvtCorrige, ind, dateEvt, TypeEvenementCivilEch.TESTING);

				final MockIndividu ind2 = createIndividu(noIndividu, null, "Zweisteinen", "Albert", true);
				marieIndividu(ind2, date(1955, 12, 18));
				addIndividuAfterEvent(idEvtCorrection, ind2, dateEvt, TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, idEvtCorrige);

				final MockIndividu ind3 = createIndividu(noIndividu, null, "Zweisteinen", "Albert", true);
				marieIndividu(ind3, date(1955, 12, 1));
				addIndividuAfterEvent(idEvtDoubleCorrection, ind3, dateEvt, TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, idEvtCorrection);
			}
		});

		// construction de l'événement de correction
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(idEvtCorrection);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.TESTING);
			evt.setAction(ActionEvenementCivilEch.CORRECTION);
			evt.setDateEvenement(dateEvt);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setRefMessageId(idEvtCorrige);
			hibernateTemplate.merge(evt);
			return null;
		});

		// traitement de l'événement de correction -> la date de mariage est différente -> pas traitable automatiquement
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(idEvtCorrection);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
			Assert.assertEquals("L'élément suivant a été modifié par la correction : état civil (dates).", evt.getCommentaireTraitement());

			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			Assert.assertNotNull(erreurs);
			Assert.assertEquals(1, erreurs.size());

			final EvenementCivilEchErreur erreur = erreurs.iterator().next();
			Assert.assertEquals("Traitement automatique non implémenté. Veuillez effectuer cette opération manuellement.", erreur.getMessage());
			return null;
		});

		// construction de l'événement de correction de correction
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(idEvtDoubleCorrection);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.TESTING);
			evt.setAction(ActionEvenementCivilEch.CORRECTION);
			evt.setDateEvenement(dateEvt);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setRefMessageId(idEvtCorrection);
			hibernateTemplate.merge(evt);
			return null;
		});

		// traitement de l'événement de correction -> la date de mariage maintenant redevenue identique -> traitable automatiquement
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			{
				final EvenementCivilEch evt = evtCivilDAO.get(idEvtCorrection);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
				Assert.assertEquals("Événement traité sans modification Unireg. Evénement et correction(s) pris en compte ensemble.", evt.getCommentaireTraitement());

				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				Assert.assertNotNull(erreurs);
				Assert.assertEquals(0, erreurs.size());
			}
			{
				final EvenementCivilEch evt = evtCivilDAO.get(idEvtDoubleCorrection);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
				Assert.assertEquals("Evénement directement pris en compte dans le traitement de l'événement référencé.", evt.getCommentaireTraitement());

				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				Assert.assertNotNull(erreurs);
				Assert.assertEquals(0, erreurs.size());
			}
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testRattrapageRefMessageIdAvecDoubleCorrection() throws Exception {

		buildStrategyOverridingTranslatorAndProcessor(true, new StrategyOverridingCallback() {
			@Override
			public void overrideStrategies(EvenementCivilEchTranslatorImplOverride translator) {
				translator.overrideStrategy(TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, strategy);
			}
		});

		final long noIndividu = 4684263L;
		final long idEvtCorrige = 464735292L;
		final long idEvtCorrection = 4326478256242L;
		final long idEvtDoubleCorrection = 45454548545L;
		final RegDate dateEvt = RegDate.get();

		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Zweisteinen", "Robert", true);
				marieIndividu(ind, date(1955, 12, 1));
				addIndividuAfterEvent(idEvtCorrige, ind, dateEvt, TypeEvenementCivilEch.TESTING);

				final MockIndividu ind2 = createIndividu(noIndividu, null, "Zweisteinen", "Albert", true);
				marieIndividu(ind2, date(1955, 12, 18));
				addIndividuAfterEvent(idEvtCorrection, ind2, dateEvt, TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, idEvtCorrige);

				final MockIndividu ind3 = createIndividu(noIndividu, null, "Zweisteinen", "Albert", true);
				marieIndividu(ind3, date(1955, 12, 1));
				addIndividuAfterEvent(idEvtDoubleCorrection, ind3, dateEvt, TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, idEvtCorrection);
			}
		});

		// construction des événements de correction
		doInNewTransactionAndSession(status -> {
			{
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(idEvtCorrection);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.TESTING);
				evt.setAction(ActionEvenementCivilEch.CORRECTION);
				evt.setDateEvenement(dateEvt);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setRefMessageId(null);
				hibernateTemplate.merge(evt);
			}
			{
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(idEvtDoubleCorrection);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.TESTING);
				evt.setAction(ActionEvenementCivilEch.CORRECTION);
				evt.setDateEvenement(dateEvt);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setRefMessageId(idEvtCorrection);
				hibernateTemplate.merge(evt);
			}
			return null;
		});

		// traitement de l'événement de correction -> la date de mariage maintenant redevenue identique -> traitable automatiquement
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			{
				final EvenementCivilEch evt = evtCivilDAO.get(idEvtCorrection);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
				Assert.assertEquals("Événement traité sans modification Unireg. Evénement et correction(s) pris en compte ensemble.", evt.getCommentaireTraitement());
				Assert.assertEquals((Long) idEvtCorrige, evt.getRefMessageId());       // doit avoir été rattrapé lors du traitement

				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				Assert.assertNotNull(erreurs);
				Assert.assertEquals(0, erreurs.size());
			}
			{
				final EvenementCivilEch evt = evtCivilDAO.get(idEvtDoubleCorrection);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
				Assert.assertEquals("Evénement directement pris en compte dans le traitement de l'événement référencé.", evt.getCommentaireTraitement());
				Assert.assertEquals((Long) idEvtCorrection, evt.getRefMessageId());     // pas de rattrapage nécessaire, mais pas de modification non plus...

				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				Assert.assertNotNull(erreurs);
				Assert.assertEquals(0, erreurs.size());
			}
			return null;
		});
	}

	@Test
	public void testModificationDateEvenementSeule() throws Exception {

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

		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Zweisteinen", "Robert", true);
				marieIndividu(ind, date(1955, 12, 1));
				addIndividuAfterEvent(idEvtCorrige, ind, dateEvtOrig, TypeEvenementCivilEch.TESTING);

				final MockIndividu ind2 = createIndividu(noIndividu, null, "Zweisteinen", "Albert", true);
				marieIndividu(ind2, date(1955, 12, 1));
				addIndividuAfterEvent(idEvtCorrection, ind2, dateEvtCorrection, TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, idEvtCorrige);
			}
		});

		// construction de l'événement de correction
		doInNewTransactionAndSession(status -> {
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
		});

		// traitement de l'événement de correction
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(idEvtCorrection);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
			Assert.assertEquals("L'élément suivant a été modifié par la correction : date de l'événement.", evt.getCommentaireTraitement());

			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			Assert.assertNotNull(erreurs);
			Assert.assertEquals(1, erreurs.size());

			final EvenementCivilEchErreur erreur = erreurs.iterator().next();
			Assert.assertEquals("Traitement automatique non implémenté. Veuillez effectuer cette opération manuellement.", erreur.getMessage());
			return null;
		});
	}

	/**
	 * [SIFISC-10801] Il y a tellement de modifications fiscales que le commentaire de traitement dépasse les 255 caractères ({@link ch.vd.unireg.common.LengthConstants#EVTCIVILECH_COMMENT})
	 */
	@Test
	public void testPleinDeModificationsFiscales() throws Exception {

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

		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, date(1930, 4, 1), "Zweisteinen", "Robert", true);
				addIndividuAfterEvent(idEvtCorrige, ind, dateEvtOrig, TypeEvenementCivilEch.TESTING);

				final MockIndividu conjoint = addIndividu(noIndividu + 1, null, "Zweisteinen", "Félicie", false);
				final MockIndividu ind2 = createIndividu(noIndividu, date(1933, 4, 1), "Zweisteinen", "Albert", true);
				marieIndividus(ind2, conjoint, date(1955, 11, 1));
				addAdresse(ind2, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, date(1955, 11, 1), null);
				addAdresse(ind2, TypeAdresseCivil.COURRIER, MockRue.CossonayVille.AvenueDuFuniculaire, null, date(1955, 11, 1), null);
				addNationalite(ind2, MockPays.RoyaumeUni, date(1933, 4, 1), null);
				addPermis(ind2, TypePermis.ETABLISSEMENT, date(1955, 11, 1), null, false);
				addIndividuAfterEvent(idEvtCorrection, ind2, dateEvtCorrection, TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.CORRECTION, idEvtCorrige);
			}
		});

		// construction de l'événement de correction
		doInNewTransactionAndSession(status -> {
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
		});

		// traitement de l'événement de correction
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(idEvtCorrection);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
			Assert.assertEquals(
					"Les éléments suivants ont été modifiés par la correction : adresse de résidence principale (apparition), date de l'événement, date de naissance, état civil, nationalité (apparition), permis (apparition), relations (conjoints (apparition)).",
					evt.getCommentaireTraitement());

			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			Assert.assertNotNull(erreurs);
			Assert.assertEquals(1, erreurs.size());

			final EvenementCivilEchErreur erreur = erreurs.iterator().next();
			Assert.assertEquals("Traitement automatique non implémenté. Veuillez effectuer cette opération manuellement.", erreur.getMessage());
			return null;
		});
	}
}
