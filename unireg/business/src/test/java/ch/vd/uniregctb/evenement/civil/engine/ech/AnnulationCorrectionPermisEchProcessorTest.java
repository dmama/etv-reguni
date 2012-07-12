package ch.vd.uniregctb.evenement.civil.engine.ech;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockPermis;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;
import ch.vd.uniregctb.type.TypePermis;

public class AnnulationCorrectionPermisEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {
	
	@Test(timeout = 10000L)
	public void testAnnulationPermisC() throws Exception {
		
		final long noIndividu = 32167845L;
		final long noEventAnnonce = 238756L;
		final RegDate dateDebutPermis = date(2000, 1, 1);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Kaderate", "Yamamoto", true);
				ind.setPermis(new MockPermis(dateDebutPermis, null, null, TypePermis.ANNUEL));

				final MockIndividu indEvent = createIndividu(noIndividu, null, "Kaderate", "Yamamoto", true);
				indEvent.setPermis(new MockPermis(dateDebutPermis, null, null, TypePermis.ETABLISSEMENT));
				addIndividuFromEvent(noEventAnnonce, indEvent, dateDebutPermis, TypeEvenementCivilEch.CORR_CATEGORIE_ETRANGER);
			}
		});
		
		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateDebutPermis.addYears(-1), MotifFor.ARRIVEE_HS, dateDebutPermis.addDays(-1), MotifFor.PERMIS_C_SUISSE, MockCommune.Lausanne, MotifRattachement.DOMICILE, ModeImposition.SOURCE);
				addForPrincipal(pp, dateDebutPermis, MotifFor.PERMIS_C_SUISSE, MockCommune.Lausanne);
				return pp.getNumero();
			}
		});

		// création de l'événement d'annulation d'obtention de permis
		final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(4678435674235674L);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.CORR_CATEGORIE_ETRANGER);
				evt.setAction(ActionEvenementCivilEch.ANNULATION);
				evt.setRefMessageId(noEventAnnonce);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setDateEvenement(dateDebutPermis);
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
				
				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(ModeImposition.SOURCE, ffp.getModeImposition());
				Assert.assertNull(ffp.getDateFin());
				return null;
			}
		});
	}

	@Test(timeout = 10000L)
	public void testAnnulationPermisNonC() throws Exception {
		
		final long noIndividu = 32167845L;
		final long noEventAnnonce = 238756L;
		final RegDate dateDebutPermis = date(2000, 1, 1);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Kaderate", "Yamamoto", true);
				ind.setPermis(new MockPermis(dateDebutPermis, null, null, TypePermis.COURTE_DUREE));

				final MockIndividu indEvent = createIndividu(noIndividu, null, "Kaderate", "Yamamoto", true);
				indEvent.setPermis(new MockPermis(dateDebutPermis, null, null, TypePermis.ANNUEL));
				addIndividuFromEvent(noEventAnnonce, indEvent, dateDebutPermis, TypeEvenementCivilEch.CORR_CATEGORIE_ETRANGER);
			}
		});
		
		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, dateDebutPermis, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				ffp.setModeImposition(ModeImposition.SOURCE);
				return pp.getNumero();
			}
		});

		// création de l'événement d'annulation d'obtention de permis
		final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(4678435674235674L);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.CORR_CATEGORIE_ETRANGER);
				evt.setAction(ActionEvenementCivilEch.ANNULATION);
				evt.setRefMessageId(noEventAnnonce);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setDateEvenement(dateDebutPermis);
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
				
				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(ModeImposition.SOURCE, ffp.getModeImposition());
				Assert.assertNull(ffp.getDateFin());
				return null;
			}
		});
	}
}
