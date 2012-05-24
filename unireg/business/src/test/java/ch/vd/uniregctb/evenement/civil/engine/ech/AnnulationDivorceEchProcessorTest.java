package ch.vd.uniregctb.evenement.civil.engine.ech;

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
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class AnnulationDivorceEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {
	
	@Test(timeout = 10000L)
	public void testAnnulationDivorce() throws Exception {

		final long noIndividuLui = 36712456523468L;
		final long noIndividuElle = 34674853272545L;
		final RegDate dateMariage = date(2003, 4, 12);
		final RegDate dateDivorce = date(2012, 2, 10);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final RegDate dateNaissanceLui = date(1960, 1, 26);
				final MockIndividu lui = addIndividu(noIndividuLui, dateNaissanceLui, "Casanova", "Paco", true);
				addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateNaissanceLui, null);
				addNationalite(lui, MockPays.Suisse, dateNaissanceLui, null);

				final RegDate dateNaissanceElle = date(1980, 6, 12);
				final MockIndividu elle = addIndividu(noIndividuElle, dateNaissanceElle, "Nette", "Jeu", false);
				addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateNaissanceElle, null);
				addNationalite(elle, MockPays.Suisse, dateNaissanceElle, null);

				marieIndividus(lui, elle, dateMariage);
			}
		});

		// mise en place fiscale avec mariage et divorce
		final long mcId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addHabitant(noIndividuLui);
				addForPrincipal(lui, date(2000, 1, 1), MotifFor.INDETERMINE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				addForPrincipal(lui, dateDivorce, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne);

				final PersonnePhysique elle = addHabitant(noIndividuElle);
				addForPrincipal(elle, date(2001, 4, 12), MotifFor.INDETERMINE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
				addForPrincipal(elle, dateDivorce, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Echallens);

				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, dateDivorce.getOneDayBefore());
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateDivorce.getOneDayBefore(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne);
				return mc.getNumero();
			}
		});

		// création de l'événement civil d'annulation de divorce
		final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(3478256623526867L);
				evt.setType(TypeEvenementCivilEch.DIVORCE);
				evt.setAction(ActionEvenementCivilEch.ANNULATION);
				evt.setDateEvenement(dateDivorce);
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
