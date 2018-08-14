package ch.vd.unireg.regimefiscal;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.WithoutSpringTest;
import ch.vd.unireg.regimefiscal.RegimeFiscalServiceConfiguration.FormeJuridiqueMapping;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;

import static ch.vd.unireg.type.FormeJuridiqueEntreprise.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Raphaël Marmier, 2017-04-21, <raphael.marmier@vd.ch>
 */
public class RegimeFiscalServiceConfigurationTest extends WithoutSpringTest {

	private final RegimeFiscalServiceConfigurationImpl helper = new RegimeFiscalServiceConfigurationImpl();

	@Test
	public void testParseConfigRegimesDiOptionnelleVd() throws Exception {
		{
			final Set<String> strings = helper.parseConfigRegimesDiOptionnelleVd("190-2,739");
			assertNotNull(strings);
			assertEquals(2, strings.size());
			Assert.assertTrue(strings.contains("190-2"));
			Assert.assertTrue(strings.contains("739"));
		}
		{
			final Set<String> strings = helper.parseConfigRegimesDiOptionnelleVd("190-2, 739");
			assertNotNull(strings);
			assertEquals(2, strings.size());
			Assert.assertTrue(strings.contains("190-2"));
			Assert.assertTrue(strings.contains("739"));
		}
		{
			final Set<String> strings = helper.parseConfigRegimesDiOptionnelleVd("190-2         , 739");
			assertNotNull(strings);
			assertEquals(2, strings.size());
			Assert.assertTrue(strings.contains("190-2"));
			Assert.assertTrue(strings.contains("739"));
		}

		{
			final Set<String> strings = helper.parseConfigRegimesDiOptionnelleVd(null);
			assertEquals(Collections.emptySet(), strings);
		}

		try {
			helper.parseConfigRegimesDiOptionnelleVd("190-2, 739; 448");
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("Propriété de configuration extprop.regimesfiscaux.regimes.di.optionnelle.vd malformée: [190-2, 739; 448]", e.getMessage());
		}
	}

	@Test
	public void testParseTableRegimes() throws Exception {
		{
			final List<FormeJuridiqueMapping> list =
					helper.parseConfigFormesJuridiquesMapping("0103=>80,0104=>80,0105=>01,0106=>01,0107=>01,0108=>01,0109=>70,0110=>70,0111=>01,0151=>01,0312=>01");
			assertNotNull(list);
			testCheckMapDefaultCodePourFormeJuridique(list);

		}
		{
			final List<FormeJuridiqueMapping> list =
					helper.parseConfigFormesJuridiquesMapping("0103=>80, 0104=>80, 0105=>01, 0106=>01, 0107=>01, 0108=>01, 0109=>70, 0110=>70, 0111=>01, 0151=>01, 0312=>01");
			assertNotNull(list);
			testCheckMapDefaultCodePourFormeJuridique(list);

		}
		{
			final List<FormeJuridiqueMapping> list =
					helper.parseConfigFormesJuridiquesMapping("  0103=>80, 0104 =>  80, 0105=>01, 0106=>01,0107=>01, 0108=>01,   0109=>70,          0110=>70, 0111=>01, 0151=>01, 0312=>01");
			assertNotNull(list);
			testCheckMapDefaultCodePourFormeJuridique(list);

		}

		{
			final List<FormeJuridiqueMapping> list = helper.parseConfigFormesJuridiquesMapping(null);
			assertEquals(Collections.emptyList(), list);
		}

		try {
			{
				final String configTable = "0103=>80,0104=>80,0105=>01,0106=>01,0107=>01,0108=>01,0109=>70,0110=>70;0111=>01,0151=>01,0312=>01";
				helper.parseConfigFormesJuridiquesMapping(configTable);
				fail("La configuration erronée a pourtant été acceptée.");
			}
		}
		catch (IllegalArgumentException e) {
			assertEquals("Propriété de configuration extprop.regimesfiscaux.table.formesjuridiques.defauts malformée: [0103=>80,0104=>80,0105=>01,0106=>01,0107=>01,0108=>01,0109=>70,0110=>70;0111=>01,0151=>01,0312=>01]", e.getMessage());
		}

		try {
			{
				final String configTable = "0103=>80, 0104=>80, 0105=>01, 0106=>01, 0107=>01, 0108=>01, 0109=>70, 0110=>70, 0111=>01, 0151=>01, 03-12=>01";
				helper.parseConfigFormesJuridiquesMapping(configTable);
				fail("La configuration erronée a pourtant été acceptée.");
			}
		}
		catch (IllegalArgumentException e) {
			assertEquals("Propriété de configuration extprop.regimesfiscaux.table.formesjuridiques.defauts invalide: paire malformée [03-12=>01]", e.getMessage());
		}

		try {
			{
				final String configTable = "0103=>80, 0944=>80, 0105=>01, 0106=>01, 0107=>01, 0108=>01, 0109=>70, 0110=>70, 0111=>01, 0151=>01, 0312=>01";
				helper.parseConfigFormesJuridiquesMapping(configTable);
				fail("La configuration erronée a pourtant été acceptée.");
			}
		}
		catch (IllegalArgumentException e) {
			assertEquals("Configuration extprop.regimesfiscaux.table.formesjuridiques.defauts potentiellement erronée: Le code 0944 ne correspond à aucune forme juridique connue d'Unireg.", e.getMessage());
		}
	}

