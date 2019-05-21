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
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementCivilEch;

public class AnnulationAbsenceEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {
	
	@Test(timeout = 10000L)
	public void testAnnulationAbsence() throws Exception {

		final long noIndividu = 3289432164723L;
		final RegDate dateAbsence = date(2012, 2, 15);

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(1945, 5, 8), "Guèréfini", "Carla", false);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(2000, 1, 1), null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
			pp.setDateDeces(dateAbsence);
			addForPrincipal(pp, date(2000, 1, 1), MotifFor.INDETERMINE, dateAbsence, MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);
			return pp.getNumero();
		});

		// création de l'événement civil d'annulation d'absence
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(23562435641278L);
			evt.setType(TypeEvenementCivilEch.ABSENCE);
			evt.setAction(ActionEvenementCivilEch.ANNULATION);
			evt.setDateEvenement(dateAbsence);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividu);

		// vérification
		doInNewTransactionAndSession(status -> {
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
			return null;
		});
	}
	@Test(timeout = 10000L)
	public void testAnnulationAbsenceAvecDecisionAci() throws Exception {

		final long noIndividu = 3289432164723L;
		final RegDate dateAbsence = date(2012, 2, 15);

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(1945, 5, 8), "Guèréfini", "Carla", false);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(2000, 1, 1), null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
			pp.setDateDeces(dateAbsence);
			addForPrincipal(pp, date(2000, 1, 1), MotifFor.INDETERMINE, dateAbsence, MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);
			addDecisionAci(pp, date(2005, 6, 1), null, MockCommune.Aubonne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null);
			return pp.getNumero();
		});

		// création de l'événement civil d'annulation d'absence
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(23562435641278L);
			evt.setType(TypeEvenementCivilEch.ABSENCE);
			evt.setAction(ActionEvenementCivilEch.ANNULATION);
			evt.setDateEvenement(dateAbsence);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividu);

		// vérification
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
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
