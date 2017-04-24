package ch.vd.uniregctb.regimefiscal;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;

import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.ADM_CH;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.ADM_CO;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.ADM_CT;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.ADM_DI;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.ADM_PUBLIQUE_HS;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.ASSOCIATION;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.CORP_DP_ADM;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.CORP_DP_ENT;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.EI;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.ENT_CH;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.ENT_CO;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.ENT_CT;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.ENT_DI;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.ENT_HS;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.ENT_PUBLIQUE_HS;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.FILIALE_CH_RC;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.FILIALE_HS_NIRC;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.FILIALE_HS_RC;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.FONDATION;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.IDP;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.INDIVISION;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.ORG_INTERNAT;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.PARTICULIER;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.PNC;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.SA;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.SARL;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.SC;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.SCA;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.SCOOP;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.SCPC;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.SICAF;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.SICAV;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.SNC;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.SS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Raphaël Marmier, 2017-04-21, <raphael.marmier@vd.ch>
 */
public class ServiceRegimeFiscalConfigurationTest extends WithoutSpringTest {

	private final ServiceRegimeFiscalConfigurationImpl helper = new ServiceRegimeFiscalConfigurationImpl();

	@Test
	public void testParseConfigRegimesDiOptionnelleVd() throws Exception {
		{
			final Set<String> strings = helper.parseConfigRegimesDiOptionnelleVd("190-2,739");
			Assert.assertNotNull(strings);
			Assert.assertEquals(2, strings.size());
			Assert.assertTrue(strings.contains("190-2"));
			Assert.assertTrue(strings.contains("739"));
		}
		{
			final Set<String> strings = helper.parseConfigRegimesDiOptionnelleVd("190-2, 739");
			Assert.assertNotNull(strings);
			Assert.assertEquals(2, strings.size());
			Assert.assertTrue(strings.contains("190-2"));
			Assert.assertTrue(strings.contains("739"));
		}
		{
			final Set<String> strings = helper.parseConfigRegimesDiOptionnelleVd("190-2         , 739");
			Assert.assertNotNull(strings);
			Assert.assertEquals(2, strings.size());
			Assert.assertTrue(strings.contains("190-2"));
			Assert.assertTrue(strings.contains("739"));
		}

		try {
			final Set<String> strings = helper.parseConfigRegimesDiOptionnelleVd(null);
			Assert.assertEquals(Collections.emptySet(), strings);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("Propriété de configuration extprop.regimesfiscaux.regimes.di.optionnelle.vd non renseigné.", e.getMessage());
		}

		try {
			final Set<String> strings = helper.parseConfigRegimesDiOptionnelleVd("190-2, 739; 448");
			Assert.fail();
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("Propriété de configuration extprop.regimesfiscaux.regimes.di.optionnelle.vd malformée: [190-2, 739; 448]", e.getMessage());
		}
	}

