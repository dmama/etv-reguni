package ch.vd.uniregctb.fourreNeutre;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.Sexe;

public class ImpressionFourreNeutreHelperImplTest extends BusinessTest {

	private ImpressionFourreNeutreHelperImpl helper;
	final static String CODE_DOCUMENT_FOURRE_NEUTRE = "060140005";


	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();

		helper = new ImpressionFourreNeutreHelperImpl();
	}

	@Test
	public void testCodeBarrePourPP() throws Exception {
		// mise en place
		final long idTiers = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				final PersonnePhysique pp = addNonHabitant("Francis", "Orange", null, Sexe.MASCULIN);


				return pp.getNumero();
			}
		});

		// demande d'impression de confirmation de délai
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Tiers pp = hibernateTemplate.get(Tiers.class, idTiers);
				FourreNeutre fourre = new FourreNeutre(pp,2015);

				final String codeBarre = CODE_DOCUMENT_FOURRE_NEUTRE +helper.calculCodeBarre(fourre);
				final String attendu = "06014000520150" + pp.getNumero()+"0000";
				Assert.assertEquals(attendu,codeBarre);
			}
		});
	}

	@Test
	public void testCodeBarrePourPMAvecIdentifiantCourt() throws Exception {
		// mise en place
		final long idTiers = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				final Entreprise es = addEntrepriseInconnueAuCivil(1);


				return es.getNumero();
			}
		});

		// demande d'impression de confirmation de délai
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Tiers es = hibernateTemplate.get(Tiers.class, idTiers);
				FourreNeutre fourre = new FourreNeutre(es,2010);
				final String codeBarre = CODE_DOCUMENT_FOURRE_NEUTRE+helper.calculCodeBarre(fourre);
				final String attendu = "060140005201000000000" + es.getNumero()+"0000";
				Assert.assertEquals(attendu,codeBarre);
			}
		});
	}

	@Test
	public void testCodeBarrePourPMAvecIdentifiantLong() throws Exception {
		// mise en place
		final long idTiers = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				final Entreprise es = addEntrepriseInconnueAuCivil(12345);


				return es.getNumero();
			}
		});

		// demande d'impression de confirmation de délai
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Tiers es = hibernateTemplate.get(Tiers.class, idTiers);
				FourreNeutre fourre = new FourreNeutre(es,2010);
				final String codeBarre = CODE_DOCUMENT_FOURRE_NEUTRE+helper.calculCodeBarre(fourre);
				final String attendu = "06014000520100000" + es.getNumero()+"0000";
				Assert.assertEquals(attendu,codeBarre);
			}
		});
	}
}