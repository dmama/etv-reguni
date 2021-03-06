package ch.vd.unireg.adresse;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseGenerique.SourceType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public abstract class AdresseTestCase {

	/**
	 * Asserte le contenu d'une adresse.
	 */
	public static void assertAdresse(@Nullable RegDate dateDebut, @Nullable RegDate dateFin, @Nullable String localite, SourceType source, boolean isDefault,
			AdresseGenerique adresse) {
		assertNotNull(adresse);
		assertEquals(dateDebut, adresse.getDateDebut());
		assertEquals(dateFin, adresse.getDateFin());
		assertEquals(localite, adresse.getLocalite());
		assertEquals(source, adresse.getSource().getType());
		assertEquals(isDefault, adresse.isDefault());
	}

	/**
	 * Asserte que les deux adresses sont égales en contenu.
	 */
	public static void assertAdressesEquals(AdresseGenerique expected, AdresseGenerique actual) {
		if (expected == null) {
			assertNull(actual);
		}
		else {
			assertAdresse(expected.getDateDebut(), expected.getDateFin(), expected.getLocalite(), expected.getSource().getType(), expected.isDefault(), actual);
		}
	}

	/**
	 * Asserte que les deux collections d'adresses sont égales en taille et en contenu.
	 */
	public static void assertAdressesEquals(List<AdresseGenerique> expected, List<AdresseGenerique> actual) {

		assertEquals(expected.size(), actual.size());

		final int size = expected.size();
		for (int i = 0; i < size; ++i) {
			AdresseGenerique e = expected.get(i);
			AdresseGenerique a = actual.get(i);
			if (e == null) {
				assertNull(a);
			}
			else {
				assertAdressesEquals(e, a);
			}
		}
	}
}