	@Test
	public void testParseTableRegimes() throws Exception {
		{
			final Map<FormeJuridiqueEntreprise, String> map =
					helper.parseConfigTableFormesJuridiquesDefauts("0103=80,0104=80,0105=01,0106=01,0107=01,0108=01,0109=70,0110=70,0111=01,0151=01,0312=01");
			Assert.assertNotNull(map);
			testCheckMapDefaultCodePourFormeJuridique(map);

		}
		{
			final Map<FormeJuridiqueEntreprise, String> map =
					helper.parseConfigTableFormesJuridiquesDefauts("0103=80, 0104=80, 0105=01, 0106=01, 0107=01, 0108=01, 0109=70, 0110=70, 0111=01, 0151=01, 0312=01");
			Assert.assertNotNull(map);
			testCheckMapDefaultCodePourFormeJuridique(map);

		}
		{
			final Map<FormeJuridiqueEntreprise, String> map =
					helper.parseConfigTableFormesJuridiquesDefauts("  0103=80, 0104 =  80, 0105=01, 0106=01,0107=01, 0108=01,   0109=70,          0110=70, 0111=01, 0151=01, 0312=01");
			Assert.assertNotNull(map);
			testCheckMapDefaultCodePourFormeJuridique(map);

		}

		{
			final Map<FormeJuridiqueEntreprise, String> map = helper.parseConfigTableFormesJuridiquesDefauts(null);
			Assert.assertEquals(Collections.emptyMap(), map);
		}

		try {
			{
				final String configTable = "0103=80,0104=80,0105=01,0106=01,0107=01,0108=01,0109=70,0110=70;0111=01,0151=01,0312=01";
				final Map<FormeJuridiqueEntreprise, String> map = helper.parseConfigTableFormesJuridiquesDefauts(configTable);
				Assert.fail("La configuration erronnée a pourtant été acceptée.");
			}
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("Propriété de configuration extprop.regimesfiscaux.table.formesjuridiques.defauts malformée: [0103=80,0104=80,0105=01,0106=01,0107=01,0108=01,0109=70,0110=70;0111=01,0151=01,0312=01]", e.getMessage());
		}

		try {
			{
				final String configTable = "0103=80, 0104=80, 0105=01, 0106=01, 0107=01, 0108=01, 0109=70, 0110=70, 0111=01, 0151=01, 03-12=01";
				final Map<FormeJuridiqueEntreprise, String> map = helper.parseConfigTableFormesJuridiquesDefauts(configTable);
				Assert.fail("La configuration erronnée a pourtant été acceptée.");
			}
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("Propriété de configuration extprop.regimesfiscaux.defauts.formesjuridiques.map invalide: paire malformée [03-12=01]", e.getMessage());
		}

		try {
			{
				final String configTable = "0103=80, 0944=80, 0105=01, 0106=01, 0107=01, 0108=01, 0109=70, 0110=70, 0111=01, 0151=01, 0312=01";
				final Map<FormeJuridiqueEntreprise, String> map = helper.parseConfigTableFormesJuridiquesDefauts(configTable);
				Assert.fail("La configuration erronnée a pourtant été acceptée.");
			}
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("Configuration extprop.regimesfiscaux.defauts.formesjuridiques.map potentiellement erronnée: Le code 0944 ne correspond à aucune forme juridique connue d'Unireg.", e.getMessage());
		}
	}

