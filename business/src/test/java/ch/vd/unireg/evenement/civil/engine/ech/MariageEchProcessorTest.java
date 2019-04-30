package ch.vd.unireg.evenement.civil.engine.ech;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEch;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchErreur;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementCivilEch;
import ch.vd.unireg.type.TypePermis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class MariageEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {

	@Test(timeout = 10000L)
	public void testMariage() throws Exception {

		final long noMadame = 46215611L;
		final long noMonsieur = 78215611L;
		final RegDate dateMariage = RegDate.get().addMonths(-1);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu monsieur = addIndividu(noMonsieur, date(1923, 2, 12), "Crispus", "Santacorpus", true);
				MockIndividu madame = addIndividu(noMadame, date(1974, 8, 1), "Lisette", "Bouton", false);
				marieIndividus(monsieur, madame, dateMariage);
			}
		});

		doInNewTransactionAndSession(status -> {
			PersonnePhysique monsieur = addHabitant(noMonsieur);
			addForPrincipal(monsieur, date(1943, 2, 12), MotifFor.MAJORITE, MockCommune.Echallens);
			PersonnePhysique madame = addHabitant(noMadame);
			addForPrincipal(madame, date(1992, 8, 1), MotifFor.MAJORITE, MockCommune.Chamblon);
			return null;
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long mariageId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(1235563456L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateMariage);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noMonsieur);
			evt.setType(TypeEvenementCivilEch.MARIAGE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement synchrone de l'événement
		traiterEvenements(noMonsieur);

		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(mariageId);
			assertNotNull(evt);
			assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

			final PersonnePhysique monsieur = tiersService.getPersonnePhysiqueByNumeroIndividu(noMonsieur);
			assertNotNull(monsieur);

			final PersonnePhysique madame = tiersService.getPersonnePhysiqueByNumeroIndividu(noMadame);
			assertNotNull(madame);

			final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(madame, dateMariage);
			assertNotNull(ensemble);
			assertSame(monsieur, ensemble.getPrincipal());
			assertSame(madame, ensemble.getConjoint());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testMariageEnDeuxTemps() throws Exception {

		final long noMadame = 46215611L;
		final long noMonsieur = 78215611L;
		final RegDate dateMariage = RegDate.get().addMonths(-1);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final RegDate dateNaissanceMonsieur = date(1923, 2, 12);
				final MockIndividu monsieur = addIndividu(noMonsieur, dateNaissanceMonsieur, "Crispus", "Santacorpus", true);
				final RegDate dateNaissanceMadame = date(1974, 8, 1);
				final MockIndividu madame = addIndividu(noMadame, dateNaissanceMadame, "Lisette", "Bouton", false);

				addNationalite(monsieur, MockPays.Suisse, dateNaissanceMonsieur, null);
				addNationalite(madame, MockPays.Suisse, dateNaissanceMadame, null);

				// on se place dans le cas RCPers où seul Monsieur est marié avec Madame comme conjoint (mais elle ne le sait pas encore... chut!)
				marieIndividu(monsieur, dateMariage);
				addRelationConjoint(monsieur, madame, dateMariage);
			}
		});

		doInNewTransactionAndSession(status -> {
			PersonnePhysique monsieur = addHabitant(noMonsieur);
			addForPrincipal(monsieur, date(1943, 2, 12), MotifFor.MAJORITE, MockCommune.Echallens);
			PersonnePhysique madame = addHabitant(noMadame);
			addForPrincipal(madame, date(1992, 8, 1), MotifFor.MAJORITE, MockCommune.Chamblon);
			return null;
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long mariageMonsieurId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(1235563456L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateMariage);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noMonsieur);
			evt.setType(TypeEvenementCivilEch.MARIAGE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement synchrone de l'événement
		traiterEvenements(noMonsieur);

		// vérification que l'évenement est en erreur et que monsieur n'est pas dans un couple
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(mariageMonsieurId);
			assertNotNull(evt);
			assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());

			final PersonnePhysique monsieur = tiersService.getPersonnePhysiqueByNumeroIndividu(noMonsieur);
			assertNotNull(monsieur);

			final PersonnePhysique madame = tiersService.getPersonnePhysiqueByNumeroIndividu(noMadame);
			assertNotNull(madame);

			final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(monsieur, dateMariage);
			assertNull(ensemble);
			return null;
		});

		// maintenant, il faut marier Madame...
		doModificationIndividus(noMonsieur, noMadame, new IndividusModification() {
			@Override
			public void modifyIndividus(MockIndividu monsieur, MockIndividu madame) {
				MockServiceCivil.marieIndividu(madame, dateMariage);
				MockServiceCivil.addRelationConjoint(madame, monsieur, dateMariage);
			}
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long mariageMadameId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(34256724756L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateMariage);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noMadame);
			evt.setType(TypeEvenementCivilEch.MARIAGE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement synchrone de l'événement
		traiterEvenements(noMadame);
		// simul recyclage de l'évenement de Monsieur
		traiterEvenements(noMonsieur);

		// vérification de la constitution du ménage complet
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evtMme = evtCivilDAO.get(mariageMadameId);
			assertNotNull(evtMme);
			assertEquals(EtatEvenementCivil.TRAITE, evtMme.getEtat());

			final EvenementCivilEch evtMr = evtCivilDAO.get(mariageMonsieurId);
			assertNotNull(evtMr);
			assertEquals(EtatEvenementCivil.REDONDANT, evtMr.getEtat());

			final PersonnePhysique monsieur = tiersService.getPersonnePhysiqueByNumeroIndividu(noMonsieur);
			assertNotNull(monsieur);

			final PersonnePhysique madame = tiersService.getPersonnePhysiqueByNumeroIndividu(noMadame);
			assertNotNull(madame);

			final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(madame, dateMariage);
			assertNotNull(ensemble);
			assertSame(monsieur, ensemble.getPrincipal());
			assertSame(madame, ensemble.getConjoint());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testMariageEnDeuxTempsAvecDatesDifferentes() throws Exception {

		final long noMadame = 46215611L;
		final long noMonsieur = 78215611L;
		final RegDate dateMariageMonsieur = RegDate.get().addMonths(-1);
		final RegDate dateMariageMadame = dateMariageMonsieur.getOneDayAfter();

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final RegDate dateNaissanceMonsieur = date(1923, 2, 12);
				final MockIndividu monsieur = addIndividu(noMonsieur, dateNaissanceMonsieur, "Crispus", "Santacorpus", true);
				final RegDate dateNaissanceMadame = date(1974, 8, 1);
				final MockIndividu madame = addIndividu(noMadame, dateNaissanceMadame, "Lisette", "Bouton", false);

				addNationalite(monsieur, MockPays.Suisse, dateNaissanceMonsieur, null);
				addNationalite(madame, MockPays.Suisse, dateNaissanceMadame, null);

				// on se place dans le cas RCPers où seul Monsieur est marié avec Madame comme conjoint (mais elle ne le sait pas encore... chut!)
				marieIndividu(monsieur, dateMariageMonsieur);
				addRelationConjoint(monsieur, madame, dateMariageMonsieur);
			}
		});

		doInNewTransactionAndSession(status -> {
			PersonnePhysique monsieur = addHabitant(noMonsieur);
			addForPrincipal(monsieur, date(1943, 2, 12), MotifFor.MAJORITE, MockCommune.Echallens);
			PersonnePhysique madame = addHabitant(noMadame);
			addForPrincipal(madame, date(1992, 8, 1), MotifFor.MAJORITE, MockCommune.Chamblon);
			return null;
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long mariageMonsieurId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(1235563456L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateMariageMonsieur);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noMonsieur);
			evt.setType(TypeEvenementCivilEch.MARIAGE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement synchrone de l'événement
		traiterEvenements(noMonsieur);

		// vérification que l'évenement est en erreur et que monsieur n'est pas dans un couple
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(mariageMonsieurId);
			assertNotNull(evt);
			assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());

			final PersonnePhysique monsieur = tiersService.getPersonnePhysiqueByNumeroIndividu(noMonsieur);
			assertNotNull(monsieur);

			final PersonnePhysique madame = tiersService.getPersonnePhysiqueByNumeroIndividu(noMadame);
			assertNotNull(madame);

			final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(monsieur, dateMariageMonsieur);
			assertNull(ensemble);
			return null;
		});


		// maintenant, il faut marier Madame...
		doModificationIndividus(noMonsieur, noMadame, new IndividusModification() {
			@Override
			public void modifyIndividus(MockIndividu monsieur, MockIndividu madame) {
				MockServiceCivil.marieIndividu(madame, dateMariageMadame);
				MockServiceCivil.addRelationConjoint(madame, monsieur, dateMariageMadame);
			}
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long mariageMadameId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(34256724756L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateMariageMadame);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noMadame);
			evt.setType(TypeEvenementCivilEch.MARIAGE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement synchrone de l'événement
		traiterEvenements(noMadame);
		// simul recyclage de l'évenement de Monsieur
		traiterEvenements(noMonsieur);

		// vérification de la constitution du ménage complet à la date de mariage de madame
		// l'evenement de monsieur devrait rester en erreur pour cause de date de mariage incohérente
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evtMme = evtCivilDAO.get(mariageMadameId);
			assertNotNull(evtMme);
			assertEquals(EtatEvenementCivil.TRAITE, evtMme.getEtat());

			final EvenementCivilEch evtMr = evtCivilDAO.get(mariageMonsieurId);
			assertNotNull(evtMr);
			assertEquals(EtatEvenementCivil.EN_ERREUR, evtMr.getEtat());

			final Set<EvenementCivilEchErreur> erreurs = evtMr.getErreurs();
			assertNotNull(erreurs);
			assertEquals(2, erreurs.size());

			final PersonnePhysique monsieur = tiersService.getPersonnePhysiqueByNumeroIndividu(noMonsieur);
			assertNotNull(monsieur);

			final PersonnePhysique madame = tiersService.getPersonnePhysiqueByNumeroIndividu(noMadame);
			assertNotNull(madame);

			final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(madame, dateMariageMadame);
			assertNotNull(ensemble);
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testMariageAvecConjointNonHabitantNonRetourneParServiceCivil() throws Exception {

		final long noIndividu = 32674254L;
		final long noIndividuConjoint = 4356724524L;
		final RegDate dateNaissanceMonsieur = date(1980, 2, 12);
		final RegDate dateMajoriteMonsieur = dateNaissanceMonsieur.addYears(18);
		final RegDate dateMariage = date(2009, 8, 12);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu monsieur = addIndividu(noIndividu, dateNaissanceMonsieur, "Crispus", "Santacorpus", true);
				final RegDate dateNaissanceMadame = date(1974, 8, 1);
				final MockIndividu madame = addIndividu(noIndividuConjoint, dateNaissanceMadame, "Lisette", "Bouton", false);
				madame.setNonHabitantNonRenvoye(true);

				addAdresse(monsieur, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateNaissanceMonsieur, null);
				addNationalite(monsieur, MockPays.Suisse, dateNaissanceMonsieur, null);
				addNationalite(madame, MockPays.Suisse, dateNaissanceMadame, null);
				marieIndividus(monsieur, madame, dateMariage);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateMajoriteMonsieur, MotifFor.MAJORITE, MockCommune.Echallens);
			return pp.getNumero();
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long mariageId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(4365785674L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateMariage);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.MARIAGE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement effectué
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(mariageId);
			assertNotNull(evt);
			assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			assertNotNull(pp);
			assertEquals((Long) noIndividu, pp.getNumeroIndividu());

			final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(pp, null);
			assertNotNull(couple);
			final MenageCommun mc = couple.getMenage();
			assertNotNull(mc);
			assertNull(couple.getConjoint(pp));     // <-- marié seul!

			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp);
			assertEquals(dateMariage, ffp.getDateDebut());
			assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffp.getMotifOuverture());
			assertNull(ffp.getDateFin());
			assertNull(ffp.getMotifFermeture());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testMariageSIFISC6021() throws Exception {

		final long noMadame = 46215611L;
		final long noMonsieur = 78215611L;
		final RegDate dateMariage = RegDate.get().addMonths(-1);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final RegDate dateNaissanceMonsieur = date(1923, 2, 12);
				final MockIndividu monsieur = addIndividu(noMonsieur, dateNaissanceMonsieur, "Crispus", "Santacorpus", true);
				final RegDate dateNaissanceMadame = date(1974, 8, 1);
				final MockIndividu madame = addIndividu(noMadame, dateNaissanceMadame, "Lisette", "Bouton", false);

				addNationalite(monsieur, MockPays.France, dateNaissanceMonsieur, null);
				addPermis(monsieur, TypePermis.SEJOUR, date(2009, 2, 12), null, false);
				addNationalite(madame, MockPays.Suisse, dateNaissanceMadame, null);

				// on se place dans le cas RCPers où seul Monsieur est marié avec Madame comme conjoint (mais elle ne le sait pas encore... chut!)
				marieIndividu(monsieur, dateMariage);
				addRelationConjoint(monsieur, madame, dateMariage);
			}
		});

		doInNewTransactionAndSession(status -> {
			PersonnePhysique monsieur = addHabitant(noMonsieur);
			addForPrincipal(monsieur, date(2009, 2, 12), MotifFor.ARRIVEE_HS, MockCommune.Echallens);
			PersonnePhysique madame = addHabitant(noMadame);
			addForPrincipal(madame, date(1992, 8, 1), MotifFor.MAJORITE, MockCommune.Chamblon);
			return null;
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long mariageMonsieurId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(1235563456L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateMariage);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noMonsieur);
			evt.setType(TypeEvenementCivilEch.MARIAGE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement synchrone de l'événement
		traiterEvenements(noMonsieur);

		// vérification que l'évenement est en erreur et que monsieur n'est pas dans un couple
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(mariageMonsieurId);
			assertNotNull(evt);
			assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());

			final PersonnePhysique monsieur = tiersService.getPersonnePhysiqueByNumeroIndividu(noMonsieur);
			assertNotNull(monsieur);

			final PersonnePhysique madame = tiersService.getPersonnePhysiqueByNumeroIndividu(noMadame);
			assertNotNull(madame);

			final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(monsieur, dateMariage);
			assertNull(ensemble);
			return null;
		});

		// On crée un marié seul dans unireg avec monsieur
		doInNewTransactionAndSession(status -> {
			PersonnePhysique monsieur = tiersService.getPersonnePhysiqueByNumeroIndividu(noMonsieur);
			MenageCommun menage = addEnsembleTiersCouple(monsieur, null, dateMariage, null).getMenage();
			tiersService.closeForFiscalPrincipal(monsieur, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			addForPrincipal(menage, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
			return null;
		});


		// maintenant, on marie Madame ...
		doModificationIndividus(noMonsieur, noMadame, new IndividusModification() {
			@Override
			public void modifyIndividus(MockIndividu monsieur, MockIndividu madame) {
				MockServiceCivil.marieIndividu(madame, dateMariage);
				MockServiceCivil.addRelationConjoint(madame, monsieur, dateMariage);
			}
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long mariageMadameId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(34256724756L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateMariage);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noMadame);
			evt.setType(TypeEvenementCivilEch.MARIAGE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement synchrone de l'événement
		traiterEvenements(noMadame);
		// simul recyclage de l'évenement de Monsieur
		traiterEvenements(noMonsieur);

		// vérification que les evenements sont tjs en erreur (comportement souhaiter dans SIFIS6021)
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evtMme = evtCivilDAO.get(mariageMadameId);
			assertNotNull(evtMme);
			assertEquals(EtatEvenementCivil.EN_ERREUR, evtMme.getEtat());

			final EvenementCivilEch evtMr = evtCivilDAO.get(mariageMonsieurId);
			assertNotNull(evtMr);
			assertEquals(EtatEvenementCivil.EN_ERREUR, evtMr.getEtat());

			final PersonnePhysique monsieur = tiersService.getPersonnePhysiqueByNumeroIndividu(noMonsieur);
			assertNotNull(monsieur);

			final PersonnePhysique madame = tiersService.getPersonnePhysiqueByNumeroIndividu(noMadame);
			assertNotNull(madame);

			// On vérifie que madame n'est pas dans un ménage
			final EnsembleTiersCouple ensemble1 = tiersService.getEnsembleTiersCouple(madame, dateMariage);
			assertNull(ensemble1);

			// Et que le ménage de monsieur n'a pas bougé
			final EnsembleTiersCouple ensemble2 = tiersService.getEnsembleTiersCouple(monsieur, dateMariage);
			assertNotNull(ensemble2);
			assertSame(monsieur, ensemble2.getPrincipal());
			assertNull(ensemble2.getConjoint());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testMariageSIFISC6022() throws Exception {

		final long noMadame = 46215611L;
		final long noMonsieur = 78215611L;
		final RegDate dateMariage = RegDate.get().addMonths(-1);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final RegDate dateNaissanceMonsieur = date(1923, 2, 12);
				final MockIndividu monsieur = addIndividu(noMonsieur, dateNaissanceMonsieur, "Crispus", "Santacorpus", true);
				final RegDate dateNaissanceMadame = date(1974, 8, 1);
				final MockIndividu madame = addIndividu(noMadame, dateNaissanceMadame, "Lisette", "Bouton", false);

				addNationalite(monsieur, MockPays.Suisse, dateNaissanceMonsieur, null);
				addNationalite(madame, MockPays.Suisse, dateNaissanceMadame, null);

				marieIndividu(monsieur, dateMariage);
			}
		});

		doInNewTransactionAndSession(status -> {
			PersonnePhysique monsieur = addHabitant(noMonsieur);
			addForPrincipal(monsieur, date(1943, 2, 12), MotifFor.MAJORITE, MockCommune.Echallens);
			PersonnePhysique madame = addHabitant(noMadame);
			addForPrincipal(madame, date(1992, 8, 1), MotifFor.MAJORITE, MockCommune.Chamblon);
			return null;
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long mariageMonsieurId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(1235563456L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateMariage);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noMonsieur);
			evt.setType(TypeEvenementCivilEch.MARIAGE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement synchrone de l'événement
		traiterEvenements(noMonsieur);

		// vérification que l'évenement est traité et que monsieur est marié seul
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(mariageMonsieurId);
			assertNotNull(evt);
			assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
			final PersonnePhysique monsieur = tiersService.getPersonnePhysiqueByNumeroIndividu(noMonsieur);
			assertNotNull(monsieur);
			final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(monsieur, dateMariage);
			assertNotNull(ensemble);
			assertSame(monsieur, ensemble.getPrincipal());
			assertNull(ensemble.getConjoint());
			return null;
		});


		// maintenant, on marie Madame ...
		doModificationIndividus(noMonsieur, noMadame, new IndividusModification() {
			@Override
			public void modifyIndividus(MockIndividu monsieur, MockIndividu madame) {
				MockServiceCivil.marieIndividu(madame, dateMariage);
				MockServiceCivil.addRelationConjoint(madame, monsieur, dateMariage);
			}
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long mariageMadameId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(34256724756L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateMariage);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noMadame);
			evt.setType(TypeEvenementCivilEch.MARIAGE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement synchrone de l'événement
		traiterEvenements(noMadame);

		// vérification que l' evenement de madame est en erruer et que un nouveau ménage commun ne soit pas créé
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evtMme = evtCivilDAO.get(mariageMadameId);
			assertNotNull(evtMme);
			assertEquals(EtatEvenementCivil.EN_ERREUR, evtMme.getEtat());

			final PersonnePhysique monsieur = tiersService.getPersonnePhysiqueByNumeroIndividu(noMonsieur);
			assertNotNull(monsieur);

			final PersonnePhysique madame = tiersService.getPersonnePhysiqueByNumeroIndividu(noMadame);
			assertNotNull(madame);

			// On vérifie que madame n'est pas dans un ménage
			final EnsembleTiersCouple ensemble1 = tiersService.getEnsembleTiersCouple(madame, dateMariage);
			assertNull(ensemble1);

			// Et que le ménage de monsieur n'a pas bougé
			final EnsembleTiersCouple ensemble2 = tiersService.getEnsembleTiersCouple(monsieur, dateMariage);
			assertNotNull(ensemble2);
			assertSame(monsieur, ensemble2.getPrincipal());
			assertNull(ensemble2.getConjoint());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testMariageAvecDecisionAci() throws Exception {

		final long noMadame = 46215611L;
		final long noMonsieur = 78215611L;
		final RegDate dateMariage = RegDate.get().addMonths(-1);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu monsieur = addIndividu(noMonsieur, date(1923, 2, 12), "Crispus", "Santacorpus", true);
				MockIndividu madame = addIndividu(noMadame, date(1974, 8, 1), "Lisette", "Bouton", false);
				marieIndividus(monsieur, madame, dateMariage);
			}
		});

		doInNewTransactionAndSession(status -> {
			PersonnePhysique monsieur = addHabitant(noMonsieur);
			addForPrincipal(monsieur, date(1943, 2, 12), MotifFor.MAJORITE, MockCommune.Echallens);
			PersonnePhysique madame = addHabitant(noMadame);
			addForPrincipal(madame, date(1992, 8, 1), MotifFor.MAJORITE, MockCommune.Chamblon);
			addDecisionAci(monsieur, date(2000, 1, 1), null, MockCommune.Echallens.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null);
			return null;
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long mariageId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(1235563456L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateMariage);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noMonsieur);
			evt.setType(TypeEvenementCivilEch.MARIAGE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement synchrone de l'événement
		traiterEvenements(noMonsieur);

		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(mariageId);
			assertNotNull(evt);
			assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());

			final PersonnePhysique monsieur = tiersService.getPersonnePhysiqueByNumeroIndividu(noMonsieur);
			assertNotNull(monsieur);
			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			Assert.assertNotNull(erreurs);
			Assert.assertEquals(1, erreurs.size());
			final EvenementCivilEchErreur erreur = erreurs.iterator().next();
			String message = String.format("Le contribuable trouvé (%s) est sous l'influence d'une décision ACI",
			                               FormatNumeroHelper.numeroCTBToDisplay(monsieur.getNumero()));
			Assert.assertEquals(message, erreur.getMessage());
			return null;
		});
	}
}
