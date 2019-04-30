package ch.vd.unireg.evenement.civil.engine.ech;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEch;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
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

public class AnnulationReconciliationEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {
	
	@Test(timeout = 10000L)
	public void testAnnulationReconciliation() throws Exception {

		final long noIndividuLui = 36712456523468L;
		final long noIndividuElle = 34674853272545L;
		final RegDate dateMariage = date(2010, 2, 10);
		final RegDate dateSeparation = date(2011, 12, 1);
		final RegDate dateReconcialiation = date(2012, 2, 10);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
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

				marieIndividus(lui, elle, dateMariage);
				separeIndividus(lui, elle, dateSeparation);
			}
		});

		// mise en place fiscale avec mariage, séparation et réconciliation
		final long mcId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique lui = addHabitant(noIndividuLui);
			addForPrincipal(lui, date(2000, 1, 1), MotifFor.INDETERMINE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
			addForPrincipal(lui, dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, dateReconcialiation.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);

			final PersonnePhysique elle = addHabitant(noIndividuElle);
			addForPrincipal(elle, date(2001, 4, 12), MotifFor.INDETERMINE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
			addForPrincipal(elle, dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, dateReconcialiation.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);

			final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, dateSeparation.getOneDayBefore());
			final MenageCommun mc = couple.getMenage();
			addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateSeparation.getOneDayBefore(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne);

			addAppartenanceMenage(mc, lui, dateReconcialiation, null, false);
			addAppartenanceMenage(mc, elle, dateReconcialiation, null, false);
			addForPrincipal(mc, dateReconcialiation, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
			return mc.getNumero();
		});

		// création de l'événement civil d'annulation de mariage
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(3478256623526867L);
			evt.setType(TypeEvenementCivilEch.CESSATION_SEPARATION);
			evt.setAction(ActionEvenementCivilEch.ANNULATION);
			evt.setDateEvenement(dateReconcialiation);
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
			Assert.assertNotNull(ffp);
			Assert.assertEquals(dateSeparation.getOneDayBefore(), ffp.getDateFin());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testAnnulationReconciliationRedondante() throws Exception {

		final long noIndividuLui = 36712456523468L;
		final long noIndividuElle = 34674853272545L;
		final RegDate dateMariage = date(2010, 2, 10);
		final RegDate dateSeparation = date(2011, 12, 1);
		final RegDate dateReconcialiation = date(2012, 2, 10);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
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

				marieIndividus(lui, elle, dateMariage);
				separeIndividus(lui, elle, dateSeparation);
			}
		});

		// mise en place fiscale avec mariage, séparation et réconciliation
		final long mcId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique lui = addHabitant(noIndividuLui);
			addForPrincipal(lui, date(2000, 1, 1), MotifFor.INDETERMINE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
			addForPrincipal(lui, dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, dateReconcialiation.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);

			final PersonnePhysique elle = addHabitant(noIndividuElle);
			addForPrincipal(elle, date(2001, 4, 12), MotifFor.INDETERMINE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
			addForPrincipal(elle, dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, dateReconcialiation.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);

			final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, dateSeparation.getOneDayBefore());
			final MenageCommun mc = couple.getMenage();
			addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateSeparation.getOneDayBefore(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne);

			addAppartenanceMenage(mc, lui, dateReconcialiation, null, false);
			addAppartenanceMenage(mc, elle, dateReconcialiation, null, false);
			addForPrincipal(mc, dateReconcialiation, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
			return mc.getNumero();
		});

		// création de l'événement civil d'annulation de mariage
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(3478256623526867L);
			evt.setType(TypeEvenementCivilEch.CESSATION_SEPARATION);
			evt.setAction(ActionEvenementCivilEch.ANNULATION);
			evt.setDateEvenement(dateReconcialiation);
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
			Assert.assertNotNull(ffp);
			Assert.assertEquals(dateSeparation.getOneDayBefore(), ffp.getDateFin());
			return null;
		});

		// création de l'événement civil d'annulation de mariage pour elle
		final long evtIdElle = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(3478256793526867L);
			evt.setType(TypeEvenementCivilEch.CESSATION_SEPARATION);
			evt.setAction(ActionEvenementCivilEch.ANNULATION);
			evt.setDateEvenement(dateReconcialiation);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividuElle);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividuElle);

		// vérification du résultat
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtIdElle);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.REDONDANT, evt.getEtat());

			final MenageCommun mc = (MenageCommun) tiersDAO.get(mcId);
			Assert.assertNotNull(mc);

			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertEquals(dateSeparation.getOneDayBefore(), ffp.getDateFin());
			return null;
		});
	}
}
