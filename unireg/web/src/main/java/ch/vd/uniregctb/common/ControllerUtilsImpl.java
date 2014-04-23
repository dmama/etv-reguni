package ch.vd.uniregctb.common;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.displaytag.tags.TableTagParameters;
import org.displaytag.util.ParamEncoder;
import org.springframework.web.util.HtmlUtils;

import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.type.Niveau;

public class ControllerUtilsImpl implements ControllerUtils {

	private static final Logger LOGGER = Logger.getLogger(ControllerUtilsImpl.class);
	
	private SecurityProviderInterface securityProvider;

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
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
	
	@Override
	public String getDisplayTagRequestParametersForPagination(String tableName, String pageParamValue, String sortUsingNameParamValue, String sortParamValue, String orderParamValue,
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
		if(htmlEscape) {
			ret = HtmlUtils.htmlEscape(b.toString());	
		} else {
			ret = b.toString();
		}
		return StringUtils.trimToNull(ret);
	}
}
