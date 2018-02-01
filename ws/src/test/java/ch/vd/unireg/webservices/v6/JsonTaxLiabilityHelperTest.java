package ch.vd.unireg.webservices.v6;

import java.util.Random;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.common.v2.Date;
import ch.vd.unireg.xml.party.taxresidence.v3.ExpenditureBased;
import ch.vd.unireg.xml.party.taxresidence.v3.ForeignCountry;
import ch.vd.unireg.xml.party.taxresidence.v3.Indigent;
import ch.vd.unireg.xml.party.taxresidence.v3.IndividualTaxLiabilityType;
import ch.vd.unireg.xml.party.taxresidence.v3.LiabilityChangeReason;
import ch.vd.unireg.xml.party.taxresidence.v3.MixedWithholding137Par1;
import ch.vd.unireg.xml.party.taxresidence.v3.MixedWithholding137Par2;
import ch.vd.unireg.xml.party.taxresidence.v3.OrdinaryResident;
import ch.vd.unireg.xml.party.taxresidence.v3.OtherCanton;
import ch.vd.unireg.xml.party.taxresidence.v3.PureWithholding;
import ch.vd.unireg.xml.party.taxresidence.v3.SwissDiplomat;
import ch.vd.unireg.xml.party.taxresidence.v3.TaxLiability;
import ch.vd.unireg.xml.party.taxresidence.v3.TaxationAuthorityType;
import ch.vd.unireg.xml.party.taxresidence.v3.Withholding;
import ch.vd.unireg.common.WithoutSpringTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class JsonTaxLiabilityHelperTest extends WithoutSpringTest {

	private static OrdinaryResident buildOrdinaryResident(LiabilityChangeReason startReason, LiabilityChangeReason endReason, Date dateTo, Date dateFrom) {
		return new OrdinaryResident(startReason, endReason, dateTo, dateFrom, null);
	}

	private static PureWithholding buildPureWithholding(LiabilityChangeReason startReason, LiabilityChangeReason endReason, Date dateTo, Date dateFrom, TaxationAuthorityType tat) {
		return new PureWithholding(startReason, endReason, dateTo, dateFrom, tat, null);
	}

	private static MixedWithholding137Par1 buildMixedWithholding137Par1(LiabilityChangeReason startReason, LiabilityChangeReason endReason, Date dateTo, Date dateFrom, TaxationAuthorityType tat) {
		return new MixedWithholding137Par1(startReason, endReason, dateTo, dateFrom, tat, null);
	}

	private static MixedWithholding137Par2 buildMixedWithholding137Par2(LiabilityChangeReason startReason, LiabilityChangeReason endReason, Date dateTo, Date dateFrom, TaxationAuthorityType tat) {
		return new MixedWithholding137Par2(startReason, endReason, dateTo, dateFrom, tat, null);
	}

	private static ExpenditureBased buildExpenditureBased(LiabilityChangeReason startReason, LiabilityChangeReason endReason, Date dateTo, Date dateFrom) {
		return new ExpenditureBased(startReason, endReason, dateTo, dateFrom, null);
	}

	private static Indigent buildIndigent(LiabilityChangeReason startReason, LiabilityChangeReason endReason, Date dateTo, Date dateFrom) {
		return new Indigent(startReason, endReason, dateTo, dateFrom, null);
	}

	private static OtherCanton buildOtherCanton(LiabilityChangeReason startReason, LiabilityChangeReason endReason, Date dateTo, Date dateFrom) {
		return new OtherCanton(startReason, endReason, dateTo, dateFrom, null);
	}

	private static ForeignCountry buildForeignCountry(LiabilityChangeReason startReason, LiabilityChangeReason endReason, Date dateTo, Date dateFrom) {
		return new ForeignCountry(startReason, endReason, dateTo, dateFrom, null);
	}

	private static SwissDiplomat buildSwissDiplomat(LiabilityChangeReason startReason, LiabilityChangeReason endReason, Date dateTo, Date dateFrom) {
		return new SwissDiplomat(startReason, endReason, dateTo, dateFrom, null);
	}

	private static TaxLiability doTest(TaxLiability src, IndividualTaxLiabilityType expectedType) {
		assertNotNull(src);
		assertFalse(src instanceof JsonTaxLiabilityHelper.JsonTaxLiability);
		final Class<? extends TaxLiability> clazzSrc = src.getClass();

		final TaxLiability json = JsonTaxLiabilityHelper.jsonEquivalentOf(src);
		assertNotNull(json);
		assertTrue(json instanceof JsonTaxLiabilityHelper.JsonTaxLiability);
		assertEquals(expectedType, ((JsonTaxLiabilityHelper.JsonTaxLiability) json).getType());

		final Class<? extends TaxLiability> clazzJson = json.getClass();
		assertTrue(clazzSrc.isAssignableFrom(clazzJson));

		assertEquals(src.getStartReason(), json.getStartReason());
		assertEquals(src.getEndReason(), json.getEndReason());
		assertEquals(src.getDateTo(), json.getDateTo());
		assertEquals(src.getDateFrom(), json.getDateFrom());

		return json;
	}

	private static void doTest(Withholding src, IndividualTaxLiabilityType expectedType) {
		final TaxLiability json = doTest((TaxLiability) src, expectedType);
		assertTrue(json instanceof Withholding);
		assertEquals(src.getTaxationAuthority(), ((Withholding) json).getTaxationAuthority());
	}

	@Test
	public void testMutationWithNulls() throws Exception {
		assertNull(JsonTaxLiabilityHelper.jsonEquivalentOf(null));
		doTest(buildExpenditureBased(null, null, null, null), IndividualTaxLiabilityType.EXPENDITURE_BASED);
		doTest(buildForeignCountry(null, null, null, null), IndividualTaxLiabilityType.FOREIGN_COUNTRY);
		doTest(buildIndigent(null, null, null, null), IndividualTaxLiabilityType.INDIGENT);
		doTest(buildMixedWithholding137Par1(null, null, null, null, null), IndividualTaxLiabilityType.MIXED_WITHHOLDING_137_1);
		doTest(buildMixedWithholding137Par2(null, null, null, null, null), IndividualTaxLiabilityType.MIXED_WITHHOLDING_137_2);
		doTest(buildOrdinaryResident(null, null, null, null), IndividualTaxLiabilityType.ORDINARY_RESIDENT);
		doTest(buildOtherCanton(null, null, null, null), IndividualTaxLiabilityType.OTHER_CANTON);
		doTest(buildPureWithholding(null, null, null, null, null), IndividualTaxLiabilityType.PURE_WITHHOLDING);
		doTest(buildSwissDiplomat(null, null, null, null), IndividualTaxLiabilityType.SWISS_DIPLOMAT);
	}

	private static Date generateRandomDate() {
		final Random rnd = new Random();
		final RegDate date = RegDate.get().addDays(rnd.nextInt(365));
		return DataHelper.coreToWeb(date);
	}

	@Test
	public void testMutationNoNulls() throws Exception {
		doTest(buildExpenditureBased(LiabilityChangeReason.SEPARATION_DIVORCE_PARTNERSHIP_ABOLITION, LiabilityChangeReason.DEPARTURE_TO_OTHER_CANTON, generateRandomDate(), generateRandomDate()), IndividualTaxLiabilityType.EXPENDITURE_BASED);
		doTest(buildForeignCountry(LiabilityChangeReason.MOVE_IN_FROM_OTHER_CANTON, LiabilityChangeReason.CHANGE_OF_TAXATION_METHOD, generateRandomDate(), generateRandomDate()), IndividualTaxLiabilityType.FOREIGN_COUNTRY);
		doTest(buildIndigent(LiabilityChangeReason.MAJORITY, LiabilityChangeReason.WIDOWHOOD_DEATH, generateRandomDate(), generateRandomDate()), IndividualTaxLiabilityType.INDIGENT);
		doTest(buildMixedWithholding137Par1(LiabilityChangeReason.MOVE_VD, LiabilityChangeReason.DEPARTURE_TO_FOREIGN_COUNTRY, generateRandomDate(), generateRandomDate(), null), IndividualTaxLiabilityType.MIXED_WITHHOLDING_137_1);
		doTest(buildMixedWithholding137Par2(LiabilityChangeReason.MOVE_IN_FROM_FOREIGN_COUNTRY, LiabilityChangeReason.C_PERMIT_SWISS, generateRandomDate(), generateRandomDate(), null), IndividualTaxLiabilityType.MIXED_WITHHOLDING_137_2);
		doTest(buildOrdinaryResident(LiabilityChangeReason.C_PERMIT_SWISS, LiabilityChangeReason.DEPARTURE_TO_FOREIGN_COUNTRY, generateRandomDate(), generateRandomDate()), IndividualTaxLiabilityType.ORDINARY_RESIDENT);
		doTest(buildOtherCanton(LiabilityChangeReason.DEPARTURE_TO_OTHER_CANTON, LiabilityChangeReason.SEPARATION_DIVORCE_PARTNERSHIP_ABOLITION, generateRandomDate(), generateRandomDate()), IndividualTaxLiabilityType.OTHER_CANTON);
		doTest(buildPureWithholding(LiabilityChangeReason.SEPARATION_DIVORCE_PARTNERSHIP_ABOLITION, LiabilityChangeReason.CANCELLATION, generateRandomDate(), generateRandomDate(), null), IndividualTaxLiabilityType.PURE_WITHHOLDING);
		doTest(buildSwissDiplomat(LiabilityChangeReason.MOVE_IN_FROM_OTHER_CANTON, LiabilityChangeReason.END_DIPLOMATIC_ACTVITY, generateRandomDate(), generateRandomDate()), IndividualTaxLiabilityType.SWISS_DIPLOMAT);
	}

	@Test
	public void testAlreadyJson() throws Exception {
		doTestAlreadyJson(buildExpenditureBased(LiabilityChangeReason.SEPARATION_DIVORCE_PARTNERSHIP_ABOLITION, LiabilityChangeReason.DEPARTURE_TO_OTHER_CANTON, generateRandomDate(), generateRandomDate()));
		doTestAlreadyJson(buildForeignCountry(LiabilityChangeReason.MOVE_IN_FROM_OTHER_CANTON, LiabilityChangeReason.CHANGE_OF_TAXATION_METHOD, generateRandomDate(), generateRandomDate()));
		doTestAlreadyJson(buildIndigent(LiabilityChangeReason.MAJORITY, LiabilityChangeReason.WIDOWHOOD_DEATH, generateRandomDate(), generateRandomDate()));
		doTestAlreadyJson(buildMixedWithholding137Par1(LiabilityChangeReason.MOVE_VD, LiabilityChangeReason.DEPARTURE_TO_FOREIGN_COUNTRY, generateRandomDate(), generateRandomDate(), null));
		doTestAlreadyJson(buildMixedWithholding137Par2(LiabilityChangeReason.MOVE_IN_FROM_FOREIGN_COUNTRY, LiabilityChangeReason.C_PERMIT_SWISS, generateRandomDate(), generateRandomDate(), null));
		doTestAlreadyJson(buildOrdinaryResident(LiabilityChangeReason.C_PERMIT_SWISS, LiabilityChangeReason.DEPARTURE_TO_FOREIGN_COUNTRY, generateRandomDate(), generateRandomDate()));
		doTestAlreadyJson(buildOtherCanton(LiabilityChangeReason.DEPARTURE_TO_OTHER_CANTON, LiabilityChangeReason.SEPARATION_DIVORCE_PARTNERSHIP_ABOLITION, generateRandomDate(), generateRandomDate()));
		doTestAlreadyJson(buildPureWithholding(LiabilityChangeReason.SEPARATION_DIVORCE_PARTNERSHIP_ABOLITION, LiabilityChangeReason.CANCELLATION, generateRandomDate(), generateRandomDate(), null));
		doTestAlreadyJson(buildSwissDiplomat(LiabilityChangeReason.MOVE_IN_FROM_OTHER_CANTON, LiabilityChangeReason.END_DIPLOMATIC_ACTVITY, generateRandomDate(), generateRandomDate()));
	}

	private void doTestAlreadyJson(TaxLiability nonJson) {
		final TaxLiability json = JsonTaxLiabilityHelper.jsonEquivalentOf(nonJson);
		assertNotSame(nonJson, json);
		assertSame(json, JsonTaxLiabilityHelper.jsonEquivalentOf(json));
	}
}
