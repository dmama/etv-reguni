package ch.vd.uniregctb.rf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;

@Controller
@RequestMapping(value = "/rf/immeuble")
public class ImmeubleController {

	private ImmeubleDAO immeubleDAO;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setImmeubleDAO(ImmeubleDAO immeubleDAO) {
		this.immeubleDAO = immeubleDAO;
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
	 * Affiche les immeubles d'un contribuable.
	 *
	 * @param ctbId le numéro de contribuable
	 * @param mav   le modèle sous-jacent
	 * @return le nom de la jsp qui affiche la liste retournée
	 * @throws ch.vd.uniregctb.security.AccessDeniedException
	 *          si l'utilisateur ne possède les droits de visualisation suffisants.
	 */
	@RequestMapping(value = "/list.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String list(@RequestParam("ctb") long ctbId, Model mav) throws AccessDeniedException {

		if (!SecurityProvider.isGranted(Role.VISU_ALL) && !SecurityProvider.isGranted(Role.VISU_IMMEUBLES)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec pour visualiser les immeubles d'un contribuable");
		}

		ControllerUtils.checkAccesDossierEnLecture(ctbId);

		final List<ImmeubleView> views = new ArrayList<ImmeubleView>();
		final List<Immeuble> list = immeubleDAO.find(ctbId);
		for (Immeuble immeuble : list) {
			views.add(new ImmeubleView(immeuble));
		}

		// // [SIFISC-3309] on trie par nom de commune croissant
		Collections.sort(views, new Comparator<ImmeubleView>() {
			@Override
			public int compare(ImmeubleView o1, ImmeubleView o2) {
				return o1.getNomCommune().compareTo(o2.getNomCommune());
			}
		});

		mav.addAttribute("immeubles", views);

		return "rf/immeuble/list";
	}

}
