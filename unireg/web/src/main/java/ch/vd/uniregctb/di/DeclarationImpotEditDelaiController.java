package ch.vd.uniregctb.di;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.di.manager.DeclarationImpotEditManager;
import ch.vd.uniregctb.di.view.DelaiDeclarationView;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;

public class DeclarationImpotEditDelaiController extends AbstractDeclarationImpotController {

	private DeclarationImpotEditManager diEditManager;

	public final static String DI_ID_PARAMETER_NAME = "idDI";

	public final static String TARGET_AJOUTER_DELAI = "ajouter";

	public final static String TARGET_IMPRIMER_DELAI = "imprimer";


	protected static final Logger LOGGER = Logger.getLogger(DeclarationImpotEditDelaiController.class);

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		//pour le moment seuls les PP sont éditables
		if (!SecurityProvider.isGranted(Role.DI_DELAI_PP)) {
			throw new AccessDeniedException("vous n'avez pas le droit d'ajouter un delai à une DI");
		}

		final Long idDi = extractLongParam(request, DI_ID_PARAMETER_NAME);

		// vérification des droits d'accès au dossier du contribuable
		final Long tiersId = diEditManager.getTiersId(idDi);
		checkAccesDossierEnEcriture(tiersId);

		return diEditManager.creerDelai(idDi);
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {

		final DelaiDeclarationView delaiDeclarationView = (DelaiDeclarationView) command;
		final Long idDI = delaiDeclarationView.getIdDeclaration();
		if (getTarget() != null && TARGET_AJOUTER_DELAI.equals(getTarget())) {
			sauverDelai(delaiDeclarationView);

		}
		else if (getTarget() != null && TARGET_IMPRIMER_DELAI.equals(getTarget())) {
			final Long idDelai = sauverDelai(delaiDeclarationView);
			final DelaiDeclarationView delaiSauve = diEditManager.getDelaiView(idDelai);
			return imprimerDelai(response, delaiSauve, errors);
		}

		return new ModelAndView("redirect:edit.do?action=editdi&id=" + idDI);

	}

	private Long sauverDelai(DelaiDeclarationView command) throws AccessDeniedException {
		DelaiDeclarationView delaiView = (DelaiDeclarationView) command;
		final Long idDI = delaiView.getIdDeclaration();

		// vérification des droits d'accès au dossier du contribuable
		final Long tiersId = diEditManager.getTiersId(idDI);
		checkAccesDossierEnEcriture(tiersId);

		return diEditManager.saveDelai(delaiView);

	}

	private ModelAndView imprimerDelai(HttpServletResponse response, final DelaiDeclarationView bean, BindException errors) throws Exception {

		final TraitementRetourEditique inbox = new TraitementRetourEditique() {
			@Override
			public ModelAndView doJob(EditiqueResultat resultat) {
				return new ModelAndView("redirect:edit.do?action=editdi&id=" + bean.getIdDeclaration());
			}
		};

		final Long idDelai = bean.getId();
		final TraitementRetourEditique erreurTimeout = new ErreurGlobaleCommunicationEditique(errors);
		final EditiqueResultat resultat = diEditManager.envoieImpressionLocalConfirmationDelai(bean.getIdDeclaration(), idDelai);
		final ModelAndView mav = traiteRetourEditique(resultat, response, "delai", inbox, erreurTimeout, erreurTimeout);
		if (mav == null && bean.getId() != null) {
		}
		return mav;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDiEditManager(DeclarationImpotEditManager diEditManager) {
		this.diEditManager = diEditManager;
	}
}
