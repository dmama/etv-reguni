package ch.vd.uniregctb.adresse;

import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.type.TypeAdresseCivil;

import static junit.framework.Assert.assertFalse;

public class AdresseCivileTest extends WithoutSpringTest {

	@Test
	public void testValidateAdresseAnnulee() {

		final AdresseCivile adresse = new AdresseCivile();

		// Adresse invalide (type nul) mais annulée => pas d'erreur
		{
			adresse.setType(null);
			adresse.setAnnule(true);
			assertFalse(adresse.validate().hasErrors());
		}

		// Adresse valide et annulée => pas d'erreur
		{
			adresse.setType(TypeAdresseCivil.COURRIER);
			adresse.setAnnule(true);
			assertFalse(adresse.validate().hasErrors());
		}
	}
}
