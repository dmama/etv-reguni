package ch.vd.uniregctb.evenement.civil.engine.ech;

import junit.framework.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.interfaces.model.Localisation;
import ch.vd.uniregctb.interfaces.model.LocalisationType;
import ch.vd.uniregctb.interfaces.model.mock.MockAdresse;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class ArriveeEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {

	private ServiceInfrastructureService infraService;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		infraService = getBean(ServiceInfrastructureService.class, "serviceInfrastructureService");
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
		traiterEvenement(noIndividu, evtId);

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
		serviceCivil.setUp(new MockServiceCivil(infraService) {
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
		traiterEvenement(noLui, evtLui);
		
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
		traiterEvenement(noElle, evtElle);

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

	@Ignore("Cas jira SIFISC-4025")
	@Test(timeout = 10000L)
	public void testArriveesCoupleMiseAJourFor() throws Exception {

		final long noLui = 246L;
		final long noElle = 3342L;
		final RegDate dateArriveeLui = date(2011, 10, 31);
		final RegDate dateArriveeElle = dateArriveeLui.addMonths(-1);        // arrivée décalée, avant le premier (si c'était après, ce serait redondant)
		final RegDate dateMariage = date(2001, 10, 1);


		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil(infraService) {
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
		traiterEvenement(noLui, evtLui);

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
		traiterEvenement(noElle, evtElle);

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
				Assert.assertEquals(dateArriveeElle, ffp.getDateDebut());               // <--- c'est ici que ça pêche !
				Assert.assertEquals(MotifFor.ARRIVEE_HS, ffp.getMotifOuverture());
				return null;
			}
		});
	}
}
