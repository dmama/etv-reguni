package ch.vd.uniregctb.adresse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.runner.RunWith;

@RunWith(JUnit4ClassRunner.class)
public class AdresseEnvoiTest {

	@Test
	public void testAddLine() {
		AdresseEnvoi adresse = new AdresseEnvoi();
		assertFalse(adresse.isHorsSuisse());
		adresse.addLine("Ligne 1");
		adresse.addLine("Ligne 2");
		adresse.addLine("Ligne 3");
		adresse.addLine("Ligne 4");
		adresse.addLine("Ligne 5");
		adresse.addLine("Ligne 6");

		final String[] lignes = adresse.getLignes();
		assertEquals("Ligne 1", lignes[0]);
		assertEquals("Ligne 2", lignes[1]);
		assertEquals("Ligne 3", lignes[2]);
		assertEquals("Ligne 4", lignes[3]);
		assertEquals("Ligne 5", lignes[4]);
		assertEquals("Ligne 6", lignes[5]);

		assertEquals("Ligne 1", adresse.getLigne(1));
		assertEquals("Ligne 2", adresse.getLigne(2));
		assertEquals("Ligne 3", adresse.getLigne(3));
		assertEquals("Ligne 4", adresse.getLigne(4));
		assertEquals("Ligne 5", adresse.getLigne(5));
		assertEquals("Ligne 6", adresse.getLigne(6));

		assertEquals("Ligne 1", adresse.getLigne1());
		assertEquals("Ligne 2", adresse.getLigne2());
		assertEquals("Ligne 3", adresse.getLigne3());
		assertEquals("Ligne 4", adresse.getLigne4());
		assertEquals("Ligne 5", adresse.getLigne5());
		assertEquals("Ligne 6", adresse.getLigne6());
	}

