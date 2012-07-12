package ch.vd.uniregctb.tiers;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.manager.TiersEditManager;
import ch.vd.uniregctb.tiers.view.TiersEditView;
import ch.vd.uniregctb.utils.RegDateEditor;

/**
 * Controller qui permet de créer un nouveau contribuable inconnu au registre des habitants ou inconnu au registre des PMs.
 */
public class TiersCreateController extends AbstractTiersController {

	protected final Logger LOGGER = Logger.getLogger(TiersCreateController.class);

	public final static String BUTTON_SAVE = "__confirmed_save";
	public final static String BUTTON_ANNULER_TIERS = "annulerTiers";

	public final static String NUMERO_CTB_ASSOCIE_PARAMETER_NAME = "numeroCtbAss";

	private TiersEditManager tiersEditManager;

	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		TiersEditView tiersView = new TiersEditView();

		String idParam = request.getParameter(TIERS_ID_PARAMETER_NAME);
		if (idParam != null) {
			Long id = Long.parseLong(idParam);
			if (!"".equals(idParam)) {
				//gestion des droits d'édition d'un tier par tiersEditManager
				tiersView = tiersEditManager.getView(id);

				// vérification des droits d'accès au dossier du contribuable
				checkAccesDossierEnLecture(id);
			}
		}
		else {
			final String natureParamString = request.getParameter(TIERS_NATURE_PARAMETER_NAME);
			final NatureTiers natureParam = NatureTiers.valueOf(natureParamString);
			if (natureParam == NatureTiers.NonHabitant) {
				//vérifier les droits de création d'un non habitant
				if(SecurityProvider.isGranted(Role.CREATE_NONHAB)){
					tiersView = tiersEditManager.creePersonne();
					tiersView.setAllowed(true);
				}
				else {
					tiersView.setAllowed(false);
				}
			}
			else if (natureParam == NatureTiers.AutreCommunaute) {
				//vérifier les droits de création d'une autre communauté
				if(SecurityProvider.isGranted(Role.CREATE_AC)){
					tiersView = tiersEditManager.creeOrganisation();
					tiersView.setAllowed(true);
				}
				else {
					tiersView.setAllowed(false);
				}
			}
			else if (natureParam == NatureTiers.DebiteurPrestationImposable) {
				String numeroCtbAssParam = request.getParameter(NUMERO_CTB_ASSOCIE_PARAMETER_NAME);
				if (numeroCtbAssParam != null) {
					//vérifier les droits de création d'un DPI
					if(SecurityProvider.isGranted(Role.CREATE_DPI)){
						Long numeroCtbAss = Long.parseLong(numeroCtbAssParam);
						tiersView = tiersEditManager.creeDebiteur(numeroCtbAss);
						this.setModified(true);
						tiersView.setAllowed(true);
					}
					else {
						tiersView.setAllowed(false);
					}
				}
			}
		}

		return tiersView;
	}

	@Override
	protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) throws ServletException {
		super.initBinder(request, binder);

		// on doit autoriser les dates partielles sur la date de naissance des tiers
		binder.registerCustomEditor(RegDate.class, "tiers.dateNaissance", new RegDateEditor(true, true, false));
	}

	@Override
	protected boolean suppressValidation(HttpServletRequest request, Object command, BindException errors) {
		if (getTarget() != null ||
				request.getParameter(BUTTON_ANNULER_TIERS) != null ||
				request.getParameter(BUTTON_BACK_TO_LIST) != null ||
				request.getParameter(ACTION_COMMON_REFRESH) != null ||
				request.getParameter(BUTTON_BACK_TO_VISU) != null) {
			return true;
		}
		return super.suppressValidation(request, command, errors);
	}

	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {
		TiersEditView bean = (TiersEditView) command;
		checkAccesDossierEnEcriture(bean.getTiers().getId());

		if (request.getParameter(BUTTON_SAVE) != null) {
			this.setModified(false);
			Tiers tierSaved = tiersEditManager.save(bean);
			LOGGER.info("Tiers saved : numero tiers :" + tierSaved.getId());
			return new ModelAndView("redirect:visu.do?id=" + tierSaved.getId());
		} // button retour liste
		else if (request.getParameter(BUTTON_BACK_TO_LIST) != null) {
			return new ModelAndView("redirect:list.do");
		} // button retour visualisation
		else if (request.getParameter(BUTTON_BACK_TO_VISU) != null) {
			return new ModelAndView("redirect:visu.do?id=" + bean.getTiers().getNumero());
		}
		tiersEditManager.refresh(bean,  bean.getTiers().getNumero());
		return showForm(request, response, errors);
	}

	public void setTiersEditManager(TiersEditManager tiersEditManager) {
		this.tiersEditManager = tiersEditManager;
	}
}
