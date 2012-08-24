package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.EtatCivilList;
import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockEtatCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchErreur;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.tiers.SituationFamilleMenageCommun;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TarifImpotSource;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class AnnulationSeparationEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {
	
	@Test(timeout = 10000L)
	public void testAnnulationSeparation() throws Exception {

		final long noIndividuLui = 36712456523468L;
		final long noIndividuElle = 34674853272545L;
		final RegDate dateMariage = date(2003, 4, 12);
		final RegDate dateSeparation = date(2012, 2, 10);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final RegDate dateNaissanceLui = date(1960, 1, 26);
				final MockIndividu lui = addIndividu(noIndividuLui, dateNaissanceLui, "Casanova", "Paco", true);
				addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateNaissanceLui, null);
				addNationalite(lui, MockPays.Suisse, dateNaissanceLui, null);

				final RegDate dateNaissanceElle = date(1980, 6, 12);
				final MockIndividu elle = addIndividu(noIndividuElle, dateNaissanceElle, "Nette", "Jeu", false);
				addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateNaissanceElle, null);
				addNationalite(elle, MockPays.Suisse, dateNaissanceElle, null);

				marieIndividus(lui, elle, dateMariage);
			}
		});

		// mise en place fiscale avec mariage et séparation
		final long mcId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addHabitant(noIndividuLui);
				addForPrincipal(lui, date(2000, 1, 1), MotifFor.INDETERMINE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				addForPrincipal(lui, dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne);

				final PersonnePhysique elle = addHabitant(noIndividuElle);
				addForPrincipal(elle, date(2001, 4, 12), MotifFor.INDETERMINE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
				addForPrincipal(elle, dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Echallens);

				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, dateSeparation.getOneDayBefore());
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateSeparation.getOneDayBefore(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne);
				return mc.getNumero();
			}
		});

		// création de l'événement civil d'annulation de séparation
		final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(3478256623526867L);
				evt.setType(TypeEvenementCivilEch.SEPARATION);
				evt.setAction(ActionEvenementCivilEch.ANNULATION);
				evt.setDateEvenement(dateSeparation);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividuLui);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividuLui);

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtId);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final MenageCommun mc = (MenageCommun) tiersDAO.get(mcId);
				Assert.assertNotNull(mc);

				final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertNull(ffp.getDateFin());
				return null;
			}
		});
	}


	@Test(timeout = 10000L)
	public void testAnnulationSeparationRedondante() throws Exception {

		final long noIndividuLui = 36712456523468L;
		final long noIndividuElle = 34674853272545L;
		final RegDate dateMariage = date(2003, 4, 12);
		final RegDate dateSeparation = date(2012, 2, 10);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final RegDate dateNaissanceLui = date(1960, 1, 26);
				final MockIndividu lui = addIndividu(noIndividuLui, dateNaissanceLui, "Casanova", "Paco", true);
				addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateNaissanceLui, null);
				addNationalite(lui, MockPays.Suisse, dateNaissanceLui, null);

				final RegDate dateNaissanceElle = date(1980, 6, 12);
				final MockIndividu elle = addIndividu(noIndividuElle, dateNaissanceElle, "Nette", "Jeu", false);
				addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateNaissanceElle, null);
				addNationalite(elle, MockPays.Suisse, dateNaissanceElle, null);

				marieIndividus(lui, elle, dateMariage);
			}
		});

		// mise en place fiscale avec mariage et séparation
		final long mcId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addHabitant(noIndividuLui);
				addForPrincipal(lui, date(2000, 1, 1), MotifFor.INDETERMINE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				addForPrincipal(lui, dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne);

				final PersonnePhysique elle = addHabitant(noIndividuElle);
				addForPrincipal(elle, date(2001, 4, 12), MotifFor.INDETERMINE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
				addForPrincipal(elle, dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Echallens);

				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, dateSeparation.getOneDayBefore());
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateSeparation.getOneDayBefore(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne);
				return mc.getNumero();
			}
		});

		// création de l'événement civil d'annulation de séparation
		final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(3478256623526867L);
				evt.setType(TypeEvenementCivilEch.SEPARATION);
				evt.setAction(ActionEvenementCivilEch.ANNULATION);
				evt.setDateEvenement(dateSeparation);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividuLui);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividuLui);

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtId);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final MenageCommun mc = (MenageCommun) tiersDAO.get(mcId);
				Assert.assertNotNull(mc);

				final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertNull(ffp.getDateFin());
				return null;
			}
		});

		// création de l'événement civil d'annulation de séparation pour elle
		final long evtIdElle = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(3478255893526867L);
				evt.setType(TypeEvenementCivilEch.SEPARATION);
				evt.setAction(ActionEvenementCivilEch.ANNULATION);
				evt.setDateEvenement(dateSeparation);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividuLui);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividuLui);

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtIdElle);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.REDONDANT, evt.getEtat());

				final MenageCommun mc = (MenageCommun) tiersDAO.get(mcId);
				Assert.assertNotNull(mc);

				final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertNull(ffp.getDateFin());
				return null;
			}
		});
	}

	@Test(timeout = 10000L)
	public void testAnnulationSeparationParEtapes() throws Exception {
		final long noIndividuLui = 36712456523468L;
		final long noIndividuElle = 34674853272545L;
		final RegDate dateMariage = date(2003, 4, 12);
		final RegDate dateSeparation = date(2012, 2, 10);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final RegDate dateNaissanceLui = date(1960, 1, 26);
				final MockIndividu lui = addIndividu(noIndividuLui, dateNaissanceLui, "Casanova", "Paco", true);
				addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateNaissanceLui, null);
				addNationalite(lui, MockPays.Suisse, dateNaissanceLui, null);

				final RegDate dateNaissanceElle = date(1980, 6, 12);
				final MockIndividu elle = addIndividu(noIndividuElle, dateNaissanceElle, "Nette", "Jeu", false);
				addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateNaissanceElle, null);
				addNationalite(elle, MockPays.Suisse, dateNaissanceElle, null);

				marieIndividus(lui, elle, dateMariage);
				separeIndividus(lui, elle, dateSeparation);
			}
		});

		// mise en place fiscale avec mariage et séparation
		final long mcId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addHabitant(noIndividuLui);
				addForPrincipal(lui, date(2000, 1, 1), MotifFor.INDETERMINE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				addForPrincipal(lui, dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne);

				final PersonnePhysique elle = addHabitant(noIndividuElle);
				addForPrincipal(elle, date(2001, 4, 12), MotifFor.INDETERMINE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
				addForPrincipal(elle, dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Echallens);

				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, dateSeparation.getOneDayBefore());
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateSeparation.getOneDayBefore(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne);
				return mc.getNumero();
			}
		});

		// réception de l'annulation de séparation de Monsieur
		doModificationIndividu(noIndividuLui, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				final EtatCivilList list = individu.getEtatsCivils();

				final EtatCivil separe = list.get(list.size() - 1);
				Assert.assertEquals(TypeEtatCivil.SEPARE, separe.getTypeEtatCivil());
				list.remove(separe);

				final MockEtatCivil marie = (MockEtatCivil) list.get(list.size() - 1);
				Assert.assertEquals(TypeEtatCivil.MARIE, marie.getTypeEtatCivil());
				marie.setDateFin(null);
			}
		});
		final long evtIdLui = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(3478255893526867L);
				evt.setType(TypeEvenementCivilEch.SEPARATION);
				evt.setAction(ActionEvenementCivilEch.ANNULATION);
				evt.setDateEvenement(dateSeparation);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividuLui);
				return hibernateTemplate.merge(evt).getId();
			}
		});
		traiterEvenements(noIndividuLui);

		// vérification des résultats (devrait partir en erreur car madame est encore considérée comme séparée)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtIdLui);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());

				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				Assert.assertEquals(1, erreurs.size());
				final EvenementCivilEchErreur erreur = erreurs.iterator().next();
				Assert.assertNotNull(erreur);
				Assert.assertEquals(String.format("Les états civils des deux conjoints (%d : %s, %d : %s) ne sont pas cohérents pour une annulation de séparation/divorce", noIndividuLui, TypeEtatCivil.MARIE, noIndividuElle, TypeEtatCivil.SEPARE), erreur.getMessage());

				final MenageCommun mc = (MenageCommun) tiersDAO.get(mcId);
				Assert.assertNotNull(mc);

				final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(dateSeparation.getOneDayBefore(), ffp.getDateFin());
				return null;
			}
		});

		// réception de l'événement civil d'annulation de séparation de Madame
		doModificationIndividu(noIndividuElle, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				final EtatCivilList list = individu.getEtatsCivils();

				final EtatCivil separe = list.get(list.size() - 1);
				Assert.assertEquals(TypeEtatCivil.SEPARE, separe.getTypeEtatCivil());
				list.remove(separe);

				final MockEtatCivil marie = (MockEtatCivil) list.get(list.size() - 1);
				Assert.assertEquals(TypeEtatCivil.MARIE, marie.getTypeEtatCivil());
				marie.setDateFin(null);
			}
		});
		final long evtIdElle = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(46732567L);
				evt.setType(TypeEvenementCivilEch.SEPARATION);
				evt.setAction(ActionEvenementCivilEch.ANNULATION);
				evt.setDateEvenement(dateSeparation);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividuElle);
				return hibernateTemplate.merge(evt).getId();
			}
		});
		traiterEvenements(noIndividuElle);

		// vérification des résultats (devrait être traité)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
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
			}
		});

		// et si finalement on re-traite l'événement de Monsieur, il doit maintenant être redondant
		traiterEvenements(noIndividuLui);
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evtLui = evtCivilDAO.get(evtIdLui);
				Assert.assertNotNull(evtLui);
				Assert.assertEquals(EtatEvenementCivil.REDONDANT, evtLui.getEtat());
				return null;
			}
		});
	}

	/**
	 * Test les effets de bord de l'annulation de séparation sur les situation de famille unireg
	 * @throws Exception
	 */
	public void testSIFISC5323(final ch.vd.uniregctb.type.EtatCivil etatCivilEnMenage, final ch.vd.uniregctb.type.EtatCivil etatCivilSepare) throws Exception {

		final long noMonsieur = 411587L;
		final RegDate dateNaissance = date(1957, 3, 29);
		final RegDate dateMariage = date(2005, 5, 5);
		final RegDate dateSeparation = date(2008, 11, 23);

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu monsieur = addIndividu(noMonsieur, dateNaissance, "Lambert", "Christophe", true);
				addNationalite(monsieur, MockPays.Suisse, dateNaissance, null);
				marieIndividu(monsieur,dateMariage);
				separeIndividu(monsieur,dateSeparation);
			}
		});

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique monsieur = addHabitant(noMonsieur);
				final MenageCommun menage = addEnsembleTiersCouple(monsieur, null, dateMariage, dateSeparation.getOneDayBefore()).getMenage();
				addForPrincipal(monsieur,
						dateNaissance.addYears(18), MotifFor.MAJORITE,
						dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
						MockCommune.Echallens);
				addForPrincipal(menage,
						dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
						dateSeparation.getOneDayBefore(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
						MockCommune.Echallens);
				addSituation(monsieur, dateNaissance, dateMariage.getOneDayBefore() , 0, ch.vd.uniregctb.type.EtatCivil.CELIBATAIRE);
				addSituation(menage, dateMariage, dateSeparation.getOneDayBefore(), 0, TarifImpotSource.NORMAL, etatCivilEnMenage);
				addSituation(monsieur, dateSeparation,null, 0, etatCivilSepare);
				return null;
			}
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long annulationSeparationId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(454563457L);
				evt.setAction(ActionEvenementCivilEch.ANNULATION);
				evt.setDateEvenement(dateSeparation);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noMonsieur);
				evt.setType(TypeEvenementCivilEch.SEPARATION);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement synchrone de l'événement
		traiterEvenements(noMonsieur);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(annulationSeparationId);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique monsieur = tiersService.getPersonnePhysiqueByNumeroIndividu(noMonsieur);
				assertNotNull(monsieur);
				final SituationFamille situationDeFamillePersonnelle = monsieur.getSituationFamilleActive();
				assertNull(situationDeFamillePersonnelle);
				final MenageCommun menage = tiersService.getEnsembleTiersCouple(monsieur, null).getMenage();
				assertNotNull(menage);
				final SituationFamilleMenageCommun situationDeFamilleMenage = (SituationFamilleMenageCommun) menage.getSituationFamilleActive();
				assertNotNull(situationDeFamilleMenage);
				assertNull(situationDeFamilleMenage.getDateFin());
				assertEquals(etatCivilEnMenage, situationDeFamilleMenage.getEtatCivil());

				return null;
			}
		});
	}

	@Test(timeout = 10000L)
	public void testHeteroSIFISC5323() throws Exception {
		testSIFISC5323(ch.vd.uniregctb.type.EtatCivil.MARIE, ch.vd.uniregctb.type.EtatCivil.DIVORCE);
	}

	@Test(timeout = 10000L)
	public void testHomoSIFISC5323() throws Exception {
		testSIFISC5323(ch.vd.uniregctb.type.EtatCivil.LIE_PARTENARIAT_ENREGISTRE, ch.vd.uniregctb.type.EtatCivil.PARTENARIAT_DISSOUS_JUDICIAIREMENT);
	}

}
