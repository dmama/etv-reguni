package ch.vd.unireg.evenement.party;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.RequestHandlerResult;
import ch.vd.unireg.jms.EsbBusinessException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.event.party.fiscact.periodic.v1.PeriodicFiscalActivityRequest;
import ch.vd.unireg.xml.event.party.fiscact.v1.FiscalActivityResponse;
import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;

public class PeriodicFiscalActivityRequestHandlerV1 implements RequestHandlerV1<PeriodicFiscalActivityRequest> {

	private TiersDAO tiersDAO;
	private SecurityProviderInterface securityProvider;

	private static final Map<Boolean, String> MESSAGES_TRAITEMENT = buildMessagesTraitement();

	@NotNull
	private static Map<Boolean, String> buildMessagesTraitement() {
		final Map<Boolean, String> map = new HashMap<>(2);
		map.put(Boolean.TRUE, "Le contribuable a un for vaudois ouvert sur la période demandée.");
		map.put(Boolean.FALSE, "Le contribuable n'a aucun for vaudois ouvert sur la période demandée.");
		return Collections.unmodifiableMap(map);
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	@Override
	public RequestHandlerResult<FiscalActivityResponse> handle(PeriodicFiscalActivityRequest request) throws ServiceException, EsbBusinessException {

		// droits généraux sur l'application
		final UserLogin login = request.getLogin();
		if (!securityProvider.isGranted(Role.VISU_ALL, login.getUserId(), login.getOid()) && !securityProvider.isGranted(Role.VISU_FORS, login.getUserId(), login.getOid())) {
			throw new ServiceException(new AccessDeniedExceptionInfo(String.format("L'utilisateur spécifié (%s/%d) n'a pas les droits d'accès en lecture sur les fors fiscaux.",
			                                                                       login.getUserId(),
			                                                                       login.getOid()),
			                                                         null));
		}

		// Récupération du tiers
		final Tiers tiers = tiersDAO.get(request.getPartyNumber(), true);
		if (tiers == null) {
			throw new ServiceException(new BusinessExceptionInfo(String.format("Le tiers n°%d n'existe pas.", request.getPartyNumber()),
			                                                     BusinessExceptionCode.UNKNOWN_PARTY.name(), null));
		}

		// Vérification des droits d'accès spécifiques au tiers (protégé ?)
		if (securityProvider.getDroitAcces(login.getUserId(), request.getPartyNumber()) == null) {
			throw new ServiceException(new AccessDeniedExceptionInfo(String.format("L'utilisateur spécifié (%s/%d) n'a pas les droits d'accès en lecture sur le tiers n° %d.",
			                                                                       login.getUserId(),
			                                                                       login.getOid(),
			                                                                       request.getPartyNumber()),
			                                                         null));
		}

		// on va rechercher les fors vaudois (principal ou secondaire) sur l'année considérée

		boolean hasActivite = false;
		final int annee = request.getYear();
		final DateRange range = new DateRangeHelper.Range(RegDate.get(annee, 1, 1), RegDate.get(annee, 12, 31));
		final List<ForFiscal> forsFiscauxNonAnnules = tiers.getForsFiscauxNonAnnules(false);
		for (ForFiscal ff : forsFiscauxNonAnnules) {
			if (ff.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD && DateRangeHelper.intersect(range, ff)) {
				hasActivite = true;
				break;
			}
		}

		// construction de la réponse
		return new RequestHandlerResult<>(new FiscalActivityResponse(hasActivite, MESSAGES_TRAITEMENT.get(hasActivite)));
	}

	@Override
	@NotNull
	public List<String> getRequestXSDs() {
		return Arrays.asList("unireg-common-1.xsd",
		                     "event/party/request-1.xsd",
		                     "event/party/fiscact-request-1.xsd",
		                     "event/party/periodic-fiscact-request-1.xsd");
	}

	@Override
	@NotNull
	public List<String> getResponseXSDs() {
		return Arrays.asList("unireg-common-1.xsd",
		                     "unireg-exception-1.xsd",
		                     "event/party/response-1.xsd",
		                     "event/party/fiscact-response-1.xsd");
	}
}
