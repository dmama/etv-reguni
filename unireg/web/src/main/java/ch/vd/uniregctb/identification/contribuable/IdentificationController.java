package ch.vd.uniregctb.identification.contribuable;


import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import ch.vd.uniregctb.identification.contribuable.manager.IdentificationMessagesStatsManager;
import ch.vd.uniregctb.identification.contribuable.view.IdentificationMessagesStatsCriteriaView;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityCheck;

@Controller
@RequestMapping(value = "/identification")
public class IdentificationController {

	private static final String ACCESS_DENIED_MESSAGE = "Vous ne possédez aucun droit IfoSec pour la gestion de l'identification de contribuable";
	private IdentificationMessagesStatsManager identificationMessagesStatsManager;
	private IdentificationMapHelper identificationMapHelper;

	public void setIdentificationMessagesStatsManager(IdentificationMessagesStatsManager identificationMessagesStatsManager) {
		this.identificationMessagesStatsManager = identificationMessagesStatsManager;
	}

	public void setIdentificationMapHelper(IdentificationMapHelper identificationMapHelper) {
		this.identificationMapHelper = identificationMapHelper;
	}


	@ModelAttribute
	protected ModelMap referenceData(ModelMap model) throws Exception {
		model.put("typesMessage", identificationMapHelper.initMapTypeMessage());
		model.put("periodesFiscales", identificationMapHelper.initMapPeriodeFiscale());
		return model;
	}

	@RequestMapping(value = "/tableau-bord/effacer.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_ADMIN}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	protected String effacerFormulaireDeRecherche(ModelMap model) {
		final IdentificationMessagesStatsCriteriaView statsCriteriaView = identificationMessagesStatsManager.getView();
		model.put("statsCriteria", statsCriteriaView);
		model.put("statistiques",identificationMessagesStatsManager.calculerStats(statsCriteriaView));
		return "identification/tableau-bord/stats";
	}



	@RequestMapping(value = {"/tableau-bord/stats.do"}, method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_ADMIN}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	protected String  getStats(HttpServletRequest request,
	                                @ModelAttribute("statsCriteria")  IdentificationMessagesStatsCriteriaView criteriaInSession,
	                                BindingResult bindingResult,
	                                ModelMap model)  {

	if(criteriaInSession.getTypeMessage()==null && criteriaInSession.getPeriodeFiscale() == null){
		criteriaInSession = identificationMessagesStatsManager.getView();
	}
	model.put("statistiques",identificationMessagesStatsManager.calculerStats(criteriaInSession));
	return "identification/tableau-bord/stats";
	}



}