	@Test
	public void testParseTableRegimesAvecPeriodeValidite() throws Exception {

		assertOneMapping(null, null, FormeJuridiqueEntreprise.SNC, "80", helper.parseConfigFormesJuridiquesMapping("0103=>80{=>}"));
		assertOneMapping(RegDate.get(1990, 3, 4), null, FormeJuridiqueEntreprise.SNC, "80", helper.parseConfigFormesJuridiquesMapping("0103=>80{19900304=>}"));
		assertOneMapping(RegDate.get(1990, 3, 4), RegDate.get(2017, 12, 12), FormeJuridiqueEntreprise.SNC, "80", helper.parseConfigFormesJuridiquesMapping("0103=>80{19900304=>20171212}"));
		assertOneMapping(null, RegDate.get(2017, 12, 12), FormeJuridiqueEntreprise.SNC, "80", helper.parseConfigFormesJuridiquesMapping("0103=>80{=>20171212}"));

		try {
			helper.parseConfigFormesJuridiquesMapping("0103=>80{toto=>tutu}");
			fail("La configuration erronée a pourtant été acceptée.");
		}
		catch (IllegalArgumentException e) {
			assertEquals("Propriété de configuration extprop.regimesfiscaux.table.formesjuridiques.defauts invalide: paire malformée [0103=>80{toto=>tutu}]", e.getMessage());
		}

	}

