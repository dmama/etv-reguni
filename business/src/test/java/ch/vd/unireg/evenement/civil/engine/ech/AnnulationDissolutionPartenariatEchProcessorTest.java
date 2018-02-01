package ch.vd.unireg.evenement.civil.engine.ech;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEch;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeEvenementCivilEch;

public class AnnulationDissolutionPartenariatEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {
	
	@Test(timeout = 10000L)
	public void testAnnulationDissolutionPartenariat() throws Exception {

		final long noIndividuLui = 36712456523468L;
		final long noIndividuLui2 = 34674853272545L;
		final RegDate datePartenariat = date(2003, 4, 12);
		final RegDate dateDissolutionPartenariat = date(2012, 2, 10);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final RegDate dateNaissanceLui = date(1960, 1, 26);
				final MockIndividu lui = addIndividu(noIndividuLui, dateNaissanceLui, "Casanova", "Paco", true);
				addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, dateNaissanceLui, null);
				addNationalite(lui, MockPays.Suisse, dateNaissanceLui, null);

				final RegDate dateNaissanceLui2 = date(1980, 6, 12);
				final MockIndividu lui2 = addIndividu(noIndividuLui2, dateNaissanceLui2, "Nau", "Jeu", false);
				addAdresse(lui2, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateNaissanceLui2, null);
				addNationalite(lui2, MockPays.Suisse, dateNaissanceLui2, null);

				marieIndividus(lui, lui2, datePartenariat);
			}
		});

		// mise en place fiscale avec enregistrement puis dissolution du partenariat
		final long mcId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addHabitant(noIndividuLui);
				addForPrincipal(lui, date(2000, 1, 1), MotifFor.INDETERMINE, datePartenariat.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				addForPrincipal(lui, dateDissolutionPartenariat, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne);

				final PersonnePhysique lui2 = addHabitant(noIndividuLui2);
				addForPrincipal(lui2, date(2001, 4, 12), MotifFor.INDETERMINE, datePartenariat.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
				addForPrincipal(lui2, dateDissolutionPartenariat, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Echallens);

				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, lui2, datePartenariat, dateDissolutionPartenariat.getOneDayBefore());
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, datePartenariat, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateDissolutionPartenariat.getOneDayBefore(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne);
				return mc.getNumero();
			}
		});

		// création de l'événement civil d'annulation de dissolution de partenariat
		final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(8256623526867L);
				evt.setType(TypeEvenementCivilEch.DISSOLUTION_PARTENARIAT);
				evt.setAction(ActionEvenementCivilEch.ANNULATION);
				evt.setDateEvenement(dateDissolutionPartenariat);
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
				Assert.assertNotNull(ffp);
				Assert.assertNull(ffp.getDateFin());
				return null;
			}
		});
	}
}
