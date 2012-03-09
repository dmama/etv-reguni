package ch.vd.uniregctb.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.NatureTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.utils.UniregModeHelper;

/**
 * Ce contrôleur permet d'afficher les {@link DatabasePreviewController#TIERS_COUNT} premiers tiers de la base de données.
 */
@Controller
public class DatabasePreviewController {

	private static final int TIERS_COUNT = 50;

	private TiersDAO tiersDao;
	private AdresseService adresseService;

	@RequestMapping(value = "/admin/dbpreview.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String index(Model mav) {

		if (!UniregModeHelper.isTestMode() || !(SecurityProvider.isGranted(Role.TESTER) || SecurityProvider.isGranted(Role.ADMIN))) {
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

		final Map<Class, List<InfoTiers>> infoMap = new HashMap<Class, List<InfoTiers>>();

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
					nomsPrenoms = Arrays.asList("Exception: " + e.getMessage());
				}

				final InfoTiers info = new InfoTiers(numero, type, nomsPrenoms);

				List<InfoTiers> infoTiers = infoMap.get(entry.getKey());
				if (infoTiers == null) {
					infoTiers = new ArrayList<InfoTiers>();
					infoMap.put(entry.getKey(), infoTiers);
				}

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
}
