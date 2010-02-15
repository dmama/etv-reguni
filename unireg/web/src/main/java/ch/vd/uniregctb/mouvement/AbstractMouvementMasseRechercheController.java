package ch.vd.uniregctb.mouvement;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableLong;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.mouvement.view.MouvementDetailView;
import ch.vd.uniregctb.mouvement.view.MouvementMasseCriteriaView;

/**
 * Classe de base pour les controlleurs des mouvements de dossier en masse qui
 * comportent une partie de recherche paramétrable
 */
public abstract class AbstractMouvementMasseRechercheController extends AbstractMouvementMasseController {

	protected static final String EFFACER = "effacer";

	private static final int PAGE_SIZE = 25;

	private static final String TABLE_ID = "mvt";

	protected void doFind(HttpServletRequest request, MouvementMasseCriteriaView view) throws InfrastructureException {
		final WebParamPagination pagination = getParamPagination(request);
		final MutableLong total = new MutableLong(0);
		final Integer noCollAdmInitiatrice = getNoCollAdmFiltree();
		final List<MouvementDetailView> results = getMouvementManager().find(view, noCollAdmInitiatrice, pagination, total);
		view.setResults(results);
		if (total.longValue() > Integer.MAX_VALUE) {
			view.setResultSize(Integer.MAX_VALUE);
		}
		else {
			view.setResultSize(total.intValue());
		}
	}

	protected static WebParamPagination getParamPagination(HttpServletRequest request) {
		final WebParamPagination param = new WebParamPagination(request, TABLE_ID, PAGE_SIZE);
		if (StringUtils.isBlank(param.getChamp())) {
			param.setChamp("contribuable.numero");
			param.setSensAscending(true);
		}
		return param;
	}

	@Override
	protected MouvementMasseCriteriaView formBackingObject(HttpServletRequest request) throws Exception {
		// dans la mesure où on arrive parfois par un GET (demande de re-pagination...)
		// on regarde s'il existe déjà une form à réutiliser dans la session
		final MouvementMasseCriteriaView existingView = (MouvementMasseCriteriaView) request.getSession().getAttribute(getFormSessionAttributeName());
		if (existingView != null) {
			// il faut refaire la recherche...
			if (existingView.getResults() != null) {
				doFind(request, existingView);
			}
			return existingView;
		}
		else {
			final MouvementMasseCriteriaView view = (MouvementMasseCriteriaView) super.formBackingObject(request);
			initView(view);
			return view;
		}
	}

	private void initView(MouvementMasseCriteriaView view) {
		final Integer noCollAdmInitiatrice = getNoCollAdmFiltree();
		view.init(noCollAdmInitiatrice == null);
	}

	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) throws Exception {
		final MouvementMasseCriteriaView view = (MouvementMasseCriteriaView) command;
		final String effacer = request.getParameter(EFFACER);
		if (effacer != null) {
			initView(view);
		}
		else {
			doFind(request, view);
		}
		return showForm(request, response, errors);
	}
}
