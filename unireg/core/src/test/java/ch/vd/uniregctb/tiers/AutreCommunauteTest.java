package ch.vd.uniregctb.tiers;

import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;

import static junit.framework.Assert.assertFalse;

public class AutreCommunauteTest extends WithoutSpringTest {

	@Test
	public void testValidateTiersAnnule() {

		final AutreCommunaute tiers = new AutreCommunaute();

		// Tiers invalide (nom nul) mais annulé => pas d'erreur
		{
			tiers.setNom(null);
			tiers.setAnnule(true);
			assertFalse(tiers.validate().hasErrors());
		}

		// Tiers valide et annulée => pas d'erreur
		{
			tiers.setNom("et vous ça va ?");
			tiers.setAnnule(true);
			assertFalse(tiers.validate().hasErrors());
		}
	}
}
