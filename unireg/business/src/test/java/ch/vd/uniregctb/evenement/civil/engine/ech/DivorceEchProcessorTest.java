package ch.vd.uniregctb.evenement.civil.engine.ech;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DivorceEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {

	@Test(timeout = 10000L)
	public void testDivorce() throws Exception {

		final long noMadame = 46215611L;
		final long noMonsieur = 78215611L;
		final RegDate dateMariage = date(2005, 5, 5);
		final RegDate dateDivorce = date(2008, 11, 23);

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu monsieur = addIndividu(noMonsieur, date(1923, 2, 12), "Crispus", "Santacorpus", true);
				addNationalite(monsieur, MockPays.Suisse, date(1923, 2, 12), null);
				final MockIndividu madame = addIndividu(noMadame, date(1974, 8, 1), "Lisette", "Bouton", false);
				addNationalite(madame, MockPays.France, date(1974, 8, 1), null);
				marieIndividus(monsieur, madame, dateMariage);
				divorceIndividus(monsieur, madame, dateDivorce);
			}
		});

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique monsieur = addHabitant(noMonsieur);
				final PersonnePhysique madame = addHabitant(noMadame);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(monsieur, madame, dateMariage, null);
				addForPrincipal(ensemble.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
				return null;
			}
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long divorceId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(454563456L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateDivorce);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noMonsieur);
				evt.setType(TypeEvenementCivilEch.DIVORCE);

				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement synchrone de l'événement
		traiterEvenement(noMonsieur, divorceId);

		// on vérifie que le ménage-commun a bien été divorcé dans le fiscal
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(divorceId);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique monsieur = tiersService.getPersonnePhysiqueByNumeroIndividu(noMonsieur);
				assertNotNull(monsieur);

				final AppartenanceMenage appartenanceMonsieur = (AppartenanceMenage) monsieur.getRapportSujetValidAt(dateDivorce.getOneDayBefore(), TypeRapportEntreTiers.APPARTENANCE_MENAGE);
				assertNotNull(appartenanceMonsieur);
				assertEquals(dateMariage, appartenanceMonsieur.getDateDebut());
				assertEquals(dateDivorce.getOneDayBefore(), appartenanceMonsieur.getDateFin());

				final PersonnePhysique madame = tiersService.getPersonnePhysiqueByNumeroIndividu(noMadame);
				assertNotNull(madame);

				final AppartenanceMenage appartenanceMadame = (AppartenanceMenage) madame.getRapportSujetValidAt(dateDivorce.getOneDayBefore(), TypeRapportEntreTiers.APPARTENANCE_MENAGE);
				assertNotNull(appartenanceMadame);
				assertEquals(dateMariage, appartenanceMadame.getDateDebut());
				assertEquals(dateDivorce.getOneDayBefore(), appartenanceMadame.getDateFin());

				assertNull(tiersService.getEnsembleTiersCouple(madame, dateDivorce));
				return null;
			}
		});
	}
}
