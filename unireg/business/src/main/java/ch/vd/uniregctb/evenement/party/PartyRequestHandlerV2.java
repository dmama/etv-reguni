package ch.vd.uniregctb.evenement.party;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.event.party.party.v2.PartyRequest;
import ch.vd.unireg.xml.event.party.party.v2.PartyResponse;
import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.unireg.xml.exception.v1.TechnicalExceptionInfo;
import ch.vd.unireg.xml.party.v2.Party;
import ch.vd.unireg.xml.party.v2.PartyPart;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.declaration.source.ListeRecapService;
import ch.vd.uniregctb.evenement.RequestHandlerResult;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.iban.IbanValidator;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.jms.BamMessageSender;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.metier.bouclement.ExerciceCommercialHelper;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.situationfamille.SituationFamilleService;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.xml.BusinessHelper;
import ch.vd.uniregctb.xml.Context;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.ServiceException;
import ch.vd.uniregctb.xml.party.v2.PartyBuilder;

public class PartyRequestHandlerV2 implements RequestHandlerV1<PartyRequest> {

	private final Context context = new Context();

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersDAO(TiersDAO tiersDAO) {
		context.tiersDAO = tiersDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersService(TiersService tiersService) {
		context.tiersService = tiersService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setSituationService(SituationFamilleService situationService) {
		context.situationService = situationService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setAdresseService(AdresseService adresseService) {
		context.adresseService = adresseService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setExerciceCommercialHelper(ExerciceCommercialHelper exerciceCommercialHelper) {
		context.exerciceCommercialHelper = exerciceCommercialHelper;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setInfraService(ServiceInfrastructureService infraService) {
		context.infraService = infraService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setIbanValidator(IbanValidator ibanValidator) {
		context.ibanValidator = ibanValidator;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setParametreService(ParametreAppService parametreService) {
		context.parametreService = parametreService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceCivil(ServiceCivilService service) {
		context.serviceCivilService = service;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceOrganisation(ServiceOrganisationService service) {
		context.serviceOrganisationService = service;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHibernateTemplate(HibernateTemplate template) {
		context.hibernateTemplate = template;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTransactionManager(PlatformTransactionManager manager) {
		context.transactionManager = manager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setLrService(ListeRecapService service) {
		context.lrService = service;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDiService(DeclarationImpotService service) {
		context.diService = service;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setBamMessageSender(BamMessageSender service) {
		context.bamSender = service;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setAssujettissementService(AssujettissementService service) {
		context.assujettissementService = service;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setPeriodeImpositionService(PeriodeImpositionService service) {
		context.periodeImpositionService = service;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		context.securityProvider = securityProvider;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvenementFiscalService(EvenementFiscalService evenementFiscalService) {
		context.evenementFiscalService = evenementFiscalService;
	}

	@Override
	public RequestHandlerResult<PartyResponse> handle(PartyRequest request) throws ServiceException {

		// Vérification des droits d'accès
		final UserLogin login = request.getLogin();
		if (!context.securityProvider.isGranted(Role.VISU_ALL, login.getUserId(), login.getOid())) {
			throw new ServiceException(
					new AccessDeniedExceptionInfo("L'utilisateur spécifié (" + login.getUserId() + '/' + login.getOid() + ") n'a pas les droits d'accès en lecture complète sur l'application.", null));
		}

		// Récupération du tiers
		final Tiers tiers = context.tiersDAO.get(request.getPartyNumber(), true);
		if (tiers == null) {
			throw new ServiceException(new BusinessExceptionInfo("Le tiers n°" + request.getPartyNumber() + " n'existe pas.", BusinessExceptionCode.UNKNOWN_PARTY.name(), null));
		}

		if (context.securityProvider.getDroitAcces(login.getUserId(), request.getPartyNumber()) == null) {
			throw new ServiceException(new AccessDeniedExceptionInfo(
					"L'utilisateur spécifié (" + login.getUserId() + '/' + login.getOid() + ") n'a pas les droits d'accès en lecture sur le tiers n° " + request.getPartyNumber() + '.', null));
		}

		// Calcul de l'adresse
		final PartyResponse response = new PartyResponse();
		response.setPartyNumber(request.getPartyNumber());
		try {
			final Party data;
			final Set<PartyPart> parts = DataHelper.toSet(PartyPart.class, request.getParts());
			if (tiers instanceof ch.vd.uniregctb.tiers.PersonnePhysique) {
				final ch.vd.uniregctb.tiers.PersonnePhysique personne = (ch.vd.uniregctb.tiers.PersonnePhysique) tiers;
				BusinessHelper.warmIndividusV2(personne, parts, context);
				data = PartyBuilder.newNaturalPerson(personne, parts, context);
			}
			else if (tiers instanceof ch.vd.uniregctb.tiers.MenageCommun) {
				final ch.vd.uniregctb.tiers.MenageCommun menage = (ch.vd.uniregctb.tiers.MenageCommun) tiers;
				BusinessHelper.warmIndividusV2(menage, parts, context);
				data = PartyBuilder.newCommonHousehold(menage, parts, context);
			}
			else if (tiers instanceof Entreprise) {
				final Entreprise entreprise = (Entreprise) tiers;
				data = PartyBuilder.newCorporation(entreprise, parts, context);
			}
			else if (tiers instanceof DebiteurPrestationImposable) {
				final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiers;
				data = PartyBuilder.newDebtor(debiteur, parts, context);
			}
			else if (tiers instanceof CollectiviteAdministrative) {
				final CollectiviteAdministrative coladm = (CollectiviteAdministrative) tiers;
				data = PartyBuilder.newAdministrativeAuthority(coladm, parts, context);
			}
			else {
				data = null;
			}

			response.setParty(data);
		}
		catch (RuntimeException e) {
			throw new ServiceException(new TechnicalExceptionInfo(e.getMessage(), null));
		}

		// on ne valide pas la donnée en sortie (voir cas SIFISC-8901)
		return new RequestHandlerResult.NotValidatedResult<>(response);
	}

	@Override
	public ClassPathResource getRequestXSD() {
		return new ClassPathResource("event/party/party-request-2.xsd");
	}

	@Override
	public List<ClassPathResource> getResponseXSD() {
		return Arrays.asList(new ClassPathResource("event/party/party-response-2.xsd"),
				new ClassPathResource("party/unireg-party-administrativeauthority-2.xsd"),
				new ClassPathResource("party/unireg-party-corporation-2.xsd"),
				new ClassPathResource("party/unireg-party-debtor-2.xsd"),
				new ClassPathResource("party/unireg-party-person-2.xsd"));
	}
}
