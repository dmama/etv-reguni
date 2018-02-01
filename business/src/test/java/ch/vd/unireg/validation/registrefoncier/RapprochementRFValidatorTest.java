package ch.vd.unireg.validation.registrefoncier;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.validation.ValidationException;
import ch.vd.unireg.registrefoncier.PersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.RapprochementRF;
import ch.vd.unireg.registrefoncier.TiersRF;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeRapprochementRF;
import ch.vd.unireg.validation.AbstractValidatorTest;

public class RapprochementRFValidatorTest extends AbstractValidatorTest<RapprochementRF> {

	@Override
	protected String getValidatorBeanName() {
		return "rapprochementRFValidator";
	}

	@Test
	public void testDetectionChevauchementRapprochementsSurTiersRF() throws Exception {

		final class Ids {
			long pp1;
			long pp2;
			long pprf;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp1 = addNonHabitant("Alfred", "de Musso", null, Sexe.MASCULIN);
				final PersonnePhysique pp2 = addNonHabitant("Angélique", "des Anges", null, Sexe.FEMININ);

				final PersonnePhysiqueRF pprf = addPersonnePhysiqueRF("Alfrangélique", "Prout prout", null, "7384374872L", 3764L, null);

				final Ids ids = new Ids();
				ids.pp1 = pp1.getNumero();
				ids.pp2 = pp2.getNumero();
				ids.pprf = pprf.getId();
				return ids;
			}
		});

		// test de validation
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp1 = (PersonnePhysique) tiersDAO.get(ids.pp1);
				final PersonnePhysique pp2 = (PersonnePhysique) tiersDAO.get(ids.pp2);
				final TiersRF rf = hibernateTemplate.get(TiersRF.class, ids.pprf);

				addRapprochementRF(null, null, TypeRapprochementRF.AUTO, pp1, rf, true);
				addRapprochementRF(null, date(2000, 12, 31), TypeRapprochementRF.MANUEL, pp1, rf, false);
				addRapprochementRF(date(2001, 1, 1), null, TypeRapprochementRF.MANUEL, pp2, rf, false);
			}
		});

		// jusqu'ici tout va bien (la validation n'a pas sauté) mais maintenant ça doit sauter...
		try {
			doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					final PersonnePhysique pp2 = (PersonnePhysique) tiersDAO.get(ids.pp2);
					final TiersRF rf = hibernateTemplate.get(TiersRF.class, ids.pprf);

					addRapprochementRF(date(2005, 1, 1), date(2006, 12, 31), TypeRapprochementRF.MANUEL, pp2, rf, false);
				}
			});
		}
		catch (ValidationException e) {
			Assert.assertTrue(e.getMessage(), e.getMessage().contains("possède plusieurs rapprochements non-annulés sur la période [01.01.2005 ; 31.12.2006]."));
		}
	}
}