	@Test
	public void testStandardConfiguration() throws Exception {
		// Standard
		{
			String formesJuridiquesDefauts = "0103=80, 0104=80, 0105=01, 0106=01, 0107=01, 0108=01, 0109=70, 0110=70, 0111=01, 0151=01, 0312=01";
			String diOptionnelleVd = "190-2, 739";

			final ServiceRegimeFiscalConfigurationImpl config = new ServiceRegimeFiscalConfigurationImpl();
			config.setConfigTableFormesJuridiquesDefauts(formesJuridiquesDefauts);
			config.setConfigRegimesDiOptionnelleVd(diOptionnelleVd);
			config.afterPropertiesSet();

			assertNull(config.getCodeTypeRegimeFiscal(EI));

			assertEquals("80", config.getCodeTypeRegimeFiscal(SNC));
			assertEquals("80", config.getCodeTypeRegimeFiscal(SC));

			assertEquals("01", config.getCodeTypeRegimeFiscal(SCA));
			assertEquals("01", config.getCodeTypeRegimeFiscal(SA));
			assertEquals("01", config.getCodeTypeRegimeFiscal(SARL));
			assertEquals("01", config.getCodeTypeRegimeFiscal(SCOOP));

			assertEquals("70", config.getCodeTypeRegimeFiscal(ASSOCIATION));
			assertEquals("70", config.getCodeTypeRegimeFiscal(FONDATION));

			assertEquals("01", config.getCodeTypeRegimeFiscal(FILIALE_HS_RC));

			assertNull(config.getCodeTypeRegimeFiscal(PARTICULIER));
			assertNull(config.getCodeTypeRegimeFiscal(SCPC));
			assertNull(config.getCodeTypeRegimeFiscal(SICAV));
			assertNull(config.getCodeTypeRegimeFiscal(SICAF));
			assertNull(config.getCodeTypeRegimeFiscal(IDP));
			assertNull(config.getCodeTypeRegimeFiscal(PNC));
			assertNull(config.getCodeTypeRegimeFiscal(INDIVISION));

			assertEquals("01", config.getCodeTypeRegimeFiscal(FILIALE_CH_RC));

			assertNull(config.getCodeTypeRegimeFiscal(ADM_CH));
			assertNull(config.getCodeTypeRegimeFiscal(ADM_CT));
			assertNull(config.getCodeTypeRegimeFiscal(ADM_DI));
			assertNull(config.getCodeTypeRegimeFiscal(ADM_CO));
			assertNull(config.getCodeTypeRegimeFiscal(CORP_DP_ADM));
			assertNull(config.getCodeTypeRegimeFiscal(ENT_CH));
			assertNull(config.getCodeTypeRegimeFiscal(ENT_CT));
			assertNull(config.getCodeTypeRegimeFiscal(ENT_DI));
			assertNull(config.getCodeTypeRegimeFiscal(ENT_CO));
			assertNull(config.getCodeTypeRegimeFiscal(CORP_DP_ENT));
			assertNull(config.getCodeTypeRegimeFiscal(SS));

			assertEquals("01", config.getCodeTypeRegimeFiscal(FILIALE_HS_NIRC));

			assertNull(config.getCodeTypeRegimeFiscal(ENT_PUBLIQUE_HS));
			assertNull(config.getCodeTypeRegimeFiscal(ADM_PUBLIQUE_HS));
			assertNull(config.getCodeTypeRegimeFiscal(ORG_INTERNAT));
			assertNull(config.getCodeTypeRegimeFiscal(ENT_HS));

			assertNull(config.getCodeTypeRegimeFiscal(null));

			assertTrue(config.isRegimeFiscalDiOptionnelleVd("190-2"));
			assertTrue(config.isRegimeFiscalDiOptionnelleVd("739"));
			assertFalse(config.isRegimeFiscalDiOptionnelleVd("1234"));
		}
		// Variante
		{
			String formesJuridiquesDefauts = "0104=80, 0105=109, 0106=109, 0107=01, 0108=109, 0109=70, 0110=70, 0111=01, 0224=769, 0312=01";
			String diOptionnelleVd = "739";

			final ServiceRegimeFiscalConfigurationImpl config = new ServiceRegimeFiscalConfigurationImpl();
			config.setConfigTableFormesJuridiquesDefauts(formesJuridiquesDefauts);
			config.setConfigRegimesDiOptionnelleVd(diOptionnelleVd);
			config.afterPropertiesSet();

			assertNull(config.getCodeTypeRegimeFiscal(EI));

			assertNull(config.getCodeTypeRegimeFiscal(SNC));
			assertEquals("80", config.getCodeTypeRegimeFiscal(SC));

			assertEquals("109", config.getCodeTypeRegimeFiscal(SCA));
			assertEquals("109", config.getCodeTypeRegimeFiscal(SA));
			assertEquals("01", config.getCodeTypeRegimeFiscal(SARL));
			assertEquals("109", config.getCodeTypeRegimeFiscal(SCOOP));

			assertEquals("70", config.getCodeTypeRegimeFiscal(ASSOCIATION));
			assertEquals("70", config.getCodeTypeRegimeFiscal(FONDATION));

			assertEquals("01", config.getCodeTypeRegimeFiscal(FILIALE_HS_RC));

			assertNull(config.getCodeTypeRegimeFiscal(PARTICULIER));
			assertNull(config.getCodeTypeRegimeFiscal(SCPC));
			assertNull(config.getCodeTypeRegimeFiscal(SICAV));
			assertNull(config.getCodeTypeRegimeFiscal(SICAF));
			assertNull(config.getCodeTypeRegimeFiscal(IDP));
			assertNull(config.getCodeTypeRegimeFiscal(PNC));
			assertNull(config.getCodeTypeRegimeFiscal(INDIVISION));

			assertNull(config.getCodeTypeRegimeFiscal(FILIALE_CH_RC));

			assertNull(config.getCodeTypeRegimeFiscal(ADM_CH));
			assertNull(config.getCodeTypeRegimeFiscal(ADM_CT));
			assertNull(config.getCodeTypeRegimeFiscal(ADM_DI));
			assertNull(config.getCodeTypeRegimeFiscal(ADM_CO));

			assertEquals("769", config.getCodeTypeRegimeFiscal(CORP_DP_ADM));

			assertNull(config.getCodeTypeRegimeFiscal(ENT_CH));
			assertNull(config.getCodeTypeRegimeFiscal(ENT_CT));
			assertNull(config.getCodeTypeRegimeFiscal(ENT_DI));
			assertNull(config.getCodeTypeRegimeFiscal(ENT_CO));
			assertNull(config.getCodeTypeRegimeFiscal(CORP_DP_ENT));
			assertNull(config.getCodeTypeRegimeFiscal(SS));

			assertEquals("01", config.getCodeTypeRegimeFiscal(FILIALE_HS_NIRC));

			assertNull(config.getCodeTypeRegimeFiscal(ENT_PUBLIQUE_HS));
			assertNull(config.getCodeTypeRegimeFiscal(ADM_PUBLIQUE_HS));
			assertNull(config.getCodeTypeRegimeFiscal(ORG_INTERNAT));
			assertNull(config.getCodeTypeRegimeFiscal(ENT_HS));

			assertNull(config.getCodeTypeRegimeFiscal(null));

			assertFalse(config.isRegimeFiscalDiOptionnelleVd("190-2"));
			assertTrue(config.isRegimeFiscalDiOptionnelleVd("739"));
			assertFalse(config.isRegimeFiscalDiOptionnelleVd("1234"));
		}
	}

