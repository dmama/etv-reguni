package ch.vd.uniregctb.di;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.di.view.DiView;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;

/**
 * Controller Spring 3 pour la gestion des déclarations d'impôt ordinaires (à compléter lors des divers refactoring)
 */
@Controller
public class DeclarationImpotController {

	private DeclarationImpotOrdinaireDAO diDAO;
	private MessageSource messageSource;

	public void setDiDAO(DeclarationImpotOrdinaireDAO diDAO) {
		this.diDAO = diDAO;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	/**
	 * @param id l'id de la déclaration d'impôt ordinaire
	 * @return les détails d'une déclaration d'impôt au format JSON
	 */
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/di/details.do", method = RequestMethod.GET)
	@ResponseBody
	public DiView details(@RequestParam("id") long id) throws AccessDeniedException {

		if (!SecurityProvider.isGranted(Role.VISU_ALL) && !SecurityProvider.isGranted(Role.VISU_LIMITE)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec de consultation pour l'application Unireg");
		}

		final DeclarationImpotOrdinaire di = diDAO.get(id);
		if (di == null) {
			return null;
		}

		// vérification des droits en lecture
		final Long tiersId = di.getTiers().getId();
		ControllerUtils.checkAccesDossierEnLecture(tiersId);

		return new DiView(di, messageSource);
	}
}
