package ch.vd.unireg.common;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.displaytag.tags.TableTagParameters;
import org.displaytag.util.ParamEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.tiers.manager.AutorisationManager;
import ch.vd.unireg.tiers.manager.Autorisations;
import ch.vd.unireg.type.Niveau;

public class ControllerUtilsImpl implements ControllerUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(ControllerUtilsImpl.class);

	private SecurityProviderInterface securityProvider;
	private TiersService tiersService;
	private AutorisationManager autorisationManager;

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setAutorisationManager(AutorisationManager autorisationManager) {
		this.autorisationManager = autorisationManager;
	}

	@Override
	public void checkAccesDossierEnLecture(Long tiersId) throws ObjectNotFoundException, AccessDeniedException {
		if (tiersId != null) {
			final Niveau acces = securityProvider.getDroitAcces(AuthenticationHelper.getCurrentPrincipal(), tiersId);
			if (acces == null) {
				final String message = String.format("L'opérateur [%s] s'est vu refuser l'accès en lecture sur le tiers n°%d",
						AuthenticationHelper.getCurrentPrincipal(), tiersId);
				LOGGER.warn(message);
				throw new AccessDeniedException(String.format("Vous ne possédez pas les droits de visualisation sur le contribuable %s.", FormatNumeroHelper.numeroCTBToDisplay(tiersId)));
			}
		}
	}

	@Override
	public void checkAccesDossierEnEcriture(Long tiersId) throws ObjectNotFoundException, AccessDeniedException {
		if (tiersId != null) {
			final Niveau acces = securityProvider.getDroitAcces(AuthenticationHelper.getCurrentPrincipal(), tiersId);
			if (acces == null || acces == Niveau.LECTURE) {
				final String message = String.format(
						"L'opérateur [%s] s'est vu refuser l'accès en écriture sur le tiers n°%d (acces autorisé=%s)", AuthenticationHelper
								.getCurrentPrincipal(), tiersId, (acces == null ? "null" : acces.toString()));
				LOGGER.warn(message);
				throw new AccessDeniedException(String.format("Vous ne possédez pas les droits d'édition sur le contribuable %s.", FormatNumeroHelper.numeroCTBToDisplay(tiersId)));
			}
		}
	}

	@Override
	public String getDisplayTagRequestParametersForPagination(HttpServletRequest request, String tableName) {
		final ParamEncoder encoder = new ParamEncoder(tableName);
		final String pageParamName = encoder.encodeParameterName(TableTagParameters.PARAMETER_PAGE);
		final String sortUsingNameParamName = encoder.encodeParameterName(TableTagParameters.PARAMETER_SORTUSINGNAME);
		final String sortParamName = encoder.encodeParameterName(TableTagParameters.PARAMETER_SORT);
		final String orderParamName = encoder.encodeParameterName(TableTagParameters.PARAMETER_ORDER);
		return getDisplayTagRequestParametersForPagination(
				tableName,
				StringUtils.trimToNull(request.getParameter(pageParamName)),
				StringUtils.trimToNull(request.getParameter(sortUsingNameParamName)),
				StringUtils.trimToNull(request.getParameter(sortParamName)),
				StringUtils.trimToNull(request.getParameter(orderParamName)),
				true
		);

	}

	@Override
	public String getDisplayTagRequestParametersForPagination(String tableName, ParamPagination pagination) {
		return getDisplayTagRequestParametersForPagination(
				tableName,
				Integer.toString(pagination.getNumeroPage()),
				"1",
				pagination.getSorting().getField(),
				pagination.getSorting().isAscending() ? "1" : "2",
				false);
	}

	private String getDisplayTagRequestParametersForPagination(String tableName, String pageParamValue, String sortUsingNameParamValue, String sortParamValue, String orderParamValue,
	                                                           boolean htmlEscape) {

		final ParamEncoder encoder = new ParamEncoder(tableName);
		final String pageParamName = encoder.encodeParameterName(TableTagParameters.PARAMETER_PAGE);
		final String sortUsingNameParamName = encoder.encodeParameterName(TableTagParameters.PARAMETER_SORTUSINGNAME);
		final String sortParamName = encoder.encodeParameterName(TableTagParameters.PARAMETER_SORT);
		final String orderParamName = encoder.encodeParameterName(TableTagParameters.PARAMETER_ORDER);

		final StringBuilder b = new StringBuilder();
		if (pageParamValue != null) {
			b.append(String.format("%s=%s", pageParamName, pageParamValue));
		}
		if (sortUsingNameParamValue != null) {
			if (b.length() > 0) {
				b.append('&');
			}
			b.append(String.format("%s=%s", sortUsingNameParamName, sortUsingNameParamValue));
		}
		if (sortParamValue != null) {
			if (b.length() > 0) {
				b.append('&');
			}
			b.append(String.format("%s=%s", sortParamName, sortParamValue));
		}
		if (orderParamValue != null) {
			if (b.length() > 0) {
				b.append('&');
			}
			b.append(String.format("%s=%s", orderParamName, orderParamValue));
		}

		String ret;
		if (htmlEscape) {
			ret = HtmlUtils.htmlEscape(b.toString());
		}
		else {
			ret = b.toString();
		}
		return StringUtils.trimToNull(ret);
	}

	@Override
	@Transactional(readOnly = true)
	public void checkTraitementContribuableAvecDecisionAci(Long tiersId) throws ObjectNotFoundException, AccessDeniedException {
		if (tiersId != null) {
			Tiers tiers = tiersService.getTiers(tiersId);
			if (tiers != null && tiers instanceof Contribuable) {
				Contribuable ctb = (Contribuable)tiers;
				Autorisations autorisations = getAutorisations(ctb);
				if (!autorisations.isDecisionsAci() && tiersService.isSousInfluenceDecisions(ctb)) {
					final String message = String.format(
							"L'opérateur [%s] s'est vu refuser le traitement sur le tiers n°%d :Décision ACI !", AuthenticationHelper.getCurrentPrincipal(),tiersId);
					LOGGER.warn(message);
					throw new AccessDeniedException(String.format("Vous ne possédez pas les droits pour traiter le contribuable %s qui est soumis à une décision ACI.", FormatNumeroHelper.numeroCTBToDisplay(tiersId)));
				}
			}
		}
	}

	private Autorisations getAutorisations(Contribuable ctb) {
		return autorisationManager.getAutorisations(ctb, AuthenticationHelper.getCurrentPrincipal(), AuthenticationHelper.getCurrentOID());
	}
}
