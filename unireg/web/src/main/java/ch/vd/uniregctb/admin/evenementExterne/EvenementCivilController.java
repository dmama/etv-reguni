package ch.vd.uniregctb.admin.evenementExterne;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springmodules.xt.ajax.AjaxResponse;
import org.springmodules.xt.ajax.AjaxSubmitEvent;
import org.springmodules.xt.ajax.action.ReplaceContentAction;
import org.springmodules.xt.ajax.component.TaggedText;
import org.springmodules.xt.ajax.web.servlet.AjaxModelAndView;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.SelectContainerValue;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.evenement.civil.regpp.jms.EvenementCivilSender;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.web.xt.AbstractEnhancedSimpleFormController;

public class EvenementCivilController extends AbstractEnhancedSimpleFormController {

	private static final Logger logger = Logger.getLogger(EvenementCivilController.class);

	private EvenementCivilSender evtCivilSender;

	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		return new CivilUnitaireView();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Map referenceData(HttpServletRequest request) throws Exception {
		Map<String, Object> map = new HashMap<String, Object>(3);
		ArrayList<SelectContainerValue> typeEvenementCivils = new ArrayList<SelectContainerValue>();
		for (TypeEvenementCivil typeEvenementCivil : TypeEvenementCivil.values()) {
			typeEvenementCivils.add(new SelectContainerValue(typeEvenementCivil.getFullDescription(), typeEvenementCivil.getId()));
		}
		map.put("typeEvenementCivils", typeEvenementCivils);
		return map;
	}



	@Override
	protected void onBindAndValidate(HttpServletRequest request, Object command, BindException errors) throws Exception {
		super.onBindAndValidate(request, command, errors);
		CivilUnitaireView view = (CivilUnitaireView) command;
		if (view.getNoIndividu() == null || "".equals(view.getNoIndividu())) {
			errors.rejectValue("noIndividu", "quittancement.null.noIndividu", "Numéro d'individu est obligatoire");
		} else {
			try {
				Long.parseLong(view.getNoIndividu());
			} catch(NumberFormatException ex) {
				errors.rejectValue("noIndividu", "quittancement.wrong.noIndividu", "Numéro d'individu est mal formaté");
			}
		}
		if (view.getNoTechnique() == null || "".equals(view.getNoTechnique())) {
			errors.rejectValue("noTechnique", "quittancement.null.noTechnique", "Numéro technique est obligatoire");
		} else {
			try {
				Long.parseLong(view.getNoTechnique());
			} catch(NumberFormatException ex) {
				errors.rejectValue("noTechnique", "quittancement.wrong.noTechnique", "Numéro technique est mal formaté");
			}
		}
		if (view.getDateEvenement() == null) {
			errors.rejectValue("dateEvenement", "quittancement.null.dateEvenement", "La date de l'événement est obligatoire");
		}
		if (view.getDateTraitement() == null) {
			errors.rejectValue("dateTraitement", "quittancement.null.dateTraitement", "La date de traitement est obligatoire");
		}
	}

	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {
		return new AjaxModelAndView(this.getSuccessView(), errors);
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public AjaxResponse send(AjaxSubmitEvent event, AjaxResponse response) throws Exception {
		if ( event.getValidationErrors().hasErrors()) {
			return response;
		}

		CivilUnitaireView view = (CivilUnitaireView) event.getCommandObject();
		try {
			EvenementCivilRegPP evt= createEvenement(view);

			sendEvent(evt);
			response.addAction( new ReplaceContentAction("error.global", new TaggedText("événement envoyé", TaggedText.Tag.DIV)));
		}
		catch (Exception ex) {
			logger.warn("Erreur lors de l'envoie d'une événement externe: ", ex);
			response.addAction( new ReplaceContentAction("error.global", new TaggedText(ex.getMessage(), TaggedText.Tag.DIV)));
		}
		return response;
	}


	public EvenementCivilRegPP createEvenement(CivilUnitaireView view) {
		EvenementCivilRegPP evenement = new EvenementCivilRegPP();
		evenement.setId(Long.parseLong(view.getNoTechnique()));
		evenement.setNumeroOfsCommuneAnnonce(Integer.parseInt(view.getNumeroOFS()));
		evenement.setNumeroIndividuPrincipal(Long.parseLong(view.getNoIndividu()));
		evenement.setDateEvenement(RegDate.get(view.getDateEvenement()));
		evenement.setType(TypeEvenementCivil.valueOf(Integer.parseInt(view.getTypeEvenementCivil())));
		return evenement;
	}

	private void sendEvent(EvenementCivilRegPP evtRegCivil) throws Exception {
		if (evtRegCivil == null) {
			throw new IllegalArgumentException("Argument evtRegCivil ne peut être null.");
		}
		evtCivilSender.sendEvent(evtRegCivil, "Unireg-Web");
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvtCivilSender(EvenementCivilSender evtCivilSender) {
		this.evtCivilSender = evtCivilSender;
	}
}
