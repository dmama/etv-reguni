package ch.vd.uniregctb.evenement.civil.engine.ech;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class EnregistrementPartenariatEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {

	@Test(timeout = 10000L)
	public void testEnregistrementPartenariat() throws Exception {

		final long noPrincipal = 78215611L;
		final long noConjoint = 46215611L;
		final RegDate dateEnregistrement = RegDate.get().addMonths(-1);

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu principal = addIndividu(noPrincipal, date(1923, 2, 12), "Crispus", "Santacorpus", true);
				MockIndividu conjoint = addIndividu(noConjoint, date(1974, 8, 1), "David", "Bouton", true);
				marieIndividus(principal, conjoint, dateEnregistrement);
			}
		});

		doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique monsieur = addHabitant(noPrincipal);
				addForPrincipal(monsieur, date(1943, 2, 12), MotifFor.MAJORITE, MockCommune.Echallens);
				PersonnePhysique madame = addHabitant(noConjoint);
				addForPrincipal(madame, date(1992, 8, 1), MotifFor.MAJORITE, MockCommune.Chamblon);
				return null;
			}
		});

		// événement civil
		final long eventId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(1235563456L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateEnregistrement);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noPrincipal);
				evt.setType(TypeEvenementCivilEch.ENREGISTREMENT_PARTENARIAT);

				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement synchrone de l'événement
		traiterEvenements(noPrincipal);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(eventId);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique principal = tiersService.getPersonnePhysiqueByNumeroIndividu(noPrincipal);
				assertNotNull(principal);

				final PersonnePhysique conjoint = tiersService.getPersonnePhysiqueByNumeroIndividu(noConjoint);
				assertNotNull(conjoint);

				final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(conjoint, dateEnregistrement);
				assertNotNull(ensemble);
				assertSame(principal, ensemble.getPrincipal());
				assertSame(conjoint, ensemble.getConjoint());
				return null;
			}
		});
	}
}
