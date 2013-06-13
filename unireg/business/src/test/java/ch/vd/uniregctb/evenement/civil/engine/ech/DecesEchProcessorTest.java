package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.Set;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchErreur;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;
import ch.vd.uniregctb.type.TypeEvenementErreur;
import ch.vd.uniregctb.type.TypePermis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DecesEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {

	@Test(timeout = 10000L)
	public void testDeces() throws Exception {

		final long noIndividu = 267813451L;
		final RegDate dateNaissance = RegDate.get().addMonths(-1).addYears(-30);
		final RegDate dateDeces = RegDate.get().addMonths(-1);

		// mise en place de son vivant
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, dateNaissance, "Quatre", "Jessica", false);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateNaissance.addYears(18), MotifFor.MAJORITE, MockCommune.Lausanne);
				return pp.getNumero();
			}
		});

		// décès civil
		doModificationIndividu(noIndividu, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				individu.setDateDeces(dateDeces);
			}
		});

		// événement de décès
		final long decesId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(67235L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateDeces);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.DECES);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement synchrone de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(decesId);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				Assert.assertNotNull(pp);
				Assert.assertEquals((Long) ppId, pp.getNumero());

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(MotifFor.VEUVAGE_DECES, ffp.getMotifFermeture());
				Assert.assertEquals(dateDeces, ffp.getDateFin());
				return null;
			}
		});
	}

	/**
	 * Problème soulevé par [SIFISC-4877] :
	 *
	 * Si l'événement de changement d'état civil du survivant arrive avant l'evenement de décés alors
	 * celui-ci est en erreur alors qu'il serait plus judicieux qu'il soit redondant si sont for est
	 * cohérent (fermé pour motif décès).
	 *
	 */
	@Test(timeout = 10000L)
	public void testEvenementDecesArriveApresEvenementChangementEtatCivilDuSurvivant () throws Exception {

		// 1. Création d'un couple marié
		final long noMadame = 46215611L;
		final long noMonsieur = 78215611L;
		final RegDate dateDecesMonsieur = RegDate.get(2012, 4, 27);

		// Dans le civil
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu monsieur = addIndividu(noMonsieur, date(1923, 2, 12), "Crispus", "Santacorpus", true);
				MockIndividu madame = addIndividu(noMadame, date(1924, 8, 1), "Lisette", "Bouton", false);
				addNationalite(monsieur, MockPays.Suisse, date(1923, 2, 12), null);
				addNationalite(madame, MockPays.Suisse, date(1924, 8, 1), null);
				marieIndividus(monsieur, madame, RegDate.get(1947,7,14));
				veuvifieIndividu(madame, dateDecesMonsieur, false);
			}
		});

		// Dans le fiscal
		final Long[] ids = doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Long[]>() {
			@Override
			public Long[] execute(TransactionStatus status) throws Exception {
				PersonnePhysique monsieur = addHabitant(noMonsieur);
				addForPrincipal(monsieur, date(1943, 2, 12), MotifFor.MAJORITE, date(1947, 7, 13), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
				addAdresseSuisse(monsieur, TypeAdresseTiers.DOMICILE, date(1943, 2, 12), null, MockRue.Echallens.GrandRue);
				PersonnePhysique madame = addHabitant(noMadame);
				addForPrincipal(madame, date(1944, 8, 1), MotifFor.MAJORITE, date(1947, 7, 13), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Chamblon);
				addAdresseSuisse(madame, TypeAdresseTiers.DOMICILE, date(1944, 8, 1), date(1947, 7, 13), MockRue.Chamblon.RueDesUttins);
				addAdresseSuisse(madame, TypeAdresseTiers.DOMICILE, date(1947, 7, 14), null, MockRue.Echallens.GrandRue);
				EnsembleTiersCouple ensembleTiersCouple = addEnsembleTiersCouple(monsieur, madame, date(1947, 7, 14), null);
				addForPrincipal(ensembleTiersCouple.getMenage(), date(1947, 7, 14), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
				return new Long[] {ensembleTiersCouple.getMenage().getId(), ensembleTiersCouple.getPrincipal().getId(), ensembleTiersCouple.getConjoint().getId()};
			}
		});
		final long menageId = ids[0];
		final long monsieurId = ids[1];
		final long madameId = ids[2];

		// 2. Création d'un evenement de "changement d'état civil" pour la survivante
		final long veuvageId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(1235563456L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateDecesMonsieur);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noMadame);
				evt.setType(TypeEvenementCivilEch.CHGT_ETAT_CIVIL_PARTENAIRE);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// 3. Traitement de l'évenement
		traiterEvenements(noMadame);

		// 4. Contrôle de cohérence des fors pour les 2 membres du couple
		//    - for du couple fermé pour motif déces à la date de l'événement
		//    - ouverture d'un for sur le survivant

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(veuvageId);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique madame = (PersonnePhysique) tiersDAO.get(madameId);
				assertNotNull(madame);
				assertEquals((Long) noMadame, madame.getNumeroIndividu());

				// Verification que madame n'est plus en couple
				assertNull("Madame ne doit plus être en couple le jour suivant le décès de monsieur", tiersService.getEnsembleTiersCouple(madame, dateDecesMonsieur.getOneDayAfter()));

				final MenageCommun menage  = (MenageCommun)tiersService.getTiers(menageId);

				final ForFiscalPrincipal ancienffp = menage.getForFiscalPrincipalAt(dateDecesMonsieur);
				assertNotNull("Un for sur le ménage doit exister à la date de décès de Monsieur", ancienffp);
				assertEquals("Le for du couple doit être fermé à la date du décès de Monsieur", dateDecesMonsieur, ancienffp.getDateFin());
				assertEquals("Le for du couple doit être fermé pour motif décès/veuvage", MotifFor.VEUVAGE_DECES, ancienffp.getMotifFermeture());

				final ForFiscalPrincipal ffp = madame.getDernierForFiscalPrincipal();
				assertNotNull(ffp);
				assertEquals(dateDecesMonsieur.getOneDayAfter() , ffp.getDateDebut());
				assertEquals(MotifFor.VEUVAGE_DECES, ffp.getMotifOuverture());
				assertNull(ffp.getDateFin());
				assertNull(ffp.getMotifFermeture());

				return null;
			}
		});

		// 5. Création d'un évenemement "Décés" pour Monsieur (paix à son âme)
		final long decesId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(1235563457L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateDecesMonsieur);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noMonsieur);
				evt.setType(TypeEvenementCivilEch.DECES);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// 6. Traitement de l'evenement
		traiterEvenements(noMonsieur);

		// 7. Verification de l'événement redondant.
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(decesId);
				assertNotNull(evt);
				assertEquals("L'evenement de monsieur doit être dans l'état redondant", EtatEvenementCivil.REDONDANT, evt.getEtat());
				return null;
			}
		});

	}

	/**
	 * Test le deces des 2 membres d'un couple le meme jour.
	 *
	 * Dans le cas ou les deux individus décédent le même jour au même moment et qu'il est impossible de determiner qui est le veuf de qui,
	 * Rcpers envoit seulement 2 evenements de déces et pas d'évenement de veuvage
	 *
	 */
	@Test(timeout = 10000L)
	public void testDecesDes2ConjointsLeMemeJourSansVeuvage () throws Exception {

		// 1. Création d'un couple marié
		final long noMadame = 46215611L;
		final long noMonsieur = 78215611L;
		final RegDate dateDeces = RegDate.get(2012, 4, 27);

		// Dans le civil
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu monsieur = addIndividu(noMonsieur, date(1923, 2, 12), "Crispus", "Santacorpus", true);
				MockIndividu madame = addIndividu(noMadame, date(1924, 8, 1), "Lisette", "Bouton", false);
				addNationalite(monsieur, MockPays.Suisse, date(1923, 2, 12), null);
				addNationalite(madame, MockPays.Suisse, date(1924, 8, 1), null);
				marieIndividus(monsieur, madame, RegDate.get(1947, 7, 14));
			}
		});

		// Dans le fiscal
		final Long[] ids = doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Long[]>() {
			@Override
			public Long[] execute(TransactionStatus status) throws Exception {
				PersonnePhysique monsieur = addHabitant(noMonsieur);
				addForPrincipal(monsieur, date(1943, 2, 12), MotifFor.MAJORITE, date(1947, 7, 13), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
				addAdresseSuisse(monsieur, TypeAdresseTiers.DOMICILE, date(1943, 2, 12), null, MockRue.Echallens.GrandRue);
				PersonnePhysique madame = addHabitant(noMadame);
				addForPrincipal(madame, date(1944, 8, 1), MotifFor.MAJORITE, date(1947, 7, 13), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Chamblon);
				addAdresseSuisse(madame, TypeAdresseTiers.DOMICILE, date(1944, 8, 1), date(1947, 7, 13), MockRue.Chamblon.RueDesUttins);
				addAdresseSuisse(madame, TypeAdresseTiers.DOMICILE, date(1947, 7, 14), null, MockRue.Echallens.GrandRue);
				EnsembleTiersCouple ensembleTiersCouple = addEnsembleTiersCouple(monsieur, madame, date(1947, 7, 14), null);
				addForPrincipal(ensembleTiersCouple.getMenage(), date(1947, 7, 14), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
				return new Long[] {ensembleTiersCouple.getMenage().getId(), ensembleTiersCouple.getPrincipal().getId(), ensembleTiersCouple.getConjoint().getId()};
			}
		});
		final long menageId = ids[0];
		final long monsieurId = ids[1];
		final long madameId = ids[2];

		// 2. Création d'un evenement de "deces" pour un membre du couple (ici madame)
		final long decesMadameId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(1235563456L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateDeces);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noMadame);
				evt.setType(TypeEvenementCivilEch.DECES);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// 3. Traitement de l'évenement
		traiterEvenements(noMadame);

		// 4. Contrôle de cohérence des fors pour les 2 membres du couple
		//    - for du couple fermé pour motif déces à la date de l'événement
		//    - ouverture d'un for sur le survivant

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(decesMadameId);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique madame = (PersonnePhysique) tiersDAO.get(madameId);
				assertNotNull(madame);
				assertEquals((Long) noMadame, madame.getNumeroIndividu());

				final PersonnePhysique monsieur = (PersonnePhysique) tiersDAO.get(monsieurId);
				assertNotNull(monsieur);
				assertEquals((Long) noMonsieur, monsieur.getNumeroIndividu());


				// Verification que monsieur n'est plus en couple
				assertNull(tiersService.getEnsembleTiersCouple(monsieur, null));

				final MenageCommun menage  = (MenageCommun)tiersService.getTiers(menageId);

				final ForFiscalPrincipal ancienffp = menage.getForFiscalPrincipalAt(dateDeces);
				assertNotNull("Le for du ménage doit exister à la date du décès", ancienffp);
				assertEquals ("La date de fin du for doit correspondre à celle du décès", dateDeces, ancienffp.getDateFin());
				assertEquals ("Le for doit être fermé pour motif de veuvage/décès", MotifFor.VEUVAGE_DECES, ancienffp.getMotifFermeture());

				final ForFiscalPrincipal ffp = monsieur.getForFiscalPrincipalAt(dateDeces.getOneDayAfter());
				assertNotNull("Monsieur devrait avoir un for ouvert apres la date du deces de sa conjointe, on ne sait pas encore qu'il est également décédé", ffp);
				assertEquals("le for de monsieur doit être ouvert un jour après la date de décès du couple", dateDeces.getOneDayAfter() , ffp.getDateDebut());
				assertEquals(MotifFor.VEUVAGE_DECES, ffp.getMotifOuverture());
				assertNull("le for de monsieur doit être ouvert (date de fin null)", ffp.getDateFin());
				assertNull("le for de monsieur doit être ouvert (motif de fermeture null)",ffp.getMotifFermeture());

				return null;
			}
		});

		// 5. Création d'un évenemement "Décés" pour le deuxième membre du couple: monsieur
		final long decesMonsieurId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(1235563457L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateDeces);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noMonsieur);
				evt.setType(TypeEvenementCivilEch.DECES);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// 6. Traitement de l'evenement
		traiterEvenements(noMonsieur);

		// 7. Verification de l'événement est traité et que le for de monsieur precedemment ouvert à bien étét annulé.
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evtMr = evtCivilDAO.get(decesMonsieurId);
				assertNotNull(evtMr);
				assertEquals(EtatEvenementCivil.TRAITE, evtMr.getEtat());

				final PersonnePhysique monsieur = (PersonnePhysique) tiersDAO.get(monsieurId);
				assertNotNull(monsieur);
				assertEquals((Long) noMonsieur, monsieur.getNumeroIndividu());

				final ForFiscalPrincipal ffp = monsieur.getForFiscalPrincipalAt(dateDeces.getOneDayAfter());
				assertNull("Monsieur ne devrait plus avoir de for principal actif le lendemain de son deces", ffp);

				return null;
			}
		});

	}

	/**
	 * TODO FRED
	 *
	 * Test le deces des 2 membres d'un couple le meme jour.
	 *
	 * Dans le cas ou les deux individus décédent le même jour mais il est possible de determiner qui est le veuf de qui,
	 * dans ce cas Rcpers envoit 3 evenements: 2 décés et 1 veuvage. (on les reçoit dans n'importe quel ordre bien-sûr)
	 *
	 */
	@Test(timeout = 10000L)
	public void testDecesDes2ConjointsLeMemeJourAvecVeuvage () throws Exception {

	}

	/**
	 * Cas du jira SIFISC-8279 : événement de décès mis en attente par événement de correction de divorce en erreur, événement qui est ensuite forcé manuellement ; lors de la relance
	 * de l'événement civil de décès, le for fiscal du contribuable ne se ferme pas... (car le forçage de l'événement de correction de divorce a provoqué le re-calcul du flag habitant
	 * et la recopie des données civiles dans Unireg, y compris la date de décès, ce qui fait que le décès est supposé déjà pris en compte fiscalement au moment du retraitement de
	 * l'événement civil de décès, pour lequel on se dit donc qu'il n'y a plus rien à faire...)
	 */
	@Test(timeout = 10000L)
	public void testDecesSansFermetureDeFor() throws Exception {

		final long noIndividu = 167267234L;
		final RegDate dateDivorceAvantCorrection = date(1989, 7, 4);
		final RegDate dateDivorce = date(1990, 5, 28);
		final RegDate dateDeces = date(2013, 2, 18);

		final long evtDivorceId = 342678325172L;
		final long evtCorrectionDivorceId = 67457242L;
		final long evtDecesId = 3564283462L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(1930, 3, 16);
				final MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Colin", "Marguerite", Sexe.FEMININ);
				divorceIndividu(individu, dateDivorce);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Pully.CheminDesRoches, null, date(2009, 7, 12), dateDeces);
				addAdresse(individu, TypeAdresseCivil.COURRIER, MockRue.Pully.CheminDesRoches, null, date(2009, 7, 12), null);
				addNationalite(individu, MockPays.Danemark, dateNaissance, null);
				addPermis(individu, TypePermis.ETABLISSEMENT, date(2003, 1, 1), null, false);
				individu.setDateDeces(dateDeces);

				final MockIndividu avantCorrection = createIndividu(noIndividu, dateNaissance, "Colin", "Marguerite", Sexe.FEMININ);
				divorceIndividu(avantCorrection, dateDivorceAvantCorrection);
				addAdresse(avantCorrection, TypeAdresseCivil.PRINCIPALE, MockRue.Pully.CheminDesRoches, null, date(2009, 7, 12), null);
				addAdresse(avantCorrection, TypeAdresseCivil.COURRIER, MockRue.Pully.CheminDesRoches, null, date(2009, 7, 12), null);
				addNationalite(avantCorrection, MockPays.Danemark, dateNaissance, null);
				addPermis(avantCorrection, TypePermis.ETABLISSEMENT, date(2003, 1, 1), null, false);

				final MockIndividu apresCorrection = createIndividu(noIndividu, dateNaissance, "Colin", "Marguerite", Sexe.FEMININ);
				divorceIndividu(apresCorrection, dateDivorce);
				addAdresse(apresCorrection, TypeAdresseCivil.PRINCIPALE, MockRue.Pully.CheminDesRoches, null, date(2009, 7, 12), null);
				addAdresse(apresCorrection, TypeAdresseCivil.COURRIER, MockRue.Pully.CheminDesRoches, null, date(2009, 7, 12), null);
				addNationalite(apresCorrection, MockPays.Danemark, dateNaissance, null);
				addPermis(apresCorrection, TypePermis.ETABLISSEMENT, date(2003, 1, 1), null, false);

				addIndividuAfterEvent(evtDivorceId, avantCorrection, dateDivorceAvantCorrection, TypeEvenementCivilEch.DIVORCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, null);
				addIndividuAfterEvent(evtCorrectionDivorceId, apresCorrection, dateDivorce, TypeEvenementCivilEch.DIVORCE, ActionEvenementCivilEch.CORRECTION, evtDivorceId);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				pp.setDateDeces(null);
				addForPrincipal(pp, dateDivorceAvantCorrection, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Pully);
				return pp.getNumero();
			}
		});

		// petite vérification intermédiaire : habitante avec for ouvert
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNotNull(pp);
				assertNull(pp.getDateDeces());
				assertTrue(pp.isHabitantVD());

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				assertNotNull(ffp);
				assertEquals(dateDivorceAvantCorrection, ffp.getDateDebut());
				assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffp.getMotifOuverture());
				assertNull(ffp.getDateFin());
				assertNull(ffp.getMotifFermeture());
				return null;
			}
		});

		// création des événements civils
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch correctionDivorce = new EvenementCivilEch();
				correctionDivorce.setId(evtCorrectionDivorceId);
				correctionDivorce.setAction(ActionEvenementCivilEch.CORRECTION);
				correctionDivorce.setDateEvenement(dateDivorce);
				correctionDivorce.setEtat(EtatEvenementCivil.A_TRAITER);
				correctionDivorce.setNumeroIndividu(noIndividu);
				correctionDivorce.setType(TypeEvenementCivilEch.DIVORCE);
				correctionDivorce.setRefMessageId(evtDivorceId);
				hibernateTemplate.merge(correctionDivorce);

				final EvenementCivilEch deces = new EvenementCivilEch();
				deces.setId(evtDecesId);
				deces.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				deces.setDateEvenement(dateDeces);
				deces.setEtat(EtatEvenementCivil.A_TRAITER);
				deces.setNumeroIndividu(noIndividu);
				deces.setType(TypeEvenementCivilEch.DECES);
				hibernateTemplate.merge(deces);

				return null;
			}
		});

		// traitement des événements reçus
		traiterEvenements(noIndividu);

		// résultat après traitement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch correctionDivorce = evtCivilDAO.get(evtCorrectionDivorceId);
				assertNotNull(correctionDivorce);
				assertEquals(EtatEvenementCivil.EN_ERREUR, correctionDivorce.getEtat());
				assertEquals("Les éléments suivants ont été modifiés par la correction : date de l'événement, état civil (dates).", correctionDivorce.getCommentaireTraitement());

				final EvenementCivilEch deces = evtCivilDAO.get(evtDecesId);
				assertNotNull(deces);
				assertEquals(EtatEvenementCivil.EN_ATTENTE, deces.getEtat());
				return null;
			}
		});

		// on force l'événement en erreur
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				evtCivilService.forceEvenement(evtCorrectionDivorceId);
				return null;
			}
		});

		// et on relance le traitement du seul événement qui reste : le décès
		traiterEvenements(noIndividu);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch deces = evtCivilDAO.get(evtDecesId);
				assertNotNull(deces);
				assertEquals(EtatEvenementCivil.A_VERIFIER, deces.getEtat());

				final Set<EvenementCivilEchErreur> erreurs = deces.getErreurs();
				assertNotNull(erreurs);
				assertEquals(1, erreurs.size());

				final EvenementCivilEchErreur warning = erreurs.iterator().next();
				assertNotNull(warning);
				assertEquals(TypeEvenementErreur.WARNING, warning.getType());
				final String expectedWarning = String.format("Il reste au moins un for fiscal ouvert sur le contribuable %s malgré la date de décès déjà renseignée sur la personne physique %s.",
				                                             FormatNumeroHelper.numeroCTBToDisplay(ppId), FormatNumeroHelper.numeroCTBToDisplay(ppId));
				assertEquals(expectedWarning, warning.getMessage());

				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNotNull(pp);
				assertFalse(pp.isHabitantVD());
				assertEquals(dateDeces, pp.getDateDeces());

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				assertNotNull(ffp);
				assertEquals(dateDivorceAvantCorrection, ffp.getDateDebut());
				assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffp.getMotifOuverture());

				// le for fiscal n'est pas fermé !!
				assertNull(ffp.getDateFin());
				assertNull(ffp.getMotifFermeture());

				return null;
			}
		});
	}

	/**
	 * SIFISC-8740 / UNIREG-2143
	 */
	@Test(timeout = 10000L)
	public void testDecesHorsCantonAvecAdresseCourrierVaudoise() throws Exception {

		final long noIndividuMonsieur = 437846327L;
		final long noIndividuMadame = 3743564L;
		final RegDate dateMariage = date(1995, 12, 2);
		final RegDate dateAchat = date(2000, 1, 3);
		final RegDate dateNaissances = date(1948, 9, 4);
		final RegDate dateDeces = date(2013, 3, 12);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu m = addIndividu(noIndividuMonsieur, dateNaissances, "Curie", "Pierre", Sexe.MASCULIN);
				final MockIndividu mme = addIndividu(noIndividuMadame, dateNaissances, "Curie", "Marie", Sexe.FEMININ);
				marieIndividus(m, mme, dateMariage);

				addAdresse(m, TypeAdresseCivil.COURRIER, MockRue.Vevey.RueDesMoulins, null, dateAchat, null);
				addAdresse(mme, TypeAdresseCivil.COURRIER, MockRue.Vevey.RueDesMoulins, null, dateAchat, null);

				addNationalite(m, MockPays.Suisse, dateNaissances, null);
				addNationalite(mme, MockPays.Suisse, dateNaissances, null);
			}
		});

		class Ids {
			long m;
			long mme;
			long mc;
		}

		// mise en place fiscale : résidents hors-canton avec for immeuble à Vevey
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique m = addHabitant(noIndividuMonsieur);
				final PersonnePhysique mme = addHabitant(noIndividuMadame);

				final EnsembleTiersCouple couple = addEnsembleTiersCouple(m, mme, dateMariage, null);
				final MenageCommun mc = couple.getMenage();

				addForPrincipal(mc, dateAchat, MotifFor.INDETERMINE, MockCommune.Sierre);
				addForSecondaire(mc, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Vevey.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

				final Ids ids = new Ids();
				ids.m = m.getNumero();
				ids.mme = mme.getNumero();
				ids.mc = mc.getNumero();
				return ids;
			}
		});

		// création d'un événement civil de décès sur Madame
		final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch deces = new EvenementCivilEch();
				deces.setId(45455L);
				deces.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				deces.setDateEvenement(dateDeces);
				deces.setEtat(EtatEvenementCivil.A_TRAITER);
				deces.setNumeroIndividu(noIndividuMadame);
				deces.setType(TypeEvenementCivilEch.DECES);
				return hibernateTemplate.merge(deces).getId();
			}
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividuMadame);

		// vérification de l'état final
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch ech = evtCivilDAO.get(evtId);
				assertNotNull(ech);
				assertEquals(EtatEvenementCivil.TRAITE, ech.getEtat());

				final PersonnePhysique m = (PersonnePhysique) tiersDAO.get(ids.m);
				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(m, dateMariage);
				assertNotNull(couple);

				final MenageCommun mc = couple.getMenage();
				assertNotNull(mc);

				final ForFiscalPrincipal ffpMc = mc.getDernierForFiscalPrincipal();
				assertNotNull(ffpMc);
				assertEquals(dateDeces, ffpMc.getDateFin());
				assertEquals(MotifFor.VEUVAGE_DECES, ffpMc.getMotifFermeture());

				final ForFiscalPrincipal ffpM = m.getDernierForFiscalPrincipal();
				assertNotNull(ffpM);
				assertEquals(dateDeces.getOneDayAfter(), ffpM.getDateDebut());
				assertEquals(MotifFor.VEUVAGE_DECES, ffpM.getMotifOuverture());

				// d'après SIFISC-8740, en l'absence d'adresse de domicile, on reprend le for du ménage
				assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ffpM.getTypeAutoriteFiscale());
				assertEquals((Integer) MockCommune.Sierre.getNoOFS(), ffpM.getNumeroOfsAutoriteFiscale());

				return null;
			}
		});
	}
}
