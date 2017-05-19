package ch.vd.uniregctb.evenement.civil.engine.ech;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.Heritage;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class AnnulationDecesEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {
	
	@Test(timeout = 10000L)
	public void testAnnulationDeces() throws Exception {
		
		final long noIndividu = 3289432164723L;
		final RegDate dateDeces = date(2012, 2, 15);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(1945, 5, 8), "Guèréfini", "Carla", false);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(2000, 1, 1), null);
			}
		});
		
		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
			pp.setDateDeces(dateDeces);
			addForPrincipal(pp, date(2000, 1, 1), MotifFor.INDETERMINE, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);
			return pp.getNumero();
		});
		
		// création de l'événement civil d'annulation de décès
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(23562435641278L);
			evt.setType(TypeEvenementCivilEch.DECES);
			evt.setAction(ActionEvenementCivilEch.ANNULATION);
			evt.setDateEvenement(dateDeces);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			return hibernateTemplate.merge(evt).getId();
		});
		
		// traitement de l'événement civil
		traiterEvenements(noIndividu);
		
		// vérification
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtId);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
				
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				Assert.assertNull(pp.getDateDeces());
				Assert.assertTrue(pp.isHabitantVD());
				
				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertNull(ffp.getDateFin());
			}
		});
	}

	@Test(timeout = 10000)
	public void testAnnulationDecesEnPresenceDeRelationsDHeritage() throws Exception {

		final long noIndividu = 3289432164723L;
		final RegDate dateDeces = date(2012, 2, 15);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(1945, 5, 8), "Guèréfini", "Carla", false);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(2000, 1, 1), null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
			pp.setDateDeces(dateDeces);
			addForPrincipal(pp, date(2000, 1, 1), MotifFor.INDETERMINE, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);

			final PersonnePhysique heritier = addNonHabitant("Cylla", "Guèréfini", null, Sexe.FEMININ);
			addHeritage(heritier, pp, dateDeces.getOneDayAfter(), null);

			return pp.getNumero();
		});

		// création de l'événement civil d'annulation de décès
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(23562435641278L);
			evt.setType(TypeEvenementCivilEch.DECES);
			evt.setAction(ActionEvenementCivilEch.ANNULATION);
			evt.setDateEvenement(dateDeces);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividu);

		// vérification
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtId);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				Assert.assertNull(pp.getDateDeces());
				Assert.assertTrue(pp.isHabitantVD());

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertNull(ffp.getDateFin());

				pp.getRapportsObjet().stream()
						.filter(Heritage.class::isInstance)
						.filter(AnnulableHelper::nonAnnule)
						.forEach(ret -> Assert.fail("Le rapport " + ret + " n'a pas été annulé !!"));
			}
		});
	}
}