	@Test
	public void testEmptyConfiguration() throws Exception {
		{
			final ServiceRegimeFiscalConfigurationImpl config = new ServiceRegimeFiscalConfigurationImpl();
			config.setConfigTableFormesJuridiquesDefauts(null);
			config.setConfigRegimesDiOptionnelleVd(null);
			config.afterPropertiesSet();
			verifyEmptyConfig(config);
		}

		{
			final ServiceRegimeFiscalConfigurationImpl config = new ServiceRegimeFiscalConfigurationImpl();
			config.setConfigTableFormesJuridiquesDefauts("");
			config.setConfigRegimesDiOptionnelleVd("");
			config.afterPropertiesSet();
			verifyEmptyConfig(config);
		}
	}

	private void verifyEmptyConfig(ServiceRegimeFiscalConfigurationImpl config) {
		assertNull(config.getCodeTypeRegimeFiscal(EI));

		assertNull(config.getCodeTypeRegimeFiscal(SNC));
		assertNull(config.getCodeTypeRegimeFiscal(SC));

		assertNull(config.getCodeTypeRegimeFiscal(SCA));
		assertNull(config.getCodeTypeRegimeFiscal(SA));
		assertNull(config.getCodeTypeRegimeFiscal(SARL));
		assertNull(config.getCodeTypeRegimeFiscal(SCOOP));

		assertNull(config.getCodeTypeRegimeFiscal(ASSOCIATION));
		assertNull(config.getCodeTypeRegimeFiscal(FONDATION));

		assertNull(config.getCodeTypeRegimeFiscal(FILIALE_HS_RC));

		assertNull(config.getCodeTypeRegimeFiscal(PARTICULIER));
		assertNull(config.getCodeTypeRegimeFiscal(SCPC));
		assertNull(config.getCodeTypeRegimeFiscal(SICAV));
		assertNull(config.getCodeTypeRegimeFiscal(SICAF));
		assertNull(config.getCodeTypeRegimeFiscal(IDP));
		assertNull(config.getCodeTypeRegimeFiscal(PNC));
		assertNull(config.getCodeTypeRegimeFiscal(INDIVISION));

		assertNull(config.getCodeTypeRegimeFiscal(FILIALE_CH_RC));

		assertNull(config.getCodeTypeRegimeFiscal(ADM_CH));
		assertNull(config.getCodeTypeRegimeFiscal(ADM_CT));
		assertNull(config.getCodeTypeRegimeFiscal(ADM_DI));
		assertNull(config.getCodeTypeRegimeFiscal(ADM_CO));
		assertNull(config.getCodeTypeRegimeFiscal(CORP_DP_ADM));
		assertNull(config.getCodeTypeRegimeFiscal(ENT_CH));
		assertNull(config.getCodeTypeRegimeFiscal(ENT_CT));
		assertNull(config.getCodeTypeRegimeFiscal(ENT_DI));
		assertNull(config.getCodeTypeRegimeFiscal(ENT_CO));
		assertNull(config.getCodeTypeRegimeFiscal(CORP_DP_ENT));
		assertNull(config.getCodeTypeRegimeFiscal(SS));

		assertNull(config.getCodeTypeRegimeFiscal(FILIALE_HS_NIRC));

		assertNull(config.getCodeTypeRegimeFiscal(ENT_PUBLIQUE_HS));
		assertNull(config.getCodeTypeRegimeFiscal(ADM_PUBLIQUE_HS));
		assertNull(config.getCodeTypeRegimeFiscal(ORG_INTERNAT));
		assertNull(config.getCodeTypeRegimeFiscal(ENT_HS));

		assertNull(config.getCodeTypeRegimeFiscal(null));

		assertFalse(config.isRegimeFiscalDiOptionnelleVd("190-2"));
		assertFalse(config.isRegimeFiscalDiOptionnelleVd("739"));
		assertFalse(config.isRegimeFiscalDiOptionnelleVd("1234"));
	}

