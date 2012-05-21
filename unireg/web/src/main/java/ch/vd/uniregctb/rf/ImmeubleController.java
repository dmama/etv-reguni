package ch.vd.uniregctb.rf;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;

@Controller
@RequestMapping(value = "/rf/immeuble")
public class ImmeubleController {

	private ImmeubleDAO immeubleDAO;
	private MessageSource messageSource;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setImmeubleDAO(ImmeubleDAO immeubleDAO) {
		this.immeubleDAO = immeubleDAO;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	/**
	 * Retourne le nombre d'immeubles d'un contribuable sous forme JSON (voir http://blog.springsource.com/2010/01/25/ajax-simplifications-in-spring-3-0/)
	 *
	 * @param ctbId le numéro de contribuable
	 * @return le nombre d'immeubles du contribuable spécifié.
	 * @throws ch.vd.uniregctb.security.AccessDeniedException
	 *          si l'utilisateur ne possède les droits de visualisation suffisants.
	 */
	@RequestMapping(value = "/count.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@ResponseBody
	public int count(@RequestParam("ctb") long ctbId) throws AccessDeniedException {
		return immeubleDAO.count(ctbId);
	}

	/**
	 * Retourne les informations des immeubles d'un contribuable, page par page.
	 *
	 * @param ctbId    le numéro de contribuable
	 * @param page     le numéro de page courant (1-based)
	 * @param pageSize le nombre d'immeubles affichés par page
	 * @return les immeubles de la page demandée au format JSON
	 * @throws ch.vd.uniregctb.security.AccessDeniedException
	 *          si l'utilisateur ne possède les droits de visualisation suffisants.
	 */
	@ResponseBody
	@RequestMapping(value = "/list.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public ImmeublesPage list(@RequestParam("ctb") long ctbId,
	                          @RequestParam(value = "page", required = false, defaultValue = "1") int page,
	                          @RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize) throws AccessDeniedException {

		if (!SecurityProvider.isAnyGranted(Role.VISU_ALL, Role.VISU_IMMEUBLES)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec pour visualiser les immeubles d'un contribuable");
		}

		ControllerUtils.checkAccesDossierEnLecture(ctbId);

		final int totalCount = immeubleDAO.count(ctbId);

		final List<ImmeubleView> views = new ArrayList<ImmeubleView>();
		final ParamPagination pagination = new ParamPagination(page, pageSize, null, true);
		final List<Immeuble> list = immeubleDAO.find(ctbId, pagination);
		for (Immeuble immeuble : list) {
			views.add(new ImmeubleView(immeuble, messageSource));
		}

		return new ImmeublesPage(views, page, totalCount);
	}
}
