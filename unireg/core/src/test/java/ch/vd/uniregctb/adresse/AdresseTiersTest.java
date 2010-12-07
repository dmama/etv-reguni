package ch.vd.uniregctb.adresse;

import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.type.TypeAdresseTiers;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class AdresseTiersTest extends WithoutSpringTest {

	@Test
	public void testValidateDateDebut() {

		final AdresseTiers adresse = new AdresseTiers() {
			public AdresseTiers duplicate() {
				throw new NotImplementedException();
			}
		};
		adresse.setUsage(TypeAdresseTiers.COURRIER);

		// Date de début nulle
		{
			final ValidationResults results = adresse.validate();
			assertTrue(results.hasErrors());
			final List<String> errors = results.getErrors();
			assertEquals(1, errors.size());
			assertEquals("L'adresse AdresseTiers{id=null, dateDebut=null, dateFin=null, usage=COURRIER, tiers=null} possède une date de début nulle", errors.get(0));
		}

		// Date de début renseignée
		{
			adresse.setDateDebut(RegDate.get(2000, 1, 1));
			assertFalse(adresse.validate().hasErrors());
		}
	}

	@Test
	public void testValidateForAnnule() {

		final AdresseTiers adresse = new AdresseTiers() {
			public AdresseTiers duplicate() {
				throw new NotImplementedException();
			}
		};

		// Adresse invalide (date de début nulle) mais annulée => pas d'erreur
		{
			adresse.setAnnule(true);
			assertFalse(adresse.validate().hasErrors());
		}

		// Adresse valide et annulée => pas d'erreur
		{
			adresse.setAnnule(true);
			adresse.setDateDebut(RegDate.get(2000, 1, 1));
			assertFalse(adresse.validate().hasErrors());
		}
	}
}
