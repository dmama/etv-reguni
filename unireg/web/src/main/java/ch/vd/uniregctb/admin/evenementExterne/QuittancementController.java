package ch.vd.uniregctb.admin.evenementExterne;


import ch.vd.fiscalite.taxation.evtQuittanceListeV1.EvtQuittanceListeDocument;
import ch.vd.fiscalite.taxation.evtQuittanceListeV1.ListeType;
import ch.vd.fiscalite.taxation.evtQuittanceListeV1.QuittanceType;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.SelectContainerValue;
import ch.vd.uniregctb.evenement.externe.EvenementExterneService;
import ch.vd.uniregctb.web.xt.AbstractEnhancedSimpleFormController;
import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springmodules.xt.ajax.AjaxResponse;
import org.springmodules.xt.ajax.AjaxSubmitEvent;
import org.springmodules.xt.ajax.action.ReplaceContentAction;
import org.springmodules.xt.ajax.component.TaggedText;
import org.springmodules.xt.ajax.web.servlet.AjaxModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class QuittancementController extends AbstractEnhancedSimpleFormController {

	private static final Logger logger = Logger.getLogger(QuittancementController.class);

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
		typeQuittances.add(new SelectContainerValue(QuittanceType.QUITTANCEMENT.toString()));
		typeQuittances.add(new SelectContainerValue(QuittanceType.ANNULATION.toString()));
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
		if (view.getDateQuittance() == null && QuittanceType.QUITTANCEMENT.toString().equals(view.getTypeQuittance())) {
			errors.rejectValue("dateQuittance", "quittancement.null.dateQuittance",
					"La date de quittancement est obligatoire lors d'un quittancement");
		}
	}

	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {
		return new AjaxModelAndView(this.getSuccessView(), errors);
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public AjaxResponse send(AjaxSubmitEvent event, AjaxResponse response) throws Exception {
		if (event.getValidationErrors().hasErrors()) {
			return response;
		}
		QuittancementView view = (QuittancementView) event.getCommandObject();
		try {
			QuittanceType.Enum type = QuittanceType.Enum.forString(view.getTypeQuittance());
			EvtQuittanceListeDocument doc = evenementExterneService
					.createEvenementQuittancement(type, Long.parseLong(view.getNumeroCtb()), ListeType.LR, RegDate.get(view.getDateDebut()), RegDate.get(view.getDateFin()), RegDate.get(view.getDateQuittance()));

			evenementExterneService.sendEvent("UNIREG-" + System.currentTimeMillis(), doc);
			response.addAction(new ReplaceContentAction("error.global", new TaggedText("événement envoyé", TaggedText.Tag.DIV)));
		}
		catch (Exception ex) {
			logger.warn("Erreur lors de l'envoie d'une événement externe: ", ex);
			response.addAction(new ReplaceContentAction("error.global", new TaggedText(ex.getMessage(), TaggedText.Tag.DIV)));
		}
		return response;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvenementExterneService(EvenementExterneService evenementExterneService) {
		this.evenementExterneService = evenementExterneService;
	}
}
