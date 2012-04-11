package ch.vd.uniregctb.evenement.civil.engine.ech;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class DonneesUpiEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {

	@Test(timeout = 10000L)
	public void testAttributionUpi() throws Exception {
		
		final long noIndividu = 4367834253L;
		final String avsAssigne = "7567839088263";

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, null, "Clette", "Lara", false);
				individu.setNouveauNoAVS(avsAssigne);
			}
		});
		
		// mise en place fiscale
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Lara", "Clette", null, Sexe.FEMININ);
				pp.setNumeroIndividu(noIndividu);
				Assert.assertNull(pp.getNumeroAssureSocial());
				return null;
			}
		});
		
		// création de l'événement d'attribution de données UPI
		final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(432627827L);
				evt.setNumeroIndividu(noIndividu);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setType(TypeEvenementCivilEch.ATTRIBUTION_DONNEES_UPI);
				evt.setDateEvenement(RegDate.get());
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification que le traitement s'est bien passé
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtId);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				Assert.assertNotNull(pp);
				Assert.assertEquals(avsAssigne, pp.getNumeroAssureSocial());
				return null;
			}
		});
	}

	@Test(timeout = 10000L)
	public void testCorrectionUpiAvecAvsVideSurNonHabitantQuiEnADejaUn() throws Exception {

		final long noIndividu = 4367834253L;
		final String avsAssigne = "7567839088263";

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, null, "Clette", "Lara", false);
				individu.setNouveauNoAVS(null);
			}
		});

		// mise en place fiscale
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Lara", "Clette", null, Sexe.FEMININ);
				pp.setNumeroIndividu(noIndividu);
				pp.setNumeroAssureSocial(avsAssigne);
				return null;
			}
		});

		// création de l'événement de correction de données UPI
		final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(432627827L);
				evt.setNumeroIndividu(noIndividu);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setType(TypeEvenementCivilEch.CORR_DONNEES_UPI);
				evt.setDateEvenement(RegDate.get());
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification que le traitement s'est bien passé
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtId);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				Assert.assertNotNull(pp);
				Assert.assertEquals(avsAssigne, pp.getNumeroAssureSocial());
				return null;
			}
		});
	}

	@Test(timeout = 10000L)
	public void testCorrectionUpiSurNonHabitantQuiEnADejaUn() throws Exception {

		final long noIndividu = 4367834253L;
		final String avsAssigneCivil = "7561330208557";
		final String avsAssigneFiscal = "7567839088263";

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, null, "Clette", "Lara", false);
				individu.setNouveauNoAVS(avsAssigneCivil);
			}
		});

		// mise en place fiscale
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Lara", "Clette", null, Sexe.FEMININ);
				pp.setNumeroIndividu(noIndividu);
				pp.setNumeroAssureSocial(avsAssigneFiscal);
				return null;
			}
		});

		// création de l'événement de correction de données UPI
		final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(432627827L);
				evt.setNumeroIndividu(noIndividu);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setType(TypeEvenementCivilEch.CORR_DONNEES_UPI);
				evt.setDateEvenement(RegDate.get());
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification que le traitement s'est bien passé
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtId);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				Assert.assertNotNull(pp);
				Assert.assertEquals(avsAssigneCivil, pp.getNumeroAssureSocial());
				return null;
			}
		});
	}

	@Test(timeout = 10000L)
	public void testAnnulationUpiAvecAvsVideSurNonHabitantQuiEnADejaUn() throws Exception {

		final long noIndividu = 4367834253L;
		final String avsAssigne = "7567839088263";

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, null, "Clette", "Lara", false);
				individu.setNouveauNoAVS(null);
			}
		});

		// mise en place fiscale
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Lara", "Clette", null, Sexe.FEMININ);
				pp.setNumeroIndividu(noIndividu);
				pp.setNumeroAssureSocial(avsAssigne);
				return null;
			}
		});

		// création de l'événement d'annulation de données UPI
		final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(432627827L);
				evt.setNumeroIndividu(noIndividu);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setType(TypeEvenementCivilEch.ANNULATION_DONNEES_UPI);
				evt.setDateEvenement(RegDate.get());
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification que le traitement s'est bien passé
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtId);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				Assert.assertNotNull(pp);
				Assert.assertNull(pp.getNumeroAssureSocial());
				return null;
			}
		});
	}
}
