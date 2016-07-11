package ch.vd.uniregctb.webservices.v7;

import java.util.Random;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.common.v2.Date;
import ch.vd.unireg.xml.party.address.v3.PostAddress;
import ch.vd.unireg.xml.party.agent.v1.Agent;
import ch.vd.unireg.xml.party.agent.v1.AgentType;
import ch.vd.unireg.xml.party.agent.v1.GeneralAgent;
import ch.vd.unireg.xml.party.agent.v1.SpecialAgent;
import ch.vd.uniregctb.common.WithoutSpringTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class JsonAgentHelperTest extends WithoutSpringTest {

	private static final Random RND = new Random();

	private static GeneralAgent buildGeneralAgent(Date dateFrom, Date dateTo, PostAddress postAddress, boolean withCopy) {
		return new GeneralAgent(dateFrom, dateTo, postAddress, withCopy, null);
	}

	private static SpecialAgent buildSpecialAgent(Date dateFrom, Date dateTo, PostAddress postAddress, boolean withCopy, String taxKind) {
		return new SpecialAgent(dateFrom, dateTo, postAddress, withCopy, taxKind, null);
	}

	private static Agent doTest(Agent src, AgentType expectedType) {
		assertNotNull(src);
		assertFalse(src instanceof JsonAgentHelper.JsonAgent);
		final Class<? extends Agent> clazzSrc = src.getClass();

		final Agent json = JsonAgentHelper.jsonEquivalentOf(src);
		assertNotNull(json);
		assertTrue(json instanceof JsonAgentHelper.JsonAgent);
		assertEquals(expectedType, ((JsonAgentHelper.JsonAgent) json).getType());

		final Class<? extends Agent> clazzJson = json.getClass();
		assertTrue(clazzSrc.isAssignableFrom(clazzJson));

		assertEquals(src.getDateTo(), json.getDateTo());
		assertEquals(src.getDateFrom(), json.getDateFrom());
		assertEquals(src.isWithCopy(), json.isWithCopy());
		assertSame(src.getPostAddress(), json.getPostAddress());
		return json;
	}

	private static void doTest(GeneralAgent src, AgentType expectedType) {
		final Agent json = doTest((Agent) src, expectedType);
		assertTrue(json instanceof GeneralAgent);
	}

	private static void doTest(SpecialAgent src, AgentType expectedType) {
		final Agent json = doTest((Agent) src, expectedType);
		assertTrue(json instanceof SpecialAgent);
		assertEquals(src.getTaxKind(), ((SpecialAgent) json).getTaxKind());
	}

	private static Date generateRandomDate() {
		final RegDate date = RegDate.get().addDays(RND.nextInt(365));
		return DataHelper.coreToWeb(date);
	}

	private static boolean generateRandomBoolean() {
		return RND.nextBoolean();
	}

	private static PostAddress generateRandomPostAddress() {
		// puisque le test est un "isSame", ça jouera
		return new PostAddress();
	}

	private static final char[] ALLOWED_TAX_KIND_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_".toCharArray();

	private static String generateRandomTaxKind() {
		final int length = 5 + RND.nextInt(5);      // entre 5 et 9 caractères
		final char[] chars = new char[length];
		for (int i = 0 ; i < length ; ++ i) {
			chars[i] = ALLOWED_TAX_KIND_CHARS[RND.nextInt(ALLOWED_TAX_KIND_CHARS.length)];
		}
		return new String(chars);
	}

	@Test
	public void testMutationWithNulls() throws Exception {
		assertNull(JsonRelationBetweenPartiesHelper.jsonEquivalentOf(null));
		doTest(buildGeneralAgent(null, null, null, generateRandomBoolean()), AgentType.GENERAL);
		doTest(buildSpecialAgent(null, null, null, generateRandomBoolean(), null), AgentType.SPECIAL);
	}

	@Test
	public void testMutationNonNulls() throws Exception {
		doTest(buildGeneralAgent(generateRandomDate(), generateRandomDate(), generateRandomPostAddress(), generateRandomBoolean()), AgentType.GENERAL);
		doTest(buildSpecialAgent(generateRandomDate(), generateRandomDate(), generateRandomPostAddress(), generateRandomBoolean(), generateRandomTaxKind()), AgentType.SPECIAL);
	}

	@Test
	public void testAlreadyJson() throws Exception {
		doTestAlreadyJson(buildGeneralAgent(generateRandomDate(), generateRandomDate(), generateRandomPostAddress(), generateRandomBoolean()));
		doTestAlreadyJson(buildSpecialAgent(generateRandomDate(), generateRandomDate(), generateRandomPostAddress(), generateRandomBoolean(), generateRandomTaxKind()));
	}

	private static void doTestAlreadyJson(Agent nonJson) {
		final Agent json = JsonAgentHelper.jsonEquivalentOf(nonJson);
		assertNotSame(nonJson, json);
		assertSame(json, JsonAgentHelper.jsonEquivalentOf(json));
	}
}