	@Test
	public void testStandardConfiguration() throws Exception {
		// Standard
		{
			String formesJuridiquesDefauts = "0103=>80, 0104=>80, 0105=>01, 0106=>01, 0107=>01, 0108=>01, 0109=>70, 0110=>70, 0111=>01, 0151=>01, 0312=>01";
			String diOptionnelleVd = "190-2, 739";

			final RegimeFiscalServiceConfigurationImpl config = new RegimeFiscalServiceConfigurationImpl();
			config.setConfigTableFormesJuridiquesDefauts(formesJuridiquesDefauts);
			config.setConfigRegimesDiOptionnelleVd(diOptionnelleVd);
			config.afterPropertiesSet();

			assertEmpty(config.getMapping(EI));

			assertOneMapping(null, null, SNC, "80", config.getMapping(SNC));
			assertOneMapping(null, null, SC, "80", config.getMapping(SC));

			assertOneMapping(null, null, SCA, "01", config.getMapping(SCA));
			assertOneMapping(null, null, SA, "01", config.getMapping(SA));
			assertOneMapping(null, null, SARL, "01", config.getMapping(SARL));
			assertOneMapping(null, null, SCOOP, "01", config.getMapping(SCOOP));

			assertOneMapping(null, null, ASSOCIATION, "70", config.getMapping(ASSOCIATION));
			assertOneMapping(null, null, FONDATION, "70", config.getMapping(FONDATION));

			assertOneMapping(null, null, FILIALE_HS_RC, "01", config.getMapping(FILIALE_HS_RC));

			assertEmpty(config.getMapping(PARTICULIER));
			assertEmpty(config.getMapping(SCPC));
			assertEmpty(config.getMapping(SICAV));
			assertEmpty(config.getMapping(SICAF));
			assertEmpty(config.getMapping(IDP));
			assertEmpty(config.getMapping(PNC));
			assertEmpty(config.getMapping(INDIVISION));

			assertOneMapping(null, null, FILIALE_CH_RC, "01", config.getMapping(FILIALE_CH_RC));

			assertEmpty(config.getMapping(ADM_CH));
			assertEmpty(config.getMapping(ADM_CT));
			assertEmpty(config.getMapping(ADM_DI));
			assertEmpty(config.getMapping(ADM_CO));
			assertEmpty(config.getMapping(CORP_DP_ADM));
			assertEmpty(config.getMapping(ENT_CH));
			assertEmpty(config.getMapping(ENT_CT));
			assertEmpty(config.getMapping(ENT_DI));
			assertEmpty(config.getMapping(ENT_CO));
			assertEmpty(config.getMapping(CORP_DP_ENT));
			assertEmpty(config.getMapping(SS));

			assertOneMapping(null, null, FILIALE_HS_NIRC, "01", config.getMapping(FILIALE_HS_NIRC));

			assertEmpty(config.getMapping(ENT_PUBLIQUE_HS));
			assertEmpty(config.getMapping(ADM_PUBLIQUE_HS));
			assertEmpty(config.getMapping(ORG_INTERNAT));
			assertEmpty(config.getMapping(ENT_HS));

			assertEmpty(config.getMapping(null));

			assertTrue(config.isRegimeFiscalDiOptionnelleVd("190-2"));
			assertTrue(config.isRegimeFiscalDiOptionnelleVd("739"));
			assertFalse(config.isRegimeFiscalDiOptionnelleVd("1234"));
		}
		// Variante
		{
			String formesJuridiquesDefauts = "0104=>80, 0105=>109, 0106=>109, 0107=>01, 0108=>109, 0109=>70, 0110=>70, 0111=>01, 0224=>769, 0312=>01";
			String diOptionnelleVd = "739";

			final RegimeFiscalServiceConfigurationImpl config = new RegimeFiscalServiceConfigurationImpl();
			config.setConfigTableFormesJuridiquesDefauts(formesJuridiquesDefauts);
			config.setConfigRegimesDiOptionnelleVd(diOptionnelleVd);
			config.afterPropertiesSet();

			assertEmpty(config.getMapping(EI));

			assertEmpty(config.getMapping(SNC));
			assertOneMapping(null, null, SC, "80", config.getMapping(SC));

			assertOneMapping(null, null, SCA, "109", config.getMapping(SCA));
			assertOneMapping(null, null, SA, "109", config.getMapping(SA));
			assertOneMapping(null, null, SARL, "01", config.getMapping(SARL));
			assertOneMapping(null, null, SCOOP, "109", config.getMapping(SCOOP));

			assertOneMapping(null, null, ASSOCIATION, "70", config.getMapping(ASSOCIATION));
			assertOneMapping(null, null, FONDATION, "70", config.getMapping(FONDATION));

			assertOneMapping(null, null, FILIALE_HS_RC, "01", config.getMapping(FILIALE_HS_RC));

			assertEmpty(config.getMapping(PARTICULIER));
			assertEmpty(config.getMapping(SCPC));
			assertEmpty(config.getMapping(SICAV));
			assertEmpty(config.getMapping(SICAF));
			assertEmpty(config.getMapping(IDP));
			assertEmpty(config.getMapping(PNC));
			assertEmpty(config.getMapping(INDIVISION));

			assertEmpty(config.getMapping(FILIALE_CH_RC));

			assertEmpty(config.getMapping(ADM_CH));
			assertEmpty(config.getMapping(ADM_CT));
			assertEmpty(config.getMapping(ADM_DI));
			assertEmpty(config.getMapping(ADM_CO));

			assertOneMapping(null, null, CORP_DP_ADM, "769", config.getMapping(CORP_DP_ADM));

			assertEmpty(config.getMapping(ENT_CH));
			assertEmpty(config.getMapping(ENT_CT));
			assertEmpty(config.getMapping(ENT_DI));
			assertEmpty(config.getMapping(ENT_CO));
			assertEmpty(config.getMapping(CORP_DP_ENT));
			assertEmpty(config.getMapping(SS));

			assertOneMapping(null, null, FILIALE_HS_NIRC, "01", config.getMapping(FILIALE_HS_NIRC));

			assertEmpty(config.getMapping(ENT_PUBLIQUE_HS));
			assertEmpty(config.getMapping(ADM_PUBLIQUE_HS));
			assertEmpty(config.getMapping(ORG_INTERNAT));
			assertEmpty(config.getMapping(ENT_HS));

			assertEmpty(config.getMapping(null));

			assertFalse(config.isRegimeFiscalDiOptionnelleVd("190-2"));
			assertTrue(config.isRegimeFiscalDiOptionnelleVd("739"));
			assertFalse(config.isRegimeFiscalDiOptionnelleVd("1234"));
		}
	}

	@Test
	public void testEmptyConfiguration() throws Exception {
		{
			final RegimeFiscalServiceConfigurationImpl config = new RegimeFiscalServiceConfigurationImpl();
			config.setConfigTableFormesJuridiquesDefauts(null);
			config.setConfigRegimesDiOptionnelleVd(null);
			config.afterPropertiesSet();
			verifyEmptyConfig(config);
		}

		{
			final RegimeFiscalServiceConfigurationImpl config = new RegimeFiscalServiceConfigurationImpl();
			config.setConfigTableFormesJuridiquesDefauts("");
			config.setConfigRegimesDiOptionnelleVd("");
			config.afterPropertiesSet();
			verifyEmptyConfig(config);
		}
	}

