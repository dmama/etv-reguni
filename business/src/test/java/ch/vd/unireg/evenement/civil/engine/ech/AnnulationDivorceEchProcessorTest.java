package ch.vd.unireg.evenement.civil.engine.ech;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEch;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchErreur;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;
import ch.vd.unireg.interfaces.civil.mock.MockEtatCivil;
import ch.vd.unireg.interfaces.civil.mock.MockEtatCivilList;
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

public class AnnulationDivorceEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {
	
	@Test(timeout = 10000L)
	public void testAnnulationDivorce() throws Exception {

		final long noIndividuLui = 36712456523468L;
		final long noIndividuElle = 34674853272545L;
		final RegDate dateMariage = date(2003, 4, 12);
		final RegDate dateDivorce = date(2012, 2, 10);

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

				marieIndividus(lui, elle, dateMariage);
			}
		});

		// mise en place fiscale avec mariage et divorce
		final long mcId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique lui = addHabitant(noIndividuLui);
			addForPrincipal(lui, date(2000, 1, 1), MotifFor.INDETERMINE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
			addForPrincipal(lui, dateDivorce, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne);

			final PersonnePhysique elle = addHabitant(noIndividuElle);
			addForPrincipal(elle, date(2001, 4, 12), MotifFor.INDETERMINE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
			addForPrincipal(elle, dateDivorce, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Echallens);

			final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, dateDivorce.getOneDayBefore());
			final MenageCommun mc = couple.getMenage();
			addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateDivorce.getOneDayBefore(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne);
			return mc.getNumero();
		});

		// création de l'événement civil d'annulation de divorce
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(3478256623526867L);
			evt.setType(TypeEvenementCivilEch.DIVORCE);
			evt.setAction(ActionEvenementCivilEch.ANNULATION);
			evt.setDateEvenement(dateDivorce);
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
			Assert.assertNull(ffp.getDateFin());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testAnnulationDivorceParEtapes() throws Exception {
		final long noIndividuLui = 36712456523468L;
		final long noIndividuElle = 34674853272545L;
		final RegDate dateMariage = date(2003, 4, 12);
		final RegDate dateDivorce = date(2012, 2, 10);

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

				marieIndividus(lui, elle, dateMariage);
				divorceIndividus(lui, elle, dateDivorce);
			}
		});

		// mise en place fiscale avec mariage et divorce
		final long mcId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique lui = addHabitant(noIndividuLui);
			addForPrincipal(lui, date(2000, 1, 1), MotifFor.INDETERMINE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
			addForPrincipal(lui, dateDivorce, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne);

			final PersonnePhysique elle = addHabitant(noIndividuElle);
			addForPrincipal(elle, date(2001, 4, 12), MotifFor.INDETERMINE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
			addForPrincipal(elle, dateDivorce, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Echallens);

			final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, dateDivorce.getOneDayBefore());
			final MenageCommun mc = couple.getMenage();
			addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateDivorce.getOneDayBefore(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne);
			return mc.getNumero();
		});

		// réception de l'annulation de divorce de Monsieur
		doModificationIndividu(noIndividuLui, individu -> {
			final MockEtatCivilList list = individu.getEtatsCivils();

			final EtatCivil separe = list.getEtatCivilAt(null);
			Assert.assertEquals(TypeEtatCivil.DIVORCE, separe.getTypeEtatCivil());
			list.remove(separe);

			final MockEtatCivil marie = (MockEtatCivil) list.getEtatCivilAt(null);
			Assert.assertEquals(TypeEtatCivil.MARIE, marie.getTypeEtatCivil());
		});
		final long evtIdLui = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(3478255893526867L);
			evt.setType(TypeEvenementCivilEch.DIVORCE);
			evt.setAction(ActionEvenementCivilEch.ANNULATION);
			evt.setDateEvenement(dateDivorce);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividuLui);
			return hibernateTemplate.merge(evt).getId();
		});
		traiterEvenements(noIndividuLui);

		// vérification des résultats (devrait partir en erreur car madame est encore considérée comme divorcée)
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtIdLui);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());

			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			Assert.assertEquals(1, erreurs.size());
			final EvenementCivilEchErreur erreur = erreurs.iterator().next();
			Assert.assertNotNull(erreur);
			Assert.assertEquals(String.format("Les états civils des deux conjoints (%d : %s, %d : %s) ne sont pas cohérents pour une annulation de séparation/divorce", noIndividuLui, TypeEtatCivil.MARIE, noIndividuElle, TypeEtatCivil.DIVORCE),
			                    erreur.getMessage());

			final MenageCommun mc = (MenageCommun) tiersDAO.get(mcId);
			Assert.assertNotNull(mc);

			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertEquals(dateDivorce.getOneDayBefore(), ffp.getDateFin());
			return null;
		});

		// réception de l'événement civil d'annulation de divorce de Madame
		doModificationIndividu(noIndividuElle, individu -> {
			final MockEtatCivilList list = individu.getEtatsCivils();

			final EtatCivil separe = list.getEtatCivilAt(null);
			Assert.assertEquals(TypeEtatCivil.DIVORCE, separe.getTypeEtatCivil());
			list.remove(separe);

			final MockEtatCivil marie = (MockEtatCivil) list.getEtatCivilAt(null);
			Assert.assertEquals(TypeEtatCivil.MARIE, marie.getTypeEtatCivil());
		});
		final long evtIdElle = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(46732567L);
			evt.setType(TypeEvenementCivilEch.DIVORCE);
			evt.setAction(ActionEvenementCivilEch.ANNULATION);
			evt.setDateEvenement(dateDivorce);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividuElle);
			return hibernateTemplate.merge(evt).getId();
		});
		traiterEvenements(noIndividuElle);

		// vérification des résultats (devrait être traité)
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evtElle = evtCivilDAO.get(evtIdElle);
			Assert.assertNotNull(evtElle);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evtElle.getEtat());

			// mais son événement à lui est toujours en erreur
			final EvenementCivilEch evtLui = evtCivilDAO.get(evtIdLui);
			Assert.assertNotNull(evtLui);
			Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evtLui.getEtat());

			final MenageCommun mc = (MenageCommun) tiersDAO.get(mcId);
			Assert.assertNotNull(mc);

			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertNull(ffp.getDateFin());
			return null;
		});

		// et si finalement on re-traite l'événement de Monsieur, il doit maintenant être redondant
		traiterEvenements(noIndividuLui);
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evtLui = evtCivilDAO.get(evtIdLui);
			Assert.assertNotNull(evtLui);
			Assert.assertEquals(EtatEvenementCivil.REDONDANT, evtLui.getEtat());
			return null;
		});
	}
}
