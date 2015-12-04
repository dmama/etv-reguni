package ch.vd.uniregctb.migration.pm.extractor;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.uniregctb.migration.pm.MigrationResultCollector;
import ch.vd.uniregctb.migration.pm.engine.MockGraphe;
import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCoordonneesFinancieres;
import ch.vd.uniregctb.migration.pm.regpm.RegpmInstitutionFinanciere;

public class IbanExtractorTest {

	private static RegpmInstitutionFinanciere buildInstitutionFinanciere(int clearing) {
		final RegpmInstitutionFinanciere inst = new RegpmInstitutionFinanciere();
		inst.setNoClearing(Integer.toString(clearing));
		return inst;
	}

	private static RegpmCoordonneesFinancieres buildCoordonneesFinancieres(String bicSwift, String iban, RegpmInstitutionFinanciere institutionFinanciere, String ccp, String noCompteBancaire) {
		final RegpmCoordonneesFinancieres cf = new RegpmCoordonneesFinancieres();
		cf.setBicSwift(bicSwift);
		cf.setIban(iban);
		cf.setInstitutionFinanciere(institutionFinanciere);
		cf.setNoCCP(ccp);
		cf.setNoCompteBancaire(noCompteBancaire);
		return cf;
	}

	@Test
	public void testDepuisIban() throws Exception {
		{
			final MigrationResultCollector mr = new MigrationResultCollector(new MockGraphe(null, null, null));
			Assert.assertEquals("CH003247823", IbanExtractor.extractIban(buildCoordonneesFinancieres(null, "CH003247823", null, "1-000001-2", "36324214423"), mr));

			final Map<LogCategory, List<MigrationResultCollector.Message>> messages = mr.getMessages();
			Assert.assertEquals(EnumSet.of(LogCategory.COORDONNEES_FINANCIERES), messages.keySet());

			final List<String> textes = messages.get(LogCategory.COORDONNEES_FINANCIERES).stream().map(m -> m.text).collect(Collectors.toList());
			Assert.assertEquals(Collections.singletonList("IBAN déjà présent dans les données source : CH003247823."), textes);
		}
		{
			final MigrationResultCollector mr = new MigrationResultCollector(new MockGraphe(null, null, null));
			Assert.assertEquals("CH003247822", IbanExtractor.extractIban(buildCoordonneesFinancieres("CHPOFIBEXX", "CH003247822", null, "1-000001-2", "36324214423"), mr));

			final Map<LogCategory, List<MigrationResultCollector.Message>> messages = mr.getMessages();
			Assert.assertEquals(EnumSet.of(LogCategory.COORDONNEES_FINANCIERES), messages.keySet());

			final List<String> textes = messages.get(LogCategory.COORDONNEES_FINANCIERES).stream().map(m -> m.text).collect(Collectors.toList());
			Assert.assertEquals(Collections.singletonList("IBAN déjà présent dans les données source : CH003247822."), textes);
		}
		{
			final MigrationResultCollector mr = new MigrationResultCollector(new MockGraphe(null, null, null));
			Assert.assertEquals("CH003247824", IbanExtractor.extractIban(buildCoordonneesFinancieres("DUMMY", "CH003247824", null, "1-000001-2", "36324214423"), mr));

			final Map<LogCategory, List<MigrationResultCollector.Message>> messages = mr.getMessages();
			Assert.assertEquals(EnumSet.of(LogCategory.COORDONNEES_FINANCIERES), messages.keySet());

			final List<String> textes = messages.get(LogCategory.COORDONNEES_FINANCIERES).stream().map(m -> m.text).collect(Collectors.toList());
			Assert.assertEquals(Collections.singletonList("IBAN déjà présent dans les données source : CH003247824."), textes);
		}
	}

