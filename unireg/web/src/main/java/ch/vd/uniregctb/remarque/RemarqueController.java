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
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.Remarque;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.dao.RemarqueDAO;

@Controller
@RequestMapping(value = "/remarque")
public class RemarqueController {

	private TiersService tiersService;

	private RemarqueDAO remarqueDAO;
	private ControllerUtils controllerUtils;
	private SecurityProviderInterface securityProvider;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
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
	public RemarquesPage list(@RequestParam(value = "tiersId", required = true) Long tiersId,
	                          @RequestParam(value = "showHisto", required = false, defaultValue = "false") boolean showHisto,
	                          @RequestParam(value = "page", required = false, defaultValue = "1") int page,
	                          @RequestParam(value = "pageSize", required = false, defaultValue = "20") int pageSize) throws Exception {

		if (!SecurityHelper.isGranted(securityProvider, Role.VISU_ALL)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec de consultation pour l'application Unireg");
		}
		controllerUtils.checkAccesDossierEnLecture(tiersId);

		final Tiers tiers = tiersService.getTiers(tiersId);
		if (tiers == null) {
			throw new TiersNotFoundException(tiersId);
		}

		int totalCount = 0;
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
			if (showHisto || !remarque.isAnnule()) {
				totalCount++;
				// La liste contenue dans l'objet ne contient que les éléments de la page courante
				if (list.size() < pageSize && totalCount > (page - 1) * pageSize) {
					list.add(new RemarqueView(remarque));
				}
			}
		}
		page = ParamPagination.adjustPage(page, pageSize, totalCount);
		return new RemarquesPage(tiersId, list, showHisto, page, totalCount);
	}

	@RequestMapping(value = "/add.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	@ResponseBody
	public boolean add(@RequestParam(value = "tiersId", required = true) Long tiersId, @RequestParam(value = "text", required = true) String texte) throws Exception {

		if (!canManageRemarque()) {
			throw new AccessDeniedException("Vous ne possédez par les droits IfoSec pour ajouter une remarque");
		}
		controllerUtils.checkAccesDossierEnEcriture(tiersId);

		if (StringUtils.isBlank(texte)) {
			return false;
		}

		final Tiers tiers = tiersService.getTiers(tiersId);
		Assert.notNull(tiers);

		final Remarque remarque = new Remarque();
		final String t = (texte.length() > LengthConstants.TIERS_REMARQUE ? texte.substring(0, LengthConstants.TIERS_REMARQUE - 1) : texte);
		remarque.setTexte(t);
		remarque.setTiers(tiers);
		remarqueDAO.save(remarque);

		return true;
	}

	@RequestMapping(value = "/cancel.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	public String cancel(long remarqueId) throws Exception {

		if (!canManageRemarque()) {
			throw new AccessDeniedException("Vous ne possédez par les droits IfoSec pour annuler une remarque");
		}

		final Remarque remarque = remarqueDAO.get(remarqueId);

		controllerUtils.checkAccesDossierEnEcriture(remarque.getTiers().getId());

		remarque.setAnnule(true);

		return "redirect:/tiers/visu.do?id=" + remarque.getTiers().getId();
	}

	/**
	 * @return <b>vrai</b> si l'utilisateur courant peut ajouter une remarque sur le tiers courant; <b>faux</b> autrement.
	 */
	private boolean canManageRemarque() {
		return SecurityHelper.isAnyGranted(securityProvider, Role.REMARQUE_TIERS);
	}
}
