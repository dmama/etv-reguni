package ch.vd.unireg.evenement.infra;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.event.infra.taxoffices.v1.TaxOfficesRequest;
import ch.vd.unireg.xml.event.infra.taxoffices.v1.TaxOfficesResponse;
import ch.vd.unireg.xml.event.infra.v1.Response;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.unireg.xml.exception.v1.ServiceExceptionInfo;
import ch.vd.unireg.xml.infra.taxoffices.v1.TaxOffice;
import ch.vd.unireg.xml.infra.taxoffices.v1.TaxOffices;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TaxOfficesRequestEsbHandlerV1ItTest extends InfraRequestEsbHandlerItTest {

	@Override
	protected String getRequestXSD() {
		return "event/infra/taxoffices-request-1.xsd";
	}

	@Override
	protected List<String> getResponseXSD() {
		return Collections.singletonList("event/infra/taxoffices-response-1.xsd");
	}

	@Test
	public void testCommuneVaudoise() throws Exception {

		final TaxOfficesRequest request = new TaxOfficesRequest();
		request.setDate(DataHelper.coreToXMLv2(date(2014, 7, 12)));
		request.setMunicipalityFSOId(5477);     // Cossonay

		// Envoie le message
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());
				return null;
			}
		});

		final Response response = parseResponse(getEsbMessage(getOutputQueue()));
		assertNotNull(response);
		assertEquals(TaxOfficesResponse.class, response.getClass());

		final TaxOfficesResponse toResponse = (TaxOfficesResponse) response;
		Assert.assertNull(toResponse.getError());

		final TaxOffices taxOffices = toResponse.getTaxOffices();
		Assert.assertNotNull(taxOffices);

		final TaxOffice district = taxOffices.getDistrict();
		final TaxOffice region = taxOffices.getRegion();
		Assert.assertNotNull(district);
		Assert.assertNotNull(region);
		Assert.assertEquals(12, district.getAdmCollNo());       // OID Nyon
		Assert.assertEquals(12, region.getAdmCollNo());         // OID Nyon
	}

	@Test
	public void testCommuneNonVaudoise() throws Exception {

		final TaxOfficesRequest request = new TaxOfficesRequest();
		request.setDate(DataHelper.coreToXMLv2(date(2014, 7, 12)));
		request.setMunicipalityFSOId(6458);     // Neuchâtel

		// Envoie le message
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());
				return null;
			}
		});

		final Response response = parseResponse(getEsbMessage(getOutputQueue()));
		assertNotNull(response);
		assertEquals(TaxOfficesResponse.class, response.getClass());

		final TaxOfficesResponse toResponse = (TaxOfficesResponse) response;
		Assert.assertNull(toResponse.getTaxOffices());

		final ServiceExceptionInfo error = toResponse.getError();
		Assert.assertNotNull(error);
		Assert.assertEquals(BusinessExceptionInfo.class, error.getClass());
		Assert.assertEquals("Commune 6458 inconnue dans le canton de Vaud.", error.getMessage());
		Assert.assertEquals(BusinessExceptionCode.INFRASTRUCTURE.name(), ((BusinessExceptionInfo) error).getCode());
	}
}
