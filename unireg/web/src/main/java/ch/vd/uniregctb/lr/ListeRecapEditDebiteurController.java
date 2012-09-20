package ch.vd.uniregctb.lr;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.lr.manager.ListeRecapEditManager;
import ch.vd.uniregctb.lr.view.ListeRecapListView;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;

public class ListeRecapEditDebiteurController extends AbstractListeRecapController{

	private ListeRecapEditManager lrEditManager;

	/**
	 * Le nom du parametre utilise dans la request.
	 */
	public final static String NUMERO_DEBITEUR_PARAMETER_NAME = "numero";

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		ListeRecapListView  lrListView = null;
		String idParam = request.getParameter(NUMERO_DEBITEUR_PARAMETER_NAME);
		if (idParam != null) {
			Long id = Long.parseLong(idParam);
			if (idParam != null && !"".equals(idParam)) {
				if(SecurityProvider.isGranted(Role.LR)){
					lrListView = lrEditManager.findByNumero(id);
				}
				else {
					throw new AccessDeniedException("vous n'avez pas le droit d'accéder aux listes récapitulatives pour l'application Unireg");
				}
			}
		}

		return lrListView;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse,
	 *      org.springframework.validation.BindException, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors, Map model) throws Exception {
		ModelAndView mav = super.showForm(request, response, errors, model);
		return mav;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.BaseCommandController#onBindAndValidate(javax.servlet.http.HttpServletRequest,
	 *      java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected void onBindAndValidate(HttpServletRequest request, Object command, BindException errors) throws Exception {
		super.onBindAndValidate(request, command, errors);
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
		throws Exception {
		//bouton ajouter une lr
		super.getFormSessionAttributeName();
		ListeRecapListView lrListView = (ListeRecapListView) command;
		return new ModelAndView( new RedirectView("/lr/edit.do?numero=" + lrListView.getDpi().getNumero(), true));
	}

	public ListeRecapEditManager getLrEditManager() {
		return lrEditManager;
	}

	public void setLrEditManager(ListeRecapEditManager lrEditManager) {
		this.lrEditManager = lrEditManager;
	}

}
