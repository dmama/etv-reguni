package ch.vd.unireg.adresse;

import org.junit.Test;

import ch.vd.unireg.common.WithoutSpringTest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LocaliteInvalideMatcherServiceTest extends WithoutSpringTest {

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
	}

	@Test
	public void testMatchBlank() throws Exception {
		LocaliteInvalideMatcherService bean = setupNormal(true);
		//noinspection NullableProblems
		assertTrue("null n'est jamais une localité valide", bean.match(null));
		assertTrue("Une chaîne de 'blancs' n'est jamais une localité valide", bean.match(" \n\r\t"));
	}

	@Test
	public void testDisabled() throws Exception {
		LocaliteInvalideMatcherService bean = setupNormal(false);
		assertFalse(bean.match("inconnu"));
	}

	@Test
	public void testAucuneInitialisation() throws Exception {
		LocaliteInvalideMatcherService bean = new LocaliteInvalideMatcherServiceImpl();
		assertFalse(bean.match("inconnu"));
	}

	@Test
	public void testInitialiseMaisAucuneProprieteSettee() throws Exception {
		LocaliteInvalideMatcherServiceImpl bean = new LocaliteInvalideMatcherServiceImpl();
		bean.afterPropertiesSet();
		assertFalse(bean.match("inconnu"));
	}

	@Test
	public void testBasic() throws Exception {
		LocaliteInvalideMatcherServiceImpl bean = new LocaliteInvalideMatcherServiceImpl();
		bean.setEnabled(true);
		bean.setLocalitesInvalides("inconu");
		bean.afterPropertiesSet();
    	assertTrue("''inconu' est le pattern tel quel", bean.match("inconu"));
		assertTrue("''parti dans un pays inconu' contient le pattern tel quel", bean.match("parti dans un pays inconu"));
		assertFalse("''inco' ne contenant pas tout le pattern", bean.match("inco"));
		assertFalse("'parti dans un pays inco' ne contenant pas tout le pattern", bean.match("parti dans un pays inco"));
	}

	@Test
	public void testMatchMalgreUnNombreInapprorieDeLettre() throws Exception {
		LocaliteInvalideMatcherServiceImpl bean = new LocaliteInvalideMatcherServiceImpl();
		bean.setEnabled(true);
		bean.setLocalitesInvalides("inconu");
		bean.afterPropertiesSet();
		assertTrue("'innccconu' correspond au pattern avec un nombre de caractères identiques incorrect, ça devrait matcher", bean.match("innccconu"));
		bean.setLocalitesInvalides("inconnu");
		bean.afterPropertiesSet();
		assertFalse("'inconu' ne matche plus car il faut au moins 2 'n'", bean.match("inconu"));
	}

	@Test
	public void testMatchMalgreDesEspacesEtDesSeparateurs() throws Exception {
		LocaliteInvalideMatcherServiceImpl bean = new LocaliteInvalideMatcherServiceImpl();
		bean.setEnabled(true);
		bean.setLocalitesInvalides("inconu");
		bean.afterPropertiesSet();
		assertTrue("'i   n/ c| o, n----u' correspond au pattern avec des séparteurs entre chaque caratère, ça devrait matcher", bean.match("i   n/ c| o, n----u"));
	}

	@Test
	public void testMatchMalgreCasseDifferente() throws Exception {
		LocaliteInvalideMatcherServiceImpl bean = new LocaliteInvalideMatcherServiceImpl();
		bean.setEnabled(true);
		bean.setLocalitesInvalides("etranger");
		bean.afterPropertiesSet();
		assertTrue("'ETRANGER' correspond au pattern mais en majuscule", bean.match("ETRANGER"));
		assertTrue("'EtRaNgEr' correspond au pattern mais avec certaines lettres en majuscule", bean.match("EtRaNgEr"));
	}

	@Test
	public void testMatchMalgreAccents() throws Exception {
		LocaliteInvalideMatcherServiceImpl bean = new LocaliteInvalideMatcherServiceImpl();
		bean.setEnabled(true);
		bean.setLocalitesInvalides("etranger");
		bean.afterPropertiesSet();
		assertTrue("'étranger' correspond au pattern mais avec un accent", bean.match("étranger"));
		assertTrue("'êträngẽr' correspond au pattern même si fantaisiste", bean.match("êträngẽr"));
	}

	@Test
	public void testSimpleFauxPositifs() throws Exception {
		LocaliteInvalideMatcherServiceImpl bean = new LocaliteInvalideMatcherServiceImpl();
		bean.setEnabled(true);
		bean.setLocalitesInvalides("adrese");
		bean.setFauxPositifs("sainte-adresse");
		bean.afterPropertiesSet();
		assertTrue("'sainte-adrese' correspond au pattern et n'est pas un faux positif", bean.match("sainte-adrese"));
		assertFalse("'sainte-adresse' correspond au pattern mais est un faux positif", bean.match("sainte-adresse"));
		assertFalse("'12000 sainte-adresse' correspond au pattern mais est un faux positif", bean.match("12000 sainte-adresse"));
	}

	@Test
	public void testDeuxPatternsEtDeuxFauxPositifs() throws Exception {
		LocaliteInvalideMatcherServiceImpl bean = new LocaliteInvalideMatcherServiceImpl();
		bean.setEnabled(true);
		bean.setLocalitesInvalides("adrese,etranger");
		bean.setFauxPositifs("sainte-adresse,etrangerio");
		bean.afterPropertiesSet();
		assertTrue(bean.match("Sans Adresse"));
		assertTrue(bean.match("E T R A N G E R"));
		assertFalse(bean.match("F-23100 Sainte-Adresse"));
		assertFalse(bean.match("E-34570 ETRANGERIO"));
	}

	@Test
	public void testTroisPatternsEtTroisFauxPositifs() throws Exception {
		LocaliteInvalideMatcherServiceImpl bean = new LocaliteInvalideMatcherServiceImpl();
		bean.setEnabled(true);
		bean.setLocalitesInvalides("adrese,etranger,inconu");
		bean.setFauxPositifs("sainte-adresse,etrangerio,inconusburg");
		bean.afterPropertiesSet();
		assertTrue(bean.match("Sans Adresse"));
		assertTrue(bean.match("E T R A N G E R"));
		assertTrue(bean.match("Domicile INNCONU !"));
		assertFalse(bean.match("F-23100 Sainte-Adresse"));
		assertFalse(bean.match("E-34570 ETRANGERIO"));
		assertFalse(bean.match("D-37880 INCONUSBURG"));
	}

	/**
	 * Les espaces entourant les virgules dans les propriétés localiteInvalide et fauxPositif
	 * ne doivent pas avoir d'incidence sur le résultat
	 *
	 */
	@Test
	public void testAvecDesEspacesAutourDesVirgules() throws Exception {
		LocaliteInvalideMatcherServiceImpl bean = new LocaliteInvalideMatcherServiceImpl();
		bean.setEnabled(true);
		bean.setLocalitesInvalides("adrese , etranger ,inconu");
		bean.setFauxPositifs("sainte-adresse , etrangerio , inconusburg");
		bean.afterPropertiesSet();
		assertTrue(bean.match("adrese"));
		assertTrue(bean.match("adresse"));
		assertTrue(bean.match("etranger"));
		assertTrue(bean.match("E T R A N G E R"));
		assertTrue(bean.match("inconu"));
		assertTrue(bean.match("innconue"));
		assertFalse(bean.match("Sainte-Adresse-Sur-Venoge"));
		assertFalse(bean.match("ETrangerio"));
		assertFalse(bean.match("D-37880 INCONUSBURG"));
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
		LocaliteInvalideMatcherService bean = setupNormal(true);
		for (String p : patternsInvalidesLesPlusCourantDansUnireg) {
			assertTrue(p + " est un pattern courant de localité invalide et n'a pas été detecté", bean.match(p));
		}
		for (String p : autresPatterns) {
			assertTrue(p + " pas detecté", bean.match(p));
		}
	}

	@Test
	public void testFauxPositifs() throws Exception {
		LocaliteInvalideMatcherService bean = setupNormal(true);
		for (String p : patternsFauxPositif) {
			assertFalse(p + " detecté à tord", bean.match(p));
		}
	}

	/**
	 * Setup de LocaliteInvalideMatcher tel qu'on peut le trouver en prod
	 *
	 * @param enabled composant activé ou pas
	 *
	 * @throws Exception
	 */
	private LocaliteInvalideMatcherServiceImpl setupNormal(boolean enabled) throws Exception {
		LocaliteInvalideMatcherServiceImpl bean = new LocaliteInvalideMatcherServiceImpl();
		bean.setEnabled(enabled);
		bean.setLocalitesInvalides("etranger,inconu,adrese,nonindique,parti,sdc");
		bean.setFauxPositifs("particino,sainte-adresse");
		bean.afterPropertiesSet();
		return bean;
	}
}
