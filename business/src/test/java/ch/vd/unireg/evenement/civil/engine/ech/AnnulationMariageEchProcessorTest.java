package ch.vd.unireg.evenement.civil.engine.ech;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessTestingConstants;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEch;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeEvenementCivilEch;

@ContextConfiguration(locations = {
		BusinessTestingConstants.UNIREG_BUSINESS_UT_TACHES
})
public class AnnulationMariageEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		// nécessaire pour le calcul des tâches d'envoi de DI
		doInNewTransactionAndSession(status -> {
			for (int i = 2003; i < RegDate.get().year(); ++i) {
				addPeriodeFiscale(i);
			}
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testAnnulationMariage() throws Exception {

		final long noIndividuLui = 36712456523468L;
		final long noIndividuElle = 34674853272545L;
		final RegDate dateMariage = date(2012, 2, 10);
		final RegDate dateOuvertureFor2 = date(2000, 1, 1);
		final RegDate dateOuvertureFor1 = date(1995, 1, 1);

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final RegDate dateNaissanceLui = date(1960, 1, 26);
				final MockIndividu lui = addIndividu(noIndividuLui, dateNaissanceLui, "Casanova", "Paco", true);
				addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, dateNaissanceLui, null);
				addNationalite(lui, MockPays.Suisse, dateNaissanceLui, null);

				final RegDate dateNaissanceElle = date(1980, 6, 12);
				final MockIndividu elle = addIndividu(noIndividuElle, dateNaissanceElle, "Nette", "Jeu", false);
				addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateNaissanceElle, null);
				addNationalite(elle, MockPays.Suisse, dateNaissanceElle, null);
			}
		});

		// mise en place fiscale avec mariage
		final long mcId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique lui = addHabitant(noIndividuLui);

			addForPrincipal(lui, date(1990, 1, 1), MotifFor.INDETERMINE, dateOuvertureFor1.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, MockCommune.Morges);


			addForPrincipal(lui, dateOuvertureFor1, MotifFor.DEMENAGEMENT_VD, dateOuvertureFor2.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, MockCommune.VufflensLaVille);

			addForPrincipal(lui, dateOuvertureFor2, MotifFor.DEMENAGEMENT_VD, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);

			final PersonnePhysique elle = addHabitant(noIndividuElle);
			addForPrincipal(elle, date(2001, 4, 12), MotifFor.INDETERMINE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);

			final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, null);
			final MenageCommun mc = couple.getMenage();
			addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
			return mc.getNumero();
		});

		// création de l'événement civil d'annulation de mariage
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(3478256623526867L);
			evt.setType(TypeEvenementCivilEch.MARIAGE);
			evt.setAction(ActionEvenementCivilEch.ANNULATION);
			evt.setDateEvenement(dateMariage);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividuLui);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividuLui);

		// vérification du résultat
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

			final MenageCommun mc = (MenageCommun) tiersDAO.get(mcId);
			Assert.assertNotNull(mc);

			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			Assert.assertNull(ffp);
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testAnnulationMariageAvecRedondance() throws Exception {

		final long noIndividuLui = 36712456523468L;
		final long noIndividuElle = 34674853272545L;
		final RegDate dateMariage = date(2012, 2, 10);

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final RegDate dateNaissanceLui = date(1960, 1, 26);
				final MockIndividu lui = addIndividu(noIndividuLui, dateNaissanceLui, "Casanova", "Paco", true);
				addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, dateNaissanceLui, null);
				addNationalite(lui, MockPays.Suisse, dateNaissanceLui, null);

				final RegDate dateNaissanceElle = date(1980, 6, 12);
				final MockIndividu elle = addIndividu(noIndividuElle, dateNaissanceElle, "Nette", "Jeu", false);
				addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateNaissanceElle, null);
				addNationalite(elle, MockPays.Suisse, dateNaissanceElle, null);
			}
		});

		// mise en place fiscale avec mariage
		final long mcId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique lui = addHabitant(noIndividuLui);
			addForPrincipal(lui, date(2000, 1, 1), MotifFor.INDETERMINE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);

			final PersonnePhysique elle = addHabitant(noIndividuElle);
			addForPrincipal(elle, date(2001, 4, 12), MotifFor.INDETERMINE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);

			final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, null);
			final MenageCommun mc = couple.getMenage();
			addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
			return mc.getNumero();
		});

		// création de l'événement civil d'annulation de mariage
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(3478251253526867L);
			evt.setType(TypeEvenementCivilEch.MARIAGE);
			evt.setAction(ActionEvenementCivilEch.ANNULATION);
			evt.setDateEvenement(dateMariage);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividuLui);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividuLui);

		// vérification du résultat
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

			final MenageCommun mc = (MenageCommun) tiersDAO.get(mcId);
			Assert.assertNotNull(mc);

			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			Assert.assertNull(ffp);
			return null;
		});

		//Event d'annulation de mariage pour madame
		// création de l'événement civil d'annulation de mariage
		final long evtIdMadame = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evtMadame = new EvenementCivilEch();
			evtMadame.setId(3478256623526867L);
			evtMadame.setType(TypeEvenementCivilEch.MARIAGE);
			evtMadame.setAction(ActionEvenementCivilEch.ANNULATION);
			evtMadame.setDateEvenement(dateMariage);
			evtMadame.setEtat(EtatEvenementCivil.A_TRAITER);
			evtMadame.setNumeroIndividu(noIndividuElle);
			return hibernateTemplate.merge(evtMadame).getId();
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividuElle);

		// vérification du résultat
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtIdMadame);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.REDONDANT, evt.getEtat());

			final MenageCommun mc = (MenageCommun) tiersDAO.get(mcId);
			Assert.assertNotNull(mc);

			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			Assert.assertNull(ffp);
			return null;
		});
	}
}

