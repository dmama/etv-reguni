package ch.vd.uniregctb.evenement.civil.engine.ech;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class VeuvageEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {

	@Test(timeout = 10000L)
	public void testVeuvage() throws Exception {

		final long noMadame = 46215611L;
		final long noMonsieur = 78215611L;
		final RegDate dateMariage = date(2005, 5, 5);
		final RegDate dateVeuvage = date(2008, 11, 23);

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu monsieur = addIndividu(noMonsieur, date(1923, 2, 12), "duTonnerre", "Bouchon", true);
				addNationalite(monsieur, MockPays.Suisse, date(1923, 2, 12), null);
				addAdresse(monsieur, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, dateMariage, null);
				final MockIndividu madame = addIndividu(noMadame, date(1974, 8, 1), "Tulipia", "fleur", false);
				addNationalite(madame, MockPays.France, date(1974, 8, 1), null);
				addAdresse(madame, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, dateMariage, null);
				marieIndividus(monsieur, madame, dateMariage);
			}
		});

		final long mcId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique monsieur = addHabitant(noMonsieur);
				final PersonnePhysique madame = addHabitant(noMadame);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(monsieur, madame, dateMariage, null);
				addForPrincipal(ensemble.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bussigny);
				return ensemble.getMenage().getNumero();
			}
		});

		// décès fictif de monsieur juste pour le test
		doModificationIndividu(noMonsieur, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				individu.setDateDeces(dateVeuvage);
			}
		});

		// veuvage de madame pour le test
		doModificationIndividu(noMadame, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				MockServiceCivil.veuvifieIndividu(individu, dateVeuvage, false);
			}
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long veuvageId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(454563456L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateVeuvage);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noMadame);
				evt.setType(TypeEvenementCivilEch.CHGT_ETAT_CIVIL_PARTENAIRE);

				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement synchrone de l'événement
		traiterEvenements(noMadame);

		// on vérifie que le ménage-commun a bien été fermé suite au veuvage
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(veuvageId);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique madame = tiersService.getPersonnePhysiqueByNumeroIndividu(noMadame);
				assertNotNull(madame);
				final ForFiscalPrincipal ffpMadame = madame.getDernierForFiscalPrincipal();
				assertNotNull(ffpMadame);
				assertEquals(dateVeuvage.getOneDayAfter(), ffpMadame.getDateDebut());
				assertEquals(MotifFor.VEUVAGE_DECES, ffpMadame.getMotifOuverture());

				final AppartenanceMenage appartenanceMadame = (AppartenanceMenage) madame.getRapportSujetValidAt(dateVeuvage, TypeRapportEntreTiers.APPARTENANCE_MENAGE);
				assertNotNull(appartenanceMadame);
				assertEquals(dateMariage, appartenanceMadame.getDateDebut());
				assertEquals(dateVeuvage, appartenanceMadame.getDateFin());
				assertNull(tiersService.getEnsembleTiersCouple(madame, dateVeuvage.getOneDayAfter()));

				final MenageCommun mc = (MenageCommun) tiersDAO.get(mcId);
				assertNotNull(mc);
				final ForFiscalPrincipal ffpMenage = mc.getDernierForFiscalPrincipal();
				assertNotNull(ffpMenage);
				assertEquals(dateVeuvage, ffpMenage.getDateFin());
				assertEquals(MotifFor.VEUVAGE_DECES, ffpMenage.getMotifFermeture());
				return null;
			}
		});
	}

	@Test(timeout = 10000L)
	public void testVeuvageRedondant() throws Exception {

		final long noMadame = 46215611L;
		final long noMonsieur = 78215611L;
		final RegDate dateMariage = date(2005, 5, 5);
		final RegDate dateVeuvage = date(2008, 11, 23);

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu monsieur = addIndividu(noMonsieur, date(1923, 2, 12), "duTonnerre", "Bouchon", true);
				addNationalite(monsieur, MockPays.Suisse, date(1923, 2, 12), null);
				final MockIndividu madame = addIndividu(noMadame, date(1974, 8, 1), "Tulipia", "fleur", false);
				addNationalite(madame, MockPays.France, date(1974, 8, 1), null);
				marieIndividus(monsieur, madame, dateMariage);
			}


		});


		// décès fictif de monsieur juste pour le test
		doModificationIndividu(noMonsieur, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				individu.setDateDeces(dateVeuvage);
			}
		});

		// veuvage de madame pour le test
		doModificationIndividu(noMadame, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				MockServiceCivil.veuvifieIndividu(individu, dateVeuvage, false);
			}
		});

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique monsieur = addHabitant(noMonsieur);
				final PersonnePhysique madame = addHabitant(noMadame);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(monsieur, madame, dateMariage, dateVeuvage);
				addForPrincipal(ensemble.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateVeuvage, MotifFor.VEUVAGE_DECES, MockCommune.Bussigny);
				addForPrincipal(madame, dateVeuvage.getOneDayAfter(), MotifFor.VEUVAGE_DECES, null, null, MockCommune.ChateauDoex);
				return null;
			}
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long veuvageId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(454563456L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateVeuvage);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noMadame);
				evt.setType(TypeEvenementCivilEch.CHGT_ETAT_CIVIL_PARTENAIRE);

				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement synchrone de l'événement
		traiterEvenements(noMadame);

		// on vérifie que le ménage-commun a bien été fermé suite au veuvage
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(veuvageId);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.REDONDANT, evt.getEtat());
				return null;
			}
		});
	}

	@Test(timeout = 10000L)
	public void testVeuvagePartenaire() throws Exception {
		final long noPrincipal = 46215611L;
		final long noConjoint = 78215611L;
		final RegDate dateUnion = date(2005, 5, 5);
		final RegDate dateVeuvage = date(2008, 11, 23);

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu conjoint = addIndividu(noConjoint, date(1923, 2, 12), "duTonnerre", "Bouchonita", false);
				addNationalite(conjoint, MockPays.Suisse, date(1923, 2, 12), null);
				addAdresse(conjoint, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, dateUnion, null);
				final MockIndividu principal = addIndividu(noPrincipal, date(1974, 8, 1), "Tulipia", "fleur", false);
				addNationalite(principal, MockPays.France, date(1974, 8, 1), null);
				addAdresse(principal, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, dateUnion, null);
				marieIndividus(conjoint, principal, dateUnion);
			}
		});

		final long mcId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique monsieur = addHabitant(noConjoint);
				final PersonnePhysique madame = addHabitant(noPrincipal);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(monsieur, madame, dateUnion, null);
				addForPrincipal(ensemble.getMenage(), dateUnion, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bussigny);
				return ensemble.getMenage().getNumero();
			}
		});

		// décès fictif du conjoint juste pour le test
		doModificationIndividu(noConjoint, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				individu.setDateDeces(dateVeuvage);
			}
		});

		// veuvage du principal pour le test
		doModificationIndividu(noPrincipal, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				MockServiceCivil.veuvifieIndividu(individu, dateVeuvage, true);
			}
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long veuvageId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(454563456L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateVeuvage);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noPrincipal);
				evt.setType(TypeEvenementCivilEch.CHGT_ETAT_CIVIL_PARTENAIRE);

				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement synchrone de l'événement
		traiterEvenements(noPrincipal);

		// on vérifie que le ménage-commun a bien été fermé suite au veuvage
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(veuvageId);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique survivant = tiersService.getPersonnePhysiqueByNumeroIndividu(noPrincipal);
				assertNotNull(survivant);
				final ForFiscalPrincipal ffpSurvivant = survivant.getDernierForFiscalPrincipal();
				assertNotNull(ffpSurvivant);
				assertEquals(dateVeuvage.getOneDayAfter(), ffpSurvivant.getDateDebut());
				assertEquals(MotifFor.VEUVAGE_DECES, ffpSurvivant.getMotifOuverture());

				final AppartenanceMenage appartenanceMadame = (AppartenanceMenage) survivant.getRapportSujetValidAt(dateVeuvage, TypeRapportEntreTiers.APPARTENANCE_MENAGE);
				assertNotNull(appartenanceMadame);
				assertEquals(dateUnion, appartenanceMadame.getDateDebut());
				assertEquals(dateVeuvage, appartenanceMadame.getDateFin());
				assertNull(tiersService.getEnsembleTiersCouple(survivant, dateVeuvage.getOneDayAfter()));

				final MenageCommun mc = (MenageCommun) tiersDAO.get(mcId);
				assertNotNull(mc);
				final ForFiscalPrincipal ffpMenage = mc.getDernierForFiscalPrincipal();
				assertNotNull(ffpMenage);
				assertEquals(dateVeuvage, ffpMenage.getDateFin());
				assertEquals(MotifFor.VEUVAGE_DECES, ffpMenage.getMotifFermeture());
				return null;
			}
		});
	}
}
