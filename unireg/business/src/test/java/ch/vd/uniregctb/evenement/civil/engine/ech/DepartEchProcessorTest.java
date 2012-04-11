package ch.vd.uniregctb.evenement.civil.engine.ech;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.interfaces.model.Localisation;
import ch.vd.uniregctb.interfaces.model.LocalisationType;
import ch.vd.uniregctb.interfaces.model.mock.MockAdresse;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class DepartEchProcessorTest  extends AbstractEvenementCivilEchProcessorTest  {

	private ServiceInfrastructureService infraService;
	private TiersService tiersService;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		infraService = getBean(ServiceInfrastructureService.class, "serviceInfrastructureService");
		tiersService = getBean(TiersService.class, "tiersService");
	}

	@Test
	public void testDepartCelibataireHorsSuisse() throws Exception {

		final long noIndividu = 126673246L;
		final RegDate depart = date(2011, 10, 31);
		final RegDate arrivee = date(2003, 1, 1);

		// le p'tit nouveau
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(1967, 4, 23);
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Enrique", "Luis", true);


				final MockAdresse adresseVaudoise = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, arrivee, depart);
				adresseVaudoise.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE, MockPays.Espagne.getNoOFS()));
				addNationalite(ind, MockPays.Espagne, dateNaissance, null);
			}
		});

		doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique luis = addHabitant(noIndividu);
				addForPrincipal(luis,arrivee,MotifFor.ARRIVEE_HS, MockCommune.Bussigny);
				return null;
			}
		});

		// événement de départ
		final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(depart);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.DEPART);
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

				final ForFiscalPrincipal ffp1 = pp.getForFiscalPrincipalAt(date(2011,9,1));
				Assert.assertNotNull(ffp1);
				Assert.assertEquals(depart, ffp1.getDateFin());
				Assert.assertEquals(MotifFor.DEPART_HS, ffp1.getMotifFermeture());
				
				final ForFiscalPrincipal ffp2 = pp.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp2);
				Assert.assertEquals(null, ffp2.getDateFin());
				Assert.assertEquals(MockPays.Espagne.getNoOFS(), ffp2.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(MotifFor.DEPART_HS, ffp2.getMotifOuverture());
				return null;
			}
		});
	}

	@Test
	public void testDepartDestinationInconnue() throws Exception {

		final long noIndividu = 126673246L;
		final RegDate depart = date(2011, 10, 31);
		final RegDate arrivee = date(2003, 1, 1);

		// le p'tit nouveau
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(1967, 4, 23);
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Enrique", "Luis", true);


				final MockAdresse adresseVaudoise = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, arrivee, depart);
				addNationalite(ind, MockPays.Espagne, dateNaissance, null);
			}
		});

		doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique luis = addHabitant(noIndividu);
				addForPrincipal(luis,arrivee,MotifFor.ARRIVEE_HS, MockCommune.Bussigny);
				return null;
			}
		});

		// événement d'arrivée
		final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(depart);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.DEPART);
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

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				Assert.assertNotNull(pp);

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(null, ffp.getDateFin());
				Assert.assertEquals(MotifFor.DEPART_HS, ffp.getMotifOuverture());
				return null;
			}
		});
	}

	@Test
	public void testDepartCelibataireHorsCanton() throws Exception {

		final long noIndividu = 126673246L;
		final RegDate depart = date(2011, 10, 31);
		final RegDate arrivee = date(2003, 1, 1);

		// le p'tit nouveau
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(1967, 4, 23);
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Enrique", "Luis", true);


				final MockAdresse adresseVaudoise = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, arrivee, depart);
				adresseVaudoise.setLocalisationSuivante(new Localisation(LocalisationType.HORS_CANTON, MockCommune.Zurich.getNoOFS()));
				addNationalite(ind, MockPays.Espagne, dateNaissance, null);
			}
		});

		doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique luis = addHabitant(noIndividu);
				addForPrincipal(luis,arrivee,MotifFor.ARRIVEE_HS, MockCommune.Bussigny);
				return null;
			}
		});

		// événement d'arrivée
		final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(depart);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.DEPART);
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
				Assert.assertEquals(null, ffp.getDateFin());
				Assert.assertEquals(MotifFor.DEPART_HC, ffp.getMotifOuverture());
				return null;
			}
		});
	}


	@Test
	public void testDepartResidenceSecondaire() throws Exception {

		final long noIndividu = 126673246L;
		final RegDate depart = date(2011, 10, 31);
		final RegDate arrivee = date(2003, 1, 1);

		// le p'tit nouveau
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(1967, 4, 23);
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Enrique", "Luis", true);


				final MockAdresse adresseVaudoise = addAdresse(ind, TypeAdresseCivil.SECONDAIRE, MockRue.Pully.CheminDesRoches, null, arrivee, depart);
				addNationalite(ind, MockPays.Espagne, dateNaissance, null);
			}
		});

		doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique luis = addHabitant(noIndividu);
				addForPrincipal(luis,arrivee,MotifFor.INDETERMINE, MockPays.Espagne);
				addForSecondaire(luis,arrivee,MotifFor.ARRIVEE_HS,MockCommune.Pully.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				return null;
			}
		});

		// événement d'arrivée
		final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(depart);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.DEPART);
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

	@Test
	public void testDepartVaudois() throws Exception {

		final long noIndividu = 126673246L;
		final RegDate depart = date(2011, 10, 31);
		final RegDate arrivee = date(2003, 1, 1);

		// le p'tit nouveau
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(1967, 4, 23);
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Enrique", "Luis", true);


				final MockAdresse adresseVaudoise = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, arrivee, depart);
				adresseVaudoise.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD, MockCommune.Lausanne.getNoOFS()));
				addNationalite(ind, MockPays.Espagne, dateNaissance, null);
			}
		});

		doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique luis = addHabitant(noIndividu);
				addForPrincipal(luis,arrivee,MotifFor.ARRIVEE_HS, MockCommune.Bussigny);
				return null;
			}
		});

		// événement de départ
		final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(depart);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.DEPART);
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
				Assert.assertEquals("Ignoré car considéré comme un départ vaudois: la nouvelle commune de résidence Lausanne est toujours dans le canton.",evt.getCommentaireTraitement());
				return null;
			}
		});
	}
}
