package ch.vd.uniregctb.evenement.civil.engine.ech;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
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

public class AnnulationVeuvageEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {
	
	@Test(timeout = 10000L)
	public void testAnnulationVeuvage() throws Exception {
		
		final long noIndividuLui = 4678345236723517L;
		final long noIndividuElle = 454624L;
		final RegDate dateMariage = date(1978, 4, 14);
		final RegDate dateDeces = date(2012, 1, 31);
		
		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
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
		final long mcId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addHabitant(noIndividuLui);
				final PersonnePhysique elle = addHabitant(noIndividuElle);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, dateDeces);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);
				addForPrincipal(lui, dateDeces.getOneDayAfter(), MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);
				tiersService.changeHabitantenNH(elle);
				elle.setDateDeces(dateDeces);
				return mc.getNumero();
			}
		});
		
		// événement civil d'annulation de veuvage
		final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(235672453L);
				evt.setType(TypeEvenementCivilEch.CHGT_ETAT_CIVIL_PARTENAIRE);
				evt.setAction(ActionEvenementCivilEch.ANNULATION);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividuLui);
				evt.setDateEvenement(dateDeces);
				return hibernateTemplate.merge(evt).getId();
			}
		});
		
		// traitement de l'événement
		traiterEvenements(noIndividuLui);
		
		// vérification des résultats
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtId);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
				
				final MenageCommun mc = (MenageCommun) tiersDAO.get(mcId);
				Assert.assertNotNull(mc);
				
				final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertNull(ffp.getDateFin());
				return null;
			}
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
        serviceCivil.setUp(new MockServiceCivil() {
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
        final long mcId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
            @Override
            public Long doInTransaction(TransactionStatus status) {
                final PersonnePhysique lui = addHabitant(noIndividuLui);
                final PersonnePhysique elle = addHabitant(noIndividuElle);
                final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, dateDeces);
                final MenageCommun mc = couple.getMenage();
                addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);
                addForPrincipal(lui, dateDeces.getOneDayAfter(), MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);
                tiersService.changeHabitantenNH(elle);
                elle.setDateDeces(dateDeces);
                return mc.getNumero();
            }
        });

        // événement civil d'annulation de deces
        final long evtIdAnnulationDeces = doInNewTransactionAndSession(new TransactionCallback<Long>() {
            @Override
            public Long doInTransaction(TransactionStatus status) {
                final EvenementCivilEch evt = new EvenementCivilEch();
                evt.setId(235672453L);
                evt.setType(TypeEvenementCivilEch.DECES);
                evt.setAction(ActionEvenementCivilEch.ANNULATION);
                evt.setEtat(EtatEvenementCivil.A_TRAITER);
                evt.setNumeroIndividu(noIndividuElle);
                evt.setDateEvenement(dateDeces);
                return hibernateTemplate.merge(evt).getId();
            }
        });

        // traitement de l'événement
        traiterEvenements(noIndividuElle);

        // vérification des résultats
        doInNewTransactionAndSession(new TransactionCallback<Object>() {
            @Override
            public Object doInTransaction(TransactionStatus status) {
                final EvenementCivilEch evt = evtCivilDAO.get(evtIdAnnulationDeces);
                Assert.assertNotNull(evt);
                Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

                final MenageCommun mc = (MenageCommun) tiersDAO.get(mcId);
                Assert.assertNotNull(mc);

                final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
                Assert.assertNotNull(ffp);
                Assert.assertNull(ffp.getDateFin());
                return null;
            }
        });

        // événement civil d'annulation de veuvage
        final long evtIdAnnulationVeuvage = doInNewTransactionAndSession(new TransactionCallback<Long>() {
            @Override
            public Long doInTransaction(TransactionStatus status) {
                final EvenementCivilEch evt = new EvenementCivilEch();
                evt.setId(235672454L);
                evt.setType(TypeEvenementCivilEch.CHGT_ETAT_CIVIL_PARTENAIRE);
                evt.setAction(ActionEvenementCivilEch.ANNULATION);
                evt.setEtat(EtatEvenementCivil.A_TRAITER);
                evt.setNumeroIndividu(noIndividuLui);
                evt.setDateEvenement(dateDeces);
                return hibernateTemplate.merge(evt).getId();
            }
        });

        // traitement de l'événement
        traiterEvenements(noIndividuLui);

        // vérification des résultats (L'annualtion doit être redondante
        doInNewTransactionAndSession(new TransactionCallback<Object>() {
            @Override
            public Object doInTransaction(TransactionStatus status) {
                final EvenementCivilEch evt = evtCivilDAO.get(evtIdAnnulationVeuvage);
                Assert.assertNotNull(evt);
                Assert.assertEquals(EtatEvenementCivil.REDONDANT, evt.getEtat());
                return null;
            }
        });

    }
}
