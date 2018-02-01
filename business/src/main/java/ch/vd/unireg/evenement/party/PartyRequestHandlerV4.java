package ch.vd.unireg.evenement.party;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.event.party.party.v4.PartyRequest;
import ch.vd.unireg.xml.event.party.party.v4.PartyResponse;
import ch.vd.unireg.xml.party.v4.Party;
import ch.vd.unireg.xml.party.v4.PartyPart;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.declaration.source.ListeRecapService;
import ch.vd.unireg.efacture.EFactureService;
import ch.vd.unireg.evenement.RequestHandlerResult;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.iban.IbanValidator;
import ch.vd.unireg.interfaces.service.ServiceCivilService;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.interfaces.service.ServiceOrganisationService;
import ch.vd.unireg.jms.BamMessageSender;
import ch.vd.unireg.jms.EsbBusinessCode;
import ch.vd.unireg.jms.EsbBusinessException;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionService;
import ch.vd.unireg.metier.bouclement.ExerciceCommercialHelper;
import ch.vd.unireg.metier.piis.PeriodeImpositionImpotSourceService;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.regimefiscal.RegimeFiscalService;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.situationfamille.SituationFamilleService;
import ch.vd.unireg.tiers.AutreCommunaute;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.xml.Context;
import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.party.v4.PartyBuilder;

public class PartyRequestHandlerV4 implements RequestHandlerV1<PartyRequest> {

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

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvenementFiscalService(EvenementFiscalService evenementFiscalService) {
		context.evenementFiscalService = evenementFiscalService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setRegimeFiscalService(RegimeFiscalService regimeFiscalService) {
		context.regimeFiscalService = regimeFiscalService;
	}

	@Override
	public RequestHandlerResult<PartyResponse> handle(PartyRequest request) throws EsbBusinessException {

		// Vérification des droits d'accès
		final UserLogin login = request.getLogin();
		if (!context.securityProvider.isGranted(Role.VISU_ALL, login.getUserId(), login.getOid())) {
			throw new EsbBusinessException(EsbBusinessCode.DROITS_INSUFFISANTS,
			                               String.format("L'utilisateur spécifié (%s/%d) n'a pas les droits d'accès en lecture complète sur l'application.", login.getUserId(), login.getOid()),
			                               null);
		}

		// Récupération du tiers
		final Tiers tiers = context.tiersDAO.get(request.getPartyNumber(), true);
		if (tiers == null) {
			throw new EsbBusinessException(EsbBusinessCode.CTB_INEXISTANT, String.format("Le tiers n°%d n'existe pas.", request.getPartyNumber()), null);
		}

		if (context.securityProvider.getDroitAcces(login.getUserId(), request.getPartyNumber()) == null) {
			throw new EsbBusinessException(EsbBusinessCode.DROITS_INSUFFISANTS,
			                               String.format("L'utilisateur spécifié (%s/%d) n'a pas les droits d'accès en lecture sur le tiers n° %d.", login.getUserId(), login.getOid(), request.getPartyNumber()),
			                               null);
		}

		// Calcul de l'adresse
		final PartyResponse response = new PartyResponse();
		response.setPartyNumber(request.getPartyNumber());
		try {
			final Party data;
			final Set<PartyPart> parts = DataHelper.toSet(PartyPart.class, request.getParts());
			if (tiers instanceof ch.vd.unireg.tiers.PersonnePhysique) {
				final ch.vd.unireg.tiers.PersonnePhysique personne = (ch.vd.unireg.tiers.PersonnePhysique) tiers;
				data = PartyBuilder.newNaturalPerson(personne, parts, context);
			}
			else if (tiers instanceof ch.vd.unireg.tiers.MenageCommun) {
				final ch.vd.unireg.tiers.MenageCommun menage = (ch.vd.unireg.tiers.MenageCommun) tiers;
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
			else if (tiers instanceof AutreCommunaute) {
				final AutreCommunaute autreCommunaute = (AutreCommunaute) tiers;
				data = PartyBuilder.newOtherCommunity(autreCommunaute, parts, context);
			}
			else {
				throw new EsbBusinessException(EsbBusinessCode.REPONSE_IMPOSSIBLE, "Type de tiers " + tiers.getNatureTiers() + " non exposé.", null);
			}

			response.setParty(data);
		}
		catch (ServiceException | ObjectNotFoundException e) {
			throw new EsbBusinessException(EsbBusinessCode.REPONSE_IMPOSSIBLE, e.getMessage(), e);
		}

		return new RequestHandlerResult<>(response);
	}

	@Override
	public ClassPathResource getRequestXSD() {
		return new ClassPathResource("event/party/party-request-4.xsd");
	}

	@Override
	public List<ClassPathResource> getResponseXSD() {
		return Arrays.asList(new ClassPathResource("event/party/party-response-4.xsd"),
				new ClassPathResource("party/unireg-party-administrativeauthority-4.xsd"),
				new ClassPathResource("party/unireg-party-corporation-4.xsd"),
				new ClassPathResource("party/unireg-party-othercommunity-2.xsd"),
				new ClassPathResource("party/unireg-party-debtor-4.xsd"),
				new ClassPathResource("party/unireg-party-establishment-1.xsd"),
				new ClassPathResource("party/unireg-party-person-4.xsd"));
	}
}