	@Test
	public void testAddLineHS() {
		AdresseEnvoi adresse = new AdresseEnvoi();
		adresse.setHorsSuisse(true);
		adresse.addLine("Ligne 1");
		adresse.addLine("Ligne 2");
		adresse.addLine("Ligne 3");
		adresse.addLine("Ligne 4");
		adresse.addLine("Ligne 5");
		adresse.addLine("Ligne 6");
		adresse.addLine("Ligne 7");

		final String[] lignes = adresse.getLignes();
		assertEquals("Ligne 1", lignes[0]);
		assertEquals("Ligne 2", lignes[1]);
		assertEquals("Ligne 3", lignes[2]);
		assertEquals("Ligne 4", lignes[3]);
		assertEquals("Ligne 5", lignes[4]);
		assertEquals("Ligne 6", lignes[5]);
		assertEquals("Ligne 7", lignes[6]);

		assertEquals("Ligne 1", adresse.getLigne(1));
		assertEquals("Ligne 2", adresse.getLigne(2));
		assertEquals("Ligne 3", adresse.getLigne(3));
		assertEquals("Ligne 4", adresse.getLigne(4));
		assertEquals("Ligne 5", adresse.getLigne(5));
		assertEquals("Ligne 6", adresse.getLigne(6));
		assertEquals("Ligne 7", adresse.getLigne(7));

		assertEquals("Ligne 1", adresse.getLigne1());
		assertEquals("Ligne 2", adresse.getLigne2());
		assertEquals("Ligne 3", adresse.getLigne3());
		assertEquals("Ligne 4", adresse.getLigne4());
		assertEquals("Ligne 5", adresse.getLigne5());
		assertEquals("Ligne 6", adresse.getLigne6());
		assertEquals("Ligne 7", adresse.getLigne7());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testAddLinePlusDe6Lignes() {

		AdresseEnvoi adresse = new AdresseEnvoi();
		assertFalse(adresse.isHorsSuisse());
		adresse.addLine("Ligne 1");
		adresse.addLine("Ligne 2");
		adresse.addLine("Ligne 3");
		adresse.addLine("Ligne 4");
		adresse.addLine("Ligne 5");
		adresse.addLine("Ligne 6");

		adresse.addLine("Ligne 7"); // <-- exception
	}

	@Test(expected=IllegalArgumentException.class)
	public void testAddLinePlusDe7LignesHS() {

		AdresseEnvoi adresse = new AdresseEnvoi();
		adresse.setHorsSuisse(true);
		adresse.addLine("Ligne 1");
		adresse.addLine("Ligne 2");
		adresse.addLine("Ligne 3");
		adresse.addLine("Ligne 4");
		adresse.addLine("Ligne 5");
		adresse.addLine("Ligne 6");
		adresse.addLine("Ligne 7");

		adresse.addLine("Ligne 8"); // <-- exception
	}

	@Test
	public void testAddLineLignesOptionelles() {

		AdresseEnvoi adresse = new AdresseEnvoi();
		assertFalse(adresse.isHorsSuisse());

		// Ajout de six lignes optionnelles
		adresse.addLine("Ligne optionnelle 1", 1);
		adresse.addLine("Ligne optionnelle 2", 2);
		adresse.addLine("Ligne optionnelle 3", 3);
		adresse.addLine("Ligne optionnelle 4", 4);
		adresse.addLine("Ligne optionnelle 5", 5);
		adresse.addLine("Ligne optionnelle 6", 6);

		assertEquals("Ligne optionnelle 1", adresse.getLigne1());
		assertEquals("Ligne optionnelle 2", adresse.getLigne2());
		assertEquals("Ligne optionnelle 3", adresse.getLigne3());
		assertEquals("Ligne optionnelle 4", adresse.getLigne4());
		assertEquals("Ligne optionnelle 5", adresse.getLigne5());
		assertEquals("Ligne optionnelle 6", adresse.getLigne6());

		// Ajout étape-par-étape de lignes obligatoires
		adresse.addLine("Ligne obligatoire 1");
		assertEquals("Ligne optionnelle 1", adresse.getLigne1());
		assertEquals("Ligne optionnelle 2", adresse.getLigne2());
		assertEquals("Ligne optionnelle 3", adresse.getLigne3());
		assertEquals("Ligne optionnelle 4", adresse.getLigne4());
		assertEquals("Ligne optionnelle 5", adresse.getLigne5());
		assertEquals("Ligne obligatoire 1", adresse.getLigne6());

		adresse.addLine("Ligne obligatoire 2");
		assertEquals("Ligne optionnelle 1", adresse.getLigne1());
		assertEquals("Ligne optionnelle 2", adresse.getLigne2());
		assertEquals("Ligne optionnelle 3", adresse.getLigne3());
		assertEquals("Ligne optionnelle 4", adresse.getLigne4());
		assertEquals("Ligne obligatoire 1", adresse.getLigne5());
		assertEquals("Ligne obligatoire 2", adresse.getLigne6());

		adresse.addLine("Ligne obligatoire 3");
		assertEquals("Ligne optionnelle 1", adresse.getLigne1());
		assertEquals("Ligne optionnelle 2", adresse.getLigne2());
		assertEquals("Ligne optionnelle 3", adresse.getLigne3());
		assertEquals("Ligne obligatoire 1", adresse.getLigne4());
		assertEquals("Ligne obligatoire 2", adresse.getLigne5());
		assertEquals("Ligne obligatoire 3", adresse.getLigne6());

		adresse.addLine("Ligne obligatoire 4");
		assertEquals("Ligne optionnelle 1", adresse.getLigne1());
		assertEquals("Ligne optionnelle 2", adresse.getLigne2());
		assertEquals("Ligne obligatoire 1", adresse.getLigne3());
		assertEquals("Ligne obligatoire 2", adresse.getLigne4());
		assertEquals("Ligne obligatoire 3", adresse.getLigne5());
		assertEquals("Ligne obligatoire 4", adresse.getLigne6());

		adresse.addLine("Ligne obligatoire 5");
		assertEquals("Ligne optionnelle 1", adresse.getLigne1());
		assertEquals("Ligne obligatoire 1", adresse.getLigne2());
		assertEquals("Ligne obligatoire 2", adresse.getLigne3());
		assertEquals("Ligne obligatoire 3", adresse.getLigne4());
		assertEquals("Ligne obligatoire 4", adresse.getLigne5());
		assertEquals("Ligne obligatoire 5", adresse.getLigne6());

		adresse.addLine("Ligne obligatoire 6");
		assertEquals("Ligne obligatoire 1", adresse.getLigne1());
		assertEquals("Ligne obligatoire 2", adresse.getLigne2());
		assertEquals("Ligne obligatoire 3", adresse.getLigne3());
		assertEquals("Ligne obligatoire 4", adresse.getLigne4());
		assertEquals("Ligne obligatoire 5", adresse.getLigne5());
		assertEquals("Ligne obligatoire 6", adresse.getLigne6());
	}

	@Test
	public void testAddLineLignesOptionellesHS() {

		AdresseEnvoi adresse = new AdresseEnvoi();
		adresse.setHorsSuisse(true);

		// Ajout de sept lignes optionnelles
		adresse.addLine("Ligne optionnelle 1", 1);
		adresse.addLine("Ligne optionnelle 2", 2);
		adresse.addLine("Ligne optionnelle 3", 3);
		adresse.addLine("Ligne optionnelle 4", 4);
		adresse.addLine("Ligne optionnelle 5", 5);
		adresse.addLine("Ligne optionnelle 6", 6);
		adresse.addLine("Ligne optionnelle 7", 7);

		assertEquals("Ligne optionnelle 1", adresse.getLigne1());
		assertEquals("Ligne optionnelle 2", adresse.getLigne2());
		assertEquals("Ligne optionnelle 3", adresse.getLigne3());
		assertEquals("Ligne optionnelle 4", adresse.getLigne4());
		assertEquals("Ligne optionnelle 5", adresse.getLigne5());
		assertEquals("Ligne optionnelle 6", adresse.getLigne6());
		assertEquals("Ligne optionnelle 7", adresse.getLigne7());

		// Ajout étape-par-étape de lignes obligatoires
		adresse.addLine("Ligne obligatoire 1");
		assertEquals("Ligne optionnelle 1", adresse.getLigne1());
		assertEquals("Ligne optionnelle 2", adresse.getLigne2());
		assertEquals("Ligne optionnelle 3", adresse.getLigne3());
		assertEquals("Ligne optionnelle 4", adresse.getLigne4());
		assertEquals("Ligne optionnelle 5", adresse.getLigne5());
		assertEquals("Ligne optionnelle 6", adresse.getLigne6());
		assertEquals("Ligne obligatoire 1", adresse.getLigne7());

		adresse.addLine("Ligne obligatoire 2");
		assertEquals("Ligne optionnelle 1", adresse.getLigne1());
		assertEquals("Ligne optionnelle 2", adresse.getLigne2());
		assertEquals("Ligne optionnelle 3", adresse.getLigne3());
		assertEquals("Ligne optionnelle 4", adresse.getLigne4());
		assertEquals("Ligne optionnelle 5", adresse.getLigne5());
		assertEquals("Ligne obligatoire 1", adresse.getLigne6());
		assertEquals("Ligne obligatoire 2", adresse.getLigne7());

		adresse.addLine("Ligne obligatoire 3");
		assertEquals("Ligne optionnelle 1", adresse.getLigne1());
		assertEquals("Ligne optionnelle 2", adresse.getLigne2());
		assertEquals("Ligne optionnelle 3", adresse.getLigne3());
		assertEquals("Ligne optionnelle 4", adresse.getLigne4());
		assertEquals("Ligne obligatoire 1", adresse.getLigne5());
		assertEquals("Ligne obligatoire 2", adresse.getLigne6());
		assertEquals("Ligne obligatoire 3", adresse.getLigne7());

		adresse.addLine("Ligne obligatoire 4");
		assertEquals("Ligne optionnelle 1", adresse.getLigne1());
		assertEquals("Ligne optionnelle 2", adresse.getLigne2());
		assertEquals("Ligne optionnelle 3", adresse.getLigne3());
		assertEquals("Ligne obligatoire 1", adresse.getLigne4());
		assertEquals("Ligne obligatoire 2", adresse.getLigne5());
		assertEquals("Ligne obligatoire 3", adresse.getLigne6());
		assertEquals("Ligne obligatoire 4", adresse.getLigne7());

		adresse.addLine("Ligne obligatoire 5");
		assertEquals("Ligne optionnelle 1", adresse.getLigne1());
		assertEquals("Ligne optionnelle 2", adresse.getLigne2());
		assertEquals("Ligne obligatoire 1", adresse.getLigne3());
		assertEquals("Ligne obligatoire 2", adresse.getLigne4());
		assertEquals("Ligne obligatoire 3", adresse.getLigne5());
		assertEquals("Ligne obligatoire 4", adresse.getLigne6());
		assertEquals("Ligne obligatoire 5", adresse.getLigne7());

		adresse.addLine("Ligne obligatoire 6");
		assertEquals("Ligne optionnelle 1", adresse.getLigne1());
		assertEquals("Ligne obligatoire 1", adresse.getLigne2());
		assertEquals("Ligne obligatoire 2", adresse.getLigne3());
		assertEquals("Ligne obligatoire 3", adresse.getLigne4());
		assertEquals("Ligne obligatoire 4", adresse.getLigne5());
		assertEquals("Ligne obligatoire 5", adresse.getLigne6());
		assertEquals("Ligne obligatoire 6", adresse.getLigne7());

		adresse.addLine("Ligne obligatoire 7");
		assertEquals("Ligne obligatoire 1", adresse.getLigne1());
		assertEquals("Ligne obligatoire 2", adresse.getLigne2());
		assertEquals("Ligne obligatoire 3", adresse.getLigne3());
		assertEquals("Ligne obligatoire 4", adresse.getLigne4());
		assertEquals("Ligne obligatoire 5", adresse.getLigne5());
		assertEquals("Ligne obligatoire 6", adresse.getLigne6());
		assertEquals("Ligne obligatoire 7", adresse.getLigne7());
	}

	@Test
	public void testAddLineLignesOptionellesIgnorees() {

		AdresseEnvoi adresse = new AdresseEnvoi();
		assertFalse(adresse.isHorsSuisse());
		adresse.addLine("Ligne 1");
		adresse.addLine("Ligne 2");
		adresse.addLine("Ligne 3");
		adresse.addLine("Ligne 4");
		adresse.addLine("Ligne 5");
		adresse.addLine("Ligne 6");
		assertEquals("Ligne 1", adresse.getLigne1());
		assertEquals("Ligne 2", adresse.getLigne2());
		assertEquals("Ligne 3", adresse.getLigne3());
		assertEquals("Ligne 4", adresse.getLigne4());
		assertEquals("Ligne 5", adresse.getLigne5());
		assertEquals("Ligne 6", adresse.getLigne6());

		/*
		 * on ajoute des lignes optionnelles -> elles doivent être ignorées puisque six lignes existent déjà
		 */
		adresse.addLine("Ligne optionnelle 1", 1);
		adresse.addLine("Ligne optionnelle 2", 2);
		adresse.addLine("Ligne optionnelle 3", 3);
		assertEquals("Ligne 1", adresse.getLigne1());
		assertEquals("Ligne 2", adresse.getLigne2());
		assertEquals("Ligne 3", adresse.getLigne3());
		assertEquals("Ligne 4", adresse.getLigne4());
		assertEquals("Ligne 5", adresse.getLigne5());
		assertEquals("Ligne 6", adresse.getLigne6());
	}

	@Test
	public void testAddLineLignesOptionellesIgnoreesHS() {

		AdresseEnvoi adresse = new AdresseEnvoi();
		adresse.setHorsSuisse(true);
		adresse.addLine("Ligne 1");
		adresse.addLine("Ligne 2");
		adresse.addLine("Ligne 3");
		adresse.addLine("Ligne 4");
		adresse.addLine("Ligne 5");
		adresse.addLine("Ligne 6");
		adresse.addLine("Ligne 7");
		assertEquals("Ligne 1", adresse.getLigne1());
		assertEquals("Ligne 2", adresse.getLigne2());
		assertEquals("Ligne 3", adresse.getLigne3());
		assertEquals("Ligne 4", adresse.getLigne4());
		assertEquals("Ligne 5", adresse.getLigne5());
		assertEquals("Ligne 6", adresse.getLigne6());
		assertEquals("Ligne 7", adresse.getLigne7());

		/*
		 * on ajoute des lignes optionnelles -> elles doivent être ignorées puisque sept lignes existent déjà
		 */
		adresse.addLine("Ligne optionnelle 1", 1);
		adresse.addLine("Ligne optionnelle 2", 2);
		adresse.addLine("Ligne optionnelle 3", 3);
		assertEquals("Ligne 1", adresse.getLigne1());
		assertEquals("Ligne 2", adresse.getLigne2());
		assertEquals("Ligne 3", adresse.getLigne3());
		assertEquals("Ligne 4", adresse.getLigne4());
		assertEquals("Ligne 5", adresse.getLigne5());
		assertEquals("Ligne 6", adresse.getLigne6());
		assertEquals("Ligne 7", adresse.getLigne7());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testAddLineLignesOptionellesMauvaiseGranularite() {

		// Ajout de six lignes optionnelles
		AdresseEnvoi adresse = new AdresseEnvoi();
		assertFalse(adresse.isHorsSuisse());
		adresse.addLine("Ligne obligatoire 1");
		adresse.addLine("Ligne obligatoire 2");
		adresse.addLine("Ligne obligatoire 3");
		adresse.addLine("Ligne obligatoire 4");
		adresse.addLine("Ligne optionnelle 5", 1);
		adresse.addLine("Ligne optionnelle 6", 1);

		/*
		 * La granularité des adresses optionnelles n'est pas suffisante -> on ne peut choisir laquelle des deux adresses optionnelle doit
		 * être supprimée !
		 */
		adresse.addLine("Ligne obligatoire 5");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testAddLineLignesOptionellesMauvaiseGranulariteHS() {

		// Ajout de six lignes optionnelles
		AdresseEnvoi adresse = new AdresseEnvoi();
		adresse.setHorsSuisse(true);
		adresse.addLine("Ligne obligatoire 1");
		adresse.addLine("Ligne obligatoire 2");
		adresse.addLine("Ligne obligatoire 3");
		adresse.addLine("Ligne obligatoire 4");
		adresse.addLine("Ligne obligatoire 5");
		adresse.addLine("Ligne optionnelle 1", 1);
		adresse.addLine("Ligne optionnelle 2", 1);

		/*
		 * La granularité des adresses optionnelles n'est pas suffisante -> on ne peut choisir laquelle des deux adresses optionnelle doit
		 * être supprimée !
		 */
		adresse.addLine("Ligne obligatoire 6");
	}
}
