package ch.vd.uniregctb.validation.rapport;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.validation.ValidationException;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

public class AppartenanceMenageValidatorTest extends AbstractValidatorTest<AppartenanceMenage> {

	@Override
	protected String getValidatorBeanName() {
		return "appartenanceMenageValidator";
	}

	@Test
	@Transactional(rollbackFor = RuntimeException.class)
	public void testAppartenanceMenageComplete() throws Exception {
		final PersonnePhysique pp = addNonHabitant("Toto", "LeHéro", null, Sexe.MASCULIN);
		addEnsembleTiersCouple(pp, null, date(2010, 4, 12), null);
		final AppartenanceMenage am = (AppartenanceMenage) pp.getDernierRapportSujet(TypeRapportEntreTiers.APPARTENANCE_MENAGE);
		final ValidationResults vr = validate(am);
		Assert.assertNotNull(vr);
		Assert.assertEquals(0, vr.getErrorsList().size());
	}

	@Test
	public void testAppartenanceMenageInversee() throws Exception {
		try {
			doInNewTransactionAndSession(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					final PersonnePhysique pp = addNonHabitant("Toto", "LeHéro", null, Sexe.MASCULIN);
					final MenageCommun mc = hibernateTemplate.merge(new MenageCommun());
					final AppartenanceMenage am = new AppartenanceMenage();
					am.setDateDebut(date(2010, 4, 12));
					am.setObjet(pp);
					am.setSujet(mc);
					hibernateTemplate.merge(am);
					return null;
				}
			});
			Assert.fail("Le rapport d'appartenance ménage est pourtant à l'envers...");
		}
		catch (ValidationException e) {
			Assert.assertEquals(2, e.getErrors().size());
			Assert.assertTrue(e.toString(), e.getErrors().get(0).getMessage().contains("n'est pas un ménage commun"));
			Assert.assertTrue(e.toString(), e.getErrors().get(1).getMessage().contains("n'est pas une personne physique"));
		}
	}
}
