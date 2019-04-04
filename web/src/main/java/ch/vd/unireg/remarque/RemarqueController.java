package ch.vd.unireg.remarque;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.unireg.common.ControllerUtils;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.common.TiersNotFoundException;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.Remarque;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.tiers.dao.RemarqueDAO;

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
			throw new AccessDeniedException("Vous ne possédez pas les droits de consultation pour l'application Unireg");
		}
		controllerUtils.checkAccesDossierEnLecture(tiersId);

		final Tiers tiers = tiersService.getTiers(tiersId);
		if (tiers == null) {
			throw new TiersNotFoundException(tiersId);
		}

		int totalCount = 0;
		final List<Remarque> remarques = remarqueDAO.getRemarques(tiersId);

		// On affiche les remarques les plus récentes en premier
		remarques.sort(Comparator.comparing(Remarque::getLogCreationDate).reversed());

		final List<RemarqueView> list = new ArrayList<>();
		for (Remarque remarque : remarques) {
			if (showHisto || !remarque.isAnnule()) {
				totalCount++;
				// La liste contenue dans l'objet ne contient que les éléments de la page courante
				if (pageSize == 0 || (list.size() < pageSize && totalCount > (page - 1) * pageSize)) {
					list.add(new RemarqueView(remarque));
				}
			}
		}
		page = pageSize == 0 ? 1 : ParamPagination.adjustPage(page, pageSize, totalCount);
		return new RemarquesPage(tiersId, list, showHisto, page, totalCount);
	}

	@RequestMapping(value = "/add.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	@ResponseBody
	public boolean add(@RequestParam(value = "tiersId", required = true) Long tiersId, @RequestParam(value = "text", required = true) String texte) throws Exception {

		if (!canManageRemarque()) {
			throw new AccessDeniedException("Vous ne possédez par les droits pour ajouter une remarque");
		}
		controllerUtils.checkAccesDossierEnEcriture(tiersId);

		if (StringUtils.isBlank(texte)) {
			return false;
		}

		final Tiers tiers = tiersService.getTiers(tiersId);
		if (tiers == null) {
			throw new IllegalArgumentException();
		}

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
			throw new AccessDeniedException("Vous ne possédez par les droits pour annuler une remarque");
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
