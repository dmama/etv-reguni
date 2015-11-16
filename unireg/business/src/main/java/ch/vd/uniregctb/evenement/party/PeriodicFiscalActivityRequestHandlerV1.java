package ch.vd.uniregctb.evenement.party;

import java.util.Collections;
import java.util.List;

import org.springframework.core.io.ClassPathResource;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.event.party.fiscact.periodic.v1.PeriodicFiscalActivityRequest;
import ch.vd.unireg.xml.event.party.fiscact.v1.FiscalActivityResponse;
import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.uniregctb.jms.EsbBusinessException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.xml.ServiceException;

public class PeriodicFiscalActivityRequestHandlerV1 implements RequestHandler<PeriodicFiscalActivityRequest> {

	private TiersDAO tiersDAO;
	private SecurityProviderInterface securityProvider;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	@Override
	public RequestHandlerResult handle(PeriodicFiscalActivityRequest request) throws ServiceException, EsbBusinessException {

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
		return new RequestHandlerResult(new FiscalActivityResponse(hasActivite));
	}

	@Override
	public ClassPathResource getRequestXSD() {
		return new ClassPathResource("event/party/periodic-fiscact-request-1.xsd");
	}

	@Override
	public List<ClassPathResource> getResponseXSD() {
		return Collections.singletonList(new ClassPathResource("event/party/fiscact-response-1.xsd"));
	}
}
