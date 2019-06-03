package ch.vd.unireg.evenement.civil.engine.ech;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import net.sf.ehcache.CacheManager;
import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.data.CivilDataEventService;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEch;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchErreur;
import ch.vd.unireg.evenement.civil.interne.depart.DepartEchTranslationStrategy;
import ch.vd.unireg.interfaces.civil.cache.IndividuConnectorCache;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.civil.data.LocalisationType;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockIndividuConnector;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementCivilEch;
import ch.vd.unireg.type.TypeEvenementErreur;
import ch.vd.unireg.type.TypePermis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DepartEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {

	@Test(timeout = 10000L)
	public void testDepartCelibataireHorsSuisse() throws Exception {

		final long noIndividu = 126673246L;
		final RegDate depart = date(2011, 10, 31);
		final RegDate arrivee = date(2003, 1, 1);

		// le p'tit nouveau
		serviceCivil.setUp(new DefaultMockIndividuConnector(false) {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(1967, 4, 23);
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Enrique", "Luis", true);


				final MockAdresse adresseVaudoise = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, arrivee, depart);
				adresseVaudoise.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE, MockPays.Espagne.getNoOFS(), null));
				addNationalite(ind, MockPays.Espagne, dateNaissance, null);
			}
		});

		doInNewTransactionAndSession(status -> {
			PersonnePhysique luis = addHabitant(noIndividu);
			addForPrincipal(luis, arrivee, MotifFor.ARRIVEE_HS, MockCommune.Bussigny);
			return null;
		});

		// événement de départ
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(depart);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.DEPART);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
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
		});
	}

	@Test(timeout = 10000L)
	public void testDepartDestinationNonRenseignee() throws Exception {

		final long noIndividu = 126673246L;
		final RegDate depart = date(2011, 10, 31);
		final RegDate arrivee = date(2003, 1, 1);

		// le p'tit nouveau
		serviceCivil.setUp(new DefaultMockIndividuConnector(false) {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(1967, 4, 23);
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Enrique", "Luis", true);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, arrivee, depart);
				addNationalite(ind, MockPays.Espagne, dateNaissance, null);
			}
		});

		doInNewTransactionAndSession(status -> {
			PersonnePhysique luis = addHabitant(noIndividu);
			addForPrincipal(luis, arrivee, MotifFor.ARRIVEE_HS, MockCommune.Bussigny);
			return null;
		});

		// événement d'arrivée
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(depart);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.DEPART);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
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
		});
	}

	@Test(timeout = 10000L)
	public void testDepartDestinationInconnue() throws Exception {

		final long noIndividu = 126673246L;
		final RegDate depart = date(2011, 10, 31);
		final RegDate arrivee = date(2003, 1, 1);

		// le p'tit nouveau
		serviceCivil.setUp(new DefaultMockIndividuConnector(false) {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(1967, 4, 23);
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Enrique", "Luis", true);


				final MockAdresse adresseVaudoise = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, arrivee, depart);
				adresseVaudoise.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE, MockPays.PaysInconnu.getNoOFS(), null));
				addNationalite(ind, MockPays.Espagne, dateNaissance, null);
			}
		});

		doInNewTransactionAndSession(status -> {
			PersonnePhysique luis = addHabitant(noIndividu);
			addForPrincipal(luis, arrivee, MotifFor.ARRIVEE_HS, MockCommune.Bussigny);
			return null;
		});

		// événement d'arrivée
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(depart);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.DEPART);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
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
		});
	}

	@Test(timeout = 10000L)
	public void testDepartDestinationNonIdentifiable() throws Exception {

		final long noIndividu = 126673246L;
		final RegDate depart = date(2011, 10, 31);
		final RegDate arrivee = date(2003, 1, 1);

		// le p'tit nouveau
		serviceCivil.setUp(new DefaultMockIndividuConnector(false) {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(1967, 4, 23);
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Enrique", "Luis", true);


				final MockAdresse adresseVaudoise = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, arrivee, depart);
				adresseVaudoise.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE, null, null));
				addNationalite(ind, MockPays.Espagne, dateNaissance, null);
			}
		});

		doInNewTransactionAndSession(status -> {
			PersonnePhysique luis = addHabitant(noIndividu);
			addForPrincipal(luis, arrivee, MotifFor.ARRIVEE_HS, MockCommune.Bussigny);
			return null;
		});

		// événement d'arrivée
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(depart);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.DEPART);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());

			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			Assert.assertNotNull(erreurs);
			Assert.assertEquals(1, erreurs.size());
			final EvenementCivilEchErreur erreur = erreurs.iterator().next();
			String message = "La destination de départ n'est pas identifiable car le numéro OFS de destination n'est pas renseigné";
			Assert.assertEquals(message, erreur.getMessage());

			final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
			Assert.assertNotNull(pp);


			final ForFiscalPrincipal forVaudois = pp.getForFiscalPrincipalAt(depart);
			Assert.assertNotNull(forVaudois);
			Assert.assertEquals(null, forVaudois.getDateFin());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testDepartCelibataireHorsCanton() throws Exception {

		final long noIndividu = 126673246L;
		final RegDate depart = date(2011, 10, 31);
		final RegDate arrivee = date(2003, 1, 1);

		// le p'tit nouveau
		serviceCivil.setUp(new DefaultMockIndividuConnector(false) {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(1967, 4, 23);
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Enrique", "Luis", true);


				final MockAdresse adresseVaudoise = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, arrivee, depart);
				adresseVaudoise.setLocalisationSuivante(new Localisation(LocalisationType.HORS_CANTON, MockCommune.Zurich.getNoOFS(), null));
				addNationalite(ind, MockPays.Espagne, dateNaissance, null);
			}
		});

		doInNewTransactionAndSession(status -> {
			PersonnePhysique luis = addHabitant(noIndividu);
			addForPrincipal(luis, arrivee, MotifFor.ARRIVEE_HS, MockCommune.Bussigny);
			return null;
		});

		// événement d'arrivée
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(depart);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.DEPART);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
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
		});
	}

	@Test(timeout = 10000L)
	public void testDepartResidenceSecondaire() throws Exception {

		final long noIndividu = 126673246L;
		final RegDate depart = date(2011, 10, 31);
		final RegDate arrivee = date(2003, 1, 1);

		// le p'tit nouveau
		serviceCivil.setUp(new DefaultMockIndividuConnector(false) {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(1967, 4, 23);
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Enrique", "Luis", true);
				addAdresse(ind, TypeAdresseCivil.SECONDAIRE, MockRue.Pully.CheminDesRoches, null, arrivee, depart);
				addNationalite(ind, MockPays.Espagne, dateNaissance, null);
			}
		});

		doInNewTransactionAndSession(status -> {
			PersonnePhysique luis = addHabitant(noIndividu);
			addForPrincipal(luis, arrivee, MotifFor.INDETERMINE, MockPays.Espagne);
			addForSecondaire(luis, arrivee, MotifFor.ARRIVEE_HS, MockCommune.Pully, MotifRattachement.IMMEUBLE_PRIVE);
			return null;
		});

		// événement d'arrivée
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(depart);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.DEPART);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testDepartResidenceSecondaireDestinationInconnue() throws Exception {

		final long noIndividu = 126673246L;
		final RegDate depart = date(2011, 10, 31);
		final RegDate arrivee = date(2003, 1, 1);

		// le p'tit nouveau
		serviceCivil.setUp(new DefaultMockIndividuConnector(false) {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(1967, 4, 23);
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Enrique", "Luis", true);
				addAdresse(ind, TypeAdresseCivil.SECONDAIRE, MockRue.Pully.CheminDesRoches, null, arrivee, depart);
				addNationalite(ind, MockPays.Espagne, dateNaissance, null);
			}
		});

		doInNewTransactionAndSession(status -> {
			PersonnePhysique luis = addHabitant(noIndividu);
			addForPrincipal(luis, arrivee, MotifFor.INDETERMINE, MockPays.Espagne);
			addForSecondaire(luis, arrivee, MotifFor.ARRIVEE_HS, MockCommune.Pully, MotifRattachement.IMMEUBLE_PRIVE);
			return null;
		});

		// événement d'arrivée
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(depart);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.DEPART);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testDepartVaudois() throws Exception {

		final long noIndividu = 126673246L;
		final RegDate depart = date(2011, 10, 31);
		final RegDate arrivee = date(2003, 1, 1);

		// le p'tit nouveau
		serviceCivil.setUp(new DefaultMockIndividuConnector(false) {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(1967, 4, 23);
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Enrique", "Luis", true);


				final MockAdresse adresseVaudoise = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, arrivee, depart);
				adresseVaudoise.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD, MockCommune.Lausanne.getNoOFS(), null));
				addNationalite(ind, MockPays.Espagne, dateNaissance, null);
			}
		});

		doInNewTransactionAndSession(status -> {
			PersonnePhysique luis = addHabitant(noIndividu);
			addForPrincipal(luis, arrivee, MotifFor.ARRIVEE_HS, MockCommune.Bussigny);
			return null;
		});

		// événement de départ
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(depart);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.DEPART);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
			Assert.assertEquals("Ignoré car départ vaudois : la nouvelle commune de résidence Lausanne est toujours dans le canton.", evt.getCommentaireTraitement());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testDepartVaudois_SIFISC_4912() throws Exception {
		//Les départs vaudois doivent être ignorée

		final long noIndividu = 126673246L;
		final RegDate depart = RegDate.get();
		final RegDate arrivee = date(2003, 1, 1);

		// le p'tit nouveau
		serviceCivil.setUp(new DefaultMockIndividuConnector(false) {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(1967, 4, 23);
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Enrique", "Luis", true);


				final MockAdresse adresseVaudoise = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, arrivee, depart);
				adresseVaudoise.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD, MockCommune.Lausanne.getNoOFS(), null));
				addNationalite(ind, MockPays.Espagne, dateNaissance, null);
			}
		});

		doInNewTransactionAndSession(status -> {
			PersonnePhysique luis = addHabitant(noIndividu);
			addForPrincipal(luis, arrivee, MotifFor.ARRIVEE_HS, MockCommune.Bussigny);
			return null;
		});

		// événement de départ
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(depart);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.DEPART);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
			Assert.assertEquals("Ignoré car départ vaudois : la nouvelle commune de résidence Lausanne est toujours dans le canton.", evt.getCommentaireTraitement());
			return null;
		});
	}

	//Test le cas d'un depart vaudois en secondaire avec une destination inconnue
	@Test(timeout = 10000L)
	public void testDepartResidenceSecondaireAvecForPrincipal() throws Exception {

		final long noIndividu = 126673246L;
		final RegDate depart = date(2011, 10, 31);
		final RegDate arrivee = date(2003, 1, 1);

		// le p'tit nouveau
		serviceCivil.setUp(new DefaultMockIndividuConnector(false) {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(1967, 4, 23);
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Enrique", "Luis", true);
				addAdresse(ind, TypeAdresseCivil.SECONDAIRE, MockRue.Pully.CheminDesRoches, null, arrivee, depart);
				addNationalite(ind, MockPays.Espagne, dateNaissance, null);
			}
		});

		doInNewTransactionAndSession(status -> {
			PersonnePhysique luis = addHabitant(noIndividu);
			addForPrincipal(luis, arrivee, MotifFor.ARRIVEE_HS, MockCommune.Pully);
			return null;
		});

		// événement d'arrivée
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(depart);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.DEPART);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
			return null;
		});

		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
			assertNotNull(pp);


			final Set<ForFiscal> ff = pp.getForsFiscaux();
			assertNotNull(ff);
			assertEquals(2, ff.size());

			final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
			assertNotNull(ffp);
			assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
			assertEquals(MockPays.PaysInconnu.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale().intValue());
			assertEquals(depart.getOneDayAfter(), ffp.getDateDebut());
			return null;
		});
	}

	//Vérifie que les démenagements vaudois secondaires avec présence d'un for principal vont bien en erreur avec le message approprié
	@Test(timeout = 10000L)
	public void testDepartResidenceSecondaireVaudoisForPrincipal() throws Exception {

		final long noIndividu = 126673246L;
		final RegDate depart = date(2011, 10, 31);
		final RegDate arrivee = date(2003, 1, 1);


		// le p'tit nouveau
		serviceCivil.setUp(new DefaultMockIndividuConnector(false) {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(1967, 4, 23);
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Enrique", "Luis", true);
				final MockAdresse adresseVaudoise = addAdresse(ind, TypeAdresseCivil.SECONDAIRE, MockRue.Pully.CheminDesRoches, null, arrivee, depart);
				adresseVaudoise.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD, MockCommune.Lausanne.getNoOFS(), null));
				addAdresse(ind, TypeAdresseCivil.SECONDAIRE, MockRue.Lausanne.AvenueDeBeaulieu, null, depart.getOneDayAfter(), null);
				addNationalite(ind, MockPays.Espagne, dateNaissance, null);
			}
		});

		final Long numeroCtb = doInNewTransactionAndSession(status -> {
			PersonnePhysique luis = addHabitant(noIndividu);
			addForPrincipal(luis, arrivee, MotifFor.ARRIVEE_HS, MockCommune.Pully);
			return luis.getNumero();
		});

		// événement de départ
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(depart);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.DEPART);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			Assert.assertNotNull(erreurs);
			Assert.assertEquals(1, erreurs.size());
			final EvenementCivilEchErreur erreur = erreurs.iterator().next();
			String message = String.format("A la date de l'événement, la personne physique (ctb: %s) associée à l'individu possède un for principal vaudois actif (arrangement fiscal ?)", numeroCtb);
			Assert.assertEquals(message, erreur.getMessage());
			Assert.assertNull(evt.getCommentaireTraitement());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testNettoyageCacheAvantDecisionStrategie() throws Exception {

		final long noIndividu = 12546744578L;
		final RegDate dateNaissance = date(1965, 3, 12);
		final RegDate dateDepart = date(2011, 12, 6);

		final CacheManager cacheManager = getBean(CacheManager.class, "ehCacheManager");

		// créée le service civil et un cache par devant
		final IndividuConnectorCache cache = new IndividuConnectorCache();
		cache.setCache(cacheManager.getCache("serviceCivil"));
		cache.setCivilDataEventService(getBean(CivilDataEventService.class, "civilDataEventService"));
		cache.setTarget(new MockIndividuConnector() {
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
			doInNewTransactionAndSession(status -> {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateNaissance.addYears(18), MotifFor.MAJORITE, MockCommune.Lausanne);
				return pp.getNumero();
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
					oldAddress.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD, MockCommune.Cossonay.getNoOFS(), null));

					final MockAdresse newAddress = new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateDepart.getOneDayAfter(), null);
					newAddress.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD, MockCommune.Lausanne.getNoOFS(), null));
					individu.addAdresse(newAddress);
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
				Assert.assertEquals(MockLocalite.Lausanne.getNomAbrege(), adresseAvantModif.getLocalite());
			}

			// maintenant, on fait arriver un événement de départ (qui correspond au changement d'adresse)
			final long evtId = doInNewTransactionAndSession(status -> {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateDepart);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.DEPART);
				return hibernateTemplate.merge(evt).getId();
			});

			// traitement de l'événement
			traiterEvenements(noIndividu);

			// vérification du résultat (il doit avoir été ignoré, le bug que nous avions dans SIFISC-4882 était que l'événement partait en erreur
			// avec le message "impossible de déterminer si départ principal ou secondaire" car le cache n'avait pas encore été invalidé au moment
			// où on inspectait les adresses pour retrouver celle qui se terminait à la date de l'événement)
			doInNewTransactionAndSession(status -> {
				final EvenementCivilEch evt = evtCivilDAO.get(evtId);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
				return null;
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
				Assert.assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), oldAddress.getLocalisationSuivante().getNoOfs());
				Assert.assertEquals(LocalisationType.CANTON_VD, oldAddress.getLocalisationSuivante().getType());
				Assert.assertEquals(dateDepart, oldAddress.getDateFin());
				Assert.assertEquals(MockLocalite.Lausanne.getNomAbrege(), oldAddress.getLocalite());

				final Adresse newAddress = iterator.next();
				Assert.assertNotNull(newAddress);
				Assert.assertNotNull(newAddress.getLocalisationPrecedente());
				Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), newAddress.getLocalisationPrecedente().getNoOfs());
				Assert.assertEquals(LocalisationType.CANTON_VD, newAddress.getLocalisationPrecedente().getType());
				Assert.assertEquals(MockLocalite.CossonayVille.getNomAbrege(), newAddress.getLocalite());
			}
		}
		finally {
			cache.destroy();
		}
	}

	//Test le cas d'un depart vaudois en secondaire avec une destination en secondaire
	@Test(timeout = 10000L)
	public void testDepartResidenceSecondaireVersSecondaire() throws Exception {

		final long noIndividu = 126673246L;
		final RegDate depart = date(2011, 10, 31);
		final RegDate arrivee = date(2003, 1, 1);

		// le p'tit nouveau
		serviceCivil.setUp(new DefaultMockIndividuConnector(false) {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(1967, 4, 23);
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Enrique", "Luis", true);
				final MockAdresse adresseVaudoise = addAdresse(ind, TypeAdresseCivil.SECONDAIRE, MockRue.Pully.CheminDesRoches, null, arrivee, depart);
				adresseVaudoise.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD, MockCommune.Echallens.getNoOFS(), null));
				addAdresse(ind, TypeAdresseCivil.SECONDAIRE, MockRue.Echallens.GrandRue, null, depart.getOneDayAfter(), null);
				addNationalite(ind, MockPays.Espagne, dateNaissance, null);
			}
		});

		doInNewTransactionAndSession(status -> {
			addHabitant(noIndividu);
			return null;
		});

		// événement d'arrivée
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(depart);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.DEPART);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
			return null;
		});

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
			Assert.assertEquals("Ignoré car départ secondaire vaudois : la nouvelle commune de résidence Echallens est toujours dans le canton.", evt.getCommentaireTraitement());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testDepartPrincipalAvecDateFinAdresseDifferenteSansDecalageAdmis() throws Exception {

		final long noIndividu = 467425267L;
		final RegDate dateArrivee = date(2000, 1, 1);
		final RegDate dateFinAdresse = date(2012, 5, 31);
		final RegDate dateEvtDepart = dateFinAdresse.addDays(1);

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, date(1974, 9, 17), "Clette", "Lara", false);
				final MockAdresse adresse = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeLaGare, null, dateArrivee, dateFinAdresse);
				adresse.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD, MockCommune.Aubonne.getNoOFS(), null));
			}
		});

		// mise en place fiscale
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			return pp.getNumero();
		});

		// création de l'événement civil de départ
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(217483457L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateEvtDepart);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.DEPART);
			return hibernateTemplate.merge(evt).getId();
		});

		buildStrategyOverridingTranslatorAndProcessor(true, new StrategyOverridingCallback() {
			@Override
			public void overrideStrategies(EvenementCivilEchTranslatorImplOverride translator) {
				translator.overrideStrategy(TypeEvenementCivilEch.DEPART, ActionEvenementCivilEch.PREMIERE_LIVRAISON, new DepartEchTranslationStrategy(0));     // aucun décalage accepté
			}
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement de l'événement (ignoré car départ vaudois)
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());

			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			Assert.assertNotNull(erreurs);
			Assert.assertEquals(1, erreurs.size());

			final EvenementCivilEchErreur erreur = erreurs.iterator().next();
			Assert.assertNotNull(erreur);
			Assert.assertEquals("Aucune adresse principale ou secondaire ne se termine à la date de l'événement.", erreur.getMessage());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testDepartPrincipalAvecDateFinAdresseDifferenteMaisAcceptable() throws Exception {

		final long noIndividu = 467425267L;
		final RegDate dateArrivee = date(2000, 1, 1);
		final RegDate dateFinAdresse = date(2012, 5, 31);
		final RegDate dateEvtDepart = dateFinAdresse.addDays(1);

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, date(1974, 9, 17), "Clette", "Lara", false);
				final MockAdresse adresse = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeLaGare, null, dateArrivee, dateFinAdresse);
				adresse.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD, MockCommune.Aubonne.getNoOFS(), null));
			}
		});

		// mise en place fiscale
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			return pp.getNumero();
		});

		// création de l'événement civil de départ
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(217483457L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateEvtDepart);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.DEPART);
			return hibernateTemplate.merge(evt).getId();
		});

		buildStrategyOverridingTranslatorAndProcessor(true, new StrategyOverridingCallback() {
			@Override
			public void overrideStrategies(EvenementCivilEchTranslatorImplOverride translator) {
				translator.overrideStrategy(TypeEvenementCivilEch.DEPART, ActionEvenementCivilEch.PREMIERE_LIVRAISON, new DepartEchTranslationStrategy(1));     // 1 jour de décalage accepté
			}
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement de l'événement (ignoré car départ vaudois)
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
			Assert.assertEquals("Ignoré car départ vaudois : la nouvelle commune de résidence Aubonne est toujours dans le canton.", evt.getCommentaireTraitement());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testDepartPrincipalAvecDateFinAdresseTropDifferente() throws Exception {

		final long noIndividu = 467425267L;
		final RegDate dateArrivee = date(2000, 1, 1);
		final RegDate dateFinAdresse = date(2012, 5, 31);
		final RegDate dateEvtDepart = dateFinAdresse.addDays(2);

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, date(1974, 9, 17), "Clette", "Lara", false);
				final MockAdresse adresse = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeLaGare, null, dateArrivee, dateFinAdresse);
				adresse.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD, MockCommune.Aubonne.getNoOFS(), null));
			}
		});

		// mise en place fiscale
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			return pp.getNumero();
		});

		// création de l'événement civil de départ
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(217483457L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateEvtDepart);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.DEPART);
			return hibernateTemplate.merge(evt).getId();
		});

		buildStrategyOverridingTranslatorAndProcessor(true, new StrategyOverridingCallback() {
			@Override
			public void overrideStrategies(EvenementCivilEchTranslatorImplOverride translator) {
				translator.overrideStrategy(TypeEvenementCivilEch.DEPART, ActionEvenementCivilEch.PREMIERE_LIVRAISON, new DepartEchTranslationStrategy(1));     // 1 jour de décalage accepté
			}
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement de l'événement (ignoré car départ vaudois)
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());

			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			Assert.assertNotNull(erreurs);
			Assert.assertEquals(1, erreurs.size());

			final EvenementCivilEchErreur erreur = erreurs.iterator().next();
			Assert.assertNotNull(erreur);
			Assert.assertEquals("Aucune adresse principale ou secondaire ne se termine 1 jour ou moins avant la date de l'événement.", erreur.getMessage());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testDepartSecondaireAvecDateFinAdresseDifferenteMaisAcceptable() throws Exception {
		final long noIndividu = 467425267L;
		final RegDate dateArrivee = date(2000, 1, 1);
		final RegDate dateFinAdresse = date(2012, 5, 31);
		final RegDate dateEvtDepart = dateFinAdresse.addDays(1);

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, date(1974, 9, 17), "Clette", "Lara", false);
				final MockAdresse adresse = addAdresse(ind, TypeAdresseCivil.SECONDAIRE, MockRue.Lausanne.AvenueDeLaGare, null, dateArrivee, dateFinAdresse);
				adresse.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD, MockCommune.Aubonne.getNoOFS(), null));
			}
		});

		// mise en place fiscale
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateArrivee, MotifFor.ACHAT_IMMOBILIER, MockCommune.Bern);
			addForSecondaire(pp, dateArrivee, MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne, MotifRattachement.IMMEUBLE_PRIVE);
			return pp.getNumero();
		});

		// création de l'événement civil de départ
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(217483457L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateEvtDepart);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.DEPART);
			return hibernateTemplate.merge(evt).getId();
		});

		buildStrategyOverridingTranslatorAndProcessor(true, new StrategyOverridingCallback() {
			@Override
			public void overrideStrategies(EvenementCivilEchTranslatorImplOverride translator) {
				translator.overrideStrategy(TypeEvenementCivilEch.DEPART, ActionEvenementCivilEch.PREMIERE_LIVRAISON, new DepartEchTranslationStrategy(1));     // 1 jour de décalage accepté
			}
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement de l'événement (ignoré car départ vaudois)
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
			Assert.assertEquals("Ignoré car départ secondaire vaudois : la nouvelle commune de résidence Aubonne est toujours dans le canton.", evt.getCommentaireTraitement());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testDepartSecondaireAvecDateFinAdresseTropDifferente() throws Exception {
		final long noIndividu = 467425267L;
		final RegDate dateArrivee = date(2000, 1, 1);
		final RegDate dateFinAdresse = date(2012, 5, 31);
		final RegDate dateEvtDepart = dateFinAdresse.addDays(2);

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, date(1974, 9, 17), "Clette", "Lara", false);
				final MockAdresse adresse = addAdresse(ind, TypeAdresseCivil.SECONDAIRE, MockRue.Lausanne.AvenueDeLaGare, null, dateArrivee, dateFinAdresse);
				adresse.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD, MockCommune.Aubonne.getNoOFS(), null));
			}
		});

		// mise en place fiscale
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateArrivee, MotifFor.ACHAT_IMMOBILIER, MockCommune.Bern);
			addForSecondaire(pp, dateArrivee, MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne, MotifRattachement.IMMEUBLE_PRIVE);
			return pp.getNumero();
		});

		// création de l'événement civil de départ
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(217483457L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateEvtDepart);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.DEPART);
			return hibernateTemplate.merge(evt).getId();
		});

		buildStrategyOverridingTranslatorAndProcessor(true, new StrategyOverridingCallback() {
			@Override
			public void overrideStrategies(EvenementCivilEchTranslatorImplOverride translator) {
				translator.overrideStrategy(TypeEvenementCivilEch.DEPART, ActionEvenementCivilEch.PREMIERE_LIVRAISON, new DepartEchTranslationStrategy(1));     // 1 jour de décalage accepté
			}
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement de l'événement (ignoré car départ vaudois)
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());

			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			Assert.assertNotNull(erreurs);
			Assert.assertEquals(1, erreurs.size());

			final EvenementCivilEchErreur erreur = erreurs.iterator().next();
			Assert.assertNotNull(erreur);
			Assert.assertEquals("Aucune adresse principale ou secondaire ne se termine 1 jour ou moins avant la date de l'événement.", erreur.getMessage());
			return null;
		});
	}

	/**
	 * Cas d'un couple d'anciens habitants (HS) dont un des membres revient seul (mais toujours marié) et repart
	 * --> vérification de l'endroit où est ouvert le for du couple après le deuxième départ
	 */
	@Test(timeout = 10000L)
	public void testDepartIndividuMarieAvecConjointDejaHorsSuisse() throws Exception {

		final long noIndividuLui = 346L;
		final long noIndividuElle = 326L;
		final RegDate dateMariage = date(1990, 4, 12);
		final RegDate dateDepartInitial = date(2000, 1, 1);
		final RegDate dateRetourMonsieur = date(2006, 5, 12);
		final RegDate dateNouveauDepartMonsieur = date(2010, 6, 30);

		// monsieur et madame sont mariés, partis hors-Suisse
		// seul monsieur revient... puis repart...
		// où est alors mis le for du couple après le départ de Monsieur ?

		// service civil
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu lui = addIndividu(noIndividuLui, null, "Petit", "Albert", Sexe.MASCULIN);
				final MockAdresse adrLui1 = addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateMariage, dateDepartInitial);
				adrLui1.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));
				final MockAdresse adrLui2 = addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Moudon.LeBourg, null, dateRetourMonsieur, dateNouveauDepartMonsieur);
				adrLui2.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));
				adrLui2.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE, MockPays.Allemagne.getNoOFS(), null));

				final MockIndividu elle = addIndividu(noIndividuElle, null, "Petit", "Françoise", Sexe.FEMININ);
				final MockAdresse adrElle1 = addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateMariage, dateDepartInitial);
				adrElle1.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));

				marieIndividus(lui, elle, dateMariage);
			}
		});

		// fiscal
		final long idMc = doInNewTransactionAndSession(status -> {
			final PersonnePhysique lui = addHabitant(noIndividuLui);
			final PersonnePhysique elle = tiersService.createNonHabitantFromIndividu(noIndividuElle);

			final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, null);
			final MenageCommun mc = couple.getMenage();
			addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateDepartInitial, MotifFor.DEPART_HS, MockCommune.Lausanne);
			addForPrincipal(mc, dateDepartInitial.getOneDayAfter(), MotifFor.DEPART_HS, dateRetourMonsieur.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.France);
			addForPrincipal(mc, dateRetourMonsieur, MotifFor.ARRIVEE_HS, MockCommune.Moudon);
			return mc.getNumero();
		});

		// création d'un événement civil de départ de monsieur
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(217483457L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateNouveauDepartMonsieur);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividuLui);
			evt.setType(TypeEvenementCivilEch.DEPART);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividuLui);

		// vérification de l'emplacement du for du couple après ça...
		doInNewTransactionAndSession(status -> {
			final MenageCommun mc = (MenageCommun) tiersDAO.get(idMc);
			Assert.assertNotNull(mc);

			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertEquals(dateNouveauDepartMonsieur.getOneDayAfter(), ffp.getDateDebut());
			Assert.assertNull(ffp.getDateFin());
			Assert.assertEquals(MotifFor.DEPART_HS, ffp.getMotifOuverture());
			Assert.assertNull(ffp.getMotifFermeture());
			Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
			Assert.assertEquals((Integer) MockPays.Allemagne.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
			return null;
		});
	}

	/**
	 * Test de non régression pour SIFISC_6068 (et 6382)
	 * @throws Exception
	 */
	@Test(timeout = 10000L)
	public void testDepartDepuisFraction() throws Exception {

		final long noIndividu = 132748428L;
		final RegDate dateNaissance = date(1956, 12, 5);
		final RegDate dateDepart = date(2012, 5, 12);

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Collins", "Felipe", Sexe.MASCULIN);
				addNationalite(ind, MockPays.Suisse, dateNaissance, null);

				final MockAdresse adr = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.LeSentier.GrandRue, null, dateNaissance, dateDepart);
				adr.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateNaissance.addYears(18), MotifFor.MAJORITE, MockCommune.Fraction.LeSentier);
			return pp.getNumero();
		});

		// création de l'événement civil de départ HS
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(12323L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateDepart);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.DEPART);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch ech = evtCivilDAO.get(evtId);
			Assert.assertNotNull(ech);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, ech.getEtat());

			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			Assert.assertNotNull(pp);
			Assert.assertFalse(pp.isHabitantVD());

			final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertEquals(MotifFor.DEPART_HS, ffp.getMotifOuverture());
			Assert.assertEquals(dateDepart.getOneDayAfter(), ffp.getDateDebut());
			Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
			Assert.assertEquals((Integer) MockPays.France.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testDepartContribuableDepenseNiSuisseNiPermisC() throws Exception {

		final long noIndividu = 45263L;
		final RegDate dateNaissance = date(1956, 12, 5);
		final RegDate dateDepart = date(2012, 5, 12);

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Collins", "Felipe", Sexe.MASCULIN);
				addNationalite(ind, MockPays.France, dateNaissance, null);
				final MockAdresse adr = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.LeSentier.GrandRue, null, dateNaissance, dateDepart);
				adr.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateNaissance.addYears(18), MotifFor.MAJORITE, MockCommune.Bussigny, ModeImposition.DEPENSE);
			return pp.getNumero();
		});

		// création de l'événement civil de départ HS
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(12323L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateDepart);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.DEPART);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification : le contribuable doit maintenant avoir un for source à l'étranger (F)
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch ech = evtCivilDAO.get(evtId);
			Assert.assertNotNull(ech);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, ech.getEtat());

			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			Assert.assertNotNull(pp);
			Assert.assertFalse(pp.isHabitantVD());

			final ForFiscalPrincipalPP ffp = pp.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertEquals(MotifFor.DEPART_HS, ffp.getMotifOuverture());
			Assert.assertEquals(dateDepart.getOneDayAfter(), ffp.getDateDebut());
			Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
			Assert.assertEquals((Integer) MockPays.France.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
			Assert.assertEquals(ModeImposition.SOURCE, ffp.getModeImposition());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testDepartContribuableDepensePermisC() throws Exception {

		final long noIndividu = 45263L;
		final RegDate dateNaissance = date(1956, 12, 5);
		final RegDate dateDepart = date(2012, 5, 12);

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Collins", "Felipe", Sexe.MASCULIN);
				addNationalite(ind, MockPays.France, dateNaissance, null);
				addPermis(ind, TypePermis.ETABLISSEMENT, dateNaissance, null, false);
				final MockAdresse adr = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.LeSentier.GrandRue, null, dateNaissance, dateDepart);
				adr.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateNaissance.addYears(18), MotifFor.MAJORITE, MockCommune.Bussigny, ModeImposition.DEPENSE);
			return pp.getNumero();
		});

		// création de l'événement civil de départ HS
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(12323L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateDepart);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.DEPART);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification : le contribuable doit maintenant avoir un for source à l'étranger (F)
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch ech = evtCivilDAO.get(evtId);
			Assert.assertNotNull(ech);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, ech.getEtat());

			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			Assert.assertNotNull(pp);
			Assert.assertFalse(pp.isHabitantVD());

			final ForFiscalPrincipalPP ffp = pp.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertEquals(MotifFor.DEPART_HS, ffp.getMotifOuverture());
			Assert.assertEquals(dateDepart.getOneDayAfter(), ffp.getDateDebut());
			Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
			Assert.assertEquals((Integer) MockPays.France.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
			Assert.assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testDepartContribuableDepenseAvecForSecondaireNiSuisseNiPermisC() throws Exception {
		final long noIndividu = 45263L;
		final RegDate dateNaissance = date(1956, 12, 5);
		final RegDate dateDepart = date(2012, 5, 12);
		final RegDate dateAchat = dateDepart.addYears(-2);

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Collins", "Felipe", Sexe.MASCULIN);
				addNationalite(ind, MockPays.France, dateNaissance, null);
				final MockAdresse adr = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.LeSentier.GrandRue, null, dateNaissance, dateDepart);
				adr.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateNaissance.addYears(18), MotifFor.MAJORITE, MockCommune.Bussigny, ModeImposition.DEPENSE);
			addForSecondaire(pp, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Aigle, MotifRattachement.IMMEUBLE_PRIVE);
			return pp.getNumero();
		});

		// création de l'événement civil de départ HS
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(12323L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateDepart);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.DEPART);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification : le contribuable doit maintenant avoir un for source à l'étranger (F)
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch ech = evtCivilDAO.get(evtId);
			Assert.assertNotNull(ech);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, ech.getEtat());

			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			Assert.assertNotNull(pp);
			Assert.assertFalse(pp.isHabitantVD());

			final ForFiscalPrincipalPP ffp = pp.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertEquals(MotifFor.DEPART_HS, ffp.getMotifOuverture());
			Assert.assertEquals(dateDepart.getOneDayAfter(), ffp.getDateDebut());
			Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
			Assert.assertEquals((Integer) MockPays.France.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
			Assert.assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());
			return null;
		});
	}

	/**
	 * SIFISC-8741
	 */
	@Test
	public void testDepartSecondaireMemeJourQueDemenagementDansCommuneVaudoise() throws Exception {

		final long noIndividu = 234732L;
		final RegDate dateDepart = date(2012, 4, 30);

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(1980, 10, 25);
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Alambic", "Ernest", Sexe.MASCULIN);
				addNationalite(ind, MockPays.Suisse, dateNaissance, null);

				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.BoulevardGrancy, null, dateNaissance, dateDepart);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.CheminDeMornex, null, dateDepart.getOneDayAfter(), null);

				final MockAdresse adrSecondaire = addAdresse(ind, TypeAdresseCivil.SECONDAIRE, MockRue.Echallens.GrandRue, null, dateDepart.addYears(-3), dateDepart);
				adrSecondaire.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE, MockPays.PaysInconnu.getNoOFS(), null));
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, date(2000, 1, 1), MotifFor.INDETERMINE, MockCommune.Lausanne);
			return pp.getNumero();
		});

		// création de l'individu de départ
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(4454544L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateDepart);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.DEPART);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividu);

		// vérification des résultats
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch ech = evtCivilDAO.get(evtId);
			Assert.assertNotNull(ech);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, ech.getEtat());

			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			Assert.assertNotNull(pp);

			// il s'agit d'un départ secondaire, donc le for principal ne devrait pas changer
			final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertEquals(date(2000, 1, 1), ffp.getDateDebut());
			Assert.assertNull(ffp.getDateFin());
			Assert.assertNull(ffp.getMotifFermeture());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
			Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
			return null;
		});
	}


	@Test(timeout = 10000L)
	public void testDepartCelibataireAvecDecisionAci() throws Exception {

		final long noIndividu = 126673246L;
		final RegDate depart = date(2011, 10, 31);
		final RegDate arrivee = date(2003, 1, 1);

		// le p'tit nouveau
		serviceCivil.setUp(new DefaultMockIndividuConnector(false) {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(1967, 4, 23);
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Enrique", "Luis", true);


				final MockAdresse adresseVaudoise = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, arrivee, depart);
				adresseVaudoise.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE, MockPays.Espagne.getNoOFS(), null));
				addNationalite(ind, MockPays.Espagne, dateNaissance, null);
			}
		});

		doInNewTransactionAndSession(status -> {
			PersonnePhysique luis = addHabitant(noIndividu);
			addForPrincipal(luis, arrivee, MotifFor.ARRIVEE_HS, MockCommune.Bussigny);
			addDecisionAci(luis, arrivee, null, MockCommune.Vevey.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null);
			return null;
		});

		// événement de départ
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(depart);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.DEPART);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
			final PersonnePhysique luis = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
			Assert.assertNotNull(luis);
			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			Assert.assertNotNull(erreurs);
			Assert.assertEquals(1, erreurs.size());
			final EvenementCivilEchErreur erreur = erreurs.iterator().next();
			String message = String.format("Le contribuable trouvé (%s) est sous l'influence d'une décision ACI",
			                               FormatNumeroHelper.numeroCTBToDisplay(luis.getNumero()));
			Assert.assertEquals(message, erreur.getMessage());
			return null;
		});
	}

	/**
	 *  SIFISC-11521 : traitement d'un départ HS pour lequel les adresses civiles font état d'une arrivée secondaire au lendemain du départ
	 */
	@Test(timeout = 10000L)
	public void testDepartPrincipalHorsSuisseSuiviArriveeSecondaireIndividuSeul() throws Exception {

		final long noIndividu = 4674L;
		final RegDate dateNaissance = date(1967, 5, 3);
		final RegDate dateDepart = date(2014, 3, 6);
		final RegDate dateArriveeSecondaire = dateDepart.getOneDayAfter();

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Pruno", "Firmin", Sexe.MASCULIN);

				final MockAdresse prn = addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, date(2000, 1, 1), dateDepart);
				prn.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE, MockPays.Albanie.getNoOFS(), null));

				addAdresse(individu, TypeAdresseCivil.SECONDAIRE, MockRue.Echallens.GrandRue, null, dateArriveeSecondaire, null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateNaissance.addYears(18), MotifFor.MAJORITE, MockCommune.Cossonay);
			return pp.getNumero();
		});

		final class Ids {
			long evtDepartId;
			long evtArriveeId;
		}

		// création d'un événement civil de départ et d'un autre d'arrivée (c'est le cas réel vu en production)
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Ids ids1 = new Ids();
			{
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateDepart);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.DEPART);
				ids1.evtDepartId = hibernateTemplate.merge(evt).getId();
			}
			{
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14533L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArriveeSecondaire);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				ids1.evtArriveeId = hibernateTemplate.merge(evt).getId();
			}
			return ids1;
		});

		// traitement des deux événements civils de l'individu
		traiterEvenements(noIndividu);

		// vérification du résultat
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evtDepart = evtCivilDAO.get(ids.evtDepartId);
			Assert.assertNotNull(evtDepart);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evtDepart.getEtat());

			final EvenementCivilEch evtArrivee = evtCivilDAO.get(ids.evtArriveeId);
			Assert.assertNotNull(evtArrivee);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evtDepart.getEtat());

			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			Assert.assertNotNull(pp);

			final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertFalse(ffp.isAnnule());
			Assert.assertEquals(dateDepart.getOneDayAfter(), ffp.getDateDebut());
			Assert.assertNull(ffp.getDateFin());
			Assert.assertEquals(MotifFor.DEPART_HS, ffp.getMotifOuverture());
			Assert.assertNull(ffp.getMotifFermeture());
			Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
			Assert.assertEquals((Integer) MockPays.Albanie.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
			return null;
		});
	}

	/**
	 *  SIFISC-11521 : traitement d'un départ HS pour lequel les adresses civiles font état d'une arrivée secondaire au lendemain du départ pour les deux individus d'un couple
	 */
	@Test(timeout = 10000L)
	public void testDepartPrincipalHorsSuisseSuiviArriveeSecondaireCouple() throws Exception {

		final long noIndividuLui = 4674L;
		final long noIndividuElle = 4262L;
		final RegDate dateMariage = date(1976, 4, 2);
		final RegDate dateDepart = date(2014, 3, 6);
		final RegDate dateArriveeSecondaire = dateDepart.getOneDayAfter();

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu lui = addIndividu(noIndividuLui, null, "Pruno", "Firmin", Sexe.MASCULIN);
				final MockIndividu elle = addIndividu(noIndividuElle, null, "Pruno", "Bécassine", Sexe.FEMININ);
				marieIndividus(lui, elle, dateMariage);

				{
					final MockAdresse prn = addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, date(2000, 1, 1), dateDepart);
					prn.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE, MockPays.Albanie.getNoOFS(), null));
					addAdresse(lui, TypeAdresseCivil.SECONDAIRE, MockRue.Echallens.GrandRue, null, dateArriveeSecondaire, null);
				}

				// dans un premier temps, "elle" n'est pas encore partie (= on ne le sait pas encore)
				addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, date(2000, 1, 1), null);
			}
		});

		// mise en place fiscale
		final long mcId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique lui = addHabitant(noIndividuLui);
			final PersonnePhysique elle = addHabitant(noIndividuElle);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, null);
			final MenageCommun mc = couple.getMenage();
			addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Cossonay);
			return mc.getNumero();
		});

		final class Ids {
			long evtDepartId;
			long evtArriveeId;
		}

		// création d'un événement civil de départ et d'un autre d'arrivée (c'est le cas réel vu en production)
		final Ids idsLui = doInNewTransactionAndSession(status -> {
			final Ids ids = new Ids();
			{
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateDepart);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividuLui);
				evt.setType(TypeEvenementCivilEch.DEPART);
				ids.evtDepartId = hibernateTemplate.merge(evt).getId();
			}
			{
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14533L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArriveeSecondaire);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividuLui);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				ids.evtArriveeId = hibernateTemplate.merge(evt).getId();
			}
			return ids;
		});

		// traitement des deux événements civils de l'individu LUI
		traiterEvenements(noIndividuLui);

		// vérification du résultat
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evtDepart = evtCivilDAO.get(idsLui.evtDepartId);
			Assert.assertNotNull(evtDepart);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evtDepart.getEtat());

			final EvenementCivilEch evtArrivee = evtCivilDAO.get(idsLui.evtArriveeId);
			Assert.assertNotNull(evtArrivee);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evtDepart.getEtat());

			final MenageCommun mc = (MenageCommun) tiersDAO.get(mcId);
			Assert.assertNotNull(mc);

			//
			// le couple a toujours son for vaudois, car "elle" n'est pas partie
			//

			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertFalse(ffp.isAnnule());
			Assert.assertEquals(dateMariage, ffp.getDateDebut());
			Assert.assertNull(ffp.getDateFin());
			Assert.assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffp.getMotifOuverture());
			Assert.assertNull(ffp.getMotifFermeture());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
			Assert.assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
			return null;
		});

		// son départ à elle-maintenant
		doModificationIndividu(noIndividuElle, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				final Collection<Adresse> adresses = individu.getAdresses();
				Assert.assertEquals(1, adresses.size());

				final MockAdresse prn = (MockAdresse) adresses.iterator().next();
				prn.setDateFinValidite(dateDepart);
				prn.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE, MockPays.Albanie.getNoOFS(), null));

				individu.addAdresse(new MockAdresse(TypeAdresseCivil.SECONDAIRE, MockRue.Echallens.GrandRue, null, dateArriveeSecondaire, null));
			}
		});

		// événements civils de départ/arrivée de Madame
		final Ids idsElle = doInNewTransactionAndSession(status -> {
			final Ids ids = new Ids();
			{
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14542L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateDepart);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividuElle);
				evt.setType(TypeEvenementCivilEch.DEPART);
				ids.evtDepartId = hibernateTemplate.merge(evt).getId();
			}
			{
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14543L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArriveeSecondaire);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividuElle);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				ids.evtArriveeId = hibernateTemplate.merge(evt).getId();
			}
			return ids;
		});

		// traitement des événements civils de Madame
		traiterEvenements(noIndividuElle);

		// et vérification des résultats... on aimerait bien que le départ HS ait été passé sur le ménage commun
		// (même si, au moment du second départ, "lui" (= conjoint) est en fait toujours considéré comme habitant
		// en raison de sa résidence secondaire)

		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evtDepart = evtCivilDAO.get(idsElle.evtDepartId);
			Assert.assertNotNull(evtDepart);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evtDepart.getEtat());

			final EvenementCivilEch evtArrivee = evtCivilDAO.get(idsElle.evtArriveeId);
			Assert.assertNotNull(evtArrivee);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evtDepart.getEtat());

			final MenageCommun mc = (MenageCommun) tiersDAO.get(mcId);
			Assert.assertNotNull(mc);

			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertFalse(ffp.isAnnule());
			Assert.assertEquals(dateDepart.getOneDayAfter(), ffp.getDateDebut());
			Assert.assertNull(ffp.getDateFin());
			Assert.assertEquals(MotifFor.DEPART_HS, ffp.getMotifOuverture());
			Assert.assertNull(ffp.getMotifFermeture());
			Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
			Assert.assertEquals((Integer) MockPays.Albanie.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
			return null;
		});
	}

	/**
	 * [SIFISC-14429] Tentative de rattrapage
	 * <ul>
	 *     <li>Deux conjoints d'un couple vaudois quittent le canton à des dates différentes</li>
	 *     <li>Les événements de départ sont traités dans l'ordre inverse de l'ordre des dates de départ</li>
	 * </ul>
	 */
	@Test(timeout = 10000L)
	public void testDepartConjointsDatesDifferentesTraitesOrdreInverse() throws Exception {

		//
		// Le meneur part d'abord, le suiveur... suit
		//

		final long noIndividuMeneur = 3674532L;
		final long noIndividuSuiveur = 3764325623L;
		final RegDate dateMariage = date(2005, 5, 1);
		final RegDate dateDepartMeneur = date(2014, 5, 30);
		final RegDate dateDepartSuiveur = dateDepartMeneur.addMonths(1);

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu meneur = addIndividu(noIndividuMeneur, null, "Dubalai", "Philibert", Sexe.MASCULIN);
				final MockIndividu suiveur = addIndividu(noIndividuSuiveur, null, "Dubalai", "Martina", Sexe.FEMININ);
				marieIndividus(meneur, suiveur, dateMariage);

				addAdresse(meneur, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateMariage, null);
				addAdresse(suiveur, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateMariage, null);
			}
		});

		final class Ids {
			long idMeneur;
			long idSuiveur;
			long idMenage;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique meneur = addHabitant(noIndividuMeneur);
			final PersonnePhysique suiveur = addHabitant(noIndividuSuiveur);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(meneur, suiveur, dateMariage, null);
			final MenageCommun mc = couple.getMenage();

			addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Cossonay);

			final Ids ids1 = new Ids();
			ids1.idMenage = mc.getNumero();
			ids1.idMeneur = meneur.getNumero();
			ids1.idSuiveur = suiveur.getNumero();
			return ids1;
		});

		//
		// traitement du premier départ : celui du suiveur (on les traite justement dans le désordre)
		// (départ vers l'Allemagne)
		//

		doModificationIndividu(noIndividuSuiveur, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				final Collection<Adresse> adresses = individu.getAdresses();
				Assert.assertEquals(1, adresses.size());

				final MockAdresse prn = (MockAdresse) adresses.iterator().next();
				prn.setDateFinValidite(dateDepartSuiveur);
				prn.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE, MockPays.Allemagne.getNoOFS(), null));
			}
		});

		final long evtSuiveurId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateDepartSuiveur);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividuSuiveur);
			evt.setType(TypeEvenementCivilEch.DEPART);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividuSuiveur);

		//
		// vérification du for sur le couple (à ce stade, il est encore vaudois, puisque le conjoint n'est pas connu comme parti)
		//

		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtSuiveurId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

			final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.idMenage);
			Assert.assertNotNull(mc);

			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertEquals(dateMariage, ffp.getDateDebut());
			Assert.assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffp.getMotifOuverture());
			Assert.assertNull(ffp.getDateFin());
			Assert.assertNull(ffp.getMotifFermeture());
			Assert.assertFalse(ffp.isAnnule());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
			Assert.assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
			return null;
		});

		//
		// traitement du deuxième départ : celui du meneur (on les traite justement dans le désordre)
		// (départ vers la France)
		//

		doModificationIndividu(noIndividuMeneur, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				final Collection<Adresse> adresses = individu.getAdresses();
				Assert.assertEquals(1, adresses.size());

				final MockAdresse prn = (MockAdresse) adresses.iterator().next();
				prn.setDateFinValidite(dateDepartMeneur);
				prn.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));
			}
		});

		final long evtMeneurId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(64748L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateDepartMeneur);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividuMeneur);
			evt.setType(TypeEvenementCivilEch.DEPART);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividuMeneur);

		//
		// vérification du for sur le couple (à ce stade, on s'attend, grace au rattrapage, à ce que le for soit passé en France depuis le lendemain de la date de départ du SUIVEUR
		// alors qu'on est bien en train de traiter un départ du MENEUR...)
		//

		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtMeneurId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());      // même autorité fiscale -> TRAITE

			final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.idMenage);
			Assert.assertNotNull(mc);

			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertEquals(dateDepartSuiveur.getOneDayAfter(), ffp.getDateDebut());
			Assert.assertEquals(MotifFor.DEPART_HS, ffp.getMotifOuverture());
			Assert.assertNull(ffp.getDateFin());
			Assert.assertNull(ffp.getMotifFermeture());
			Assert.assertFalse(ffp.isAnnule());
			Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
			Assert.assertEquals((Integer) MockPays.France.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());   // lieu du deuxième traitement;
			return null;
		});
	}

	/**
	 * [SIFISC-18224] Tentative de rattrapage
	 * <ul>
	 *     <li>Deux conjoints d'un couple vaudois quittent le canton à des dates différentes</li>
	 *     <li>Les événements de départ sont traités dans l'ordre inverse de l'ordre des dates de départ</li>
	 *     <li>Ils ne se rendent pas au même endroit</li>
	 * </ul>
	 */
	@Test(timeout = 10000L)
	public void testDepartConjointsDatesDifferentesTraitesOrdreInverseEtDestinationsDifferentes() throws Exception {

		//
		// Le meneur part d'abord, le suiveur... suit
		//

		final long noIndividuMeneur = 3674532L;
		final long noIndividuSuiveur = 3764325623L;
		final RegDate dateMariage = date(2005, 5, 1);
		final RegDate dateDepartMeneur = date(2014, 5, 30);
		final RegDate dateDepartSuiveur = dateDepartMeneur.addMonths(1);

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu meneur = addIndividu(noIndividuMeneur, null, "Dubalai", "Philibert", Sexe.MASCULIN);
				final MockIndividu suiveur = addIndividu(noIndividuSuiveur, null, "Dubalai", "Martina", Sexe.FEMININ);
				marieIndividus(meneur, suiveur, dateMariage);

				addAdresse(meneur, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateMariage, null);
				addAdresse(suiveur, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateMariage, null);
			}
		});

		final class Ids {
			long idMeneur;
			long idSuiveur;
			long idMenage;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique meneur = addHabitant(noIndividuMeneur);
			final PersonnePhysique suiveur = addHabitant(noIndividuSuiveur);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(meneur, suiveur, dateMariage, null);
			final MenageCommun mc = couple.getMenage();

			addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Cossonay);

			final Ids ids1 = new Ids();
			ids1.idMenage = mc.getNumero();
			ids1.idMeneur = meneur.getNumero();
			ids1.idSuiveur = suiveur.getNumero();
			return ids1;
		});

		//
		// traitement du premier départ : celui du suiveur (on les traite justement dans le désordre)
		// (celui-ci part en Allemagne)
		//

		doModificationIndividu(noIndividuSuiveur, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				final Collection<Adresse> adresses = individu.getAdresses();
				Assert.assertEquals(1, adresses.size());

				final MockAdresse prn = (MockAdresse) adresses.iterator().next();
				prn.setDateFinValidite(dateDepartSuiveur);
				prn.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE, MockPays.Allemagne.getNoOFS(), null));
			}
		});

		final long evtSuiveurId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateDepartSuiveur);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividuSuiveur);
			evt.setType(TypeEvenementCivilEch.DEPART);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividuSuiveur);

		//
		// vérification du for sur le couple (à ce stade, il est encore vaudois, puisque le conjoint n'est pas connu comme parti)
		//

		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtSuiveurId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

			final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.idMenage);
			Assert.assertNotNull(mc);

			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertEquals(dateMariage, ffp.getDateDebut());
			Assert.assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffp.getMotifOuverture());
			Assert.assertNull(ffp.getDateFin());
			Assert.assertNull(ffp.getMotifFermeture());
			Assert.assertFalse(ffp.isAnnule());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
			Assert.assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
			return null;
		});

		//
		// traitement du deuxième départ : celui du meneur (on les traite justement dans le désordre)
		// (celui-ci part à ZH)
		//

		doModificationIndividu(noIndividuMeneur, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				final Collection<Adresse> adresses = individu.getAdresses();
				Assert.assertEquals(1, adresses.size());

				final MockAdresse prn = (MockAdresse) adresses.iterator().next();
				prn.setDateFinValidite(dateDepartMeneur);
				prn.setLocalisationSuivante(new Localisation(LocalisationType.HORS_CANTON, MockCommune.Zurich.getNoOFS(), null));
			}
		});

		final long evtMeneurId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(64748L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateDepartMeneur);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividuMeneur);
			evt.setType(TypeEvenementCivilEch.DEPART);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividuMeneur);

		//
		// vérification du for sur le couple (à ce stade, on s'attend, grace au rattrapage, à ce que le for soit passé en Allemage depuis le lendemain de la date de départ du SUIVEUR
		// alors qu'on est bien en train de traiter un départ du MENEUR...)
		//

		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtMeneurId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.A_VERIFIER, evt.getEtat());      // types d'autorité fiscale différents -> A_VERIFIER

			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			Assert.assertNotNull(erreurs);
			Assert.assertEquals(1, erreurs.size());

			final EvenementCivilEchErreur warning = erreurs.iterator().next();
			Assert.assertNotNull(warning);
			Assert.assertEquals(TypeEvenementErreur.WARNING, warning.getType());
			Assert.assertEquals("Le type de destination entre les deux conjoints n'est pas identique (hors Suisse / hors canton). Veuillez contrôler la destination du for principal.", warning.getMessage());

			final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.idMenage);
			Assert.assertNotNull(mc);

			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertEquals(dateDepartSuiveur.getOneDayAfter(), ffp.getDateDebut());
			Assert.assertEquals(MotifFor.DEPART_HC, ffp.getMotifOuverture());
			Assert.assertNull(ffp.getDateFin());
			Assert.assertNull(ffp.getMotifFermeture());
			Assert.assertFalse(ffp.isAnnule());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ffp.getTypeAutoriteFiscale());
			Assert.assertEquals((Integer) MockCommune.Zurich.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());    // lieu du départ du deuxième traitement;
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testDepartConjointAvecMariToujoursVaudois() throws Exception {

		//
		// Le meneur part d'abord, le suiveur... suit
		//

		final long noIndividuMonsieur = 3674532L;
		final long noIndividuMadame = 3764325623L;
		final RegDate dateMariage = date(2005, 5, 1);
		final RegDate dateDepartMadame = date(2014, 5, 30);

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu individuMonsieur = addIndividu(noIndividuMonsieur, null, "Dubalai", "Philibert", Sexe.MASCULIN);
				final MockIndividu individuMadame = addIndividu(noIndividuMadame, null, "Dubalai", "Martina", Sexe.FEMININ);
				marieIndividus(individuMonsieur, individuMadame, dateMariage);

				addAdresse(individuMonsieur, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateMariage, null);
				addAdresse(individuMadame, TypeAdresseCivil.SECONDAIRE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateMariage, null);
			}
		});

		final class Ids {
			long idMonsieur;
			long idMadame;
			long idMenage;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique monsieur = addHabitant(noIndividuMonsieur);
			final PersonnePhysique madame = addHabitant(noIndividuMadame);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(monsieur, madame, dateMariage, null);
			final MenageCommun mc = couple.getMenage();

			addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Cossonay);


			final Ids ids1 = new Ids();
			ids1.idMenage = mc.getNumero();
			ids1.idMonsieur = monsieur.getNumero();
			ids1.idMadame = madame.getNumero();
			return ids1;
		});

		//
		//

		doModificationIndividu(noIndividuMadame, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				final Collection<Adresse> adresses = individu.getAdresses();
				Assert.assertEquals(1, adresses.size());

				final MockAdresse prn = (MockAdresse) adresses.iterator().next();
				prn.setDateFinValidite(dateDepartMadame);
				prn.setLocalisationSuivante(new Localisation(LocalisationType.HORS_CANTON, MockCommune.Geneve.getNoOFS(), null));
			}
		});

		final long evtMadameId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateDepartMadame);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividuMadame);
			evt.setType(TypeEvenementCivilEch.DEPART);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividuMadame);

		//
		// vérification du for sur le couple (à ce stade, il est encore vaudois, puisque le conjoint n'est pas connu comme parti)
		//

		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtMadameId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

			final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.idMenage);
			Assert.assertNotNull(mc);

			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertEquals(dateMariage, ffp.getDateDebut());
			Assert.assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffp.getMotifOuverture());
			Assert.assertNull(ffp.getDateFin());
			Assert.assertNull(ffp.getMotifFermeture());
			Assert.assertFalse(ffp.isAnnule());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
			Assert.assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
			return null;
		});

		//
		// traitement du deuxième départ : celui du meneur (on les traite justement dans le désordre)
		//

	}
}
