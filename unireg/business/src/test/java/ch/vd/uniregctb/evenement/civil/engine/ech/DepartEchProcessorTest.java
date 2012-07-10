package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import junit.framework.Assert;
import net.sf.ehcache.CacheManager;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.cache.ServiceCivilCache;
import ch.vd.unireg.interfaces.civil.data.Adresse;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.civil.data.LocalisationType;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchErreur;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class DepartEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {

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
				addForPrincipal(luis, arrivee, MotifFor.ARRIVEE_HS, MockCommune.Bussigny);
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

				final ForFiscalPrincipal ffp1 = pp.getForFiscalPrincipalAt(date(2011, 9, 1));
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
	public void testDepartDestinationNonRenseignee() throws Exception {

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
				addForPrincipal(luis, arrivee, MotifFor.ARRIVEE_HS, MockCommune.Bussigny);
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
				Assert.assertEquals(MotifFor.DEPART_HS, ffp.getMotifOuverture());
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
				adresseVaudoise.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE,MockPays.PaysInconnu.getNoOFS()));
				addNationalite(ind, MockPays.Espagne, dateNaissance, null);
			}
		});

		doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique luis = addHabitant(noIndividu);
				addForPrincipal(luis, arrivee, MotifFor.ARRIVEE_HS, MockCommune.Bussigny);
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


				final ForFiscalPrincipal forVaudois = pp.getForFiscalPrincipalAt(depart);
				Assert.assertNotNull(forVaudois);
				Assert.assertEquals(depart, forVaudois.getDateFin());
				Assert.assertEquals(MotifFor.DEPART_HS, forVaudois.getMotifFermeture());


				final ForFiscalPrincipal dernierForFiscalPrincipal = pp.getDernierForFiscalPrincipal();
				Assert.assertNotNull(dernierForFiscalPrincipal);
				Assert.assertEquals(null, dernierForFiscalPrincipal.getDateFin());
				Assert.assertEquals(MotifFor.DEPART_HS, dernierForFiscalPrincipal.getMotifOuverture());
				return null;
			}
		});
	}

	@Test
	public void testDepartDestinationNonIdentifiable() throws Exception {

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
				adresseVaudoise.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE,null));
				addNationalite(ind, MockPays.Espagne, dateNaissance, null);
			}
		});

		doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique luis = addHabitant(noIndividu);
				addForPrincipal(luis, arrivee, MotifFor.ARRIVEE_HS, MockCommune.Bussigny);
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
				Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());

				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				Assert.assertNotNull(erreurs);
				Assert.assertEquals(1, erreurs.size());
				final EvenementCivilEchErreur erreur = erreurs.iterator().next();
				String message = "La destination de départ n'est pas identifiable car le numéro OFS de destination n'est pas renseigné";
				Assert.assertEquals(message,erreur.getMessage());

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				Assert.assertNotNull(pp);


				final ForFiscalPrincipal forVaudois = pp.getForFiscalPrincipalAt(depart);
				Assert.assertNotNull(forVaudois);
				Assert.assertEquals(null, forVaudois.getDateFin());
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
				addForPrincipal(luis, arrivee, MotifFor.ARRIVEE_HS, MockCommune.Bussigny);
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
				addForPrincipal(luis, arrivee, MotifFor.INDETERMINE, MockPays.Espagne);
				addForSecondaire(luis, arrivee, MotifFor.ARRIVEE_HS, MockCommune.Pully.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
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
	public void testDepartResidenceSecondaireDestinationInconnue() throws Exception {

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
				addForPrincipal(luis, arrivee, MotifFor.INDETERMINE, MockPays.Espagne);
				addForSecondaire(luis, arrivee, MotifFor.ARRIVEE_HS, MockCommune.Pully.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
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
				addForPrincipal(luis, arrivee, MotifFor.ARRIVEE_HS, MockCommune.Bussigny);
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
				Assert.assertEquals("Ignoré car considéré comme un départ vaudois: la nouvelle commune de résidence Lausanne est toujours dans le canton.", evt.getCommentaireTraitement());
				return null;
			}
		});
	}

	@Test
	public void testDepartVaudois_SIFISC_4912() throws Exception {
		//Les départs vaudois doivent être ignorée

		final long noIndividu = 126673246L;
		final RegDate depart = RegDate.get();
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
				addForPrincipal(luis, arrivee, MotifFor.ARRIVEE_HS, MockCommune.Bussigny);
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
				Assert.assertEquals("Ignoré car considéré comme un départ vaudois: la nouvelle commune de résidence Lausanne est toujours dans le canton.", evt.getCommentaireTraitement());
				return null;
			}
		});
	}

	//Test le cas d'un depart vaudois en secondaire avec une destination inconnue
	@Test
	public void testDepartResidenceSecondaireAvecForPrincipal() throws Exception {

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
				addForPrincipal(luis, arrivee, MotifFor.ARRIVEE_HS, MockCommune.Pully);
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

		doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				Assert.assertNotNull(pp);


				final Set<ForFiscal> ff = pp.getForsFiscaux();
				Assert.assertNotNull(ff);
				Assert.assertEquals(2, ff.size());

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals(MockPays.PaysInconnu.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(depart.getOneDayAfter(), ffp.getDateDebut());
				return null;
			}
		});
	}
	//Vérifie que les démenagements vaudois secondaires avec présence d'un for principal vont bien en erreur avec le message approprié

	@Test
	public void testDepartResidenceSecondaireVaudoisForPrincipal() throws Exception {

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
				adresseVaudoise.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD,MockCommune.Lausanne.getNoOFS()));
				final MockAdresse nouvelleAdresseVaudoise = addAdresse(ind, TypeAdresseCivil.SECONDAIRE, MockRue.Lausanne.AvenueDeBeaulieu, null, depart.getOneDayAfter(), null);
				addNationalite(ind, MockPays.Espagne, dateNaissance, null);
			}
		});

		final Long numeroCtb = doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				PersonnePhysique luis = addHabitant(noIndividu);
				addForPrincipal(luis, arrivee, MotifFor.ARRIVEE_HS, MockCommune.Pully);
				return luis.getNumero();
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
				Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				Assert.assertNotNull(erreurs);
				Assert.assertEquals(1, erreurs.size());
				final EvenementCivilEchErreur erreur = erreurs.iterator().next();
				String message = String.format("A la date de l'événement, la personne physique (ctb: %s) associée à l'individu possède un for principal vaudois sur sa résidence secondaire(Arrangement fiscal?)",
						numeroCtb);
				Assert.assertEquals(message,erreur.getMessage());
				return null;
			}
		});
	}

	@Test
	public void testNettoyageCacheAvantDecisionStrategie() throws Exception {

		final long noIndividu = 12546744578L;
		final RegDate dateNaissance = date(1965, 3, 12);
		final RegDate dateDepart = date(2011, 12, 6);

		// créée le service civil et un cache par devant
		final ServiceCivilCache cache = new ServiceCivilCache();
		cache.setCacheManager(getBean(CacheManager.class, "ehCacheManager"));
		cache.setCacheName("serviceCivil");
		cache.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		cache.setTarget(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu gerard = addIndividu(noIndividu, dateNaissance, "Manfind", "Gérard", true);
				addAdresse(gerard, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.BoulevardGrancy, null, dateNaissance, null);
			}
		});
		cache.afterPropertiesSet();
		try {
			serviceCivil.setUp(cache);

			// création de la personne physique fiscale
			final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
				@Override
				public Long doInTransaction(TransactionStatus status) {
					final PersonnePhysique pp = addHabitant(noIndividu);
					addForPrincipal(pp, dateNaissance.addYears(18), MotifFor.MAJORITE, MockCommune.Lausanne);
					return pp.getNumero();
				}
			});

			// on préchauffe le cache avec Gérard (la date "null" est due au cas jira SIFISC-4250, voir ServiceCivilBase.getAdresses(long, RegDate, boolean)
			{
				final Individu cached = serviceCivil.getIndividu(noIndividu, null, AttributeIndividu.ADRESSES);
				Assert.assertNotNull(cached);
				Assert.assertEquals(dateNaissance, cached.getDateNaissance());
			}
			{
				// pour éviter les surprises plus tard, on préchauffe également le cache de l'individu à la date de l'événement
				final Individu cached = serviceCivil.getIndividu(noIndividu, dateDepart, AttributeIndividu.ADRESSES);
				Assert.assertNotNull(cached);
				Assert.assertEquals(dateNaissance, cached.getDateNaissance());
			}

			// changement d'adresse
			doModificationIndividu(noIndividu, new IndividuModification() {
				@Override
				public void modifyIndividu(MockIndividu individu) {
					final MockAdresse oldAddress = (MockAdresse) individu.getAdresses().iterator().next();
					oldAddress.setDateFinValidite(dateDepart);
					oldAddress.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD, MockCommune.Cossonay.getNoOFSEtendu()));

					final MockAdresse newAddress = MockServiceCivil.newAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateDepart.getOneDayAfter(), null);
					newAddress.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD, MockCommune.Lausanne.getNoOFSEtendu()));
					individu.getAdresses().add(newAddress);
				}
			});

			// puisqu'il est déjà dans le cache, on ne devrait pas encore voir la nouvelle adresse
			{
				final Individu individuDansCache = serviceCivil.getIndividu(noIndividu, null, AttributeIndividu.ADRESSES);
				final Collection<Adresse> adresses = individuDansCache.getAdresses();
				Assert.assertNotNull(adresses);
				Assert.assertEquals(1, adresses.size());
				final Adresse adresseAvantModif = adresses.iterator().next();
				Assert.assertNull(adresseAvantModif.getLocalisationSuivante());
				Assert.assertNull(adresseAvantModif.getDateFin());
				Assert.assertEquals(MockLocalite.Lausanne.getNomAbregeMinuscule(), adresseAvantModif.getLocalite());
			}

			// maintenant, on fait arriver un événement de départ (qui correspond au changement d'adresse)
			final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
				@Override
				public Long doInTransaction(TransactionStatus status) {
					final EvenementCivilEch evt = new EvenementCivilEch();
					evt.setId(14532L);
					evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
					evt.setDateEvenement(dateDepart);
					evt.setEtat(EtatEvenementCivil.A_TRAITER);
					evt.setNumeroIndividu(noIndividu);
					evt.setType(TypeEvenementCivilEch.DEPART);
					return hibernateTemplate.merge(evt).getId();
				}
			});

			// traitement de l'événement
			traiterEvenements(noIndividu);

			// vérification du résultat (il doit avoir été ignoré, le bug que nous avions dans SIFISC-4882 était que l'événement partait en erreur
			// avec le message "impossible de déterminer si départ principal ou secondaire" car le cache n'avait pas encore été invalidé au moment
			// où on inspectait les adresses pour retrouver celle qui se terminait à la date de l'événement)
			doInNewTransactionAndSession(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					final EvenementCivilEch evt = evtCivilDAO.get(evtId);
					Assert.assertNotNull(evt);
					Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
					return null;
				}
			});

			// par acquis de conscience, vérifions maintenant que l'on a bien maintenant la nouvelle adresse
			{
				final Individu individuDansCache = serviceCivil.getIndividu(noIndividu, null, AttributeIndividu.ADRESSES);
				final Collection<Adresse> adresses = individuDansCache.getAdresses();
				Assert.assertNotNull(adresses);
				Assert.assertEquals(2, adresses.size());

				final Iterator<Adresse> iterator = adresses.iterator();
				final Adresse oldAddress = iterator.next();
				Assert.assertNotNull(oldAddress.getLocalisationSuivante());
				Assert.assertEquals((Integer) MockCommune.Cossonay.getNoOFSEtendu(), oldAddress.getLocalisationSuivante().getNoOfs());
				Assert.assertEquals(LocalisationType.CANTON_VD, oldAddress.getLocalisationSuivante().getType());
				Assert.assertEquals(dateDepart, oldAddress.getDateFin());
				Assert.assertEquals(MockLocalite.Lausanne.getNomAbregeMinuscule(), oldAddress.getLocalite());

				final Adresse newAddress = iterator.next();
				Assert.assertNotNull(newAddress);
				Assert.assertNotNull(newAddress.getLocalisationPrecedente());
				Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFSEtendu(), newAddress.getLocalisationPrecedente().getNoOfs());
				Assert.assertEquals(LocalisationType.CANTON_VD, newAddress.getLocalisationPrecedente().getType());
				Assert.assertEquals(MockLocalite.CossonayVille.getNomAbregeMinuscule(), newAddress.getLocalite());
			}
		}
		finally {
			cache.destroy();
		}
	}

	//Test le cas d'un depart vaudois en secondaire avec une destination en secondaire
	@Test
	public void testDepartResidenceSecondaireVersSecondaire() throws Exception {

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
				adresseVaudoise.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD,MockCommune.Echallens.getNoOFS()));
				final MockAdresse adresseVaudoiseSuivante = addAdresse(ind, TypeAdresseCivil.SECONDAIRE, MockRue.Echallens.GrandRue, null, depart.getOneDayAfter(), null);
				addNationalite(ind, MockPays.Espagne, dateNaissance, null);
			}
		});

		doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique luis = addHabitant(noIndividu);
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

		// vérification du traitement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtId);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
				Assert.assertEquals("Ignoré car considéré comme un départ secondaire vaudois: la nouvelle commune de résidence Echallens est toujours dans le canton.", evt.getCommentaireTraitement());
				return null;
			}
		});
	}
}
