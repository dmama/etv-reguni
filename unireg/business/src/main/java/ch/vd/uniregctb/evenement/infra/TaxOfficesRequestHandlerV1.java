package ch.vd.uniregctb.evenement.infra;

import java.util.Collections;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.unireg.xml.event.infra.taxoffices.v1.TaxOfficesRequest;
import ch.vd.unireg.xml.event.infra.taxoffices.v1.TaxOfficesResponse;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.declaration.source.ListeRecapService;
import ch.vd.uniregctb.efacture.EFactureService;
import ch.vd.uniregctb.evenement.RequestHandlerResult;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.iban.IbanValidator;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.jms.BamMessageSender;
import ch.vd.uniregctb.jms.EsbBusinessException;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.metier.bouclement.ExerciceCommercialHelper;
import ch.vd.uniregctb.metier.piis.PeriodeImpositionImpotSourceService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.regimefiscal.ServiceRegimeFiscal;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.situationfamille.SituationFamilleService;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.xml.Context;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.ServiceException;
import ch.vd.uniregctb.xml.infra.v1.TaxOfficesBuilder;

public class TaxOfficesRequestHandlerV1 implements RequestHandler<TaxOfficesRequest> {

	private final Context context = new Context();

	public void setTiersDAO(TiersDAO tiersDAO) {
		context.tiersDAO = tiersDAO;
	}

	public void setTiersService(TiersService tiersService) {
		context.tiersService = tiersService;
	}

	public void setSituationService(SituationFamilleService situationService) {
		context.situationService = situationService;
	}

	public void setAdresseService(AdresseService adresseService) {
		context.adresseService = adresseService;
	}

	public void setExerciceCommercialHelper(ExerciceCommercialHelper exerciceCommercialHelper) {
		context.exerciceCommercialHelper = exerciceCommercialHelper;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		context.infraService = infraService;
	}

	public void setIbanValidator(IbanValidator ibanValidator) {
		context.ibanValidator = ibanValidator;
	}

	public void setParametreService(ParametreAppService parametreService) {
		context.parametreService = parametreService;
	}

	public void setServiceCivil(ServiceCivilService service) {
		context.serviceCivilService = service;
	}

	public void setServiceOrganisation(ServiceOrganisationService service) {
		context.serviceOrganisationService = service;
	}

	public void setHibernateTemplate(HibernateTemplate template) {
		context.hibernateTemplate = template;
	}

	public void setTransactionManager(PlatformTransactionManager manager) {
		context.transactionManager = manager;
	}

	public void setLrService(ListeRecapService service) {
		context.lrService = service;
	}

	public void setDiService(DeclarationImpotService service) {
		context.diService = service;
	}

	public void setBamMessageSender(BamMessageSender service) {
		context.bamSender = service;
	}

	public void setAssujettissementService(AssujettissementService service) {
		context.assujettissementService = service;
	}

	public void setPeriodeImpositionService(PeriodeImpositionService service) {
		context.periodeImpositionService = service;
	}

	public void setPeriodeImpositionImpotSourceService(PeriodeImpositionImpotSourceService service) {
		context.periodeImpositionImpotSourceService = service;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		context.securityProvider = securityProvider;
	}

	public void setEFactureService(EFactureService eFactureService) {
		context.eFactureService = eFactureService;
	}

	public void setEvenementFiscalService(EvenementFiscalService evenementFiscalService) {
		context.evenementFiscalService = evenementFiscalService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceRegimeFiscal(ServiceRegimeFiscal serviceRegimeFiscal) {
		context.serviceRegimeFiscal = serviceRegimeFiscal;
	}

	@Override
	public RequestHandlerResult<TaxOfficesResponse> handle(TaxOfficesRequest request) throws ServiceException, EsbBusinessException {
		final TaxOfficesResponse response = new TaxOfficesResponse();
		try {
			response.setTaxOffices(TaxOfficesBuilder.newTaxOffices(request.getMunicipalityFSOId(),
			                                                       DataHelper.xmlToCore(request.getDate()),
			                                                       context));
		}
		catch (ObjectNotFoundException e) {
			response.setError(new BusinessExceptionInfo(e.getMessage(), BusinessExceptionCode.INFRASTRUCTURE.name(), null));
		}
		return new RequestHandlerResult<>(response);
	}

	@Override
	public ClassPathResource getRequestXSD() {
		return new ClassPathResource("event/infra/taxoffices-request-1.xsd");
	}

	@Override
	public List<ClassPathResource> getResponseXSD() {
		return Collections.singletonList(new ClassPathResource("event/infra/taxoffices-response-1.xsd"));
	}
}
