package ch.vd.uniregctb.evenement.civil.engine.ech;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class AnnulationArriveeEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {
	
	@Test(timeout = 10000L)
	public void testAnnulationArriveeDeMineur() throws Exception {
		
		final long noIndividu = 2378435L;
		final RegDate dateNaissance = RegDate.get().addYears(-15);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, dateNaissance, "Poucet", "Lepeti", true);
			}
		});
		
		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				return pp.getNumero();
			}
		});
		
		// création de l'événement civil d'annulation d'arrivée
		final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(67485482L);
				evt.setDateEvenement(RegDate.get().getOneDayBefore());
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				evt.setAction(ActionEvenementCivilEch.ANNULATION);
				evt.setNumeroIndividu(noIndividu);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtId);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
				return null;
			}
		});
	}
}
