package ch.vd.uniregctb.foncier.migration;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.registrefoncier.CommuneRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;

public class IdentificationImmeubleHelperTest extends BusinessTest {

	private ImmeubleRFDAO immeubleRFDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		this.immeubleRFDAO = getBean(ImmeubleRFDAO.class, "immeubleRFDAO");
	}

	@Test
	public void testFindImmeubleDirectement() throws Exception {

		// mise en place
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final CommuneRF commune = addCommuneRF(15451, MockCommune.Lausanne.getNomOfficiel(), MockCommune.Lausanne.getNoOFS());
				addBienFondRF("4783424LJD", null, commune, 1234, 2, null, null);
				addBienFondRF("58658dkf", null, commune, 1234, 1, null, null);
			}
		});

		// contrôle
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final ImmeubleRF immeuble = IdentificationImmeubleHelper.findImmeuble(immeubleRFDAO, MockCommune.Lausanne, MigrationParcelle.valueOf("4542712879", "1234-1", null));
				Assert.assertNotNull(immeuble);
				Assert.assertEquals("58658dkf", immeuble.getIdRF());
			}
		});
	}

	@Test
	public void testFindImmeubleNonTrouvePasBonIndex() throws Exception {

		// mise en place
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final CommuneRF commune = addCommuneRF(15451, MockCommune.Lausanne.getNomOfficiel(), MockCommune.Lausanne.getNoOFS());
				addBienFondRF("4783424LJD", null, commune, 1234, 2, null, null);
				addBienFondRF("6487426s", null, commune, 1234, 1, null, null);
			}
		});

		// contrôle
		try {
			doInNewTransactionAndSession(new TxCallbackWithoutResult() {
				@Override
				public void execute(TransactionStatus transactionStatus) throws Exception {
					final ImmeubleRF immeuble = IdentificationImmeubleHelper.findImmeuble(immeubleRFDAO, MockCommune.Lausanne, MigrationParcelle.valueOf("4542712879", "1234-3", null));
					Assert.fail("L'appel aurait-dû échouer");
				}
			});
		}
		catch (ImmeubleNotFoundException e) {
			Assert.assertEquals("L'immeuble avec la parcelle [1234/3/null/null] n'existe pas sur la commune de Lausanne (5586).", e.getMessage());
		}
	}

	@Test
	public void testFindImmeubleTrouveSansIndex() throws Exception {

		// mise en place
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final CommuneRF commune = addCommuneRF(15451, MockCommune.Lausanne.getNomOfficiel(), MockCommune.Lausanne.getNoOFS());
				addBienFondRF("4783424LJD", null, commune, 1234, null, null, null);
				addBienFondRF("4783295dh", null, commune, 1235, null, null, null);
			}
		});

		// contrôle
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final ImmeubleRF immeuble = IdentificationImmeubleHelper.findImmeuble(immeubleRFDAO, MockCommune.Lausanne, MigrationParcelle.valueOf("4542712879", "1234", null));
				Assert.assertNotNull(immeuble);
				Assert.assertEquals("4783424LJD", immeuble.getIdRF());
			}
		});
	}

}
