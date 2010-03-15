package ch.vd.uniregctb.di;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.delai.DelaiDeclarationView;
import ch.vd.uniregctb.di.manager.DeclarationImpotEditManager;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;

public class DeclarationImpotEditDelaiController extends AbstractDeclarationImpotController {

	private DeclarationImpotEditManager diEditManager;

	public final static String DI_ID_PARAMETER_NAME = "idDI";
	public final static String DI_ID = "idDI";

	protected static final Logger LOGGER = Logger.getLogger(DeclarationImpotEditDelaiController.class);
	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		//pour le moment seuls les PP sont éditables
		if(!SecurityProvider.isGranted(Role.DI_DELAI_PP)){
			throw new AccessDeniedException("vous n'avez pas le droit d'ajouter un delai à une DI");
		}

		Long idDi = extractLongParam(request, DI_ID_PARAMETER_NAME);

		DelaiDeclarationView  delaiView = diEditManager.creerDelai(idDi);

		// vérification des droits d'accès au dossier du contribuable
		DeclarationImpotOrdinaire di = getDiDAO().get(delaiView.getIdDeclaration());
		checkAccesDossierEnEcriture(di.getTiers().getNumero());

		return delaiView;
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
		ModelAndView mav = super.onSubmit(request, response, command, errors);

		DelaiDeclarationView delaiView = (DelaiDeclarationView) command;

		// vérification des droits d'accès au dossier du contribuable
		DeclarationImpotOrdinaire di = getDiDAO().get(delaiView.getIdDeclaration());
		checkAccesDossierEnEcriture(di.getTiers().getNumero());

		diEditManager.saveDelai(delaiView);

		return mav;
	}

	public DeclarationImpotEditManager getDiEditManager() {
		return diEditManager;
	}

	public void setDiEditManager(DeclarationImpotEditManager diEditManager) {
		this.diEditManager = diEditManager;
	}


}
