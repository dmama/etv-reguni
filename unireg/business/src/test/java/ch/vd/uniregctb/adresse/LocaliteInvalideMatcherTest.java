package ch.vd.uniregctb.adresse;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Test;

import ch.vd.uniregctb.utils.UniregProperties;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LocaliteInvalideMatcherTest {

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

	UniregProperties properties;

	private void setupNormal(boolean enabled) throws Exception {
		LocaliteInvalideMatcher bean = new LocaliteInvalideMatcher();
		// Un stub tout bête pour mocker le fichier unireg.properties
		properties = EasyMock.createNiceMock(UniregProperties.class);
		expect(properties.getProperty(eq("extprop." + LocaliteInvalideMatcherProperties.PROPERTY_ENABLED))).andStubReturn(Boolean.toString(enabled));
		expect(properties.getProperty(eq("extprop." + LocaliteInvalideMatcherProperties.PROPERTY_PATTERNS))).andStubReturn("etranger,inconu,adrese,nonindique,parti,sdc");
		expect(properties.getProperty(eq("extprop." + LocaliteInvalideMatcherProperties.PROPERTY_FAUX_POSITIFS))).andStubReturn("particino,sainte-adresse");
		replay(properties);

		// Mock l'instantiation par spring
		bean.setUniregProperties(properties);
		bean.afterPropertiesSet();
	}

	private void setupSansFichierProperties() throws Exception {
		LocaliteInvalideMatcher bean = new LocaliteInvalideMatcher();
		bean.afterPropertiesSet();
	}

	private void setupAvecFichierPropertiesMaisSansLesProprietes() throws Exception {
		LocaliteInvalideMatcher bean = new LocaliteInvalideMatcher();
		// Un stub tout bête pour mocker le fichier unireg.properties
		properties = EasyMock.createNiceMock(UniregProperties.class);
		replay(properties);
		// Mock l'instantiation par spring
		bean.setUniregProperties(properties);
		bean.afterPropertiesSet();
	}

	@After
	public void tearDown() {
		// Remise à zéro des champs static de la classe LocaliteInvalideMatcher
		LocaliteInvalideMatcher.reset();
	}


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

	@Test
	public void testDisabled() throws Exception {
		setupNormal(false);
		assertFalse(LocaliteInvalideMatcher.match(patternsInvalidesLesPlusCourantDansUnireg[0]));
	}

	@Test
	public void testAppelMatchSansInitialisation () {
		assertTrue("Sans initialisation, le composant s'auto-initialise avec les paramètres par défaut et devrait donc matcher le pattern invalide",
				LocaliteInvalideMatcher.match(patternsInvalidesLesPlusCourantDansUnireg[0]));
	}

	@Test
	public void testAppelMatchSansFichierProperties() throws Exception {
		setupSansFichierProperties();
		assertTrue("Sans fichier de propriétés le composant s'initialise avec les paramètres pas défaut et devrait donc matcher le pattern invalide",
				LocaliteInvalideMatcher.match(patternsInvalidesLesPlusCourantDansUnireg[0]));
	}

	@Test
	public void testAppelMatchAvecFichierPropertiesMaisSansLesProprietes() throws Exception {
		setupAvecFichierPropertiesMaisSansLesProprietes();
		assertFalse(
				"Avec un fichier de propriété présent, le composant s'attend à trouver les infos de config dedans, comme ce n'est pas le cas, le pattern invalide ne devrait pas matcher",
				LocaliteInvalideMatcher.match(patternsInvalidesLesPlusCourantDansUnireg[0]));
	}


}
