package ch.vd.uniregctb.webservice.v7;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.party.corporation.v5.Corporation;
import ch.vd.unireg.xml.party.landtaxlightening.v1.HousingActData;
import ch.vd.unireg.xml.party.landtaxlightening.v1.IciAbatement;
import ch.vd.unireg.xml.party.landtaxlightening.v1.IciAbatementRequest;
import ch.vd.unireg.xml.party.landtaxlightening.v1.IfoncExemption;
import ch.vd.unireg.xml.party.landtaxlightening.v1.UseData;
import ch.vd.unireg.xml.party.v5.Party;
import ch.vd.unireg.xml.party.v5.PartyPart;

import static ch.vd.uniregctb.webservice.v7.WebServiceGetPartyItTest.buildUriAndParams;
import static ch.vd.uniregctb.webservice.v7.WebServiceLandRegistryItTest.assertDate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class WebServiceLandTaxLighteningsItTest extends AbstractWebServiceItTest {

	private static final String DB_UNIT_DATA_FILE = "WebServiceLandTaxLighteningsItTest.xml";

	private static boolean alreadySetUp = false;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		if (!alreadySetUp) {
			loadDatabase(DB_UNIT_DATA_FILE);
			alreadySetUp = true;
		}
	}

	@Test
	public void testGetImmovablePropertyWithTaxLightenings() throws Exception {

		final int noOrg = 312;
		final int noImm = 264310664;

		final Pair<String, Map<String, ?>> params = buildUriAndParams(noOrg, EnumSet.of(PartyPart.LAND_TAX_LIGHTENINGS));
		final ResponseEntity<Party> resp = get(Party.class, MediaType.APPLICATION_XML, params.getLeft(), params.getRight());
		assertNotNull(resp);
		assertEquals(HttpStatus.OK, resp.getStatusCode());

		final Party party = resp.getBody();
		assertNotNull(party);
		assertTrue(party instanceof Corporation);
		final Corporation corp = (Corporation) party;

		final List<IfoncExemption> exemptions = corp.getIfoncExemptions();
		assertNotNull(exemptions);
		assertEquals(1, exemptions.size());
		assertExemption(RegDate.get(2010, 1, 1), RegDate.get(2018, 12, 31), BigDecimal.valueOf(4523, 2), noImm, exemptions.get(0));

		final List<IciAbatement> abatements = corp.getIciAbatements();
		assertNotNull(abatements);
		assertEquals(2, abatements.size());
		assertAbatement(noImm, abatements.get(0), RegDate.get(2013, 3, 22), null,
		                12000, 6500, 230, BigDecimal.valueOf(80), BigDecimal.valueOf(60),
		                11000, 5000, 250, BigDecimal.valueOf(20), BigDecimal.valueOf(40),
		                RegDate.get(2015, 3, 1), RegDate.get(2020, 2, 25), BigDecimal.valueOf(825, 2));
		assertAbatement(noImm, abatements.get(1), RegDate.get(2015, 6, 1), RegDate.get(2015, 6, 30),
		                null, null, null, null, null,
		                null, null, null, null, null,
		                null, null, null);

		final List<IciAbatementRequest> requests = corp.getIciAbatementRequests();
		assertNotNull(requests);
		assertEquals(1, requests.size());
		assertAbatementRequest(RegDate.get(2016, 2, 3), RegDate.get(2016, 6, 30), RegDate.get(2016, 7, 1), RegDate.get(2016, 11, 2), 2015, 1, 264310664, requests.get(0));
	}

	private static void assertAbatement(int noImm, IciAbatement abatement, RegDate dateFrom, RegDate dateTo,
	                                    @Nullable Integer rentalIncome, @Nullable Integer rentalVolume, @Nullable Integer rentalArea, BigDecimal rentalDeclaredPercent, BigDecimal rentalApprovedPercent,
	                                    @Nullable Integer ownUseIncome, @Nullable Integer ownUseVolume, @Nullable Integer ownUseArea, BigDecimal ownUseDeclaredPercent, BigDecimal ownUseApprovedPercent,
	                                    RegDate grantDate, RegDate expireDate, BigDecimal socialNaturePercent) {
		assertDate(dateFrom, abatement.getDateFrom());
		assertDate(dateTo, abatement.getDateTo());
		assertEquals(noImm, abatement.getImmovablePropertyId());
		assertUseData(rentalIncome, rentalVolume, rentalArea, rentalDeclaredPercent, rentalApprovedPercent, abatement.getRentalUse());
		assertUseData(ownUseIncome, ownUseVolume, ownUseArea, ownUseDeclaredPercent, ownUseApprovedPercent, abatement.getOwnUse());
		assertHousingAct(grantDate, expireDate, socialNaturePercent, abatement.getHousingAct());
	}

	private static void assertAbatementRequest(RegDate sendDate, RegDate deadline, RegDate reminderDate, RegDate returnDate, int taxPeriod, int sequenceNumber, int immovablePropId, IciAbatementRequest request) {
		assertNotNull(request);
		assertDate(sendDate, request.getSendDate());
		assertDate(deadline, request.getDeadline());
		assertDate(reminderDate, request.getReminderDate());
		assertDate(returnDate, request.getReturnDate());
		assertEquals(taxPeriod, request.getTaxPeriod());
		assertEquals(sequenceNumber, request.getSequenceNumber());
		assertEquals(immovablePropId, request.getImmovablePropertyId());
	}

	private static void assertHousingAct(@Nullable RegDate grantDate, @Nullable RegDate expireDate, @Nullable BigDecimal socialNaturePercent, @Nullable HousingActData housingAct) {
		if (grantDate == null && expireDate == null && socialNaturePercent == null) {
			assertNull(housingAct);
		}
		else {
			assertNotNull(housingAct);
			assertDate(grantDate, housingAct.getGrantDate());
			assertDate(expireDate, housingAct.getExpiryDate());
			assertEquals(socialNaturePercent, housingAct.getSocialNaturePercent());
		}
	}

	private static void assertUseData(@Nullable Integer income, @Nullable Integer volume, @Nullable Integer area, @Nullable BigDecimal declaredPercent, @Nullable BigDecimal approvedPercent, @Nullable UseData useData) {
		if (income == null && volume == null && area == null && declaredPercent == null && approvedPercent == null) {
			assertNull(useData);
		}
		else {
			assertNotNull(useData);
			assertEquals(income, useData.getIncome());
			assertEquals(volume, useData.getVolume());
			assertEquals(area, useData.getArea());
			assertEquals(declaredPercent, useData.getDeclaredPercent());
			assertEquals(approvedPercent, useData.getApprovedPercent());
		}
	}

	private static void assertExemption(RegDate dateFrom, RegDate dateTo, BigDecimal percent, long immovablePropId, IfoncExemption exemption) {
		assertNotNull(exemption);
		assertDate(dateFrom, exemption.getDateFrom());
		assertDate(dateTo, exemption.getDateTo());
		assertEquals(percent, exemption.getExemptionPercent());
		assertEquals(immovablePropId, exemption.getImmovablePropertyId());
	}
}
