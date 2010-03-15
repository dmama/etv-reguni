package ch.vd.uniregctb.admin.evenementExterne;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springmodules.xt.ajax.AjaxActionEvent;
import org.springmodules.xt.ajax.AjaxEvent;
import org.springmodules.xt.ajax.AjaxResponse;
import org.springmodules.xt.ajax.AjaxResponseImpl;
import org.springmodules.xt.ajax.action.ReplaceContentAction;
import org.springmodules.xt.ajax.component.Component;
import org.springmodules.xt.ajax.component.Option;
import org.springmodules.xt.ajax.component.TableRow;
import org.springmodules.xt.ajax.component.TextRenderingCallback;
import org.springmodules.xt.ajax.web.servlet.AjaxModelAndView;

import ch.vd.uniregctb.evenement.externe.EmmetteurType;
import ch.vd.uniregctb.evenement.externe.EtatEvenementExterne;
import ch.vd.uniregctb.evenement.externe.EvenementExterne;
import ch.vd.uniregctb.evenement.externe.EvenementExterneDAO;
import ch.vd.uniregctb.evenement.externe.EvenementExterneType;
import ch.vd.uniregctb.utils.UniregModeHelper;
import ch.vd.uniregctb.web.xt.AbstractEnhancedSimpleFormController;
import ch.vd.uniregctb.web.xt.component.InternalJspComponent;
import ch.vd.uniregctb.web.xt.component.SimpleText;

public class EvenementExterneController extends AbstractEnhancedSimpleFormController {

