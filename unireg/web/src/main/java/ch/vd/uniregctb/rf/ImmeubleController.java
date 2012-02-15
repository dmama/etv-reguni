package ch.vd.uniregctb.rf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
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
	
	protected static final Comparator<String> NO_IMMEUBLE_COMPARATOR = new Comparator<String>() {
		@Override
		public int compare(String o1, String o2) {
			final int[] d1 = decompose(o1);
			final int[] d2 = decompose(o2);
			final int maxIndex = Math.min(d1.length, d2.length);
			int comparison = 0;
			for (int i = 0 ; i < maxIndex ; ++ i) {
				comparison = d1[i] - d2[i];
				if (comparison != 0) {
					break;
				}
			}
			// en cas d'égalité jusque là, le plus court passe devant
			return comparison == 0 ? d1.length - d2.length : comparison;
		}
		
		private int[] EMPTY = new int[0];
		
		private int[] decompose(String noImmeuble) {
			final String[] strs = StringUtils.isNotBlank(noImmeuble) ? noImmeuble.split("[^0-9]") : null;
			if (strs != null && strs.length > 0) {
				int[] ints = new int[strs.length];
				for (int i = 0 ; i < strs.length ; ++ i) {
					ints[i] = StringUtils.isNotBlank(strs[i]) ? Integer.parseInt(strs[i]) : 0;
				}
				return ints;
			}
			return EMPTY;
		}
	};

	/**
	 * [SIFISC-3309] on trie par nom de commune croissant<br/>
	 * [SIFISC-4216] ... puis par numéro d'immeuble
	 */
	private static final Comparator<ImmeubleView> IMMEUBLE_COMPARATOR = new Comparator<ImmeubleView>() {
		@Override
		public int compare(ImmeubleView o1, ImmeubleView o2) {
			int comparison = o1.getNomCommune().compareTo(o2.getNomCommune());
			if (comparison == 0) {
				comparison = NO_IMMEUBLE_COMPARATOR.compare(o1.getNumero(), o2.getNumero());
			}
			return comparison;
		}
	};

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

		Collections.sort(views, IMMEUBLE_COMPARATOR);

		mav.addAttribute("immeubles", views);

		return "rf/immeuble/list";
	}

}
