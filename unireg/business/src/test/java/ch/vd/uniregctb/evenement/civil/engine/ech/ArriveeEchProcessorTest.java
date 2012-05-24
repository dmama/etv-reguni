package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.Set;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.civil.data.LocalisationType;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchErreur;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;
import ch.vd.uniregctb.type.TypeEvenementErreur;

public class ArriveeEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {

	private MetierService metierService;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		metierService = getBean(MetierService.class,"metierService");
	}

	@Test(timeout = 10000L)
	public void testArriveeCelibataire() throws Exception {

		final long noIndividu = 126673246L;
		final RegDate dateArrivee = date(2011, 10, 31);

		// le p'tit nouveau
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(1956, 4, 23);
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Zorro", "Alessandro", true);

				final MockAdresse adresse = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateArrivee, null);
				adresse.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.Espagne.getNoOFS()));

				addNationalite(ind, MockPays.Espagne, dateNaissance, null);
			}
		});

		// événement d'arrivée
		final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArrivee);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
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

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				Assert.assertNotNull(pp);

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(dateArrivee, ffp.getDateDebut());
				Assert.assertEquals(MotifFor.ARRIVEE_HS, ffp.getMotifOuverture());
				return null;
			}
		});
	}
	
	@Test(timeout = 10000L)
	public void testArriveesCoupleAvecRedondance() throws Exception {

		final long noLui = 246L;
		final long noElle = 3342L;
		final RegDate dateArrivee = date(2011, 10, 31);
		final RegDate dateMariage = date(2001, 10, 1);
		
		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final RegDate naissanceLui = date(1970, 3, 12);
				final MockIndividu lui = addIndividu(noLui, naissanceLui, "Tartempion", "François", true);
				final RegDate naissanceElle = date(1971, 6, 21);
				final MockIndividu elle = addIndividu(noElle, naissanceElle, "Tartempion", "Françoise", false);
				addNationalite(lui, MockPays.France, naissanceLui, null);
				addNationalite(elle, MockPays.France, naissanceElle, null);
				marieIndividus(lui, elle, dateMariage);
				
				final MockAdresse adrLui = addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateArrivee, null);
				adrLui.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS()));
				
				final MockAdresse adrElle = addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateArrivee, null);
				adrElle.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS()));
			}
		});

		// création de l'événement civil pour l'arrivée de monsieur
		final long evtLui = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArrivee);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noLui);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			}
		});
		
		// traitement de l'arrivée de monsieur
		traiterEvenements(noLui);
		
		// vérification de l'état de traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtLui);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noLui);
				Assert.assertNotNull(pp);

				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(pp, dateMariage);
				Assert.assertNotNull(couple);
				Assert.assertNotNull(couple.getMenage());
				Assert.assertNotNull(couple.getConjoint());
				Assert.assertEquals(pp.getId(), couple.getPrincipal().getNumero());
				
				final MenageCommun mc = couple.getMenage();
				final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(dateArrivee, ffp.getDateDebut());
				Assert.assertEquals(MotifFor.ARRIVEE_HS, ffp.getMotifOuverture());
				
				return null;
			}
		});
		
		// événement civil de l'arrivée de madame
		final long evtElle = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(321674L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArrivee);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noElle);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'arrivée de monsieur
		traiterEvenements(noElle);

		// vérification de l'état de traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtElle);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.REDONDANT, evt.getEtat());

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noElle);
				Assert.assertNotNull(pp);

				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(pp, dateMariage);
				Assert.assertNotNull(couple);
				Assert.assertNotNull(couple.getMenage());
				Assert.assertNotNull(couple.getPrincipal());
				Assert.assertEquals(pp.getId(), couple.getConjoint().getNumero());

				final MenageCommun mc = couple.getMenage();
				final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(dateArrivee, ffp.getDateDebut());
				Assert.assertEquals(MotifFor.ARRIVEE_HS, ffp.getMotifOuverture());

				return null;
			}
		});
	}

	@Test(timeout = 10000L)
	public void testArriveesCoupleAvecRedondancePosterieure() throws Exception {

		final long noLui = 246L;
		final long noElle = 3342L;
		final RegDate dateArriveeLui = date(2011, 10, 31);
		final RegDate dateArriveeElle = dateArriveeLui.addMonths(1);
		final RegDate dateMariage = date(2001, 10, 1);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final RegDate naissanceLui = date(1970, 3, 12);
				final MockIndividu lui = addIndividu(noLui, naissanceLui, "Tartempion", "François", true);
				final RegDate naissanceElle = date(1971, 6, 21);
				final MockIndividu elle = addIndividu(noElle, naissanceElle, "Tartempion", "Françoise", false);
				addNationalite(lui, MockPays.France, naissanceLui, null);
				addNationalite(elle, MockPays.France, naissanceElle, null);
				marieIndividus(lui, elle, dateMariage);

				final MockAdresse adrLui = addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateArriveeLui, null);
				adrLui.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS()));

				final MockAdresse adrElle = addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateArriveeElle, null);
				adrElle.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS()));
			}
		});

		// création de l'événement civil pour l'arrivée de monsieur
		final long evtLui = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArriveeLui);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noLui);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'arrivée de monsieur
		traiterEvenements(noLui);

		// vérification de l'état de traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtLui);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noLui);
				Assert.assertNotNull(pp);

				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(pp, dateMariage);
				Assert.assertNotNull(couple);
				Assert.assertNotNull(couple.getMenage());
				Assert.assertNotNull(couple.getConjoint());
				Assert.assertEquals(pp.getId(), couple.getPrincipal().getNumero());

				final MenageCommun mc = couple.getMenage();
				final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(dateArriveeLui, ffp.getDateDebut());
				Assert.assertEquals(MotifFor.ARRIVEE_HS, ffp.getMotifOuverture());

				return null;
			}
		});

		// événement civil de l'arrivée de madame
		final long evtElle = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(321674L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArriveeElle);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noElle);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'arrivée de monsieur
		traiterEvenements(noElle);

		// vérification de l'état de traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtElle);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noElle);
				Assert.assertNotNull(pp);

				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(pp, dateMariage);
				Assert.assertNotNull(couple);
				Assert.assertNotNull(couple.getMenage());
				Assert.assertNotNull(couple.getPrincipal());
				Assert.assertEquals(pp.getId(), couple.getConjoint().getNumero());

				final MenageCommun mc = couple.getMenage();
				final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(dateArriveeLui, ffp.getDateDebut());
				Assert.assertEquals(MotifFor.ARRIVEE_HS, ffp.getMotifOuverture());

				return null;
			}
		});
	}

	@Test
	public void testArriveesAnterieurCouple() throws Exception {

		final long noLui = 246L;
		final long noElle = 3342L;
		final RegDate dateArriveeLui = date(2011, 10, 31);
		final RegDate dateArriveeElle = dateArriveeLui.addMonths(-1);        // arrivée décalée, avant le premier (si c'était après, ce serait traité)
		final RegDate dateMariage = date(2001, 10, 1);


		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final RegDate naissanceLui = date(1970, 3, 12);
				final MockIndividu lui = addIndividu(noLui, naissanceLui, "Tartempion", "François", true);
				final RegDate naissanceElle = date(1971, 6, 21);
				final MockIndividu elle = addIndividu(noElle, naissanceElle, "Tartempion", "Françoise", false);
				addNationalite(lui, MockPays.France, naissanceLui, null);
				addNationalite(elle, MockPays.France, naissanceElle, null);
				marieIndividus(lui, elle, dateMariage);

				final MockAdresse adrLui = addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateArriveeLui, null);
				adrLui.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS()));

				final MockAdresse adrElle = addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateArriveeElle, null);
				adrElle.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS()));
			}
		});

		// création de l'événement civil pour l'arrivée de monsieur
		final long evtLui = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArriveeLui);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noLui);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'arrivée de monsieur
		traiterEvenements(noLui);

		// vérification de l'état de traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtLui);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noLui);
				Assert.assertNotNull(pp);

				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(pp, dateMariage);
				Assert.assertNotNull(couple);
				Assert.assertNotNull(couple.getMenage());
				Assert.assertNotNull(couple.getConjoint());
				Assert.assertEquals(pp.getId(), couple.getPrincipal().getNumero());

				final MenageCommun mc = couple.getMenage();
				final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(dateArriveeLui, ffp.getDateDebut());
				Assert.assertEquals(MotifFor.ARRIVEE_HS, ffp.getMotifOuverture());

				return null;
			}
		});

		// événement civil de l'arrivée de madame
		final long evtElle = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(321674L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArriveeElle);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noElle);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'arrivée de monsieur
		traiterEvenements(noElle);

		// vérification de l'état de traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtElle);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noElle);
				Assert.assertNotNull(pp);

				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(pp, dateMariage);
				Assert.assertNotNull(couple);
				Assert.assertNotNull(couple.getMenage());
				Assert.assertNotNull(couple.getPrincipal());
				Assert.assertEquals(pp.getId(), couple.getConjoint().getNumero());

				final MenageCommun mc = couple.getMenage();
				final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				final RegDate dateDebutFor = ffp.getDateDebut();

				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				Assert.assertNotNull(erreurs);
				Assert.assertEquals(1, erreurs.size());
				final EvenementCivilEchErreur erreur = erreurs.iterator().next();
				String message = String.format("la date d'arrivée (%s) de l'individu (n° %s) est antérieure à l'arrivée de son menage commun", RegDateHelper.dateToDashString(dateArriveeElle),noElle);
				Assert.assertEquals(message,erreur.getMessage());
				return null;
			}
		});
	}

	@Test
	public void testRetourCoupleApresDepart() throws Exception {

		final long noLui = 246L;
		final long noElle = 3342L;
		final RegDate dateDepart = date(2010,5,7);
		final RegDate dateArriveeLui = date(2011, 10, 31);
		final RegDate dateArriveeElle = dateArriveeLui.addMonths(-1);        // arrivée décalée, avant le premier
		final RegDate dateMariage = date(2001, 10, 1);



		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final RegDate naissanceLui = date(1970, 3, 12);
				final MockIndividu lui = addIndividu(noLui, naissanceLui, "Tartempion", "François", true);
				final RegDate naissanceElle = date(1971, 6, 21);
				final MockIndividu elle = addIndividu(noElle, naissanceElle, "Tartempion", "Françoise", false);
				addNationalite(lui, MockPays.France, naissanceLui, null);
				addNationalite(elle, MockPays.France, naissanceElle, null);
				marieIndividus(lui, elle, dateMariage);

				final MockAdresse adrAvantDepartLui = addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateMariage, dateDepart);
				adrAvantDepartLui.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS()));

				final MockAdresse adrFranceLui = addAdresse(lui, TypeAdresseCivil.PRINCIPALE,null, null,null,null,null,MockPays.France, dateDepart.getOneDayAfter(), dateArriveeLui.getOneDayBefore());
				adrFranceLui.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD, MockCommune.Lausanne.getNoOFS()));
				adrFranceLui.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD, MockCommune.Lausanne.getNoOFS()));

				final MockAdresse adrLui = addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateArriveeLui, null);
				adrLui.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS()));


				final MockAdresse adrAvantDepartElle = addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateMariage, dateDepart);
				adrAvantDepartElle.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS()));

				final MockAdresse adrFranceElle = addAdresse(elle, TypeAdresseCivil.PRINCIPALE,null, null,null,null,null,MockPays.France, dateDepart.getOneDayAfter(), dateArriveeElle.addMonths(-1));
				adrFranceElle.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD, MockCommune.Lausanne.getNoOFS()));
				adrFranceElle.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD, MockCommune.Lausanne.getNoOFS()));

				final MockAdresse adrElle = addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateArriveeElle, null);
				adrElle.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS()));

			}
		});

		doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique ppLui = addHabitant(noLui);
				PersonnePhysique ppElle = addHabitant(noElle);
				MenageCommun menage = metierService.marie(dateMariage,ppLui,ppElle,null, EtatCivil.MARIE,false,null);
				tiersService.closeAllForsFiscaux(menage,dateDepart,MotifFor.DEPART_HS);
				addForPrincipal(menage,dateDepart.getOneDayAfter(),MotifFor.DEPART_HS,MockPays.France);
				return null;
			}
		});

		// création de l'événement civil pour l'arrivée de monsieur
		final long evtLui = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArriveeLui);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noLui);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'arrivée de monsieur
		traiterEvenements(noLui);

		// vérification de l'état de traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtLui);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noLui);
				Assert.assertNotNull(pp);

				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(pp, dateMariage);
				Assert.assertNotNull(couple);
				Assert.assertNotNull(couple.getMenage());
				Assert.assertNotNull(couple.getConjoint());
				Assert.assertEquals(pp.getId(), couple.getPrincipal().getNumero());

				final MenageCommun mc = couple.getMenage();
				final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(dateArriveeLui, ffp.getDateDebut());
				Assert.assertEquals(MotifFor.ARRIVEE_HS, ffp.getMotifOuverture());

				return null;
			}
		});

		// événement civil de l'arrivée de madame
		final long evtElle = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(321674L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArriveeElle);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noElle);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'arrivée de monsieur
		traiterEvenements(noElle);

		// vérification de l'état de traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtElle);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noElle);
				Assert.assertNotNull(pp);

				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(pp, dateMariage);
				Assert.assertNotNull(couple);
				Assert.assertNotNull(couple.getMenage());
				Assert.assertNotNull(couple.getPrincipal());
				Assert.assertEquals(pp.getId(), couple.getConjoint().getNumero());

				final MenageCommun mc = couple.getMenage();
				final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				final RegDate dateDebutFor = ffp.getDateDebut();

				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				Assert.assertNotNull(erreurs);
				Assert.assertEquals(1, erreurs.size());
				final EvenementCivilEchErreur erreur = erreurs.iterator().next();
				String message = String.format("la date d'arrivée (%s) de l'individu (n° %s) est antérieure à l'arrivée de son menage commun", RegDateHelper.dateToDashString(dateArriveeElle),noElle);
				Assert.assertEquals(message,erreur.getMessage());
				return null;
			}
		});
	}


	@Test(timeout = 10000L)
	public void testArriveeNumeroOfsCommuneInconnue() throws Exception {

		final long noIndividu = 126673246L;
		final RegDate dateArrivee = date(2011, 10, 31);

		// le p'tit nouveau
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(1956, 4, 23);
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Zorro", "Alessandro", true);

				final MockAdresse adresse = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateArrivee, null);
				adresse.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD, null));

				addNationalite(ind, MockPays.Espagne, dateNaissance, null);
			}
		});

		// événement d'arrivée
		final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArrivee);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
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
				Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());


				return null;
			}
		});

	}

	@Test(timeout = 10000L)
	public void testArriveeProvenanceAbsente() throws Exception {

		final long noIndividu = 126673246L;
		final RegDate dateArrivee = date(2011, 10, 31);

		// le p'tit nouveau
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(1956, 4, 23);
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Zorro", "Alessandro", true);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateArrivee, null);
				addNationalite(ind, MockPays.Espagne, dateNaissance, null);
			}
		});

		// événement d'arrivée
		final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArrivee);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
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
				Assert.assertEquals(EtatEvenementCivil.A_VERIFIER, evt.getEtat());

				final Set<EvenementCivilEchErreur> warnings = evt.getErreurs();
				Assert.assertNotNull(warnings);
				Assert.assertEquals(1, warnings.size());
				final EvenementCivilEchErreur warning = warnings.iterator().next();
				Assert.assertNotNull(warning);
				Assert.assertEquals(TypeEvenementErreur.WARNING, warning.getType());
				Assert.assertEquals("Ancienne adresse avant l'arrivée inconnue : veuillez indiquer le motif d'ouverture du for principal", warning.getMessage());
				return null;
			}
		});
	}




}
