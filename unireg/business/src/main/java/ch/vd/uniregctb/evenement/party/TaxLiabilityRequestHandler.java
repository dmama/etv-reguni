package ch.vd.uniregctb.evenement.party;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.core.io.ClassPathResource;

import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.event.party.taxliab.v2.CommonHouseholdInfo;
import ch.vd.unireg.xml.event.party.taxliab.v2.Failure;
import ch.vd.unireg.xml.event.party.taxliab.v2.MinorInfo;
import ch.vd.unireg.xml.event.party.taxliab.v2.TaxLiabilityRequest;
import ch.vd.unireg.xml.event.party.taxliab.v2.TaxLiabilityResponse;
import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.uniregctb.evenement.party.control.ControlRuleException;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.xml.Context;
import ch.vd.uniregctb.xml.ServiceException;

public abstract class TaxLiabilityRequestHandler  implements RequestHandler<TaxLiabilityRequest> {

	private final Context context = new Context();

	public void setTiersDAO(TiersDAO tiersDAO) {
		context.tiersDAO = tiersDAO;
	}

	public void setTiersService(TiersService tiersService) {
		context.tiersService = tiersService;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		context.securityProvider = securityProvider;
	}

	public void setAssujettissementService(AssujettissementService assujettissementService) {
		context.assujettissementService = assujettissementService;
	}

	@Override
	public RequestHandlerResult handle(TaxLiabilityRequest request) throws ServiceException {
		// Vérification des droits d'accès
		final UserLogin login = request.getLogin();
		if (!context.securityProvider.isGranted(Role.VISU_ALL, login.getUserId(), login.getOid())) {
			throw new ServiceException(
					new AccessDeniedExceptionInfo("L'utilisateur spécifié (" + login.getUserId() + '/' + login.getOid() + ") n'a pas les droits d'accès en lecture complète sur l'application.", null));
		}

		final int number = request.getPartyNumber();

		final Tiers tiers = context.tiersDAO.get(number, true);
		if (tiers == null) {
			throw new ServiceException(new BusinessExceptionInfo("Le tiers n°" + number + " n'existe pas.", BusinessExceptionCode.UNKNOWN_PARTY.name(), null));
		}
		TaxliabilityControlManager controlManager = new TaxliabilityControlManager(context);
		TaxliabilityControlResult result = null;
		try {
			result = runControl(controlManager, request);
		}
		catch (ControlRuleException e) {
			//TODO a finaliser
			//ServiceExceptionInfo infoException = new BusinessExceptionInfo(e.getMessage())
			//throw  new ServiceException(e.getMessage());
		}
		return getHandlerResult(result);

	}

	public abstract TaxliabilityControlResult runControl(TaxliabilityControlManager controlManager,TaxLiabilityRequest request) throws ControlRuleException;


	protected RequestHandlerResult getHandlerResult(TaxliabilityControlResult result) {
		Integer partyNumber = null;
		Failure failure = null;
		final TaxliabilityControlEchec echec = result.getEchec();
		if (echec != null) {
			failure = new Failure();
			if (TaxliabilityControlEchecType.CONTROLE_NUMERO_KO == echec.getType()) {
				failure.setNoTaxLiability("Tiers non assujetti");
			}
			if (TaxliabilityControlEchecType.AUCUN_MC_ASSOCIE_TROUVE == echec.getType()) {
				failure.setNoCommonHousehold("Tiers non assujetti sans ménage commun");
			}
			if (TaxliabilityControlEchecType.UN_PLUSIEURS_MC_NON_ASSUJETTI_TROUVES == echec.getType()) {
				List<Integer> menageCommunsIds = getListOfInteger(echec.getMenageCommunIds());
				CommonHouseholdInfo commonHouseholdInfo = new CommonHouseholdInfo(menageCommunsIds);
				failure.setNoTaxLiableCommonHouseholds(commonHouseholdInfo);
			}
			if (TaxliabilityControlEchecType.PLUSIEURS_MC_ASSUJETTI_TROUVES == echec.getType()) {
				List<Integer> menageCommunsIds = getListOfInteger(echec.getMenageCommunIds());
				CommonHouseholdInfo commonHouseholdInfo = new CommonHouseholdInfo(menageCommunsIds);
				failure.setMultipleTaxLiableCommonHouseholds(commonHouseholdInfo);
			}
			if (TaxliabilityControlEchecType.CONTROLE_SUR_PARENTS_KO == echec.getType()) {

				List<Integer> menageCommunsParentIds = null;
				List<Integer> parentIds = null;
				final List<Long> parentsIdsFromCore = echec.getParentsIds();
				final List<Long> menageParentIdFromCore = echec.getMenageCommunParentsIds();
				if (parentsIdsFromCore != null) {
					parentIds = getListOfInteger(parentsIdsFromCore);
				}

				if (menageParentIdFromCore != null) {
					menageCommunsParentIds = getListOfInteger(menageParentIdFromCore);
				}
				MinorInfo minorInfo = new MinorInfo(menageCommunsParentIds,parentIds);
				failure.setNoTaxLiableMinorTaxPayer(minorInfo);
			}
		}
		else if (result.getIdTiersAssujetti() != null) {
			partyNumber = result.getIdTiersAssujetti().intValue();
		}
		TaxLiabilityResponse response = new TaxLiabilityResponse(partyNumber,failure);

		return new RequestHandlerResult(response);
	}

	private List<Integer> getListOfInteger(List<Long> menageCommunIds) {
		final List<Integer> result = new ArrayList<Integer>();
		for (Long menageCommunId : menageCommunIds) {
			result.add(menageCommunId.intValue());
		}
		return result;

	}

	@Override
	public List<ClassPathResource> getResponseXSD() {
		return Arrays.asList(new ClassPathResource("event/party/taxliab-response-2.xsd"));
	}
}
