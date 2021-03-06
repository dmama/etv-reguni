package ch.vd.unireg.evenement.civil.engine.ech;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEch;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeEvenementCivilEch;

public class AnnulationVeuvageEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {
	
	@Test(timeout = 10000L)
	public void testAnnulationVeuvage() throws Exception {

		final long noIndividuLui = 4678345236723517L;
		final long noIndividuElle = 454624L;
		final RegDate dateMariage = date(1978, 4, 14);
		final RegDate dateDeces = date(2012, 1, 31);

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu lui = addIndividu(noIndividuLui, null, "Lesurvivant", "Ken", true);
				addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Epesses.RueDeLaMottaz, null, null, null);
				final MockIndividu elle = addIndividu(noIndividuElle, null, "Lapamorte", "Kelly", false);
				addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Epesses.RueDeLaMottaz, null, null, null);
				marieIndividus(lui, elle, dateMariage);
			}
		});

		// mise en place fiscale avec décès
		final long mcId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique lui = addHabitant(noIndividuLui);
			final PersonnePhysique elle = tiersService.createNonHabitantFromIndividu(noIndividuElle);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, dateDeces);
			final MenageCommun mc = couple.getMenage();
			addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);
			addForPrincipal(lui, dateDeces.getOneDayAfter(), MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);
			elle.setDateDeces(dateDeces);
			return mc.getNumero();
		});

		// événement civil d'annulation de veuvage
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(235672453L);
			evt.setType(TypeEvenementCivilEch.CHGT_ETAT_CIVIL_PARTENAIRE);
			evt.setAction(ActionEvenementCivilEch.ANNULATION);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividuLui);
			evt.setDateEvenement(dateDeces);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividuLui);

		// vérification des résultats
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

			final MenageCommun mc = (MenageCommun) tiersDAO.get(mcId);
			Assert.assertNotNull(mc);

			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertNull(ffp.getDateFin());
			return null;
		});
	}

    /*
     * [SIFISC-5053] L'annulation de veuvage doit être redonante si elle survient après une annulation de deces
     */
    @Test(timeout = 10000L)
    public void testAnnulationVeuvageApresAnnulationDeDeces() throws Exception {

	    final long noIndividuLui = 4678345236723517L;
	    final long noIndividuElle = 454624L;
	    final RegDate dateMariage = date(1978, 4, 14);
	    final RegDate dateDeces = date(2012, 1, 31);

	    // mise en place civile
	    serviceCivil.setUp(new MockIndividuConnector() {
		    @Override
		    protected void init() {
			    final MockIndividu lui = addIndividu(noIndividuLui, null, "Lesurvivant", "Ken", true);
			    addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Epesses.RueDeLaMottaz, null, null, null);
			    final MockIndividu elle = addIndividu(noIndividuElle, null, "Lapamorte", "Kelly", false);
			    addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Epesses.RueDeLaMottaz, null, null, null);
			    marieIndividus(lui, elle, dateMariage);
		    }
	    });

	    // mise en place fiscale avec décès
	    final long mcId = doInNewTransactionAndSession(status -> {
		    final PersonnePhysique lui = addHabitant(noIndividuLui);
		    final PersonnePhysique elle = tiersService.createNonHabitantFromIndividu(noIndividuElle);
		    final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, dateDeces);
		    final MenageCommun mc = couple.getMenage();
		    addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);
		    addForPrincipal(lui, dateDeces.getOneDayAfter(), MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);
		    elle.setDateDeces(dateDeces);
		    return mc.getNumero();
	    });

	    // événement civil d'annulation de deces
	    final long evtIdAnnulationDeces = doInNewTransactionAndSession(status -> {
		    final EvenementCivilEch evt = new EvenementCivilEch();
		    evt.setId(235672453L);
		    evt.setType(TypeEvenementCivilEch.DECES);
		    evt.setAction(ActionEvenementCivilEch.ANNULATION);
		    evt.setEtat(EtatEvenementCivil.A_TRAITER);
		    evt.setNumeroIndividu(noIndividuElle);
		    evt.setDateEvenement(dateDeces);
		    return hibernateTemplate.merge(evt).getId();
	    });

	    // traitement de l'événement
	    traiterEvenements(noIndividuElle);

	    // vérification des résultats
	    doInNewTransactionAndSession(status -> {
		    final EvenementCivilEch evt = evtCivilDAO.get(evtIdAnnulationDeces);
		    Assert.assertNotNull(evt);
		    Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

		    final MenageCommun mc = (MenageCommun) tiersDAO.get(mcId);
		    Assert.assertNotNull(mc);

		    final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
		    Assert.assertNotNull(ffp);
		    Assert.assertNull(ffp.getDateFin());
		    return null;
	    });

	    // événement civil d'annulation de veuvage
	    final long evtIdAnnulationVeuvage = doInNewTransactionAndSession(status -> {
		    final EvenementCivilEch evt = new EvenementCivilEch();
		    evt.setId(235672454L);
		    evt.setType(TypeEvenementCivilEch.CHGT_ETAT_CIVIL_PARTENAIRE);
		    evt.setAction(ActionEvenementCivilEch.ANNULATION);
		    evt.setEtat(EtatEvenementCivil.A_TRAITER);
		    evt.setNumeroIndividu(noIndividuLui);
		    evt.setDateEvenement(dateDeces);
		    return hibernateTemplate.merge(evt).getId();
	    });

	    // traitement de l'événement
	    traiterEvenements(noIndividuLui);

	    // vérification des résultats (L'annualtion doit être redondante
	    doInNewTransactionAndSession(status -> {
		    final EvenementCivilEch evt = evtCivilDAO.get(evtIdAnnulationVeuvage);
		    Assert.assertNotNull(evt);
		    Assert.assertEquals(EtatEvenementCivil.REDONDANT, evt.getEtat());
		    return null;
	    });

    }
}
