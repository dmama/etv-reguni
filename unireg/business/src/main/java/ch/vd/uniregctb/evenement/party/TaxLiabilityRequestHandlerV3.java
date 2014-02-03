package ch.vd.uniregctb.evenement.party;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.ClassPathResource;

import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.event.party.taxliab.v3.CommonHouseholdInfo;
import ch.vd.unireg.xml.event.party.taxliab.v3.Failure;
import ch.vd.unireg.xml.event.party.taxliab.v3.MinorInfo;
import ch.vd.unireg.xml.event.party.taxliab.v2.TaxLiabilityRequest;
import ch.vd.unireg.xml.event.party.taxliab.v3.TaxLiabilityResponse;
import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.uniregctb.evenement.party.control.ControlRuleException;
import ch.vd.uniregctb.evenement.party.control.TaxLiabilityControlEchec;
import ch.vd.uniregctb.evenement.party.control.TaxLiabilityControlResult;
import ch.vd.uniregctb.evenement.party.control.TaxLiabilityControlService;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.xml.ServiceException;

public abstract class TaxLiabilityRequestHandlerV3<T extends TaxLiabilityRequest> implements RequestHandler<T> {

	private TiersDAO tiersDAO;
	private SecurityProviderInterface securityProvider;
	private TaxLiabilityControlService taxliabilityControlService;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setTaxliabilityControlService(TaxLiabilityControlService taxliabilityControlService) {
		this.taxliabilityControlService = taxliabilityControlService;
	}

	protected final TaxLiabilityControlService getTaxliabilityControlService() {
		return taxliabilityControlService;
	}

	@Override
	public RequestHandlerResult handle(T request) throws ServiceException {
		// Vérification des droits d'accès
		final UserLogin login = request.getLogin();
		if (!securityProvider.isGranted(Role.VISU_ALL, login.getUserId(), login.getOid())) {
			throw new ServiceException(
					new AccessDeniedExceptionInfo("L'utilisateur spécifié (" + login.getUserId() + '/' + login.getOid() + ") n'a pas les droits d'accès en lecture complète sur l'application.", null));
		}

		final int number = request.getPartyNumber();

		final Tiers tiers = tiersDAO.get(number, true);
		if (tiers == null) {
			throw new ServiceException(new BusinessExceptionInfo("Le tiers n°" + number + " n'existe pas.", BusinessExceptionCode.UNKNOWN_PARTY.name(), null));
		}

		try {
			final TaxLiabilityControlResult result = doControl(request, tiers);
			return builtRequestHandler(result);
		}
		catch (ControlRuleException e) {
			throw new ServiceException(new BusinessExceptionInfo(e.getMessage(), BusinessExceptionCode.TAX_LIABILITY.name(), null));
		}
	}

	/**
	 * Le vrai travail de contrôle se passe ici...
	 * @param request la requête en entrée
	 * @param tiers le tiers concerné en premier lieu
	 * @return le résultat du contrôle
	 * @throws ch.vd.uniregctb.evenement.party.control.ControlRuleException en cas de souci
	 */
	protected abstract TaxLiabilityControlResult doControl(T request, @NotNull Tiers tiers) throws ControlRuleException;

	private RequestHandlerResult builtRequestHandler(TaxLiabilityControlResult result) {

		Integer partyNumber = null;
		Failure failure = null;

		final TaxLiabilityControlEchec echec = result.getEchec();
		if (echec != null) {
			failure = new Failure();

			switch (echec.getType()) {
				case CONTROLE_NUMERO_KO:
					failure.setNoTaxLiability("Tiers non-assujetti");
					break;

				case AUCUN_MC_ASSOCIE_TROUVE:
					failure.setNoCommonHousehold("Tiers non-assujetti sans ménage commun");
					break;

				case UN_PLUSIEURS_MC_NON_ASSUJETTI_TROUVES:
				{
					final List<Integer> menageCommunsIds = getListOfInteger(echec.getMenageCommunIds());
					failure.setNoTaxLiableCommonHouseholds(new CommonHouseholdInfo(menageCommunsIds));
					break;
				}

				case PLUSIEURS_MC_ASSUJETTI_TROUVES:
				{
					final List<Integer> menageCommunsIds = getListOfInteger(echec.getMenageCommunIds());
					failure.setMultipleTaxLiableCommonHouseholds(new CommonHouseholdInfo(menageCommunsIds));
					break;
				}

				case CONTROLE_SUR_PARENTS_KO:
				{
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
					final MinorInfo minorInfo = new MinorInfo(menageCommunsParentIds,parentIds);
					failure.setNoTaxLiableMinorTaxPayer(minorInfo);
					break;
				}

				case DATE_OU_PF_DANS_FUTURE:{
					failure.setDateOrPeriodeInFuture("La date ou la période fiscale demandée est située dans le futur.");
					break;
				}

				default:
					throw new IllegalArgumentException("Value not supported for TaxliabilityRequest V3: " + echec.getType());
			}
		}
		else if (result.getIdTiersAssujetti() != null) {
			partyNumber = result.getIdTiersAssujetti().intValue();
		}
		else {
			throw new IllegalArgumentException("Ni échec ni réussite... Cas bizarre, non ?");
		}

		final TaxLiabilityResponse response = new TaxLiabilityResponse(partyNumber, failure);
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
		return Arrays.asList(new ClassPathResource("event/party/taxliab-response-3.xsd"));
	}
}