	@Test
	public void testDepuisCCP() throws Exception {
		{
			final MigrationResultCollector mr = new MigrationResultCollector(new MockGraphe(null, null, null));
			Assert.assertEquals("CH7009000000170003317", IbanExtractor.extractIban(buildCoordonneesFinancieres(null, null, null, "17-331-7", "36324214423"), mr));

			final Map<LogCategory, List<MigrationResultCollector.Message>> messages = mr.getMessages();
			Assert.assertEquals(EnumSet.of(LogCategory.COORDONNEES_FINANCIERES), messages.keySet());

			final List<String> textes = messages.get(LogCategory.COORDONNEES_FINANCIERES).stream().map(m -> m.text).collect(Collectors.toList());
			Assert.assertEquals(Collections.singletonList("IBAN extrait du numéro CCP 17-331-7 : CH7009000000170003317."), textes);
		}
		{
			final MigrationResultCollector mr = new MigrationResultCollector(new MockGraphe(null, null, null));
			Assert.assertEquals("CH4709000000250000019", IbanExtractor.extractIban(buildCoordonneesFinancieres("CHPOFIBEXX", null, null, "25-1-9", "36324214423"), mr));

			final Map<LogCategory, List<MigrationResultCollector.Message>> messages = mr.getMessages();
			Assert.assertEquals(EnumSet.of(LogCategory.COORDONNEES_FINANCIERES), messages.keySet());

			final List<String> textes = messages.get(LogCategory.COORDONNEES_FINANCIERES).stream().map(m -> m.text).collect(Collectors.toList());
			Assert.assertEquals(Collections.singletonList("IBAN extrait du numéro CCP 25-1-9 : CH4709000000250000019."), textes);
		}
		{
			final MigrationResultCollector mr = new MigrationResultCollector(new MockGraphe(null, null, null));
			Assert.assertEquals("CH1309000000120005014", IbanExtractor.extractIban(buildCoordonneesFinancieres("DUMMY", null, null, "12-00501-4", "36324214423"), mr));

			final Map<LogCategory, List<MigrationResultCollector.Message>> messages = mr.getMessages();
			Assert.assertEquals(EnumSet.of(LogCategory.COORDONNEES_FINANCIERES), messages.keySet());

			final List<String> textes = messages.get(LogCategory.COORDONNEES_FINANCIERES).stream().map(m -> m.text).collect(Collectors.toList());
			Assert.assertEquals(Collections.singletonList("IBAN extrait du numéro CCP 12-00501-4 : CH1309000000120005014."), textes);
		}
	}

	@Test
	public void testDepuisCompteBancaire() throws Exception {
		{
			final MigrationResultCollector mr = new MigrationResultCollector(new MockGraphe(null, null, null));

			// Exemple tiré de http://www.six-interbank-clearing.com/dam/downloads/fr/standardization/iban/calculator/dl_tkicch_prufziffer.pdf
			Assert.assertEquals("CH10002300A1023502601", IbanExtractor.extractIban(buildCoordonneesFinancieres(null, null, buildInstitutionFinanciere(230), null, "A-10.2350.26.01"), mr));

			final Map<LogCategory, List<MigrationResultCollector.Message>> messages = mr.getMessages();
			Assert.assertEquals(EnumSet.of(LogCategory.COORDONNEES_FINANCIERES), messages.keySet());

			final List<String> textes = messages.get(LogCategory.COORDONNEES_FINANCIERES).stream().map(m -> m.text).collect(Collectors.toList());
			Assert.assertEquals(Collections.singletonList("IBAN extrait du numéro de compte 'A-10.2350.26.01' et du clearing '230' : CH10002300A1023502601."), textes);
		}
		{
			final MigrationResultCollector mr = new MigrationResultCollector(new MockGraphe(null, null, null));

			// Exemple de http://www.ubs.com/ch/en/swissbank/private/pay-and-save/payments/iban.html
			Assert.assertEquals("CH350023023050422318T", IbanExtractor.extractIban(buildCoordonneesFinancieres(null, null, buildInstitutionFinanciere(230), null, "230-504223.18T"), mr));

			final Map<LogCategory, List<MigrationResultCollector.Message>> messages = mr.getMessages();
			Assert.assertEquals(EnumSet.of(LogCategory.COORDONNEES_FINANCIERES), messages.keySet());

			final List<String> textes = messages.get(LogCategory.COORDONNEES_FINANCIERES).stream().map(m -> m.text).collect(Collectors.toList());
			Assert.assertEquals(Collections.singletonList("IBAN extrait du numéro de compte '230-504223.18T' et du clearing '230' : CH350023023050422318T."), textes);
		}
		{
			final MigrationResultCollector mr = new MigrationResultCollector(new MockGraphe(null, null, null));

			// Exemple du cas SIFISC-16925
			Assert.assertEquals("CH4108440776115290083", IbanExtractor.extractIban(buildCoordonneesFinancieres(null, null, buildInstitutionFinanciere(8440), null, "776115.290083-6"), mr));

			final Map<LogCategory, List<MigrationResultCollector.Message>> messages = mr.getMessages();
			Assert.assertEquals(EnumSet.of(LogCategory.COORDONNEES_FINANCIERES), messages.keySet());

			final List<String> textes = messages.get(LogCategory.COORDONNEES_FINANCIERES).stream().map(m -> m.text).collect(Collectors.toList());
			Assert.assertEquals(Arrays.asList("Le numéro de compte bancaire '776115.290083-6' comporte trop (13) de caractères significatifs, il sera tronqué aux 12 premiers.",
			                                  "IBAN extrait du numéro de compte '776115.290083-6' et du clearing '8440' : CH4108440776115290083."),
			                    textes);
		}
	}
}
