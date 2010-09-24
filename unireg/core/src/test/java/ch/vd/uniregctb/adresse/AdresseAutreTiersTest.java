package ch.vd.uniregctb.adresse;

import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.type.TypeAdresseTiers;

import static junit.framework.Assert.assertFalse;

public class AdresseAutreTiersTest extends WithoutSpringTest {

	@Test
	public void testValidateAdresseAnnulee() {

		final AdresseAutreTiers adresse = new AdresseAutreTiers();

		// Adresse invalide (type nul) mais annulée => pas d'erreur
		{
			adresse.setType(null);
			adresse.setAnnule(true);
			assertFalse(adresse.validate().hasErrors());
		}

		// Adresse valide et annulée => pas d'erreur
		{
			adresse.setType(TypeAdresseTiers.COURRIER);
			adresse.setAnnule(true);
			assertFalse(adresse.validate().hasErrors());
		}
	}
}