package ch.vd.uniregctb.admin.evenementExterne;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springmodules.xt.ajax.AjaxResponse;
import org.springmodules.xt.ajax.AjaxSubmitEvent;
import org.springmodules.xt.ajax.action.ReplaceContentAction;
import org.springmodules.xt.ajax.component.TaggedText;
import org.springmodules.xt.ajax.web.servlet.AjaxModelAndView;

import ch.vd.fiscalite.registre.evenementImpotSourceV1.EvenementImpotSourceQuittanceType;
import ch.vd.fiscalite.registre.evenementImpotSourceV1.EvenementImpotSourceQuittanceType.TypeQuittance;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.SelectContainerValue;
import ch.vd.uniregctb.evenement.externe.EvenementExterneService;
import ch.vd.uniregctb.web.xt.AbstractEnhancedSimpleFormController;

public class QuittancementController extends AbstractEnhancedSimpleFormController {

	private static Logger logger = Logger.getLogger(QuittancementController.class);

	private EvenementExterneService evenementExterneService;

	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		return new QuittancementView();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Map referenceData(HttpServletRequest request) throws Exception {
		Map<String, Object> map = new HashMap<String, Object>(3);
		ArrayList<SelectContainerValue> typeQuittances = new ArrayList<SelectContainerValue>();
		typeQuittances.add(new SelectContainerValue(TypeQuittance.QUITTANCEMENT.toString()));
		typeQuittances.add(new SelectContainerValue(TypeQuittance.ANNULATION.toString()));
		map.put("typeQuittances", typeQuittances);
		return map;
	}



	@Override
	protected void onBindAndValidate(HttpServletRequest request, Object command, BindException errors) throws Exception {
		super.onBindAndValidate(request, command, errors);
		QuittancementView view = (QuittancementView) command;
		if (view.getNumeroCtb() == null || "".equals(view.getNumeroCtb())) {
			errors.rejectValue("numeroCtb", "quittancement.null.numeroCtb", "Numéro du débiteur est obligatoire");
		} else {
			try {
				Long.parseLong(view.getNumeroCtb());
			} catch(NumberFormatException ex) {
				errors.rejectValue("numeroCtb", "quittancement.wrong.numeroCtb", "Numéro du débiteur est mal formaté");
			}
		}
		if (view.getDateDebut() == null) {
			errors.rejectValue("dateDebut", "quittancement.null.dateDebut", "La date de début du récapitulatif est obligatoire");
		}
		if (view.getDateQuittance() == null && TypeQuittance.QUITTANCEMENT.toString().equals(view.getTypeQuittance())) {
			errors.rejectValue("dateQuittance", "quittancement.null.dateQuittance",
					"La date de quittancement est obligatoire lors d'un quittancement");
		}
	}

	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {
		return new AjaxModelAndView(this.getSuccessView(), errors);
	}

	public AjaxResponse send(AjaxSubmitEvent event, AjaxResponse response) throws Exception {
		if ( event.getValidationErrors().hasErrors()) {
			return response;
		}
		QuittancementView view = (QuittancementView) event.getCommandObject();
		try {
			TypeQuittance.Enum type = TypeQuittance.Enum.forString(view.getTypeQuittance());
			EvenementImpotSourceQuittanceType impotSource = evenementExterneService.createEvenementQuittancement(type, Long.parseLong(view
					.getNumeroCtb()), RegDate.get(view.getDateDebut()), RegDate.get(view.getDateFin()), RegDate.get(view.getDateQuittance()));

			evenementExterneService.sendEvenementExterne(impotSource);
			response.addAction( new ReplaceContentAction("error.global", new TaggedText("événement envoyé", TaggedText.Tag.DIV)));
		}
		catch (Exception ex) {
			logger.warn("Erreur lors de l'envoie d'une événement externe: ", ex);
			response.addAction( new ReplaceContentAction("error.global", new TaggedText(ex.getMessage(), TaggedText.Tag.DIV)));
		}
		return response;
	}



	/**
	 * @param evenementExterneService
	 *            the evenementExterneService to set
	 */
	public void setEvenementExterneService(EvenementExterneService evenementExterneService) {
		this.evenementExterneService = evenementExterneService;
	}


}
