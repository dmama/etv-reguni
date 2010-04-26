package ch.vd.uniregctb.mouvement;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.mouvement.manager.MouvementEditManager;
import ch.vd.uniregctb.mouvement.view.MouvementDetailView;
import ch.vd.uniregctb.tache.manager.TacheListManager;

public class MouvementEditController extends AbstractMouvementController {

	protected static final Logger LOGGER = Logger.getLogger(MouvementEditController.class);

	private MouvementEditManager mouvementEditManager;
	private TacheListManager tacheListManager;

	public void setMouvementEditManager(MouvementEditManager mouvementEditManager) {
		this.mouvementEditManager = mouvementEditManager;
	}

	public void setTacheListManager(TacheListManager tacheListManager) {
		this.tacheListManager = tacheListManager;
	}

	/**
	 * Le nom du parametre utilise dans la request.
	 */
	private final static String CONTRIBUABLE_ID_PARAMETER_NAME = "numero";
	private final static String TACHE_ID_TRAITE_PARAM = "idTacheTraite";

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		final String idCtbParam = request.getParameter(CONTRIBUABLE_ID_PARAMETER_NAME);
		final String idTacheTraiteParam = request.getParameter(TACHE_ID_TRAITE_PARAM);
		MouvementDetailView mvtDetailView = null;

		if (idCtbParam != null) {
			final Long idCtb = Long.parseLong(idCtbParam);
			checkAccesDossierEnLecture(idCtb);
			if (!StringUtils.isEmpty(idTacheTraiteParam)) {
				final Long idTache = Long.parseLong(idTacheTraiteParam);
				mvtDetailView = mouvementEditManager.creerMvtForTacheTransmissionDossier(idCtb, idTache);
			}
			else {
				mvtDetailView = mouvementEditManager.creerMvt(idCtb);
			}
		}

		return mvtDetailView;
	}


	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {

		MouvementDetailView bean = (MouvementDetailView) command;
		checkAccesDossierEnEcriture(bean.getContribuable().getNumero());

		mouvementEditManager.save(bean);
		this.setModified(false);
		if (bean.getIdTache() == null) {
			return new ModelAndView("redirect:edit-contribuable.do?numero=" + bean.getContribuable().getNumero());
		}
		else {
			tacheListManager.traiteTache(bean.getIdTache());
			return new ModelAndView("redirect:../tache/list.do");
		}
	}


}
