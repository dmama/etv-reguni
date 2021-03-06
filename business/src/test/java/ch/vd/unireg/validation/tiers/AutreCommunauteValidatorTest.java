package ch.vd.unireg.validation.tiers;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.tiers.AutreCommunaute;
import ch.vd.unireg.validation.AbstractValidatorTest;

public class AutreCommunauteValidatorTest extends AbstractValidatorTest {

	@Override
	protected String getValidatorBeanName() {
		return "autreCommunauteValidator";
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateTiersAnnule() {

		final AutreCommunaute tiers = new AutreCommunaute();

		// Tiers invalide (nom nul) mais annulé => pas d'erreur
		{
			tiers.setNom(null);
			tiers.setAnnule(true);
			Assert.assertFalse(validate(tiers).hasErrors());
		}

		// Tiers valide et annulée => pas d'erreur
		{
			tiers.setNom("et vous ça va ?");
			tiers.setAnnule(true);
			Assert.assertFalse(validate(tiers).hasErrors());
		}
	}
}
