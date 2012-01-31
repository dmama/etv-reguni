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

public class DissolutionPartenariatEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {

	@Test(timeout = 10000L)
	public void testDissolutionPartenariat() throws Exception {

		final long noPrincipal = 78215611L;
		final long noConjoint = 46215611L;
		final RegDate dateEnregistrement = date(2005, 5, 5);
		final RegDate dateDissolution = date(2008, 11, 23);

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu principal = addIndividu(noPrincipal, date(1923, 2, 12), "Crispus", "Santacorpus", true);
				addNationalite(principal, MockPays.Suisse, date(1923, 2, 12), null);
				final MockIndividu conjoint = addIndividu(noConjoint, date(1974, 8, 1), "David", "Bouton", true);
				addNationalite(conjoint, MockPays.France, date(1974, 8, 1), null);
				marieIndividus(principal, conjoint, dateEnregistrement);
				divorceIndividus(principal, conjoint, dateDissolution);
			}
		});

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique principal = addHabitant(noPrincipal);
				final PersonnePhysique conjoint = addHabitant(noConjoint);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(principal, conjoint, dateEnregistrement, null);
				addForPrincipal(ensemble.getMenage(), dateEnregistrement, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
				return null;
			}
		});

		// événement civil
		final long eventId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(454563456L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateDissolution);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noPrincipal);
				evt.setType(TypeEvenementCivilEch.DISSOLUTION_PARTENARIAT);

				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement synchrone de l'événement
		traiterEvenement(noPrincipal, eventId);

		// on vérifie que le ménage-commun a bien été dissolu dans le fiscal
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(eventId);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique principal = tiersService.getPersonnePhysiqueByNumeroIndividu(noPrincipal);
				assertNotNull(principal);

				final AppartenanceMenage appartenancePrincipal = (AppartenanceMenage) principal.getRapportSujetValidAt(dateDissolution.getOneDayBefore(), TypeRapportEntreTiers.APPARTENANCE_MENAGE);
				assertNotNull(appartenancePrincipal);
				assertEquals(dateEnregistrement, appartenancePrincipal.getDateDebut());
				assertEquals(dateDissolution.getOneDayBefore(), appartenancePrincipal.getDateFin());

				final PersonnePhysique conjoint = tiersService.getPersonnePhysiqueByNumeroIndividu(noConjoint);
				assertNotNull(conjoint);

				final AppartenanceMenage appartenanceConjoint = (AppartenanceMenage) conjoint.getRapportSujetValidAt(dateDissolution.getOneDayBefore(), TypeRapportEntreTiers.APPARTENANCE_MENAGE);
				assertNotNull(appartenanceConjoint);
				assertEquals(dateEnregistrement, appartenanceConjoint.getDateDebut());
				assertEquals(dateDissolution.getOneDayBefore(), appartenanceConjoint.getDateFin());

				assertNull(tiersService.getEnsembleTiersCouple(conjoint, dateDissolution));
				return null;
			}
		});
	}
}