	private EvenementExterneDAO evenementExterneDAO;

	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		EvenementExterneView cmd = new EvenementExterneView();
		return cmd;
	}

	@Override
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		if (!UniregModeHelper.isTestMode()) {
			return new ModelAndView(new RedirectView("/index.do", true));
		}
		
		return super.handleRequest(request, response);
	}

	@Override
	protected ModelAndView onSubmit(Object command, BindException errors) throws Exception {
		return new AjaxModelAndView(this.getSuccessView(), errors);
	}

	public AjaxResponse emmetteursOnLoad(AjaxActionEvent event) {
		// Create a concrete ajax response:
		AjaxResponse response = new AjaxResponseImpl();

		addActionReplaceEmmetteur(event, response);
		addActionReplaceEvenement(event, response);
		return response;
	}

	public AjaxResponse emmetteursOnChange(AjaxActionEvent event) {
		// Create a concrete ajax response:
		AjaxResponse response = new AjaxResponseImpl();
		// Add the action:
		addActionReplaceEvenement(event, response);
		ReplaceContentAction action = new ReplaceContentAction("evenement.content", new SimpleText(""));
		response.addAction(action);
		return response;
	}



	public AjaxResponse evenementsOnChange(AjaxActionEvent event) {
		// Create a concrete ajax response:
		AjaxResponse response = new AjaxResponseImpl();
		// Add the action:
		String value = event.getHttpRequest().getParameter("evenements");
		if (value != null && !"".equals(value)) {
			InternalJspComponent jsp = new InternalJspComponent(event.getHttpRequest(), "/admin/evenementExterne/" + value.toLowerCase()
					+ ".do");
			// Create an ajax action for appending it:
			ReplaceContentAction action = new ReplaceContentAction("evenement.content", jsp);
			response.addAction(action);
		}
		else {
			ReplaceContentAction action = new ReplaceContentAction("evenement.content", new SimpleText(""));
			response.addAction(action);
		}
		return response;
	}

	public AjaxResponse etatsOnLoad(AjaxActionEvent event) {
		// Create a concrete ajax response:
		AjaxResponse response = new AjaxResponseImpl();
		// Add the action:
		addActionReplaceEtatEvenement(event, response);

		return response;
	}

	public AjaxResponse etatsOnChange(AjaxEvent event) {
		String etatId = event.getHttpRequest().getParameter("etats");
		AjaxResponse response = new AjaxResponseImpl();
		if (etatId == null || "".equals(etatId)) {
			return response;
		}

		EtatEvenementExterne etat = null;
		try {
			etat = EtatEvenementExterne.valueOf(etatId);
		}
		catch (Exception ex) {
			// noop
		}
		Collection<EvenementExterne> evenementExternes = null;
		if (etat == null)
			evenementExternes = this.evenementExterneDAO.getEvenementExternes(false);
		else
			evenementExternes = this.evenementExterneDAO.getEvenementExternes(false, etat);

		// Create the components to render (a list of html table rows):
		List<Component> rows = new LinkedList<Component>();
		TextRenderingCallback renderingCallback = new TextRenderingCallback() {
			private static final long serialVersionUID = 281028973463503864L;

			public Component getRenderingComponent(String text) {
				return new ch.vd.uniregctb.web.xt.component.SimpleText(text);
			}

		};
		for (EvenementExterne ev : evenementExternes) {
			TableRow row = new TableRow(ev, new String[] {
					"numeroTiers", "etat", "dateEvenement", "dateTraitement", "errorMessage", "correlationId"
			}, renderingCallback);
			rows.add(row);
		}
		// Create an ajax action for replacing the old table body content, inserting these new rows:
		ReplaceContentAction replaceRowsAction = new ReplaceContentAction("evenementsList", rows);

		response.addAction(replaceRowsAction);

		return response;
	}



	private void addActionReplaceEvenement(AjaxActionEvent event, AjaxResponse response) {
		List<Component> options = new LinkedList<Component>();

		EmmetteurType emmetteurType = getEmmetteurType(event.getHttpRequest());
		if (emmetteurType != null) {
			Option first = new Option("", "Sélectionner...");
			options.add(first);
			EvenementExterneType[] evs = EvenementExterneType.getEvenementExterneType(emmetteurType);
			for (EvenementExterneType ev : evs) {
				Option option = new Option(ev, "name", "name");
				options.add(option);
			}
		}
		else {
			Option first = new Option("", "--- ---");
			options.add(first);
		}

		// Create an ajax action for replacing the content of the "evenements" element with the components just created:
		ReplaceContentAction action = new ReplaceContentAction("evenements", options);
		response.addAction(action);
	}

	private EmmetteurType getEmmetteurType(HttpServletRequest request) {
		String value = request.getParameter("emmetteurs");
		if (value == null || "".equals(value)) {
			return null;
		}
		return EmmetteurType.valueOf(value);
	}

	private void addActionReplaceEmmetteur(AjaxActionEvent event, AjaxResponse response) {
		List<Component> options = new LinkedList<Component>();
		Option first = new Option("", "Sélectionner...");
		options.add(first);

		EmmetteurType[] emmetteurs = EmmetteurType.values();
		for (EmmetteurType emmetteur : emmetteurs) {
			Option option = new Option(emmetteur, "name", "name");
			options.add(option);
		}
		// Create an ajax action for replacing the content of the "emetteurs" element with the components just created:
		ReplaceContentAction action = new ReplaceContentAction("emmetteurs", options);
		response.addAction(action);
	}

	private void addActionReplaceEtatEvenement(AjaxActionEvent event, AjaxResponse response) {
		List<Component> options = new LinkedList<Component>();
		Option first = new Option("", "Sélectionner...");
		options.add(first);
		options.add(new Option("ALL", "Tous"));

		EtatEvenementExterne[] etats = EtatEvenementExterne.values();
		for (EtatEvenementExterne etat : etats) {
			Option option = new Option(etat, "name", "name");
			options.add(option);
		}
		// Create an ajax action for replacing the content of the "emetteurs" element with the components just created:
		ReplaceContentAction action = new ReplaceContentAction("etats", options);
		response.addAction(action);
	}


	public static class EvenementExterneView {

		private String evenementExterneType;
		private String emmetteurType;

		/**
		 * @return the evenementExterneType
		 */
		public String getEvenementExterneType() {
			return evenementExterneType;
		}

		/**
		 * @param evenementExterneType
		 *            the evenementExterneType to set
		 */
		public void setEvenementExterneType(String evenementExterneType) {
			this.evenementExterneType = evenementExterneType;
		}

		/**
		 * @return the emmetteurType
		 */
		public String getEmmetteurType() {
			return emmetteurType;
		}

		/**
		 * @param emmetteurType
		 *            the emmetteurType to set
		 */
		public void setEmmetteurType(String emmetteurType) {
			this.emmetteurType = emmetteurType;
		}

	}

	/**
	 * @param evenementExterneDAO
	 *            the evenementExterneDAO to set
	 */
	public void setEvenementExterneDAO(EvenementExterneDAO evenementExterneDAO) {
		this.evenementExterneDAO = evenementExterneDAO;
	}
}
