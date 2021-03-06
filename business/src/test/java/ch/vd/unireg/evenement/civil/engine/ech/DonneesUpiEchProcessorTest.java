package ch.vd.unireg.evenement.civil.engine.ech;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEch;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeEvenementCivilEch;

public class DonneesUpiEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {

	@Test(timeout = 10000L)
	public void testAttributionUpi() throws Exception {

		final long noIndividu = 4367834253L;
		final String avsAssigne = "7567839088263";

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, null, "Clette", "Lara", false);
				individu.setNouveauNoAVS(avsAssigne);
			}
		});

		// mise en place fiscale
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Lara", "Clette", null, Sexe.FEMININ);
			pp.setNumeroIndividu(noIndividu);
			Assert.assertNull(pp.getNumeroAssureSocial());
			return null;
		});

		// création de l'événement d'attribution de données UPI
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(432627827L);
			evt.setNumeroIndividu(noIndividu);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setType(TypeEvenementCivilEch.ATTRIBUTION_DONNEES_UPI);
			evt.setDateEvenement(RegDate.get());
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification que le traitement s'est bien passé
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

			final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
			Assert.assertNotNull(pp);
			Assert.assertEquals(avsAssigne, pp.getNumeroAssureSocial());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testCorrectionUpiAvecAvsVideSurNonHabitantQuiEnADejaUn() throws Exception {

		final long noIndividu = 4367834253L;
		final String avsAssigne = "7567839088263";

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, null, "Clette", "Lara", false);
				individu.setNouveauNoAVS(null);
			}
		});

		// mise en place fiscale
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Lara", "Clette", null, Sexe.FEMININ);
			pp.setNumeroIndividu(noIndividu);
			pp.setNumeroAssureSocial(avsAssigne);
			return null;
		});

		// création de l'événement de correction de données UPI
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(432627827L);
			evt.setNumeroIndividu(noIndividu);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setType(TypeEvenementCivilEch.CORR_DONNEES_UPI);
			evt.setDateEvenement(RegDate.get());
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification que le traitement s'est bien passé
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

			final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
			Assert.assertNotNull(pp);
			Assert.assertEquals(avsAssigne, pp.getNumeroAssureSocial());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testCorrectionUpiSurNonHabitantQuiEnADejaUn() throws Exception {

		final long noIndividu = 4367834253L;
		final String avsAssigneCivil = "7561330208557";
		final String avsAssigneFiscal = "7567839088263";

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, null, "Clette", "Lara", false);
				individu.setNouveauNoAVS(avsAssigneCivil);
			}
		});

		// mise en place fiscale
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Lara", "Clette", null, Sexe.FEMININ);
			pp.setNumeroIndividu(noIndividu);
			pp.setNumeroAssureSocial(avsAssigneFiscal);
			return null;
		});

		// création de l'événement de correction de données UPI
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(432627827L);
			evt.setNumeroIndividu(noIndividu);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setType(TypeEvenementCivilEch.CORR_DONNEES_UPI);
			evt.setDateEvenement(RegDate.get());
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification que le traitement s'est bien passé
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

			final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
			Assert.assertNotNull(pp);
			Assert.assertEquals(avsAssigneCivil, pp.getNumeroAssureSocial());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testAnnulationUpiAvecAvsVideSurNonHabitantQuiEnADejaUn() throws Exception {

		final long noIndividu = 4367834253L;
		final String avsAssigne = "7567839088263";

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, null, "Clette", "Lara", false);
				individu.setNouveauNoAVS(null);
			}
		});

		// mise en place fiscale
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Lara", "Clette", null, Sexe.FEMININ);
			pp.setNumeroIndividu(noIndividu);
			pp.setNumeroAssureSocial(avsAssigne);
			return null;
		});

		// création de l'événement d'annulation de données UPI
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(432627827L);
			evt.setNumeroIndividu(noIndividu);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setType(TypeEvenementCivilEch.ANNULATION_DONNEES_UPI);
			evt.setDateEvenement(RegDate.get());
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification que le traitement s'est bien passé
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

			final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
			Assert.assertNotNull(pp);
			Assert.assertNull(pp.getNumeroAssureSocial());
			return null;
		});
	}
}
