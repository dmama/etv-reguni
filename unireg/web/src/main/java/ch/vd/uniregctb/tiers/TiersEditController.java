package ch.vd.uniregctb.tiers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.rapport.manager.RapportEditManager;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.manager.AdresseManager;
import ch.vd.uniregctb.tiers.manager.ForFiscalManager;
import ch.vd.uniregctb.tiers.manager.SituationFamilleManager;
import ch.vd.uniregctb.tiers.manager.TiersEditManager;
import ch.vd.uniregctb.tiers.view.TiersEditView;

/**
 * Controller spring permettant la visualisation ou la saisie d'une objet metier donne.
 *
 * @author <a href="mailto:akram.ben-aissi@vd.ch">Akram BEN AISSI</a>
 */
public class TiersEditController extends AbstractTiersController {

	public final static String BUTTON_SAVE = "__confirmed_save";
	public final static String BUTTON_ANNULER_TIERS = "annulerTiers";

	public final static String NUMERO_CTB_ASSOCIE_PARAMETER_NAME = "numeroCtbAss";

	private TiersEditManager tiersEditManager;
	private ForFiscalManager forFiscalManager;
	private RapportEditManager rapportEditManager;
	private AdresseManager adresseManager;
	private SituationFamilleManager situationFamilleManager;

	/**
	 * Un LOGGER.
	 */
	protected final Logger LOGGER = Logger.getLogger(TiersEditController.class);

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
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



	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
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

	public TiersEditManager getTiersEditManager() {
		return tiersEditManager;
	}

	public void setTiersEditManager(TiersEditManager tiersEditManager) {
		this.tiersEditManager = tiersEditManager;
	}

	public ForFiscalManager getForFiscalManager() {
		return forFiscalManager;
	}

	public void setForFiscalManager(ForFiscalManager forFiscalManager) {
		this.forFiscalManager = forFiscalManager;
	}

	public RapportEditManager getRapportEditManager() {
		return rapportEditManager;
	}

	public void setRapportEditManager(RapportEditManager rapportEditManager) {
		this.rapportEditManager = rapportEditManager;
	}

	public AdresseManager getAdresseManager() {
		return adresseManager;
	}

	public void setAdresseManager(AdresseManager adresseManager) {
		this.adresseManager = adresseManager;
	}

	public SituationFamilleManager getSituationFamilleManager() {
		return situationFamilleManager;
	}

	public void setSituationFamilleManager(SituationFamilleManager situationFamilleManager) {
		this.situationFamilleManager = situationFamilleManager;
	}

}
