package ch.vd.uniregctb.validation.situationfamille;

import junit.framework.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

public class SituationFamilleValidatorTest extends AbstractValidatorTest<SituationFamille> {

	@Override
	protected String getValidatorBeanName() {
		return "situationFamilleValidator";
	}

	@Test
	public void testValidateSituationAnnule() {

		final SituationFamille situation = new SituationFamille() {
			@Override
			public SituationFamille duplicate() {
				throw new NotImplementedException();
			}
		};

		// invalide (date de début nulle) mais annulé => pas d'erreur
		{
			situation.setDateDebut(null);
			situation.setAnnule(true);
			Assert.assertFalse(validate(situation).hasErrors());
		}

		// valide et annulée => pas d'erreur
		{
			situation.setDateDebut(RegDate.get(2000, 1, 1));
			situation.setAnnule(true);
			Assert.assertFalse(validate(situation).hasErrors());
		}
	}
}
