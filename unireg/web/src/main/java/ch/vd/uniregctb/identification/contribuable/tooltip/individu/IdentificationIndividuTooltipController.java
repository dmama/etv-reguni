package ch.vd.uniregctb.identification.contribuable.tooltip.individu;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractCommandController;

import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;

/**
 * Affiche les informations civiles d'un individu lorsque la souris est positionnée sur ses nom/prénoms.
 */
public class IdentificationIndividuTooltipController extends AbstractCommandController {

	private ServiceCivilService civilService;
	private IdentificationIndividuTooltipManager manager;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setCivilService(ServiceCivilService civilService) {
		this.civilService = civilService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setManager(IdentificationIndividuTooltipManager manager) {
		this.manager = manager;
	}

	@Override
	protected ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) throws Exception {
		final IdentificationIndividuTooltipView view = (IdentificationIndividuTooltipView) command;

		final Long noInd;

		final String noIndAsString = request.getParameter("noInd");
		if (StringUtils.isNotBlank(noIndAsString)) {
			noInd = Long.parseLong(noIndAsString);
		}
		else {
			final String noCtbAsString = request.getParameter("noCtb");
			if (StringUtils.isNotBlank(noCtbAsString)) {
				final Long noCtb = Long.parseLong(noCtbAsString);
				noInd = manager.getNumeroIndividuFromCtb(noCtb);
			}
			else {
				noInd = null;
			}
		}

		if (noInd != null) {
			final Individu individu = civilService.getIndividu(noInd, null, AttributeIndividu.NATIONALITE);
			view.init(individu);
		}
		return new ModelAndView("/identification/tooltip/individu", errors.getModel());
	}
}
