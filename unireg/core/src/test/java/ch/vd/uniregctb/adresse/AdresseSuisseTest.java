package ch.vd.uniregctb.adresse;

import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;

import static junit.framework.Assert.assertFalse;

public class AdresseSuisseTest extends WithoutSpringTest {

	@Test
	public void testValidateAdresseAnnulee() {

		final AdresseSuisse adresse = new AdresseSuisse();

		// Adresse invalide (numéro rue et ordre poste nuls) mais annulée => pas d'erreur
		{
			adresse.setNumeroRue(null);
			adresse.setNumeroOrdrePoste(null);
			adresse.setAnnule(true);
			assertFalse(adresse.validate().hasErrors());
		}

		// Adresse valide et annulée => pas d'erreur
		{
			adresse.setNumeroRue(1234);
			adresse.setAnnule(true);
			assertFalse(adresse.validate().hasErrors());
		}
	}
}