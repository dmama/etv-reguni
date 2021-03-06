package ch.vd.unireg.evenement.civil.engine.ech;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEch;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchErreur;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.civil.mock.MockPermis;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementCivilEch;
import ch.vd.unireg.type.TypePermis;

import static org.junit.Assert.assertEquals;

public class AnnulationPermisEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {
	
	@Test(timeout = 10000L)
	public void testAnnulationPermisC() throws Exception {
		
		final long noIndividu = 32167845L;
		final long noEventAnnonce = 238756L;
		final RegDate dateDebutPermis = date(2000, 1, 1);

		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Kaderate", "Yamamoto", true);
				ind.setPermis(new MockPermis(dateDebutPermis, null, null, TypePermis.SEJOUR));

				final MockIndividu indEvent = createIndividu(noIndividu, null, "Kaderate", "Yamamoto", true);
				indEvent.setPermis(new MockPermis(dateDebutPermis, null, null, TypePermis.ETABLISSEMENT));
				addIndividuAfterEvent(noEventAnnonce, indEvent, dateDebutPermis, TypeEvenementCivilEch.CHGT_CATEGORIE_ETRANGER);
			}
		});
		
		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateDebutPermis.addYears(-1), MotifFor.ARRIVEE_HS, dateDebutPermis.addDays(-1), MotifFor.PERMIS_C_SUISSE, MockCommune.Lausanne, MotifRattachement.DOMICILE, ModeImposition.SOURCE);
			addForPrincipal(pp, dateDebutPermis, MotifFor.PERMIS_C_SUISSE, MockCommune.Lausanne);
			return pp.getNumero();
		});

		// création de l'événement d'annulation d'obtention de permis
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(4678435674235674L);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.CHGT_CATEGORIE_ETRANGER);
			evt.setAction(ActionEvenementCivilEch.ANNULATION);
			evt.setRefMessageId(noEventAnnonce);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setDateEvenement(dateDebutPermis);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividu);

		// vérification des résultats
		doInNewTransaction(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			Assert.assertNotNull(pp);

			final ForFiscalPrincipalPP ffp = pp.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertEquals(ModeImposition.SOURCE, ffp.getModeImposition());
			Assert.assertNull(ffp.getDateFin());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testAnnulationPermisNonC() throws Exception {
		
		final long noIndividu = 32167845L;
		final long noEventAnnonce = 238756L;
		final RegDate dateDebutPermis = date(2000, 1, 1);

		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Kaderate", "Yamamoto", true);
				ind.setPermis(new MockPermis(dateDebutPermis, null, null, TypePermis.COURTE_DUREE));

				final MockIndividu indEvent = createIndividu(noIndividu, null, "Kaderate", "Yamamoto", true);
				indEvent.setPermis(new MockPermis(dateDebutPermis, null, null, TypePermis.SEJOUR));
				addIndividuAfterEvent(noEventAnnonce, indEvent, dateDebutPermis, TypeEvenementCivilEch.CHGT_CATEGORIE_ETRANGER);
			}
		});
		
		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateDebutPermis, MotifFor.ARRIVEE_HS, MockCommune.Lausanne, ModeImposition.SOURCE);
			return pp.getNumero();
		});

		// création de l'événement d'annulation d'obtention de permis
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(4678435674235674L);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.CHGT_CATEGORIE_ETRANGER);
			evt.setAction(ActionEvenementCivilEch.ANNULATION);
			evt.setRefMessageId(noEventAnnonce);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setDateEvenement(dateDebutPermis);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividu);

		// vérification des résultats
		doInNewTransaction(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			Assert.assertNotNull(pp);

			final ForFiscalPrincipalPP ffp = pp.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertEquals(ModeImposition.SOURCE, ffp.getModeImposition());
			Assert.assertNull(ffp.getDateFin());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testAnnulationPermisCAvecDecisionAci() throws Exception {

		final long noIndividu = 32167845L;
		final long noEventAnnonce = 238756L;
		final RegDate dateDebutPermis = date(2000, 1, 1);

		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Kaderate", "Yamamoto", true);
				ind.setPermis(new MockPermis(dateDebutPermis, null, null, TypePermis.SEJOUR));

				final MockIndividu indEvent = createIndividu(noIndividu, null, "Kaderate", "Yamamoto", true);
				indEvent.setPermis(new MockPermis(dateDebutPermis, null, null, TypePermis.ETABLISSEMENT));
				addIndividuAfterEvent(noEventAnnonce, indEvent, dateDebutPermis, TypeEvenementCivilEch.CHGT_CATEGORIE_ETRANGER);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateDebutPermis.addYears(-1), MotifFor.ARRIVEE_HS, dateDebutPermis.addDays(-1), MotifFor.PERMIS_C_SUISSE, MockCommune.Lausanne, MotifRattachement.DOMICILE, ModeImposition.SOURCE);
			addDecisionAci(pp, dateDebutPermis.addYears(-5), null, MockCommune.Vevey.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null);
			return pp.getNumero();
		});

		// création de l'événement d'annulation d'obtention de permis
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(4678435674235674L);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.CHGT_CATEGORIE_ETRANGER);
			evt.setAction(ActionEvenementCivilEch.ANNULATION);
			evt.setRefMessageId(noEventAnnonce);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setDateEvenement(dateDebutPermis);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividu);

		// vérification des résultats
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
			final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
			Assert.assertNotNull(pp);
			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			Assert.assertNotNull(erreurs);
			Assert.assertEquals(1, erreurs.size());
			final EvenementCivilEchErreur erreur = erreurs.iterator().next();
			String message = String.format("Le contribuable trouvé (%s) est sous l'influence d'une décision ACI",
			                               FormatNumeroHelper.numeroCTBToDisplay(pp.getNumero()));
			Assert.assertEquals(message, erreur.getMessage());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testAnnulationPermisNonCAvecDecisionAci() throws Exception {

		final long noIndividu = 32167845L;
		final long noEventAnnonce = 238756L;
		final RegDate dateDebutPermis = date(2000, 1, 1);

		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Kaderate", "Yamamoto", true);
				ind.setPermis(new MockPermis(dateDebutPermis, null, null, TypePermis.COURTE_DUREE));

				final MockIndividu indEvent = createIndividu(noIndividu, null, "Kaderate", "Yamamoto", true);
				indEvent.setPermis(new MockPermis(dateDebutPermis, null, null, TypePermis.SEJOUR));
				addIndividuAfterEvent(noEventAnnonce, indEvent, dateDebutPermis, TypeEvenementCivilEch.CHGT_CATEGORIE_ETRANGER);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateDebutPermis, MotifFor.ARRIVEE_HS, MockCommune.Lausanne, ModeImposition.SOURCE);
			addDecisionAci(pp, dateDebutPermis.addYears(-5), null, MockCommune.Vevey.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null);
			return pp.getNumero();
		});

		// création de l'événement d'annulation d'obtention de permis
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(4678435674235674L);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.CHGT_CATEGORIE_ETRANGER);
			evt.setAction(ActionEvenementCivilEch.ANNULATION);
			evt.setRefMessageId(noEventAnnonce);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setDateEvenement(dateDebutPermis);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividu);

		// vérification des résultats
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
			final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
			Assert.assertNotNull(pp);
			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			Assert.assertNotNull(erreurs);
			Assert.assertEquals(1, erreurs.size());
			final EvenementCivilEchErreur erreur = erreurs.iterator().next();
			String message = String.format("Le contribuable trouvé (%s) est sous l'influence d'une décision ACI",
			                               FormatNumeroHelper.numeroCTBToDisplay(pp.getNumero()));
			Assert.assertEquals(message, erreur.getMessage());
			return null;
		});
	}
}
