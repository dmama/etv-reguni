package ch.vd.uniregctb.remarque;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.Remarque;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.dao.RemarqueDAO;

@Controller
@RequestMapping(value = "/remarque")
public class RemarqueController {

	private TiersDAO tiersDAO;
	private RemarqueDAO remarqueDAO;
	private ControllerUtils controllerUtils;
	private SecurityProviderInterface securityProvider;

	@SuppressWarnings("UnusedDeclaration")
	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setRemarqueDAO(RemarqueDAO remarqueDAO) {
		this.remarqueDAO = remarqueDAO;
	}

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	@RequestMapping(value = "/list.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@ResponseBody
	public List<RemarqueView> list(@RequestParam(value = "tiersId", required = true) Long tiersId,
	                               @RequestParam(value = "avecAnnulees", defaultValue = "false") boolean avecAnnulees) throws Exception {

		if (!SecurityHelper.isGranted(securityProvider, Role.VISU_ALL)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec de consultation pour l'application Unireg");
		}
		controllerUtils.checkAccesDossierEnLecture(tiersId);

		final List<Remarque> remarques = remarqueDAO.getRemarques(tiersId);

		// On affiche les remarques les plus récentes en premier
		Collections.sort(remarques, new Comparator<Remarque>() {
			@Override
			public int compare(Remarque o1, Remarque o2) {
				return o2.getLogCreationDate().compareTo(o1.getLogCreationDate());
			}
		});

		final List<RemarqueView> list = new ArrayList<>();
		for (Remarque remarque : remarques) {
			if (avecAnnulees || !remarque.isAnnule()) {
				list.add(new RemarqueView(remarque));
			}
		}

		return list;
	}

	@RequestMapping(value = "/add.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	@ResponseBody
	public boolean add(@RequestParam(value = "tiersId", required = true) Long tiersId, @RequestParam(value = "text", required = true) String texte) throws Exception {

		if (!canAddRemark()) {
			throw new AccessDeniedException("Vous ne possédez par les droits IfoSec pour ajouter une remarque");
		}
		controllerUtils.checkAccesDossierEnEcriture(tiersId);
		
		if (StringUtils.isBlank(texte)) {
			return false;
		}

		final Tiers tiers = tiersDAO.get(tiersId);
		Assert.notNull(tiers);
		final Remarque remarque = new Remarque();
		final String t = (texte.length() > LengthConstants.TIERS_REMARQUE ? texte.substring(0, LengthConstants.TIERS_REMARQUE - 1) : texte);
		remarque.setTexte(t);
		remarque.setTiers(tiers);
		remarqueDAO.save(remarque);

		return true;
	}

	/**
	 * @return <b>vrai</b> si l'utilisateur courant peut ajouter une remarque sur le tiers courant; <b>faux</b> autrement.
	 */
	private boolean canAddRemark() {
		return SecurityHelper.isAnyGranted(securityProvider, Role.REMARQUE_TIERS);
	}
}
