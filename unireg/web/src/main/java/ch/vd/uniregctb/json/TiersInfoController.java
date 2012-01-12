package ch.vd.uniregctb.json;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;

@Controller
public class TiersInfoController {

	private TiersDAO tiersDAO;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	/**
	 * Retourne les informations d'un tiers sous forme JSON (voir http://blog.springsource.com/2010/01/25/ajax-simplifications-in-spring-3-0/)
	 *
	 * @param numero le numéro de contribuable
	 * @return le nombre d'immeubles du contribuable spécifié.
	 * @throws ch.vd.uniregctb.security.AccessDeniedException
	 *          si l'utilisateur ne possède les droits de visualisation suffisants.
	 */
	@RequestMapping(value = "/tiers/info.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@ResponseBody
	public TiersInfoView info(@RequestParam("numero") long numero) throws AccessDeniedException {

		ControllerUtils.checkAccesDossierEnLecture(numero);

		final Tiers tiers = tiersDAO.get(numero);
		if (tiers == null) {
			return null;
		}

		return new TiersInfoView(numero, tiers.getNatureTiers(), tiers.getDateDebutActivite());
	}
}
