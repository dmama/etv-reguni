package ch.vd.uniregctb.adresse;

import org.junit.After;
import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LocaliteInvalideMatcherTest extends WithoutSpringTest {


	@After
	public void tearDown() {
		// Remise à zéro des champs static de la classe LocaliteInvalideMatcher
		LocaliteInvalideMatcher.reset();
	}


	@Test
	public void testMatchBlank() throws Exception {
		setupNormal(true);
		//noinspection NullableProblems
		assertTrue("null n'est jamais une localité valide", LocaliteInvalideMatcher.match(null));
		assertTrue("Une chaîne de 'blancs' n'est jamais une localité valide", LocaliteInvalideMatcher.match(" \n\r\t"));
	}

	@Test
	public void testDisabled() throws Exception {
		setupNormal(false);
		assertFalse(LocaliteInvalideMatcher.match("inconnu"));
	}

	@Test
	public void testAucuneInitialisation() throws Exception {
		assertFalse(LocaliteInvalideMatcher.match("inconnu"));
	}

	@Test
	public void testInitialiseMaisAucuneProprieteSettee() throws Exception {
		LocaliteInvalideMatcher bean = new LocaliteInvalideMatcher();
		bean.afterPropertiesSet();
		assertFalse(LocaliteInvalideMatcher.match("inconnu"));
	}

	@Test
	public void testBasic() throws Exception {
		LocaliteInvalideMatcher bean = new LocaliteInvalideMatcher();
		bean.setEnabled(true);
		bean.setLocalitesInvalides("inconu");
		bean.afterPropertiesSet();
    	assertTrue("''inconu' est le pattern tel quel", LocaliteInvalideMatcher.match("inconu"));
		assertTrue("''parti dans un pays inconu' contient le pattern tel quel", LocaliteInvalideMatcher.match("parti dans un pays inconu"));
		assertFalse("''inco' ne contenant pas tout le pattern", LocaliteInvalideMatcher.match("inco"));
		assertFalse("'parti dans un pays inco' ne contenant pas tout le pattern", LocaliteInvalideMatcher.match("parti dans un pays inco"));
	}

	@Test
	public void testMatchMalgreUnNombreInapprorieDeLettre() throws Exception {
		LocaliteInvalideMatcher bean = new LocaliteInvalideMatcher();
		bean.setEnabled(true);
		bean.setLocalitesInvalides("inconu");
		bean.afterPropertiesSet();
		assertTrue("'innccconu' correspond au pattern avec un nombre de caractères identiques incorrect, ça devrait matcher", LocaliteInvalideMatcher.match("innccconu"));
		bean.setLocalitesInvalides("inconnu");
		bean.afterPropertiesSet();
		assertFalse("'inconu' ne matche plus car il faut au moins 2 'n'", LocaliteInvalideMatcher.match("inconu"));
	}

	@Test
	public void testMatchMalgreDesEspacesEtDesSeparateurs() throws Exception {
		LocaliteInvalideMatcher bean = new LocaliteInvalideMatcher();
		bean.setEnabled(true);
		bean.setLocalitesInvalides("inconu");
		bean.afterPropertiesSet();
		assertTrue("'i   n/ c| o, n----u' correspond au pattern avec des séparteurs entre chaque caratère, ça devrait matcher", LocaliteInvalideMatcher.match("i   n/ c| o, n----u"));
	}

	@Test
	public void testMatchMalgreCasseDifferente() throws Exception {
		LocaliteInvalideMatcher bean = new LocaliteInvalideMatcher();
		bean.setEnabled(true);
		bean.setLocalitesInvalides("etranger");
		bean.afterPropertiesSet();
		assertTrue("'ETRANGER' correspond au pattern mais en majuscule", LocaliteInvalideMatcher.match("ETRANGER"));
		assertTrue("'EtRaNgEr' correspond au pattern mais avec certaines lettres en majuscule", LocaliteInvalideMatcher.match("EtRaNgEr"));
	}

	@Test
	public void testMatchMalgreAccents() throws Exception {
		LocaliteInvalideMatcher bean = new LocaliteInvalideMatcher();
		bean.setEnabled(true);
		bean.setLocalitesInvalides("etranger");
		bean.afterPropertiesSet();
		assertTrue("'étranger' correspond au pattern mais avec un accent", LocaliteInvalideMatcher.match("étranger"));
		assertTrue("'êträngẽr' correspond au pattern même si fantaisiste", LocaliteInvalideMatcher.match("êträngẽr"));
	}

	@Test
	public void testSimpleFauxPositifs() throws Exception {
		LocaliteInvalideMatcher bean = new LocaliteInvalideMatcher();
		bean.setEnabled(true);
		bean.setLocalitesInvalides("adrese");
		bean.setFauxPositifs("sainte-adresse");
		bean.afterPropertiesSet();
		assertTrue("'sainte-adrese' correspond au pattern et n'est pas un faux positif", LocaliteInvalideMatcher.match("sainte-adrese"));
		assertFalse("'sainte-adresse' correspond au pattern mais est un faux positif", LocaliteInvalideMatcher.match("sainte-adresse"));
		assertFalse("'12000 sainte-adresse' correspond au pattern mais est un faux positif", LocaliteInvalideMatcher.match("12000 sainte-adresse"));
	}

	@Test
	public void testDeuxPatternsEtDeuxFauxPositifs() throws Exception {
		LocaliteInvalideMatcher bean = new LocaliteInvalideMatcher();
		bean.setEnabled(true);
		bean.setLocalitesInvalides("adrese,etranger");
		bean.setFauxPositifs("sainte-adresse,etrangerio");
		bean.afterPropertiesSet();
		assertTrue(LocaliteInvalideMatcher.match("Sans Adresse"));
		assertTrue(LocaliteInvalideMatcher.match("E T R A N G E R"));
		assertFalse(LocaliteInvalideMatcher.match("F-23100 Sainte-Adresse"));
		assertFalse(LocaliteInvalideMatcher.match("E-34570 ETRANGERIO"));
	}

	@Test
	public void testTroisPatternsEtTroisFauxPositifs() throws Exception {
		LocaliteInvalideMatcher bean = new LocaliteInvalideMatcher();
		bean.setEnabled(true);
		bean.setLocalitesInvalides("adrese,etranger,inconu");
		bean.setFauxPositifs("sainte-adresse,etrangerio,inconusburg");
		bean.afterPropertiesSet();
		assertTrue(LocaliteInvalideMatcher.match("Sans Adresse"));
		assertTrue(LocaliteInvalideMatcher.match("E T R A N G E R"));
		assertTrue(LocaliteInvalideMatcher.match("Domicile INNCONU !"));
		assertFalse(LocaliteInvalideMatcher.match("F-23100 Sainte-Adresse"));
		assertFalse(LocaliteInvalideMatcher.match("E-34570 ETRANGERIO"));
		assertFalse(LocaliteInvalideMatcher.match("D-37880 INCONUSBURG"));
	}

	/**
	 * Les espaces entourant les virgules dans les propriétés localiteInvalide et fauxPositif
	 * ne doivent pas avoir d'incidence sur le résultat
	 *
	 */
	@Test
	public void testAvecDesEspacesAutourDesVirgules() throws Exception {
		LocaliteInvalideMatcher bean = new LocaliteInvalideMatcher();
		bean.setEnabled(true);
		bean.setLocalitesInvalides("adrese , etranger ,inconu");
		bean.setFauxPositifs("sainte-adresse , etrangerio , inconusburg");
		bean.afterPropertiesSet();
		assertTrue(LocaliteInvalideMatcher.match("adrese"));
		assertTrue(LocaliteInvalideMatcher.match("adresse"));
		assertTrue(LocaliteInvalideMatcher.match("etranger"));
		assertTrue(LocaliteInvalideMatcher.match("E T R A N G E R"));
		assertTrue(LocaliteInvalideMatcher.match("inconu"));
		assertTrue(LocaliteInvalideMatcher.match("innconue"));
		assertFalse(LocaliteInvalideMatcher.match("Sainte-Adresse-Sur-Venoge"));
		assertFalse(LocaliteInvalideMatcher.match("ETrangerio"));
		assertFalse(LocaliteInvalideMatcher.match("D-37880 INCONUSBURG"));
	}


	private final String [] patternsInvalidesLesPlusCourantDansUnireg = new String [] {
			"Sans Adresse",
			"Inconnu",
			"S/adresse",
			"Sans Adresse Connue",
			"I N C O N N U",
			"Etat Inconnu",
			"Etranger",
			"Etat inconnu",
			"Etat Inconnu Ou Non Indique",
			"inconnu",
			"Parti Sans Adresse",
			"Destination Inconnue",
			"Dest.Inconnue",
			"Inconnue",
			"Sans adresse",
			"- Destination Inconnue",
			"Laisser D'adresse",
			"Etat Inconnu Ou Non Indiqué",
			"Lieu Inconnu",
			"Adresse Inconnue",
			"Parti S.D.C.",
			"Sans Laisser D'adresse",
			"sans adresse",
			"- Etat Inconnu -",
			"Lieu inconnu",
			"E T R A N G E R",
			"Inconnu Etranger",
			"Dest. Inconnue",
			"Domicile Inconnu",
			"Sans adresse connue",
			"Parti Sans Laisser D'adresse",
			"Partie Sans Adresse",
			"I N C O N N U E",
			"Etat inconnu ou non indiqué",
			"00000 Sans Adresse",
			"S-Adresse",
			"00000 - Inconnu" };

	private String [] autresPatterns = new String [] {
			"E.T.T.R.A_N_G_E_R",
			"inconnnu",
			"inconnñnu",
			"sãns ÃdrëSSe",
			"partî"	};

	private String [] patternsFauxPositif = new String [] {
			"Sainte-Adresse",
			"Particino"};


	@Test
	public void testMatchLocaliteInvalide() throws Exception {
		setupNormal(true);
		for (String p : patternsInvalidesLesPlusCourantDansUnireg) {
			assertTrue(p + " est un pattern courant de localité invalide et n'a pas été detecté", LocaliteInvalideMatcher.match(p));
		}
		for (String p : autresPatterns) {
			assertTrue(p + " pas detecté", LocaliteInvalideMatcher.match(p));
		}
	}

	@Test
	public void testFauxPositifs() throws Exception {
		setupNormal(true);
		for (String p : patternsFauxPositif) {
			assertFalse(p + " detecté à tord", LocaliteInvalideMatcher.match(p));
		}
	}

	/**
	 * Setup de LocaliteInvalideMatcher tel qu'on peut le trouver en prod
	 *
	 * @param enabled composant activé ou pas
	 *
	 * @throws Exception
	 */
	private void setupNormal(boolean enabled) throws Exception {
		LocaliteInvalideMatcher bean = new LocaliteInvalideMatcher();
		bean.setEnabled(enabled);
		bean.setLocalitesInvalides("etranger,inconu,adrese,nonindique,parti,sdc");
		bean.setFauxPositifs("particino,sainte-adresse");
		bean.afterPropertiesSet();
	}
}
