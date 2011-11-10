package ch.vd.uniregctb.mouvement;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import ch.vd.uniregctb.common.AbstractSimpleFormEditiqueAwareController;

public class AbstractMouvementController extends AbstractSimpleFormEditiqueAwareController {

	private MouvementMapHelper mouvementMapHelper;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setMouvementMapHelper(MouvementMapHelper mouvementMapHelper) {
		this.mouvementMapHelper = mouvementMapHelper;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Map<String, Object> referenceData(HttpServletRequest request) throws Exception {
		return mouvementMapHelper.getMaps();
	}
}