	private void verifyEmptyConfig(RegimeFiscalServiceConfigurationImpl config) {
		assertEmpty(config.getMapping(EI));

		assertEmpty(config.getMapping(SNC));
		assertEmpty(config.getMapping(SC));

		assertEmpty(config.getMapping(SCA));
		assertEmpty(config.getMapping(SA));
		assertEmpty(config.getMapping(SARL));
		assertEmpty(config.getMapping(SCOOP));

		assertEmpty(config.getMapping(ASSOCIATION));
		assertEmpty(config.getMapping(FONDATION));

		assertEmpty(config.getMapping(FILIALE_HS_RC));

		assertEmpty(config.getMapping(PARTICULIER));
		assertEmpty(config.getMapping(SCPC));
		assertEmpty(config.getMapping(SICAV));
		assertEmpty(config.getMapping(SICAF));
		assertEmpty(config.getMapping(IDP));
		assertEmpty(config.getMapping(PNC));
		assertEmpty(config.getMapping(INDIVISION));

		assertEmpty(config.getMapping(FILIALE_CH_RC));

		assertEmpty(config.getMapping(ADM_CH));
		assertEmpty(config.getMapping(ADM_CT));
		assertEmpty(config.getMapping(ADM_DI));
		assertEmpty(config.getMapping(ADM_CO));
		assertEmpty(config.getMapping(CORP_DP_ADM));
		assertEmpty(config.getMapping(ENT_CH));
		assertEmpty(config.getMapping(ENT_CT));
		assertEmpty(config.getMapping(ENT_DI));
		assertEmpty(config.getMapping(ENT_CO));
		assertEmpty(config.getMapping(CORP_DP_ENT));
		assertEmpty(config.getMapping(SS));

		assertEmpty(config.getMapping(FILIALE_HS_NIRC));

		assertEmpty(config.getMapping(ENT_PUBLIQUE_HS));
		assertEmpty(config.getMapping(ADM_PUBLIQUE_HS));
		assertEmpty(config.getMapping(ORG_INTERNAT));
		assertEmpty(config.getMapping(ENT_HS));

		assertEmpty(config.getMapping(null));

		assertFalse(config.isRegimeFiscalDiOptionnelleVd("190-2"));
		assertFalse(config.isRegimeFiscalDiOptionnelleVd("739"));
		assertFalse(config.isRegimeFiscalDiOptionnelleVd("1234"));
	}

	private void testCheckMapDefaultCodePourFormeJuridique(List<FormeJuridiqueMapping> list) {
		// "0103=>80,0104=>80,0105=>01,0106=>01,0107=>01,0108=>01,0109=>70,0110=>70,0111=>01,0151=>01,0312=>01"
		assertEquals(11, list.size());
		assertMapping(null, null, FormeJuridiqueEntreprise.SNC, "80", list.get(0));
		assertMapping(null, null, FormeJuridiqueEntreprise.SC, "80", list.get(1));
		assertMapping(null, null, FormeJuridiqueEntreprise.SCA, "01", list.get(2));
		assertMapping(null, null, FormeJuridiqueEntreprise.SA, "01", list.get(3));
		assertMapping(null, null, FormeJuridiqueEntreprise.SARL, "01", list.get(4));
		assertMapping(null, null, FormeJuridiqueEntreprise.SCOOP, "01", list.get(5));
		assertMapping(null, null, FormeJuridiqueEntreprise.ASSOCIATION, "70", list.get(6));
		assertMapping(null, null, FormeJuridiqueEntreprise.FONDATION, "70", list.get(7));
		assertMapping(null, null, FormeJuridiqueEntreprise.FILIALE_HS_RC, "01", list.get(8));
		assertMapping(null, null, FormeJuridiqueEntreprise.FILIALE_CH_RC, "01", list.get(9));
		assertMapping(null, null, FormeJuridiqueEntreprise.FILIALE_HS_NIRC, "01", list.get(10));
	}

	private static void assertMapping(RegDate dateDebut, RegDate dateFin, FormeJuridiqueEntreprise formeJuridiqueEntreprise, String codeRegime, FormeJuridiqueMapping mapping) {
		assertNotNull(mapping);
		assertEquals(dateDebut, mapping.getDateDebut());
		assertEquals(dateFin, mapping.getDateFin());
		assertEquals(formeJuridiqueEntreprise, mapping.getFormeJuridique());
		assertEquals(codeRegime, mapping.getCodeRegime());
	}

	private void assertOneMapping(RegDate dateDebut, RegDate dateFin, FormeJuridiqueEntreprise formeJuridiqueEntreprise, String codeRegime, List<FormeJuridiqueMapping> mappings) {
		assertEquals(1, mappings.size());
		assertMapping(dateDebut, dateFin, formeJuridiqueEntreprise, codeRegime, mappings.get(0));
	}
}