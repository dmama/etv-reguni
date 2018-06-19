package ch.vd.unireg.evenement.infra;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.evenement.RequestHandlerResult;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.iban.IbanValidator;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.situationfamille.SituationFamilleService;
import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.event.infra.taxoffices.v1.TaxOfficesRequest;
import ch.vd.unireg.xml.event.infra.taxoffices.v1.TaxOfficesResponse;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.unireg.xml.exception.v1.ServiceExceptionInfo;
import ch.vd.unireg.xml.infra.taxoffices.v1.TaxOffice;
import ch.vd.unireg.xml.infra.taxoffices.v1.TaxOffices;

public class TaxOfficesRequestHandlerV1Test extends BusinessTest {

	private TaxOfficesRequestHandlerV1 handler;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		handler = new TaxOfficesRequestHandlerV1();
		handler.setAdresseService(getBean(AdresseService.class, "adresseService"));
		handler.setAssujettissementService(getBean(AssujettissementService.class, "assujettissementService"));
		handler.setDiService(getBean(DeclarationImpotService.class, "diService"));
		handler.setHibernateTemplate(hibernateTemplate);
		handler.setIbanValidator(getBean(IbanValidator.class, "ibanValidator"));
		handler.setInfraService(serviceInfra);
		handler.setServiceCivil(serviceCivil);
		handler.setServiceEntreprise(serviceEntreprise);
		handler.setSituationService(getBean(SituationFamilleService.class, "situationFamilleService"));
		handler.setTiersDAO(tiersDAO);
		handler.setTiersService(tiersService);
		handler.setTransactionManager(transactionManager);
		handler.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCommuneVaudoiseSansDate() throws Exception {
		final TaxOfficesRequest request = new TaxOfficesRequest();
		request.setMunicipalityFSOId(MockCommune.Echallens.getNoOFS());
		request.setDate(null);

		final RequestHandlerResult<TaxOfficesResponse> result = handler.handle(request);
		Assert.assertNotNull(result);

		final TaxOfficesResponse response = result.getResponse();
		Assert.assertNotNull(response);
		Assert.assertNull(response.getError());

		final TaxOffices offices = response.getTaxOffices();
		Assert.assertNotNull(offices);

		final TaxOffice district = offices.getDistrict();
		final TaxOffice region = offices.getRegion();
		Assert.assertNotNull(district);
		Assert.assertNotNull(region);
		Assert.assertEquals(MockOfficeImpot.OID_ECHALLENS.getNoColAdm(), district.getAdmCollNo());
		Assert.assertEquals(MockOfficeImpot.OID_YVERDON.getNoColAdm(), region.getAdmCollNo());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCommuneVaudoiseAvecDate() throws Exception {
		final TaxOfficesRequest request = new TaxOfficesRequest();
		request.setMunicipalityFSOId(MockCommune.Echallens.getNoOFS());
		request.setDate(DataHelper.coreToXMLv2(date(2006, 6, 12)));

		final RequestHandlerResult<TaxOfficesResponse> result = handler.handle(request);
		Assert.assertNotNull(result);

		final TaxOfficesResponse response = result.getResponse();
		Assert.assertNotNull(response);
		Assert.assertNull(response.getError());

		final TaxOffices offices = response.getTaxOffices();
		Assert.assertNotNull(offices);

		final TaxOffice district = offices.getDistrict();
		final TaxOffice region = offices.getRegion();
		Assert.assertNotNull(district);
		Assert.assertNotNull(region);
		Assert.assertEquals(MockOfficeImpot.OID_ECHALLENS.getNoColAdm(), district.getAdmCollNo());
		Assert.assertEquals(MockOfficeImpot.OID_YVERDON.getNoColAdm(), region.getAdmCollNo());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCommuneNonVaudoise() throws Exception {
		final TaxOfficesRequest request = new TaxOfficesRequest();
		request.setMunicipalityFSOId(MockCommune.Bern.getNoOFS());
		request.setDate(DataHelper.coreToXMLv2(date(2006, 6, 12)));

		final RequestHandlerResult<TaxOfficesResponse> result = handler.handle(request);
		Assert.assertNotNull(result);

		final TaxOfficesResponse response = result.getResponse();
		Assert.assertNotNull(response);
		Assert.assertNull(response.getTaxOffices());

		final ServiceExceptionInfo error = response.getError();
		Assert.assertNotNull(error);
		Assert.assertEquals(BusinessExceptionInfo.class, error.getClass());
		Assert.assertEquals("Commune " + MockCommune.Bern.getNoOFS() + " inconnue dans le canton de Vaud.", error.getMessage());
		Assert.assertEquals(BusinessExceptionCode.INFRASTRUCTURE.name(), ((BusinessExceptionInfo) error).getCode());
	}
}
