package ch.vd.uniregctb.norentes;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springmodules.xt.ajax.AjaxEvent;
import org.springmodules.xt.ajax.AjaxResponse;
import org.springmodules.xt.ajax.AjaxResponseImpl;
import org.springmodules.xt.ajax.AjaxSubmitEvent;
import org.springmodules.xt.ajax.action.ExecuteJavascriptFunctionAction;
import org.springmodules.xt.ajax.action.RedirectAction;
import org.springmodules.xt.ajax.action.ReplaceContentAction;
import org.springmodules.xt.ajax.component.Component;
import org.springmodules.xt.ajax.component.Option;

import ch.vd.uniregctb.norentes.common.NorentesFactory;
import ch.vd.uniregctb.norentes.common.NorentesManager;
import ch.vd.uniregctb.norentes.common.NorentesScenario;
import ch.vd.uniregctb.norentes.common.ScenarioEtat;
import ch.vd.uniregctb.norentes.common.NorentesContext.EtapeContext;
import ch.vd.uniregctb.norentes.webcontrols.ControlScenario;
import ch.vd.uniregctb.norentes.webcontrols.Toolbar;
import ch.vd.uniregctb.norentes.webcontrols.ToolbarButton;
import ch.vd.uniregctb.norentes.webcontrols.ToolbarSeparator;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.utils.UniregModeHelper;
import ch.vd.uniregctb.web.xt.AbstractEnhancedSimpleFormController;
import ch.vd.uniregctb.web.xt.component.SimpleText;

