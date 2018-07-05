package ch.vd.unireg.webservices.v7;

import java.util.Random;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.WithoutSpringTest;
import ch.vd.unireg.xml.common.v2.Date;
import ch.vd.unireg.xml.party.relation.v4.Absorbed;
import ch.vd.unireg.xml.party.relation.v4.Absorbing;
import ch.vd.unireg.xml.party.relation.v4.Administration;
import ch.vd.unireg.xml.party.relation.v4.AfterSplit;
import ch.vd.unireg.xml.party.relation.v4.BeforeSplit;
import ch.vd.unireg.xml.party.relation.v4.Child;
import ch.vd.unireg.xml.party.relation.v4.EconomicActivity;
import ch.vd.unireg.xml.party.relation.v4.Guardian;
import ch.vd.unireg.xml.party.relation.v4.HouseholdMember;
import ch.vd.unireg.xml.party.relation.v4.InheritanceFrom;
import ch.vd.unireg.xml.party.relation.v4.InheritanceTo;
import ch.vd.unireg.xml.party.relation.v4.LegalAdviser;
import ch.vd.unireg.xml.party.relation.v4.ManagementCompany;
import ch.vd.unireg.xml.party.relation.v4.Parent;
import ch.vd.unireg.xml.party.relation.v4.PartnerRelationship;
import ch.vd.unireg.xml.party.relation.v4.RelationBetweenParties;
import ch.vd.unireg.xml.party.relation.v4.RelationBetweenPartiesType;
import ch.vd.unireg.xml.party.relation.v4.Replaced;
import ch.vd.unireg.xml.party.relation.v4.ReplacedBy;
import ch.vd.unireg.xml.party.relation.v4.Representative;
import ch.vd.unireg.xml.party.relation.v4.TaxLiabilitySubstitute;
import ch.vd.unireg.xml.party.relation.v4.TaxLiabilitySubstituteFor;
import ch.vd.unireg.xml.party.relation.v4.TaxableRevenue;
import ch.vd.unireg.xml.party.relation.v4.WealthTransferOriginator;
import ch.vd.unireg.xml.party.relation.v4.WealthTransferRecipient;
import ch.vd.unireg.xml.party.relation.v4.WelfareAdvocate;
import ch.vd.unireg.xml.party.relation.v4.WithholdingTaxContact;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class JsonRelationBetweenPartiesHelperTest extends WithoutSpringTest {

	private static final Random RND = new Random();

	private static Absorbed buildAbsorbed(Date dateFrom, Date dateTo, Date cancellationDate, int otherPartyId) {
		return new Absorbed(dateFrom, dateTo, cancellationDate, otherPartyId, null);
	}

	private static Absorbing buildAbsorbing(Date dateFrom, Date dateTo, Date cancellationDate, int otherPartyId) {
		return new Absorbing(dateFrom, dateTo, cancellationDate, otherPartyId, null);
	}

	private static AfterSplit buildAfterSplit(Date dateFrom, Date dateTo, Date cancellationDate, int otherPartyId) {
		return new AfterSplit(dateFrom, dateTo, cancellationDate, otherPartyId, null);
	}

	private static BeforeSplit buildBeforeSplit(Date dateFrom, Date dateTo, Date cancellationDate, int otherPartyId) {
		return new BeforeSplit(dateFrom, dateTo, cancellationDate, otherPartyId, null);
	}

	private static ReplacedBy buildReplacedBy(Date dateFrom, Date dateTo, Date cancellationDate, int otherPartyId) {
		return new ReplacedBy(dateFrom, dateTo, cancellationDate, otherPartyId, null);
	}

	private static Replaced buildReplaced(Date dateFrom, Date dateTo, Date cancellationDate, int otherPartyId) {
		return new Replaced(dateFrom, dateTo, cancellationDate, otherPartyId, null);
	}

	private static Child buildChild(Date dateFrom, Date dateTo, Date cancellationDate, int otherPartyId) {
		return new Child(dateFrom, dateTo, cancellationDate, otherPartyId, null);
	}

	private static EconomicActivity buildEconomicActivity(Date dateFrom, Date dateTo, Date cancellationDate, int otherPartyId, boolean principal) {
		return new EconomicActivity(dateFrom, dateTo, cancellationDate, otherPartyId, principal, null);
	}

	private static Guardian buildGuardian(Date dateFrom, Date dateTo, Date cancellationDate, int otherPartyId) {
		return new Guardian(dateFrom, dateTo, cancellationDate, otherPartyId, null);
	}

	private static HouseholdMember buildHouseholdMember(Date dateFrom, Date dateTo, Date cancellationDate, int otherPartyId) {
		return new HouseholdMember(dateFrom, dateTo, cancellationDate, otherPartyId, null);
	}

	private static LegalAdviser buildLegalAdviser(Date dateFrom, Date dateTo, Date cancellationDate, int otherPartyId) {
		return new LegalAdviser(dateFrom, dateTo, cancellationDate, otherPartyId, null);
	}

	private static Parent buildParent(Date dateFrom, Date dateTo, Date cancellationDate, int otherPartyId) {
		return new Parent(dateFrom, dateTo, cancellationDate, otherPartyId, null);
	}

	private static Representative buildRepresentative(Date dateFrom, Date dateTo, Date cancellationDate, int otherPartyId, Boolean extensionToForcedExecution) {
		return new Representative(dateFrom, dateTo, cancellationDate, otherPartyId, extensionToForcedExecution, 0, null);
	}

	private static TaxableRevenue buildTaxableRevenue(Date dateFrom, Date dateTo, Date cancellationDate, int otherPartyId, Date endDateOfLastTaxableItem) {
		return new TaxableRevenue(dateFrom, dateTo, cancellationDate, otherPartyId, endDateOfLastTaxableItem, 0, null);
	}

	private static WealthTransferOriginator buildWealthTransferOriginator(Date dateFrom, Date dateTo, Date cancellationDate, int otherPartyId) {
		return new WealthTransferOriginator(dateFrom, dateTo, cancellationDate, otherPartyId, null);
	}

	private static WealthTransferRecipient buildWealthTransferRecipient(Date dateFrom, Date dateTo, Date cancellationDate, int otherPartyId) {
		return new WealthTransferRecipient(dateFrom, dateTo, cancellationDate, otherPartyId, null);
	}

	private static WelfareAdvocate buildWelfareAdvocate(Date dateFrom, Date dateTo, Date cancellationDate, int otherPartyId) {
		return new WelfareAdvocate(dateFrom, dateTo, cancellationDate, otherPartyId, null);
	}

	private static WithholdingTaxContact buildWithholdingTaxContact(Date dateFrom, Date dateTo, Date cancellationDate, int otherPartyId) {
		return new WithholdingTaxContact(dateFrom, dateTo, cancellationDate, otherPartyId, null);
	}

	private static Administration buildAdministration(Date dateFrom, Date dateTo, Date cancellationDate, int otherPartyId, boolean chairman) {
		return new Administration(dateFrom, dateTo, cancellationDate, otherPartyId, chairman, null);
	}

	private static ManagementCompany buildManagementCompany(Date dateFrom, Date dateTo, Date cancellationDate, int otherPartyId) {
		return new ManagementCompany(dateFrom, dateTo, cancellationDate, otherPartyId, null);
	}

	private static TaxLiabilitySubstitute buildTaxLiabilitySubstitute(Date dateFrom, Date dateTo, Date cancellationDate, int otherPartyId) {
		return new TaxLiabilitySubstitute(dateFrom, dateTo, cancellationDate, otherPartyId, null);
	}

	private static TaxLiabilitySubstituteFor buildTaxLiabilitySubstituteFor(Date dateFrom, Date dateTo, Date cancellationDate, int otherPartyId) {
		return new TaxLiabilitySubstituteFor(dateFrom, dateTo, cancellationDate, otherPartyId, null);
	}

	private static InheritanceTo buildInheritanceTo(Date dateFrom, Date dateTo, Date cancellationDate, int otherPartyId, boolean principal) {
		return new InheritanceTo(dateFrom, dateTo, cancellationDate, otherPartyId, principal, null);
	}

	private static InheritanceFrom buildInheritanceFrom(Date dateFrom, Date dateTo, Date cancellationDate, int otherPartyId, boolean principal) {
		return new InheritanceFrom(dateFrom, dateTo, cancellationDate, otherPartyId, principal, null);
	}

	private static PartnerRelationship buildPartnerRelationship(Date dateFrom, Date dateTo, Date cancellationDate, int otherPartyId) {
		return new PartnerRelationship(dateFrom, dateTo, cancellationDate, otherPartyId, null);
	}

	private static RelationBetweenParties doTest(RelationBetweenParties src, RelationBetweenPartiesType expectedType) {
		assertNotNull(src);
		assertFalse(src instanceof JsonRelationBetweenPartiesHelper.JsonRelationBetweenParties);
		final Class<? extends RelationBetweenParties> clazzSrc = src.getClass();

		final RelationBetweenParties json = JsonRelationBetweenPartiesHelper.jsonEquivalentOf(src);
		assertNotNull(json);
		assertTrue(json instanceof JsonRelationBetweenPartiesHelper.JsonRelationBetweenParties);
		assertEquals(expectedType, ((JsonRelationBetweenPartiesHelper.JsonRelationBetweenParties) json).getType());

		final Class<? extends RelationBetweenParties> clazzJson = json.getClass();
		assertTrue(clazzSrc.isAssignableFrom(clazzJson));

		assertEquals(src.getDateTo(), json.getDateTo());
		assertEquals(src.getDateFrom(), json.getDateFrom());
		assertEquals(src.getCancellationDate(), json.getCancellationDate());
		assertEquals(src.getOtherPartyNumber(), json.getOtherPartyNumber());
		return json;
	}

	private static void doTest(Absorbed src, RelationBetweenPartiesType expectedType) {
		final RelationBetweenParties json = doTest((RelationBetweenParties) src, expectedType);
		assertTrue(json instanceof Absorbed);
	}

	private static void doTest(Absorbing src, RelationBetweenPartiesType expectedType) {
		final RelationBetweenParties json = doTest((RelationBetweenParties) src, expectedType);
		assertTrue(json instanceof Absorbing);
	}

	private static void doTest(AfterSplit src, RelationBetweenPartiesType expectedType) {
		final RelationBetweenParties json = doTest((RelationBetweenParties) src, expectedType);
		assertTrue(json instanceof AfterSplit);
	}

	private static void doTest(BeforeSplit src, RelationBetweenPartiesType expectedType) {
		final RelationBetweenParties json = doTest((RelationBetweenParties) src, expectedType);
		assertTrue(json instanceof BeforeSplit);
	}

	private static void doTest(ReplacedBy src, RelationBetweenPartiesType expectedType) {
		final RelationBetweenParties json = doTest((RelationBetweenParties) src, expectedType);
		assertTrue(json instanceof ReplacedBy);
	}

	private static void doTest(Replaced src, RelationBetweenPartiesType expectedType) {
		final RelationBetweenParties json = doTest((RelationBetweenParties) src, expectedType);
		assertTrue(json instanceof Replaced);
	}

	private static void doTest(Child src, RelationBetweenPartiesType expectedType) {
		final RelationBetweenParties json = doTest((RelationBetweenParties) src, expectedType);
		assertTrue(json instanceof Child);
	}

	private static void doTest(EconomicActivity src, RelationBetweenPartiesType expectedType) {
		final RelationBetweenParties json = doTest((RelationBetweenParties) src, expectedType);
		assertTrue(json instanceof EconomicActivity);
		assertEquals(src.isPrincipal(), ((EconomicActivity) json).isPrincipal());
	}

	private static void doTest(Guardian src, RelationBetweenPartiesType expectedType) {
		final RelationBetweenParties json = doTest((RelationBetweenParties) src, expectedType);
		assertTrue(json instanceof Guardian);
	}

	private static void doTest(HouseholdMember src, RelationBetweenPartiesType expectedType) {
		final RelationBetweenParties json = doTest((RelationBetweenParties) src, expectedType);
		assertTrue(json instanceof HouseholdMember);
	}

	private static void doTest(LegalAdviser src, RelationBetweenPartiesType expectedType) {
		final RelationBetweenParties json = doTest((RelationBetweenParties) src, expectedType);
		assertTrue(json instanceof LegalAdviser);
	}

	private static void doTest(Parent src, RelationBetweenPartiesType expectedType) {
		final RelationBetweenParties json = doTest((RelationBetweenParties) src, expectedType);
		assertTrue(json instanceof Parent);
	}

	private static void doTest(Representative src, RelationBetweenPartiesType expectedType) {
		final RelationBetweenParties json = doTest((RelationBetweenParties) src, expectedType);
		assertTrue(json instanceof Representative);
		assertEquals(src.isExtensionToForcedExecution(), ((Representative) json).isExtensionToForcedExecution());
	}

	private static void doTest(TaxableRevenue src, RelationBetweenPartiesType expectedType) {
		final RelationBetweenParties json = doTest((RelationBetweenParties) src, expectedType);
		assertTrue(json instanceof TaxableRevenue);
		assertEquals(src.getEndDateOfLastTaxableItem(), ((TaxableRevenue) json).getEndDateOfLastTaxableItem());
	}

	private static void doTest(WealthTransferOriginator src, RelationBetweenPartiesType expectedType) {
		final RelationBetweenParties json = doTest((RelationBetweenParties) src, expectedType);
		assertTrue(json instanceof WealthTransferOriginator);
	}

	private static void doTest(WealthTransferRecipient src, RelationBetweenPartiesType expectedType) {
		final RelationBetweenParties json = doTest((RelationBetweenParties) src, expectedType);
		assertTrue(json instanceof WealthTransferRecipient);
	}

	private static void doTest(WelfareAdvocate src, RelationBetweenPartiesType expectedType) {
		final RelationBetweenParties json = doTest((RelationBetweenParties) src, expectedType);
		assertTrue(json instanceof WelfareAdvocate);
	}

	private static void doTest(WithholdingTaxContact src, RelationBetweenPartiesType expectedType) {
		final RelationBetweenParties json = doTest((RelationBetweenParties) src, expectedType);
		assertTrue(json instanceof WithholdingTaxContact);
	}

	private static void doTest(Administration src, RelationBetweenPartiesType expectedType) {
		final RelationBetweenParties json = doTest((RelationBetweenParties) src, expectedType);
		assertTrue(json instanceof Administration);
	}

	private static void doTest(ManagementCompany src, RelationBetweenPartiesType expectedType) {
		final RelationBetweenParties json = doTest((RelationBetweenParties) src, expectedType);
		assertTrue(json instanceof ManagementCompany);
	}

	private static void doTest(TaxLiabilitySubstitute src, RelationBetweenPartiesType expectedType) {
		final RelationBetweenParties json = doTest((RelationBetweenParties) src, expectedType);
		assertTrue(json instanceof TaxLiabilitySubstitute);
	}

	private static void doTest(TaxLiabilitySubstituteFor src, RelationBetweenPartiesType expectedType) {
		final RelationBetweenParties json = doTest((RelationBetweenParties) src, expectedType);
		assertTrue(json instanceof TaxLiabilitySubstituteFor);
	}

	private static Date generateRandomDate() {
		final RegDate date = RegDate.get().addDays(RND.nextInt(365));
		return DataHelper.coreToWeb(date);
	}

	private static int generateRandomOtherPartyId() {
		return RND.nextInt(Integer.MAX_VALUE);
	}

	private static Boolean generateRandomNullableBoolean() {
		final Boolean[] values = {null, Boolean.TRUE, Boolean.FALSE};
		return values[RND.nextInt(values.length)];
	}

	private static boolean generateRandomBoolean() {
		return RND.nextInt(Integer.MAX_VALUE) % 2 == 0;
	}

	@Test
	public void testMutationWithNulls() throws Exception {
		assertNull(JsonRelationBetweenPartiesHelper.jsonEquivalentOf(null));
		doTest(buildAbsorbed(null, null, null, generateRandomOtherPartyId()), RelationBetweenPartiesType.ABSORBED);
		doTest(buildAbsorbing(null, null, null, generateRandomOtherPartyId()), RelationBetweenPartiesType.ABSORBING);
		doTest(buildAfterSplit(null, null, null, generateRandomOtherPartyId()), RelationBetweenPartiesType.AFTER_SPLIT);
		doTest(buildBeforeSplit(null, null, null, generateRandomOtherPartyId()), RelationBetweenPartiesType.BEFORE_SPLIT);
		doTest(buildReplaced(null, null, null, generateRandomOtherPartyId()), RelationBetweenPartiesType.REPLACED);
		doTest(buildReplacedBy(null, null, null, generateRandomOtherPartyId()), RelationBetweenPartiesType.REPLACED_BY);
		doTest(buildChild(null, null, null, generateRandomOtherPartyId()), RelationBetweenPartiesType.CHILD);
		doTest(buildEconomicActivity(null, null, null, generateRandomOtherPartyId(), generateRandomBoolean()), RelationBetweenPartiesType.ECONOMIC_ACTIVITY);
		doTest(buildGuardian(null, null, null, generateRandomOtherPartyId()), RelationBetweenPartiesType.GUARDIAN);
		doTest(buildHouseholdMember(null, null, null, generateRandomOtherPartyId()), RelationBetweenPartiesType.HOUSEHOLD_MEMBER);
		doTest(buildLegalAdviser(null, null, null, generateRandomOtherPartyId()), RelationBetweenPartiesType.LEGAL_ADVISER);
		doTest(buildParent(null, null, null, generateRandomOtherPartyId()), RelationBetweenPartiesType.PARENT);
		doTest(buildRepresentative(null, null, null, generateRandomOtherPartyId(), generateRandomNullableBoolean()), RelationBetweenPartiesType.REPRESENTATIVE);
		doTest(buildTaxableRevenue(null, null, null, generateRandomOtherPartyId(), null), RelationBetweenPartiesType.TAXABLE_REVENUE);
		doTest(buildWealthTransferOriginator(null, null, null, generateRandomOtherPartyId()), RelationBetweenPartiesType.WEALTH_TRANSFER_ORIGINATOR);
		doTest(buildWealthTransferRecipient(null, null, null, generateRandomOtherPartyId()), RelationBetweenPartiesType.WEALTH_TRANSFER_RECIPIENT);
		doTest(buildWelfareAdvocate(null, null, null, generateRandomOtherPartyId()), RelationBetweenPartiesType.WELFARE_ADVOCATE);
		doTest(buildWithholdingTaxContact(null, null, null, generateRandomOtherPartyId()), RelationBetweenPartiesType.WITHHOLDING_TAX_CONTACT);
		doTest(buildAdministration(null, null, null, generateRandomOtherPartyId(), generateRandomBoolean()), RelationBetweenPartiesType.ADMINISTRATION);
		doTest(buildManagementCompany(null, null, null, generateRandomOtherPartyId()), RelationBetweenPartiesType.MANAGEMENT_COMPANY);
		doTest(buildTaxLiabilitySubstitute(null, null, null, generateRandomOtherPartyId()), RelationBetweenPartiesType.TAX_LIABILITY_SUBSTITUTE);
		doTest(buildTaxLiabilitySubstituteFor(null, null, null, generateRandomOtherPartyId()), RelationBetweenPartiesType.TAX_LIABILITY_SUBSTITUTE_FOR);
		doTest(buildInheritanceTo(null, null, null, generateRandomOtherPartyId(), generateRandomBoolean()), RelationBetweenPartiesType.INHERITANCE_TO);
		doTest(buildInheritanceFrom(null, null, null, generateRandomOtherPartyId(), generateRandomBoolean()), RelationBetweenPartiesType.INHERITANCE_TO);
		doTest(buildPartnerRelationship(null, null, null, generateRandomOtherPartyId()), RelationBetweenPartiesType.PARTNER_RELATIONSHIP);
	}

	@Test
	public void testMutationNonNulls() throws Exception {
		doTest(buildAbsorbed(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()), RelationBetweenPartiesType.ABSORBED);
		doTest(buildAbsorbing(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()), RelationBetweenPartiesType.ABSORBING);
		doTest(buildAfterSplit(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()), RelationBetweenPartiesType.AFTER_SPLIT);
		doTest(buildBeforeSplit(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()), RelationBetweenPartiesType.BEFORE_SPLIT);
		doTest(buildReplaced(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()), RelationBetweenPartiesType.REPLACED);
		doTest(buildReplacedBy(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()), RelationBetweenPartiesType.REPLACED_BY);
		doTest(buildChild(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()), RelationBetweenPartiesType.CHILD);
		doTest(buildEconomicActivity(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId(), generateRandomBoolean()), RelationBetweenPartiesType.ECONOMIC_ACTIVITY);
		doTest(buildGuardian(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()), RelationBetweenPartiesType.GUARDIAN);
		doTest(buildHouseholdMember(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()), RelationBetweenPartiesType.HOUSEHOLD_MEMBER);
		doTest(buildLegalAdviser(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()), RelationBetweenPartiesType.LEGAL_ADVISER);
		doTest(buildParent(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()), RelationBetweenPartiesType.PARENT);
		doTest(buildRepresentative(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId(), generateRandomNullableBoolean()), RelationBetweenPartiesType.REPRESENTATIVE);
		doTest(buildTaxableRevenue(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId(), generateRandomDate()), RelationBetweenPartiesType.TAXABLE_REVENUE);
		doTest(buildWealthTransferOriginator(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()), RelationBetweenPartiesType.WEALTH_TRANSFER_ORIGINATOR);
		doTest(buildWealthTransferRecipient(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()), RelationBetweenPartiesType.WEALTH_TRANSFER_RECIPIENT);
		doTest(buildWelfareAdvocate(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()), RelationBetweenPartiesType.WELFARE_ADVOCATE);
		doTest(buildWithholdingTaxContact(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()), RelationBetweenPartiesType.WITHHOLDING_TAX_CONTACT);
		doTest(buildAdministration(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId(), generateRandomBoolean()), RelationBetweenPartiesType.ADMINISTRATION);
		doTest(buildManagementCompany(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()), RelationBetweenPartiesType.MANAGEMENT_COMPANY);
		doTest(buildTaxLiabilitySubstitute(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()), RelationBetweenPartiesType.TAX_LIABILITY_SUBSTITUTE);
		doTest(buildTaxLiabilitySubstituteFor(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()), RelationBetweenPartiesType.TAX_LIABILITY_SUBSTITUTE_FOR);
		doTest(buildInheritanceTo(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId(), generateRandomBoolean()), RelationBetweenPartiesType.INHERITANCE_TO);
		doTest(buildInheritanceFrom(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId(), generateRandomBoolean()), RelationBetweenPartiesType.INHERITANCE_TO);
		doTest(buildPartnerRelationship(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()), RelationBetweenPartiesType.PARTNER_RELATIONSHIP);
	}

	@Test
	public void testAlreadyJson() throws Exception {
		doTestAlreadyJson(buildAbsorbed(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()));
		doTestAlreadyJson(buildAbsorbing(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()));
		doTestAlreadyJson(buildAfterSplit(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()));
		doTestAlreadyJson(buildBeforeSplit(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()));
		doTestAlreadyJson(buildReplaced(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()));
		doTestAlreadyJson(buildReplacedBy(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()));
		doTestAlreadyJson(buildChild(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()));
		doTestAlreadyJson(buildEconomicActivity(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId(), generateRandomBoolean()));
		doTestAlreadyJson(buildGuardian(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()));
		doTestAlreadyJson(buildHouseholdMember(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()));
		doTestAlreadyJson(buildLegalAdviser(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()));
		doTestAlreadyJson(buildParent(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()));
		doTestAlreadyJson(buildRepresentative(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId(), generateRandomNullableBoolean()));
		doTestAlreadyJson(buildTaxableRevenue(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId(), generateRandomDate()));
		doTestAlreadyJson(buildWealthTransferOriginator(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()));
		doTestAlreadyJson(buildWealthTransferRecipient(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()));
		doTestAlreadyJson(buildWelfareAdvocate(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()));
		doTestAlreadyJson(buildWithholdingTaxContact(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()));
		doTestAlreadyJson(buildAdministration(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId(), generateRandomBoolean()));
		doTestAlreadyJson(buildManagementCompany(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()));
		doTestAlreadyJson(buildTaxLiabilitySubstitute(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()));
		doTestAlreadyJson(buildTaxLiabilitySubstituteFor(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()));
		doTestAlreadyJson(buildInheritanceTo(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId(), generateRandomBoolean()));
		doTestAlreadyJson(buildInheritanceFrom(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId(), generateRandomBoolean()));
		doTestAlreadyJson(buildPartnerRelationship(generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomOtherPartyId()));
	}

	private static void doTestAlreadyJson(RelationBetweenParties nonJson) {
		final RelationBetweenParties json = JsonRelationBetweenPartiesHelper.jsonEquivalentOf(nonJson);
		assertNotSame(nonJson, json);
		assertSame(json, JsonRelationBetweenPartiesHelper.jsonEquivalentOf(json));
	}
}
