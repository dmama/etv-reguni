package ch.vd.unireg.webservices.v7;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.WithoutSpringTest;
import ch.vd.unireg.xml.common.v2.Date;
import ch.vd.unireg.xml.party.landregistry.v1.AcquisitionReason;
import ch.vd.unireg.xml.party.landregistry.v1.CaseIdentifier;
import ch.vd.unireg.xml.party.landregistry.v1.EasementEncumbrance;
import ch.vd.unireg.xml.party.landregistry.v1.EasementMembership;
import ch.vd.unireg.xml.party.landregistry.v1.EasementRight;
import ch.vd.unireg.xml.party.landregistry.v1.HousingRight;
import ch.vd.unireg.xml.party.landregistry.v1.LandOwnershipRight;
import ch.vd.unireg.xml.party.landregistry.v1.LandRight;
import ch.vd.unireg.xml.party.landregistry.v1.LandRightType;
import ch.vd.unireg.xml.party.landregistry.v1.NaturalPersonIdentity;
import ch.vd.unireg.xml.party.landregistry.v1.OwnershipType;
import ch.vd.unireg.xml.party.landregistry.v1.RightHolder;
import ch.vd.unireg.xml.party.landregistry.v1.Share;
import ch.vd.unireg.xml.party.landregistry.v1.UsufructRight;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class JsonLandRightHelperTest extends WithoutSpringTest {

	private static final Random RND = new Random();

	private static LandOwnershipRight buildLandOwnershipRight(long id, Date dateFrom, Date dateTo, String startReason, String endReason, CaseIdentifier caseIdentifier, RightHolder rightHolder, long immovablePropertyId, Share share,
	                                                          OwnershipType type, Long communityId, List<AcquisitionReason> acquisitionReasons) {
		return new LandOwnershipRight(dateFrom, dateTo, startReason, endReason, caseIdentifier, rightHolder, immovablePropertyId, share, type, communityId, acquisitionReasons, 0, id, null, 0, null);
	}

	private static UsufructRight buildUsufructRight(long id, Date dateFrom, Date dateTo, String startReason, String endReason, CaseIdentifier caseIdentifier, List<RightHolder> rightHolders, List<Long> immovablePropertyIds,
	                                                List<EasementMembership> memberships, List<EasementEncumbrance> encumbrances) {
		return new UsufructRight(dateFrom, dateTo, startReason, endReason, caseIdentifier, rightHolders.get(0), immovablePropertyIds.get(0), rightHolders, immovablePropertyIds, 0, id, null, 0, memberships, encumbrances, 0, null);
	}

	private static HousingRight buildHousingRight(long id, Date dateFrom, Date dateTo, String startReason, String endReason, CaseIdentifier caseIdentifier, List<RightHolder> rightHolders, List<Long> immovablePropertyIds,
	                                              List<EasementMembership> memberships, List<EasementEncumbrance> encumbrances) {
		return new HousingRight(dateFrom, dateTo, startReason, endReason, caseIdentifier, rightHolders.get(0), immovablePropertyIds.get(0), rightHolders, immovablePropertyIds, 0, id, null, 0, memberships, encumbrances, 0, null);
	}

	private static LandRight doTest(LandRight src, LandRightType expectedType) {
		assertNotNull(src);
		assertFalse(src instanceof JsonLandRightHelper.JsonLandRight);
		final Class<? extends LandRight> clazzSrc = src.getClass();

		final LandRight json = JsonLandRightHelper.jsonEquivalentOf(src);
		assertNotNull(json);
		assertTrue(json instanceof JsonLandRightHelper.JsonLandRight);
		assertEquals(expectedType, ((JsonLandRightHelper.JsonLandRight) json).getJsonType());

		final Class<? extends LandRight> clazzJson = json.getClass();
		assertTrue(clazzSrc.isAssignableFrom(clazzJson));

		assertEquals(src.getDateTo(), json.getDateTo());
		assertEquals(src.getDateFrom(), json.getDateFrom());
		assertEquals(src.getStartReason(), json.getStartReason());
		assertEquals(src.getEndReason(), json.getEndReason());
		assertEquals(src.getEndReason(), json.getEndReason());
		assertEquals(src.getCaseIdentifier(), json.getCaseIdentifier());
		assertEquals(src.getRightHolder(), json.getRightHolder());
		assertEquals(src.getImmovablePropertyId(), json.getImmovablePropertyId());
		return json;
	}

	private static void doTest(LandOwnershipRight src, LandRightType expectedType) {
		final LandRight json = doTest((LandRight) src, expectedType);
		assertTrue(json instanceof LandOwnershipRight);

		final LandOwnershipRight jsonOwnership = (LandOwnershipRight) json;
		assertEquals(src.getShare(), jsonOwnership.getShare());
		assertEquals(src.getType(), jsonOwnership.getType());
		assertEquals(src.getCommunityId(), jsonOwnership.getCommunityId());
		assertEquals(src.getAcquisitionReasons(), jsonOwnership.getAcquisitionReasons());
	}

	private static EasementRight doTest(EasementRight src, LandRightType expectedType) {
		final LandRight json = doTest((LandRight) src, expectedType);
		assertTrue(json instanceof EasementRight);

		final EasementRight jsonEasement = (EasementRight) json;
		assertEquals(src.getRightHolders(), jsonEasement.getRightHolders());
		assertEquals(src.getImmovablePropertyIds(), jsonEasement.getImmovablePropertyIds());
		return jsonEasement;
	}

	private static void doTest(UsufructRight src, LandRightType expectedType) {
		final EasementRight json = doTest((EasementRight) src, expectedType);
		assertTrue(json instanceof UsufructRight);
	}

	private static void doTest(HousingRight src, LandRightType expectedType) {
		final EasementRight json = doTest((EasementRight) src, expectedType);
		assertTrue(json instanceof HousingRight);
	}

	private static Date generateRandomDate() {
		final RegDate date = RegDate.get().addDays(RND.nextInt(365));
		return DataHelper.coreToWeb(date);
	}

	private static long generateRandomLongId() {
		return Math.abs(RND.nextLong());
	}

	private static Long generateOptionalRandomLongId() {
		return generateRandomBoolean() ? generateRandomLongId() : null;
	}

	private static List<Long> generateRandomImmovablePropertyIds() {
		final int size = RND.nextInt(10) + 1;        // de 1 à 10
		final List<Long> list = new ArrayList<>(size);
		for (int i = 0 ; i < size ; ++ i) {
			list.add(generateRandomLongId());
		}
		return list;
	}

	private static boolean generateRandomBoolean() {
		return RND.nextInt(Integer.MAX_VALUE) % 2 == 0;
	}

	private static RightHolder generateRandomRightHolder() {
		final boolean hasTaxpayer = generateRandomBoolean();
		if (hasTaxpayer) {
			return new RightHolder(RND.nextInt(Integer.MAX_VALUE), null, null, null, 0, null);
		}
		else {
			final NaturalPersonIdentity identity = new NaturalPersonIdentity(RND.nextInt(Integer.MAX_VALUE), "Turlututu", "Tarlatata", null, 0, null);
			return new RightHolder(null, null, null, identity, 0, null);
		}
	}

	private static List<RightHolder> generateRandomRightHolders() {
		final int size = RND.nextInt(10) + 1;        // de 1 à 10
		final List<RightHolder> list = new ArrayList<>(size);
		for (int i = 0 ; i < size ; ++ i) {
			list.add(generateRandomRightHolder());
		}
		return list;
	}


	private static Share generateRandomShare() {
		final int denominator = RND.nextInt(10000) + 1;      // de 1 à 10'000
		final int numerator = RND.nextInt(denominator) + 1;         // de 1 à la valeur du dénominateur
		return new Share(numerator, denominator);
	}

	private static OwnershipType generateRandomOwnershipType() {
		final OwnershipType[] all = OwnershipType.values();
		return all[RND.nextInt(all.length)];
	}

	private static String generateRandomString() {
		final int length = RND.nextInt(50) + 1;     // de 1 à 50
		final char[] charArray = new char[length];
		for (int i = 0 ; i < length ; ++i) {
			charArray[i] = (char) ('a' + RND.nextInt(26));      // de 'a' à 'z'
		}
		return String.valueOf(charArray);
	}

	private static CaseIdentifier generateRandomCaseIdentifier() {
		return new CaseIdentifier(1 + RND.nextInt(1000),
		                                                     1950 + RND.nextInt(60),
		                                                     1 + RND.nextInt(250),
		                                                     1 + RND.nextInt(10),
		                                                     0,
		                                                     generateRandomString(),
		                                                     0,
		                                                     null);
	}

	private static List<AcquisitionReason> generateRandomAcquisitionReasons() {
		final int size = RND.nextInt(4) + 1;        // de 1 à 4
		final List<AcquisitionReason> list = new ArrayList<>(size);
		for (int i = 0 ; i < size ; ++ i) {
			list.add(new AcquisitionReason(generateRandomDate(), generateRandomString(), generateRandomCaseIdentifier(), 0, null));
		}
		return list;
	}

	private static List<EasementMembership> generateRamdomMemberships() {
		final int size = RND.nextInt(4) + 1;        // de 1 à 4
		final List<EasementMembership> list = new ArrayList<>(size);
		for (int i = 0 ; i < size ; ++ i) {
			list.add(new EasementMembership(generateRandomDate(),generateRandomDate(),generateRandomDate(), generateRandomRightHolder(), null));
		}
		return list;
	}

	private static List<EasementEncumbrance> generateRandomEncumbrances() {
		final int size = RND.nextInt(4) + 1;        // de 1 à 4
		final List<EasementEncumbrance> list = new ArrayList<>(size);
		for (int i = 0 ; i < size ; ++ i) {
			list.add(new EasementEncumbrance(generateRandomDate(),generateRandomDate(),generateRandomDate(), generateRandomLongId(), null));
		}
		return list;
	}

	@Test
	public void testMutationWithNulls() throws Exception {
		assertNull(JsonLandRightHelper.jsonEquivalentOf(null));
		doTest(buildLandOwnershipRight(generateRandomLongId(), null, null, null, null, null, generateRandomRightHolder(), generateRandomLongId(), generateRandomShare(), generateRandomOwnershipType(), null, null), LandRightType.OWNERSHIP);
		doTest(buildUsufructRight(generateRandomLongId(), null, null, null, null, null, generateRandomRightHolders(), generateRandomImmovablePropertyIds(), generateRamdomMemberships(), generateRandomEncumbrances()), LandRightType.USUFRUCT);
		doTest(buildHousingRight(generateRandomLongId(), null, null, null, null, null, generateRandomRightHolders(), generateRandomImmovablePropertyIds(), generateRamdomMemberships(), generateRandomEncumbrances()), LandRightType.HOUSING);
	}

	@Test
	public void testMutationNonNulls() throws Exception {
		doTest(buildLandOwnershipRight(generateRandomLongId(), generateRandomDate(), generateRandomDate(), generateRandomString(), generateRandomString(), generateRandomCaseIdentifier(), generateRandomRightHolder(), generateRandomLongId(), generateRandomShare(), generateRandomOwnershipType(), generateOptionalRandomLongId(), generateRandomAcquisitionReasons()), LandRightType.OWNERSHIP);
		doTest(buildUsufructRight(generateRandomLongId(), generateRandomDate(), generateRandomDate(), generateRandomString(), generateRandomString(), generateRandomCaseIdentifier(), generateRandomRightHolders(), generateRandomImmovablePropertyIds(),
		                          generateRamdomMemberships(), generateRandomEncumbrances()), LandRightType.USUFRUCT);
		doTest(buildHousingRight(generateRandomLongId(), generateRandomDate(), generateRandomDate(), generateRandomString(), generateRandomString(), generateRandomCaseIdentifier(), generateRandomRightHolders(), generateRandomImmovablePropertyIds(),
		                         generateRamdomMemberships(), generateRandomEncumbrances()), LandRightType.HOUSING);
	}

	@Test
	public void testAlreadyJson() throws Exception {
		doTestAlreadyJson(buildLandOwnershipRight(generateRandomLongId(), generateRandomDate(), generateRandomDate(), generateRandomString(), generateRandomString(), generateRandomCaseIdentifier(), generateRandomRightHolder(), generateRandomLongId(), generateRandomShare(), generateRandomOwnershipType(), generateOptionalRandomLongId(), generateRandomAcquisitionReasons()));
		doTestAlreadyJson(buildUsufructRight(generateRandomLongId(), generateRandomDate(), generateRandomDate(), generateRandomString(), generateRandomString(), generateRandomCaseIdentifier(), generateRandomRightHolders(), generateRandomImmovablePropertyIds(),
		                                     generateRamdomMemberships(), generateRandomEncumbrances()));
		doTestAlreadyJson(buildHousingRight(generateRandomLongId(), generateRandomDate(), generateRandomDate(), generateRandomString(), generateRandomString(), generateRandomCaseIdentifier(), generateRandomRightHolders(), generateRandomImmovablePropertyIds(),
		                                    generateRamdomMemberships(), generateRandomEncumbrances()));
	}

	private static void doTestAlreadyJson(LandRight nonJson) {
		final LandRight json = JsonLandRightHelper.jsonEquivalentOf(nonJson);
		assertNotSame(nonJson, json);
		assertSame(json, JsonLandRightHelper.jsonEquivalentOf(json));
	}
}
