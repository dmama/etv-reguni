package ch.vd.uniregctb.tiers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.tiers.manager.TiersEditManager;
import ch.vd.uniregctb.tiers.view.DebiteurEditView;

public class DebiteurEditController extends AbstractTiersController {

	private TiersEditManager tiersEditManager;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersEditManager(TiersEditManager tiersEditManager) {
		this.tiersEditManager = tiersEditManager;
	}

	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		final String idParam = request.getParameter(TIERS_ID_PARAMETER_NAME);
		if (StringUtils.isBlank(idParam)) {
			throw new IllegalArgumentException("Le paramètre 'id' doit être renseigné.");
		}
		final Long id = Long.parseLong(idParam);
		checkAccesDossierEnLecture(id);
		return tiersEditManager.getDebiteurEditView(id);
	}

	@Override
	protected Map<String, Object> referenceData(HttpServletRequest request) throws Exception {
		final Map<String, Object> data = super.referenceData(request);
		data.put(LIBELLE_LOGICIEL, tiersMapHelper.getAllLibellesLogiciels());
		return data;
	}

	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) throws Exception {
		final DebiteurEditView bean = (DebiteurEditView) command;
		checkAccesDossierEnEcriture(bean.getId());
		setModified(false);
		tiersEditManager.save(bean);
		return new ModelAndView("redirect:../tiers/visu.do?id=" + bean.getId());
	}

}
