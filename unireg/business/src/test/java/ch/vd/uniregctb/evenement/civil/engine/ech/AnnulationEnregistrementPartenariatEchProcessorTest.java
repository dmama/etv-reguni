package ch.vd.uniregctb.evenement.civil.engine.ech;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
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

public class AnnulationEnregistrementPartenariatEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {
	
	@Test(timeout = 10000L)
	public void testAnnulationPartenariat() throws Exception {
		
		final long noIndividuLui = 36712456523468L;
		final long noIndividuLui2 = 34674853272545L;
		final RegDate datePartenariat = date(2012, 2, 10);
		
		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final RegDate dateNaissanceLui = date(1960, 1, 26);
				final MockIndividu lui = addIndividu(noIndividuLui, dateNaissanceLui, "Casanova", "Paco", true);
				addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateNaissanceLui, null);
				addNationalite(lui, MockPays.Suisse, dateNaissanceLui, null);
				
				final RegDate dateNaissanceLui2 = date(1980, 6, 12);
				final MockIndividu lui2 = addIndividu(noIndividuLui2, dateNaissanceLui2, "Nau", "Jeu", true);
				addAdresse(lui2, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateNaissanceLui2, null);
				addNationalite(lui2, MockPays.Suisse, dateNaissanceLui2, null);
			}
		});
		
		// mise en place fiscale avec partenariat enregistré
		final long mcId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addHabitant(noIndividuLui);
				addForPrincipal(lui, date(2000, 1, 1), MotifFor.INDETERMINE, datePartenariat.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				
				final PersonnePhysique lui2 = addHabitant(noIndividuLui2);
				addForPrincipal(lui2, date(2001, 4, 12), MotifFor.INDETERMINE, datePartenariat.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
				
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, lui2, datePartenariat, null);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, datePartenariat, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				return mc.getNumero();
			}
		});
		
		// création de l'événement civil d'annulation d'enregistrement de partenariat
		final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(34782566526867L);
				evt.setType(TypeEvenementCivilEch.ENREGISTREMENT_PARTENARIAT);
				evt.setAction(ActionEvenementCivilEch.ANNULATION);
				evt.setDateEvenement(datePartenariat);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividuLui);
				return hibernateTemplate.merge(evt).getId();
			}
		});
		
		// traitement de l'événement civil
		traiterEvenements(noIndividuLui);
		
		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtId);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
				
				final MenageCommun mc = (MenageCommun) tiersDAO.get(mcId);
				Assert.assertNotNull(mc);

				final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
				Assert.assertNull(ffp);
				return null;
			}
		});
	}
}
