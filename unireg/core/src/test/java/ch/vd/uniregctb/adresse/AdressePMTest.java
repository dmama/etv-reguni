package ch.vd.uniregctb.adresse;

import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.type.TypeAdressePM;

import static junit.framework.Assert.assertFalse;

public class AdressePMTest extends WithoutSpringTest {

	@Test
	public void testValidateAdresseAnnulee() {

		final AdressePM adresse = new AdressePM();

		// Adresse invalide (type nul) mais annulée => pas d'erreur
		{
			adresse.setType(null);
			adresse.setAnnule(true);
			assertFalse(adresse.validate().hasErrors());
		}

		// Adresse valide et annulée => pas d'erreur
		{
			adresse.setType(TypeAdressePM.COURRIER);
			adresse.setAnnule(true);
			assertFalse(adresse.validate().hasErrors());
		}
	}
}