	private void testCheckMapDefaultCodePourFormeJuridique(Map<FormeJuridiqueEntreprise, String> map) throws Exception {
		Assert.assertEquals(11, map.size());

		assertNull(map.get(EI));

		assertEquals("80", map.get(SNC));
		assertEquals("80", map.get(SC));

		assertEquals("01", map.get(SCA));
		assertEquals("01", map.get(SA));
		assertEquals("01", map.get(SARL));
		assertEquals("01", map.get(SCOOP));

		assertEquals("70", map.get(ASSOCIATION));
		assertEquals("70", map.get(FONDATION));

		assertEquals("01", map.get(FILIALE_HS_RC));

		assertNull(map.get(PARTICULIER));
		assertNull(map.get(SCPC));
		assertNull(map.get(SICAV));
		assertNull(map.get(SICAF));
		assertNull(map.get(IDP));
		assertNull(map.get(PNC));
		assertNull(map.get(INDIVISION));

		assertEquals("01", map.get(FILIALE_CH_RC));

		assertNull(map.get(ADM_CH));
		assertNull(map.get(ADM_CT));
		assertNull(map.get(ADM_DI));
		assertNull(map.get(ADM_CO));
		assertNull(map.get(CORP_DP_ADM));
		assertNull(map.get(ENT_CH));
		assertNull(map.get(ENT_CT));
		assertNull(map.get(ENT_DI));
		assertNull(map.get(ENT_CO));
		assertNull(map.get(CORP_DP_ENT));
		assertNull(map.get(SS));

		assertEquals("01", map.get(FILIALE_HS_NIRC));

		assertNull(map.get(ENT_PUBLIQUE_HS));
		assertNull(map.get(ADM_PUBLIQUE_HS));
		assertNull(map.get(ORG_INTERNAT));
		assertNull(map.get(ENT_HS));
	}
}