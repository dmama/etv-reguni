package ch.vd.uniregctb.validation.rapport;

import org.junit.Test;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

import static junit.framework.Assert.assertFalse;

public class RapportEntreTiersValidatorTest extends AbstractValidatorTest<RapportEntreTiers> {

	@Override
	protected String getValidatorBeanName() {
		return "defaultRapportEntreTiersValidator";
	}

	@Test
	public void testValidateRapportAnnule() {

		final RapportEntreTiers rapport = new RapportEntreTiers() {
			@Override
			public TypeRapportEntreTiers getType() {
				throw new NotImplementedException();
			}

			public RapportEntreTiers duplicate() {
				throw new NotImplementedException();
			}
		};

		// Adresse invalide (date début nul) mais annulée => pas d'erreur
		{
			rapport.setDateDebut(null);
			rapport.setAnnule(true);
			assertFalse(validate(rapport).hasErrors());
		}

		// Adresse valide et annulée => pas d'erreur
		{
			rapport.setDateDebut(date(2000, 1, 1));
			rapport.setAnnule(true);
			assertFalse(validate(rapport).hasErrors());
		}
	}
}
