package ch.vd.unireg.webservices.v6;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.common.v2.Date;
import ch.vd.unireg.xml.party.taxdeclaration.v4.DocumentType;
import ch.vd.unireg.xml.party.taxdeclaration.v4.OrdinaryTaxDeclaration;
import ch.vd.unireg.xml.party.taxdeclaration.v4.PartnershipForm;
import ch.vd.unireg.xml.party.taxdeclaration.v4.TaxDeclaration;
import ch.vd.unireg.xml.party.taxdeclaration.v4.TaxDeclarationDeadline;
import ch.vd.unireg.xml.party.taxdeclaration.v4.TaxDeclarationStatus;
import ch.vd.unireg.xml.party.taxdeclaration.v4.TaxDeclarationStatusType;
import ch.vd.unireg.xml.party.taxdeclaration.v4.TaxDeclarationType;
import ch.vd.unireg.xml.party.taxdeclaration.v4.TaxPeriod;
import ch.vd.unireg.xml.party.taxdeclaration.v4.WithholdingTaxDeclaration;
import ch.vd.unireg.xml.party.withholding.v1.CommunicationMode;
import ch.vd.unireg.xml.party.withholding.v1.WithholdingTaxDeclarationPeriodicity;
import ch.vd.unireg.common.WithoutSpringTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class JsonTaxDeclarationHelperTest extends WithoutSpringTest {

	private static OrdinaryTaxDeclaration buildOrdinaryTaxDeclaration(long id, Date dateFrom, Date dateTo, Date cancellationDate, TaxPeriod taxPeriod,
	                                                                  List<TaxDeclarationStatus> statuses, List<TaxDeclarationDeadline> deadlines, long sequenceNumber,
	                                                                  DocumentType documentType, long managingMunicipalityFSOId, Integer segmentationCode) {
		return new OrdinaryTaxDeclaration(id, dateFrom, dateTo, cancellationDate, taxPeriod, statuses, deadlines, sequenceNumber, documentType, managingMunicipalityFSOId, segmentationCode, 0, null);
	}

	private static WithholdingTaxDeclaration buildWithholdingTaxDeclaration(long id, Date dateFrom, Date dateTo, Date cancellationDate, TaxPeriod taxPeriod,
	                                                                        List<TaxDeclarationStatus> statuses, List<TaxDeclarationDeadline> deadlines,
	                                                                        WithholdingTaxDeclarationPeriodicity periodicity, CommunicationMode communicationMode) {
		return new WithholdingTaxDeclaration(id, dateFrom, dateTo, cancellationDate, taxPeriod, statuses, deadlines, periodicity, communicationMode, null);
	}

	private static PartnershipForm buildPartnershipForm(long id, Date dateFrom, Date dateTo, Date cancellationDate, TaxPeriod taxPeriod,
	                                                    List<TaxDeclarationStatus> statuses, List<TaxDeclarationDeadline> deadlines) {
		return new PartnershipForm(id, dateFrom, dateTo, cancellationDate, taxPeriod, statuses, deadlines, 0, null);
	}

	private static TaxDeclaration doTest(TaxDeclaration src, TaxDeclarationType expectedType) {
		assertNotNull(src);
		assertFalse(src instanceof JsonTaxDeclarationHelper.JsonTaxDeclaration);
		final Class<? extends TaxDeclaration> clazzSrc = src.getClass();

		final TaxDeclaration json = JsonTaxDeclarationHelper.jsonEquivalentOf(src);
		assertNotNull(json);
		assertTrue(json instanceof JsonTaxDeclarationHelper.JsonTaxDeclaration);
		assertEquals(expectedType, ((JsonTaxDeclarationHelper.JsonTaxDeclaration) json).getType());

		final Class<? extends TaxDeclaration> clazzJson = json.getClass();
		assertTrue(clazzSrc.isAssignableFrom(clazzJson));

		assertEquals(src.getId(), json.getId());
		assertEquals(src.getDateTo(), json.getDateTo());
		assertEquals(src.getDateFrom(), json.getDateFrom());
		assertEquals(src.getCancellationDate(), json.getCancellationDate());
		assertEquals(src.getTaxPeriod(), json.getTaxPeriod());
		assertEquals(src.getStatuses(), json.getStatuses());
		assertEquals(src.getDeadlines(), json.getDeadlines());

		return json;
	}

	private static void doTest(WithholdingTaxDeclaration src, TaxDeclarationType expectedType) {
		final TaxDeclaration json = doTest((TaxDeclaration) src, expectedType);
		assertTrue(json instanceof WithholdingTaxDeclaration);
		assertEquals(src.getCommunicationMode(), ((WithholdingTaxDeclaration) json).getCommunicationMode());
		assertEquals(src.getPeriodicity(), ((WithholdingTaxDeclaration) json).getPeriodicity());
	}

	private static void doTest(OrdinaryTaxDeclaration src, TaxDeclarationType expectedType) {
		final TaxDeclaration json = doTest((TaxDeclaration) src, expectedType);
		assertTrue(json instanceof OrdinaryTaxDeclaration);
		assertEquals(src.getDocumentType(), ((OrdinaryTaxDeclaration) json).getDocumentType());
		assertEquals(src.getManagingMunicipalityFSOId(), ((OrdinaryTaxDeclaration) json).getManagingMunicipalityFSOId());
		assertEquals(src.getSegmentationCode(), ((OrdinaryTaxDeclaration) json).getSegmentationCode());
		assertEquals(src.getSequenceNumber(), ((OrdinaryTaxDeclaration) json).getSequenceNumber());
	}

	private static void doTest(PartnershipForm src, TaxDeclarationType expectedType) {
		final TaxDeclaration json = doTest((TaxDeclaration) src, expectedType);
		assertTrue(json instanceof PartnershipForm);
	}

	@Test
	public void testMutationWithNulls() throws Exception {
		assertNull(JsonTaxDeclarationHelper.jsonEquivalentOf(null));
		doTest(buildOrdinaryTaxDeclaration(0, null, null, null, null, null, null, 0, null, 0, null), TaxDeclarationType.ORDINARY_TAX_DECLARATION);
		doTest(buildWithholdingTaxDeclaration(0, null, null, null, null, null, null, null, null), TaxDeclarationType.WITHHOLDING_TAX_DECLARATION);
		doTest(buildPartnershipForm(0, null, null, null, null, null, null), TaxDeclarationType.PARTNERSHIP_FORM);
	}

	private static Date generateRandomDate() {
		final Random rnd = new Random();
		final RegDate date = RegDate.get().addDays(rnd.nextInt(365));
		return DataHelper.coreToWeb(date);
	}

	private static TaxPeriod generateRandomTaxPeriod() {
		final Random rnd = new Random();
		final int year = 1995 + rnd.nextInt(20);
		return new TaxPeriod(year, null);
	}

	@Test
	public void testMutationNoNulls() throws Exception {
		final List<TaxDeclarationStatus> statuses = new ArrayList<>();
		statuses.add(new TaxDeclarationStatus(generateRandomDate(), null, TaxDeclarationStatusType.SENT, null, 0, null));

		final List<TaxDeclarationDeadline> deadlines = new ArrayList<>();
		deadlines.add(new TaxDeclarationDeadline(generateRandomDate(), generateRandomDate(), generateRandomDate(), null, true, null));

		doTest(buildOrdinaryTaxDeclaration(12, generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomTaxPeriod(), statuses, deadlines, 42, DocumentType.CORPORATE_ENTITY_TAX_DECLARATION, 231674L, 12), TaxDeclarationType.ORDINARY_TAX_DECLARATION);
		doTest(buildWithholdingTaxDeclaration(12, generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomTaxPeriod(), statuses, deadlines, WithholdingTaxDeclarationPeriodicity.HALF_YEARLY, CommunicationMode.PAPER), TaxDeclarationType.WITHHOLDING_TAX_DECLARATION);
		doTest(buildPartnershipForm(12, generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomTaxPeriod(), statuses, deadlines), TaxDeclarationType.PARTNERSHIP_FORM);
	}

	@Test
	public void testAlreadyJson() throws Exception {
		final List<TaxDeclarationStatus> statuses = new ArrayList<>();
		statuses.add(new TaxDeclarationStatus(generateRandomDate(), null, TaxDeclarationStatusType.SENT, null, 0, null));

		final List<TaxDeclarationDeadline> deadlines = new ArrayList<>();
		deadlines.add(new TaxDeclarationDeadline(generateRandomDate(), generateRandomDate(), generateRandomDate(), null, true, null));

		doTestAlreadyJson(buildOrdinaryTaxDeclaration(12, generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomTaxPeriod(), statuses, deadlines, 42, DocumentType.CORPORATE_ENTITY_TAX_DECLARATION, 231674L, 12));
		doTestAlreadyJson(buildWithholdingTaxDeclaration(12, generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomTaxPeriod(), statuses, deadlines, WithholdingTaxDeclarationPeriodicity.HALF_YEARLY, CommunicationMode.PAPER));
		doTestAlreadyJson(buildPartnershipForm(12, generateRandomDate(), generateRandomDate(), generateRandomDate(), generateRandomTaxPeriod(), statuses, deadlines));
	}

	private void doTestAlreadyJson(TaxDeclaration nonJson) {
		final TaxDeclaration json = JsonTaxDeclarationHelper.jsonEquivalentOf(nonJson);
		assertNotSame(nonJson, json);
		assertSame(json, JsonTaxDeclarationHelper.jsonEquivalentOf(json));
	}
}
