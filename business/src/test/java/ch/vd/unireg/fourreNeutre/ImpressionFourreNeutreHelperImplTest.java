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

	final static String CODE_DOCUMENT_FOURRE_NEUTRE = "060140005";

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

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Tiers pp = hibernateTemplate.get(Tiers.class, idTiers);
				final FourreNeutre fourre = new FourreNeutre(pp,2015);

				final String codeBarre = CODE_DOCUMENT_FOURRE_NEUTRE + ImpressionFourreNeutreHelperImpl.calculCodeBarre(fourre);
				final String attendu = "06014000520150" + pp.getNumero()+"0000";
				Assert.assertEquals(attendu,codeBarre);
			}
		});
	}

	@Test
	public void testCodeBarrePourPM() throws Exception {
		// mise en place
		final long idTiers = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise es = addEntrepriseInconnueAuCivil();
				return es.getNumero();
			}
		});

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Tiers es = hibernateTemplate.get(Tiers.class, idTiers);
				FourreNeutre fourre = new FourreNeutre(es,2010);
				final String codeBarre = CODE_DOCUMENT_FOURRE_NEUTRE + ImpressionFourreNeutreHelperImpl.calculCodeBarre(fourre);
				final String attendu = "06014000520100" + String.format("%08d", es.getNumero()) +"0000";
				Assert.assertEquals(attendu,codeBarre);
			}
		});
	}
}