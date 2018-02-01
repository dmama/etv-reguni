package ch.vd.unireg.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.Flash;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.NatureTiers;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.utils.UniregModeHelper;

/**
 * Ce contrôleur permet d'afficher les {@link DatabasePreviewController#TIERS_COUNT} premiers tiers de la base de données.
 */
@Controller
public class DatabasePreviewController {

	private static final int TIERS_COUNT = 50;

	private TiersDAO tiersDao;
	private AdresseService adresseService;
	private SecurityProviderInterface securityProvider;

	@RequestMapping(value = "/admin/dbpreview.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String index(Model mav) {

		if (!UniregModeHelper.isTestMode() || !SecurityHelper.isAnyGranted(securityProvider, Role.TESTER, Role.ADMIN)) {
			Flash.warning("Vous ne possédez pas les droits suffisants pour accéder à la prévisualisation des tiers !");
			return "redirect:/tiers/list.do";
		}

		final DatabasePreview bean = new DatabasePreview();
		final Map<Class, List<InfoTiers>> infoTiers = buildInfoTiers();
		bean.setInfoTiers(infoTiers);
		mav.addAttribute("command", bean);

		return "admin/dbpreview";
	}

	/**
	 * Construit la liste des numéros, type et noms courrier des 100 premiers tiers de la base de données.
	 *
	 * @return une liste contenant des informaitons de tiers
	 */
	private Map<Class, List<InfoTiers>> buildInfoTiers() {

		final Map<Class, List<InfoTiers>> infoMap = new HashMap<>();

		final Map<Class, List<Tiers>> map = tiersDao.getFirstGroupedByClass(TIERS_COUNT);
		for (Map.Entry<Class, List<Tiers>> entry : map.entrySet()) {
			for (Tiers t : entry.getValue()) {

				final Long numero = t.getNumero();
				final NatureTiers type = t.getNatureTiers();
				List<String> nomsPrenoms;
				try {
					nomsPrenoms = adresseService.getNomCourrier(t, null, false);
				}
				catch (Exception e) {
					nomsPrenoms = Collections.singletonList("Exception: " + e.getMessage());
				}

				final InfoTiers info = new InfoTiers(numero, type, nomsPrenoms);

				final List<InfoTiers> infoTiers = infoMap.computeIfAbsent(entry.getKey(), k -> new ArrayList<>());
				infoTiers.add(info);
			}
		}

		return infoMap;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersDao(TiersDAO tiersDao) {
		this.tiersDao = tiersDao;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}
}
