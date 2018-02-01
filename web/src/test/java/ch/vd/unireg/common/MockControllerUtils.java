package ch.vd.unireg.common;

import javax.servlet.http.HttpServletRequest;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.security.AccessDeniedException;

/**
 * Mock du contrôleur qui ne fait aucun test par défaut.
 */
public class MockControllerUtils implements ControllerUtils {
	@Override
	public void checkAccesDossierEnLecture(Long tiersId) throws ObjectNotFoundException, AccessDeniedException {

	}

	@Override
	public void checkAccesDossierEnEcriture(Long tiersId) throws ObjectNotFoundException, AccessDeniedException {

	}

	@Override
	public String getDisplayTagRequestParametersForPagination(HttpServletRequest request, String tableName) {
		throw new NotImplementedException();
	}

	@Override
	public String getDisplayTagRequestParametersForPagination(String tableName, ParamPagination pagination) {
		throw new NotImplementedException();
	}

	@Override
	public void checkTraitementContribuableAvecDecisionAci(Long tiersId) throws ObjectNotFoundException, AccessDeniedException {
	}
}