public class NorentesController extends AbstractEnhancedSimpleFormController {

	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		return new NorentesBean();
	}

	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {
		return showForm(request, response, errors);
	}

	@Override
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		if (!UniregModeHelper.isTestMode()) {
			return new ModelAndView(new RedirectView("/index.do", true));
		}
		
		return super.handleRequest(request, response);
	}
	
	public AjaxResponse buttonStartEtapeOnClick(AjaxSubmitEvent event, AjaxResponse response) {
		NorentesBean bean = (NorentesBean) event.getCommandObject();
		final int etapeIndex = Integer.parseInt(event.getParameters().get("etapeIndex"));
		final NorentesManager norentesManager = NorentesFactory.getNorentesManager();
		final NorentesScenario currentNorentesScenario = norentesManager.getScenario(bean.getCurrentScenarioName());
		if (currentNorentesScenario != null && currentNorentesScenario.getEtapeAttributes().size() > 0) {
			try {
				Thread thread = new Thread(new Runnable() {

					public void run() {
						norentesManager.runToStep(currentNorentesScenario, etapeIndex);
					}

				});
				thread.run();
			}
			catch (Exception e) {
				ReplaceContentAction action = new ReplaceContentAction("scenario.content", new SimpleText(e.getMessage()));
				response.addAction(action);
				throw new RuntimeException(e);
			}
		}
		addActionReplaceContentScenario(event, currentNorentesScenario, response);
		return response;
	}

	public AjaxResponse buttonStopOnClick(AjaxSubmitEvent event, AjaxResponse response) {
		NorentesManager norentesManager = NorentesFactory.getNorentesManager();
		try {
			norentesManager.closeCurrentScenario();
		}
		catch (Exception e) {
		}
		response.addAction(new RedirectAction(this.getSuccessView(), Collections.emptyMap()) );
		return response;
	}


	public AjaxResponse currentEvenementCivilOnChange(AjaxSubmitEvent event, AjaxResponse response) {
		NorentesBean bean = (NorentesBean) event.getCommandObject();
		TypeEvenementCivil evenementCivil = bean.getCurrentEvenementCivil();
		addActionReplaceScenaries(evenementCivil, null, response);
		addActionReplaceContentScenario(event, null, response);
		return response;
	}

	public AjaxResponse currentEvenementCivilOnLoad(AjaxEvent event) {
		AjaxResponse response = new AjaxResponseImpl();
		NorentesManager manager = NorentesFactory.getNorentesManager();
		NorentesScenario scenarioSelected = manager.getCurrentScenario();
		TypeEvenementCivil currentEvenementCivil = null;
		String scenarioName = null;
		if (scenarioSelected != null) {
			currentEvenementCivil = scenarioSelected.geTypeEvenementCivil();
			scenarioName = scenarioSelected.getName();
			// execution periodique pour rafraichissement de la liste des scénaries
			if ( event.getParameters().get("periodical") != null) {
				int lastEtape = Integer.parseInt(event.getParameters().get("lastEtape") );
				EtapeContext context = manager.getCurrentEtapeContext();
				if ( lastEtape == context.getIndex() && (ScenarioEtat.Finish == context.getState() || ScenarioEtat.InError == context.getState())){
					response.addAction( new ExecuteJavascriptFunctionAction("stopPeriodical", null));
				}
			}
		}
		addActionReplaceEvenementCivil(currentEvenementCivil, response);
		addActionReplaceScenaries(currentEvenementCivil, scenarioName, response);
		addActionReplaceContentScenario(event, scenarioSelected, response);
		return response;
	}

	public AjaxResponse currentScenarioNameOnChange(AjaxSubmitEvent event, AjaxResponse response) {
		NorentesManager norentesManager = NorentesFactory.getNorentesManager();
		NorentesBean bean = (NorentesBean) event.getCommandObject();
		NorentesScenario currentNorentesScenario = norentesManager.getScenario(bean.getCurrentScenarioName());
		addActionReplaceContentScenario(event, currentNorentesScenario, response);
		return response;
	}

	private void addActionReplaceEvenementCivil(TypeEvenementCivil currentEvenementCivil, AjaxResponse response) {
		List<Component> options = new LinkedList<Component>();
		TypeEvenementCivil[] evts = NorentesFactory.getNorentesManager().getEvenementCivilsUsedForTest();
		if (evts.length > 0) {
			Option first = new Option("", "Sélectionner...");
			options.add(first);
			for (TypeEvenementCivil evt : evts) {
				Option option = new Option(evt, "name", "fullDescription");
				if (evt.equals(currentEvenementCivil)) {
					option.addAttribute("selected", "selected");
				}
				options.add(option);
			}
		}
		else {
			Option first = new Option("", "--- ---");
			options.add(first);
		}

		// Create an ajax action for replacing the content of the "evenements" element with the components just created:
		ReplaceContentAction action = new ReplaceContentAction("currentEvenementCivil", options);
		response.addAction(action);
	}

	private void addActionReplaceToolBar(final HttpServletRequest request, final NorentesScenario currentNorentesScenario,
			AjaxResponse response) {

		final NorentesManager norentesManager = NorentesFactory.getNorentesManager();
		Toolbar toolbar = new Toolbar();
		boolean enabledRefresh = (norentesManager.getCurrentScenario() != null && norentesManager.getCurrentScenario() != currentNorentesScenario) ;
		toolbar.getControls().add( createButton(request, "buttonRefresh", "refresh", enabledRefresh, "javascript:window.location.reload();", "Refresh", "Refresh"));
		toolbar.getControls().add( new ToolbarSeparator());
		boolean enabledStop = norentesManager.getCurrentScenario() != null  ;
		toolbar.getControls().add( createButton(request, "buttonStop", "stop", enabledStop, "javascript:Form.doAjaxSubmitPostBack('theForm', 'OnClick', 'buttonStop');", "Stop", "Arrête le scénario courant"));
		toolbar.getControls().add( new ToolbarSeparator());

		ReplaceContentAction action = new ReplaceContentAction("buttonStart.div", toolbar);
		response.addAction(action);
	}

	private ToolbarButton createButton(HttpServletRequest request, String id, String image, boolean enabled, String link, String labelText, String tooltip) {
		ToolbarButton button = new ToolbarButton();
		button.setEnabled( enabled);
		button.setTitle(tooltip);
		button.setText(labelText);
		button.setId(id);
		button.setLinkUrl(link);
		button.setImageUrl(request.getContextPath() + "/images/"+image+"-tiny.png");
		button.setImageDisabledUrl(request.getContextPath() + "/images/"+image+"-disabled-tiny.png");
		return button;
	}


	private void addActionReplaceScenaries(TypeEvenementCivil currentEvenementCivil, String scenarioName, AjaxResponse response) {
		List<Component> options = new LinkedList<Component>();
		Collection<NorentesScenario> scenaries = NorentesFactory.getNorentesManager().getScenaries(currentEvenementCivil);
		if (!scenaries.isEmpty()) {
			Option first = new Option("", "Sélectionner...");
			options.add(first);
			for (NorentesScenario scenario : scenaries) {
				Option option = new Option(scenario, "name", "description");
				if (scenario.getName().equals(scenarioName)) {
					option.addAttribute("selected", "selected");
				}
				options.add(option);
			}
		}
		else {
			Option first = new Option("", "--- ---");
			options.add(first);
		}

		// Create an ajax action for replacing the content of the "evenements" element with the components just created:
		ReplaceContentAction action = new ReplaceContentAction("currentScenarioName", options);
		response.addAction(action);
	}

	private void addActionReplaceContentScenario(AjaxEvent event, NorentesScenario currentNorentesScenario, AjaxResponse response) {
		addActionReplaceToolBar(event.getHttpRequest(), currentNorentesScenario, response);
		if (currentNorentesScenario == null) {
			ReplaceContentAction action = new ReplaceContentAction("scenario.content", new SimpleText(""));
			response.addAction(action);
		}
		else {
			ControlScenario ctrl = new ControlScenario(event.getHttpRequest());
			ctrl.setScenario(currentNorentesScenario);
			ReplaceContentAction action = new ReplaceContentAction("scenario.content", ctrl);
			response.addAction(action);
		}
	}
}
