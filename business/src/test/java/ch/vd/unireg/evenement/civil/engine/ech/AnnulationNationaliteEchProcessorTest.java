package ch.vd.unireg.evenement.civil.engine.ech;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockPermis;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEch;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchErreur;
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

public class AnnulationNationaliteEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {
	
	@Test(timeout = 10000L)
	public void testAnnulationNationaliteSuisse() throws Exception {
		
		final long noIndividu = 32167845L;
		final long noEventAnnonce = 238756L;
		final long noEventAnnulation = 4678435674235674L;
		final RegDate dateDebutNationalite = date(2000, 1, 1);
		final RegDate dateArrivee = date(2005, 1, 1);
		final RegDate dateDebutNationaliteSuisse = date(2012, 1, 1);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Kaderate", "Yamamoto", true);
				addNationalite(ind, MockPays.Albanie, dateDebutNationalite, null);
				ind.setPermis(new MockPermis(dateDebutNationalite, null, null, TypePermis.SEJOUR));
			}
		});
		
		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, dateDebutNationaliteSuisse.addDays(-1), MotifFor.PERMIS_C_SUISSE, MockCommune.Lausanne, MotifRattachement.DOMICILE, ModeImposition.SOURCE);
				addForPrincipal(pp, dateDebutNationaliteSuisse, MotifFor.PERMIS_C_SUISSE, MockCommune.Lausanne);
				return pp.getNumero();
			}
		});

		// création de l'événement d'annulation de nationalité
		final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(noEventAnnulation);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.NATURALISATION);
				evt.setAction(ActionEvenementCivilEch.ANNULATION);
				evt.setRefMessageId(noEventAnnonce);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setDateEvenement(dateDebutNationaliteSuisse);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividu);

		// vérification des résultats
		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
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
			}
		});
	}

	@Test(timeout = 10000L)
	public void testAnnulationNationaliteNonSuisse() throws Exception {

		final long noIndividu = 32167845L;
		final long noEventAnnonce = 238756L;
		final long noEventAnnulation = 4678435674235674L;
		final RegDate dateDebutNationalite = date(2000, 1, 1);
		final RegDate dateArrivee = date(2005, 1, 1);
		final RegDate dateDebutNationaliteNonSuisse = date(2012, 1, 1);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Kaderate", "Yamamoto", true);
				addNationalite(ind, MockPays.Albanie, dateDebutNationalite, null);
				ind.setPermis(new MockPermis(dateDebutNationalite, null, null, TypePermis.SEJOUR));
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Lausanne, ModeImposition.SOURCE);
				return pp.getNumero();
			}
		});

		// création de l'événement d'annulation de nationalité
		final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(noEventAnnulation);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.CHGT_NATIONALITE_ETRANGERE);
				evt.setAction(ActionEvenementCivilEch.ANNULATION);
				evt.setRefMessageId(noEventAnnonce);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setDateEvenement(dateDebutNationaliteNonSuisse);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividu);

		// vérification des résultats
		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
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
			}
		});
	}

	@Test(timeout = 10000L)
	public void testAnnulationNationaliteSuisseAvecDecisionAci() throws Exception {

		final long noIndividu = 32167845L;
		final long noEventAnnonce = 238756L;
		final long noEventAnnulation = 4678435674235674L;
		final RegDate dateDebutNationalite = date(2000, 1, 1);
		final RegDate dateArrivee = date(2005, 1, 1);
		final RegDate dateDebutNationaliteSuisse = date(2012, 1, 1);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Kaderate", "Yamamoto", true);
				addNationalite(ind, MockPays.Albanie, dateDebutNationalite, null);
				ind.setPermis(new MockPermis(dateDebutNationalite, null, null, TypePermis.SEJOUR));
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, dateDebutNationaliteSuisse.addDays(-1), MotifFor.PERMIS_C_SUISSE, MockCommune.Lausanne, MotifRattachement.DOMICILE, ModeImposition.SOURCE);
				addForPrincipal(pp, dateDebutNationaliteSuisse, MotifFor.PERMIS_C_SUISSE, MockCommune.Lausanne);
				addDecisionAci(pp,dateArrivee,null,MockCommune.Vevey.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,null);
				return pp.getNumero();
			}
		});

		// création de l'événement d'annulation de nationalité
		final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(noEventAnnulation);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.NATURALISATION);
				evt.setAction(ActionEvenementCivilEch.ANNULATION);
				evt.setRefMessageId(noEventAnnonce);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setDateEvenement(dateDebutNationaliteSuisse);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtId);
				org.junit.Assert.assertNotNull(evt);
				assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				org.junit.Assert.assertNotNull(pp);
				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				org.junit.Assert.assertNotNull(erreurs);
				org.junit.Assert.assertEquals(1, erreurs.size());
				final EvenementCivilEchErreur erreur = erreurs.iterator().next();
				String message = String.format("Le contribuable trouvé (%s) est sous l'influence d'une décision ACI",
						FormatNumeroHelper.numeroCTBToDisplay(pp.getNumero()));
				org.junit.Assert.assertEquals(message, erreur.getMessage());
				return null;
			}
		});
	}


}
