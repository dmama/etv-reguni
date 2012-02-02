package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.Set;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchErreur;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class CessationSeparationEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {

	@Test(timeout = 10000L)
	public void testCessationSeparationAvecRedondance() throws Exception {

		final long noMadame = 46215611L;
		final long noMonsieur = 78215611L;
		final RegDate dateMariage = RegDate.get().addYears(-1);
		final RegDate dateSeparation = dateMariage.addMonths(8);
		final RegDate dateReconciliation = dateSeparation.addMonths(2);

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu monsieur = addIndividu(noMonsieur, date(1923, 2, 12), "Crispus", "Santacorpus", true);
				MockIndividu madame = addIndividu(noMadame, date(1974, 8, 1), "Lisette", "Bouton", false);
				marieIndividus(monsieur, madame, dateMariage);
				separeIndividus(monsieur, madame, dateSeparation);
				reconcilieIndividus(monsieur, madame, dateReconciliation);
			}
		});

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final RegDate veilleMariage = dateMariage.getOneDayBefore();
				final RegDate veilleSeparation = dateSeparation.getOneDayBefore();

				final PersonnePhysique monsieur = addHabitant(noMonsieur);
				addForPrincipal(monsieur, date(1943, 2, 12), MotifFor.MAJORITE, veilleMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
				final PersonnePhysique madame = addHabitant(noMadame);
				addForPrincipal(madame, date(1992, 8, 1), MotifFor.MAJORITE, veilleMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Chamblon);

				final EnsembleTiersCouple couple = addEnsembleTiersCouple(monsieur, madame, dateMariage, veilleSeparation);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, veilleSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Echallens);
				
				addForPrincipal(monsieur, dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Echallens);
				addForPrincipal(madame, dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Chamblon);
				return null;
			}
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long evtIdMonsieur = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(1235563456L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateReconciliation);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noMonsieur);
				evt.setType(TypeEvenementCivilEch.CESSATION_SEPARATION);

				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement synchrone de l'événement
		traiterEvenement(noMonsieur, evtIdMonsieur);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtIdMonsieur);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique monsieur = tiersService.getPersonnePhysiqueByNumeroIndividu(noMonsieur);
				assertNotNull(monsieur);

				final PersonnePhysique madame = tiersService.getPersonnePhysiqueByNumeroIndividu(noMadame);
				assertNotNull(madame);

				final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(madame, dateReconciliation);
				assertNotNull(ensemble);
				assertSame(monsieur, ensemble.getPrincipal());
				assertSame(madame, ensemble.getConjoint());
				
				final ForFiscalPrincipal forMc = ensemble.getMenage().getDernierForFiscalPrincipal();
				assertNotNull(forMc);
				assertEquals(dateReconciliation, forMc.getDateDebut());
				assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, forMc.getMotifOuverture());
				return null;
			}
		});

		// événement civil pour Madame (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long evtIdMadame = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(12355634532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateReconciliation);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noMadame);
				evt.setType(TypeEvenementCivilEch.CESSATION_SEPARATION);

				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement synchrone de l'événement
		traiterEvenement(noMadame, evtIdMadame);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtIdMadame);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.REDONDANT, evt.getEtat());

				final PersonnePhysique monsieur = tiersService.getPersonnePhysiqueByNumeroIndividu(noMonsieur);
				assertNotNull(monsieur);

				final PersonnePhysique madame = tiersService.getPersonnePhysiqueByNumeroIndividu(noMadame);
				assertNotNull(madame);

				final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(madame, dateReconciliation);
				assertNotNull(ensemble);
				assertSame(monsieur, ensemble.getPrincipal());
				assertSame(madame, ensemble.getConjoint());

				final ForFiscalPrincipal forMc = ensemble.getMenage().getDernierForFiscalPrincipal();
				assertNotNull(forMc);
				assertEquals(dateReconciliation, forMc.getDateDebut());
				assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, forMc.getMotifOuverture());
				return null;
			}
		});
	}

	@Test(timeout = 10000L)
	public void testCessationSeparationAvecFausseRedondance() throws Exception {

		final long noMadame = 46215611L;
		final long noMonsieur = 78215611L;
		final RegDate dateMariage = RegDate.get().addYears(-1);
		final RegDate dateSeparation = dateMariage.addMonths(8);
		final RegDate dateReconciliation = dateSeparation.addMonths(2);

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final RegDate dateNaissanceMonsieur = date(1923, 2, 12);
				final RegDate dateNaissanceMadame = date(1974, 8, 1);
				MockIndividu monsieur = addIndividu(noMonsieur, dateNaissanceMonsieur, "Crispus", "Santacorpus", true);
				MockIndividu madame = addIndividu(noMadame, dateNaissanceMadame, "Lisette", "Bouton", false);
				addNationalite(monsieur, MockPays.Suisse, dateNaissanceMonsieur, null);
				addNationalite(madame, MockPays.Suisse, dateNaissanceMadame, null);
				marieIndividus(monsieur, madame, dateMariage);
				separeIndividus(monsieur, madame, dateSeparation);
				reconcilieIndividus(monsieur, madame, dateReconciliation);
			}
		});

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final RegDate veilleMariage = dateMariage.getOneDayBefore();
				final RegDate veilleSeparation = dateSeparation.getOneDayBefore();
				final RegDate veilleReconciliation = dateReconciliation.getOneDayBefore();

				final PersonnePhysique monsieur = addHabitant(noMonsieur);
				addForPrincipal(monsieur, date(1943, 2, 12), MotifFor.MAJORITE, veilleMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
				final PersonnePhysique madame = addHabitant(noMadame);
				addForPrincipal(madame, date(1992, 8, 1), MotifFor.MAJORITE, veilleMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Chamblon);

				final EnsembleTiersCouple couple = addEnsembleTiersCouple(monsieur, madame, dateMariage, veilleSeparation);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, veilleSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Echallens);
				
				addForPrincipal(monsieur, dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, veilleReconciliation, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
				addForPrincipal(madame, dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, veilleReconciliation, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Chamblon);

				addAppartenanceMenage(mc, monsieur, dateReconciliation, null, false);
				addAppartenanceMenage(mc, madame, dateReconciliation, null, false);
				addForPrincipal(mc, dateReconciliation, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
				return null;
			}
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long evtIdMonsieur = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(1235563456L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateReconciliation.getOneDayAfter());      // problème, ce n'est pas la date que l'on connait
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noMonsieur);
				evt.setType(TypeEvenementCivilEch.CESSATION_SEPARATION);

				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement synchrone de l'événement
		traiterEvenement(noMonsieur, evtIdMonsieur);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				// vérification du traitement en erreur
				final EvenementCivilEch evt = evtCivilDAO.get(evtIdMonsieur);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
				
				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				assertNotNull(erreurs);
				assertEquals(1, erreurs.size());
				
				final EvenementCivilEchErreur erreur = erreurs.iterator().next();
				assertNotNull(erreur);
				assertEquals(String.format("Le couple n'est pas séparé en date du %s", RegDateHelper.dateToDisplayString(evt.getDateEvenement())), erreur.getMessage());
				
				// vérification que rien n'a changé autrement

				final PersonnePhysique monsieur = tiersService.getPersonnePhysiqueByNumeroIndividu(noMonsieur);
				assertNotNull(monsieur);

				final PersonnePhysique madame = tiersService.getPersonnePhysiqueByNumeroIndividu(noMadame);
				assertNotNull(madame);

				final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(madame, dateReconciliation);
				assertNotNull(ensemble);
				assertSame(monsieur, ensemble.getPrincipal());
				assertSame(madame, ensemble.getConjoint());
				
				final ForFiscalPrincipal forMc = ensemble.getMenage().getDernierForFiscalPrincipal();
				assertNotNull(forMc);
				assertEquals(dateReconciliation, forMc.getDateDebut());
				assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, forMc.getMotifOuverture());
				return null;
			}
		});
	}
}
