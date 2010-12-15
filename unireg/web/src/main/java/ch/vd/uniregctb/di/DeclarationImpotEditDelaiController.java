package ch.vd.uniregctb.di;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.delai.DelaiDeclarationView;
import ch.vd.uniregctb.di.manager.DeclarationImpotEditManager;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;

public class DeclarationImpotEditDelaiController extends AbstractDeclarationImpotController {

	private DeclarationImpotEditManager diEditManager;

	public final static String DI_ID_PARAMETER_NAME = "idDI";

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

		final Long idDi = extractLongParam(request, DI_ID_PARAMETER_NAME);

		// vérification des droits d'accès au dossier du contribuable
		final Long tiersId = diEditManager.getTiersId(idDi);
		checkAccesDossierEnEcriture(tiersId);

		return diEditManager.creerDelai(idDi);
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
		throws Exception {

		DelaiDeclarationView delaiView = (DelaiDeclarationView) command;
		final Long idDI = delaiView.getIdDeclaration();

		// vérification des droits d'accès au dossier du contribuable
		final Long tiersId = diEditManager.getTiersId(idDI);
		checkAccesDossierEnEcriture(tiersId);

		diEditManager.saveDelai(delaiView);

		return new ModelAndView("redirect:edit.do?action=editdi&id=" + idDI);
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDiEditManager(DeclarationImpotEditManager diEditManager) {
		this.diEditManager = diEditManager;
	}
}
