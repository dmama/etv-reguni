package ch.vd.unireg.evenement.party;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.declaration.source.ListeRecapService;
import ch.vd.unireg.evenement.RequestHandlerResult;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.iban.IbanValidator;
import ch.vd.unireg.interfaces.service.ServiceCivilService;
import ch.vd.unireg.interfaces.service.ServiceEntreprise;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.jms.BamMessageSender;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionService;
import ch.vd.unireg.metier.bouclement.ExerciceCommercialHelper;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.regimefiscal.RegimeFiscalService;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.situationfamille.SituationFamilleService;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.xml.BusinessHelper;
import ch.vd.unireg.xml.Context;
import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.event.party.party.v1.PartyRequest;
import ch.vd.unireg.xml.event.party.party.v1.PartyResponse;
import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.unireg.xml.exception.v1.TechnicalExceptionInfo;
import ch.vd.unireg.xml.party.v1.Party;
import ch.vd.unireg.xml.party.v1.PartyBuilder;
import ch.vd.unireg.xml.party.v1.PartyPart;

public class PartyRequestHandlerV1 implements RequestHandlerV1<PartyRequest> {

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
	public void setServiceEntreprise(ServiceEntreprise service) {
		context.serviceEntreprise = service;
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

	@SuppressWarnings({"UnusedDeclaration"})
	public void setRegimeFiscalService(RegimeFiscalService regimeFiscalService) {
		context.regimeFiscalService = regimeFiscalService;
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
			if (tiers instanceof ch.vd.unireg.tiers.PersonnePhysique) {
				final ch.vd.unireg.tiers.PersonnePhysique personne = (ch.vd.unireg.tiers.PersonnePhysique) tiers;
				BusinessHelper.warmIndividusV1(personne, parts, context);
				data = PartyBuilder.newNaturalPerson(personne, parts, context);
			}
			else if (tiers instanceof ch.vd.unireg.tiers.MenageCommun) {
				final ch.vd.unireg.tiers.MenageCommun menage = (ch.vd.unireg.tiers.MenageCommun) tiers;
				BusinessHelper.warmIndividusV1(menage, parts, context);
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
	@NotNull
	public List<String> getRequestXSDs() {
		return Arrays.asList("eCH-0010-4-0.xsd",
		                     "eCH-0044-2-0.xsd",
		                     "unireg-common-1.xsd",
		                     "party/unireg-party-address-1.xsd",
		                     "party/unireg-party-relation-1.xsd",
		                     "party/unireg-party-debtor-type-1.xsd",
		                     "party/unireg-party-taxdeclaration-1.xsd",
		                     "party/unireg-party-taxresidence-1.xsd",
		                     "party/unireg-party-1.xsd",
		                     "party/unireg-party-debtor-1.xsd",
		                     "event/party/request-1.xsd",
		                     "event/party/party-request-1.xsd");
	}

	@Override
	@NotNull
	public List<String> getResponseXSDs() {
		return Arrays.asList("eCH-0010-4-0.xsd",
		                     "unireg-common-1.xsd",
		                     "unireg-common-1.xsd",
		                     "party/unireg-party-address-1.xsd",
		                     "party/unireg-party-relation-1.xsd",
		                     "party/unireg-party-debtor-type-1.xsd",
		                     "party/unireg-party-taxdeclaration-1.xsd",
		                     "party/unireg-party-taxresidence-1.xsd",
		                     "party/unireg-party-immovableproperty-1.xsd",
		                     "party/unireg-party-1.xsd",
		                     "party/unireg-party-taxpayer-1.xsd",
		                     "party/unireg-party-administrativeauthority-1.xsd",
		                     "party/unireg-party-corporation-1.xsd",
		                     "party/unireg-party-debtor-1.xsd",
		                     "party/unireg-party-person-1.xsd",
		                     "event/party/party-response-1.xsd");
	}
}
