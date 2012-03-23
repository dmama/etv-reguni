package ch.vd.uniregctb.evenement.civil.engine.ech;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

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

		doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique monsieur = addHabitant(noMonsieur);
				addForPrincipal(monsieur, date(1943, 2, 12), MotifFor.MAJORITE, MockCommune.Echallens);
				PersonnePhysique madame = addHabitant(noMadame);
				addForPrincipal(madame, date(1992, 8, 1), MotifFor.MAJORITE, MockCommune.Chamblon);
				return null;
			}
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long mariageId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(1235563456L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateMariage);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noMonsieur);
				evt.setType(TypeEvenementCivilEch.MARIAGE);

				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement synchrone de l'événement
		traiterEvenements(noMonsieur);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
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
			}
		});
	}

//	@Test(timeout = 10000L)
	@Test
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

		doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique monsieur = addHabitant(noMonsieur);
				addForPrincipal(monsieur, date(1943, 2, 12), MotifFor.MAJORITE, MockCommune.Echallens);
				PersonnePhysique madame = addHabitant(noMadame);
				addForPrincipal(madame, date(1992, 8, 1), MotifFor.MAJORITE, MockCommune.Chamblon);
				return null;
			}
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long mariageMonsieurId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(1235563456L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateMariage);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noMonsieur);
				evt.setType(TypeEvenementCivilEch.MARIAGE);

				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement synchrone de l'événement
		traiterEvenements(noMonsieur);

		// vérification de la création d'un marié seul
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(mariageMonsieurId);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique monsieur = tiersService.getPersonnePhysiqueByNumeroIndividu(noMonsieur);
				assertNotNull(monsieur);

				final PersonnePhysique madame = tiersService.getPersonnePhysiqueByNumeroIndividu(noMadame);
				assertNotNull(madame);

				final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(monsieur, dateMariage);
				assertNotNull(ensemble);
				assertSame(monsieur, ensemble.getPrincipal());
				assertNull(ensemble.getConjoint());
				return null;
			}
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
		final long mariageMadameId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(34256724756L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateMariage);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noMadame);
				evt.setType(TypeEvenementCivilEch.MARIAGE);

				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement synchrone de l'événement
		traiterEvenements(noMadame);

		// vérification de la reconstitution du ménage complet
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(mariageMadameId);
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
			}
		});
	}
}
