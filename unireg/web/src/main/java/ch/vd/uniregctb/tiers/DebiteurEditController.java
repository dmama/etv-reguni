package ch.vd.uniregctb.tiers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.manager.TiersEditManager;
import ch.vd.uniregctb.tiers.view.DebiteurEditView;
import ch.vd.uniregctb.type.PeriodiciteDecompte;

@Controller
@RequestMapping(value = "/debiteur")
public class DebiteurEditController {

	public static final String LIBELLES_LOGICIELS = "libellesLogiciel";
	public static final String CATEGORIES_IMPOT_SOURCE = "categoriesImpotSource";
	public static final String MODES_COMMUNICATION = "modesCommunication";
	public static final String PERIODES_DECOMPTE = "periodesDecomptes";
	public static final String PERIODICITES_DECOMPTE = "periodicitesDecomptes";

	private TiersMapHelper tiersMapHelper;
	private TiersEditManager tiersEditManager;
	private SecurityProviderInterface securityProvider;
	private ControllerUtils controllerUtils;

	public static class EnumView<T extends Enum<T>> implements Serializable {

		private static final long serialVersionUID = 4609848662920574499L;

		private final T value;
		private final String label;

		public EnumView(T value, String label) {
			this.value = value;
			this.label = label;
		}

		@SuppressWarnings("UnusedDeclaration")
		public T getValue() {
			return value;
		}

		@SuppressWarnings("UnusedDeclaration")
		public String getLabel() {
			return label;
		}

		public static <T extends Enum<T>> List<EnumView<T>> fromMap(Map<T, String> map) {
			final List<EnumView<T>> list = new ArrayList<>(map.size());
			for (Map.Entry<T, String> entry : map.entrySet()) {
				list.add(new EnumView<>(entry.getKey(), entry.getValue()));
			}
			return list;
		}

		public static <T extends Enum<T>> Map<T, String> toMap(List<EnumView<T>> list) {
			final Map<T, String> map = new LinkedHashMap<>(list.size());
			for (EnumView<T> elt : list) {
				map.put(elt.getValue(), elt.getLabel());
			}
			return map;
		}
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setTiersEditManager(TiersEditManager tiersEditManager) {
		this.tiersEditManager = tiersEditManager;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	private boolean hasFullRights() {
		return SecurityHelper.isGranted(securityProvider, Role.SUPERGRA);
	}


	@RequestMapping(value = "/periodicitesDecompte.do", method = RequestMethod.GET)
	@ResponseBody
	public List<EnumView<PeriodiciteDecompte>> getAllowedPeriodicitesDecomptes(@RequestParam(value = "periodiciteActuelle") PeriodiciteDecompte periodiciteActuelle) {
	final Map<PeriodiciteDecompte, String> map;
		if (hasFullRights()) {
			map = tiersMapHelper.getMapPeriodiciteDecompte();
		}
		else {
			map = tiersMapHelper.getMapLimiteePeriodiciteDecompte(periodiciteActuelle);
		}
		return EnumView.fromMap(map);
	}

	@RequestMapping(value = "/edit.do", method = RequestMethod.GET)
	@Transactional(readOnly = true)
	public String getDebiteurToEdit(Model model, @RequestParam(value = "id") long id) throws Exception {
		controllerUtils.checkAccesDossierEnLecture(id);
		final DebiteurEditView view = tiersEditManager.getDebiteurEditView(id);

		model.addAttribute(PERIODICITES_DECOMPTE, getAllowedPeriodicitesDecomptes(view.getPeriodiciteCourante()));
		model.addAttribute(PERIODES_DECOMPTE, tiersMapHelper.getPeriodeDecomptes());
		model.addAttribute(MODES_COMMUNICATION, tiersMapHelper.getMapModeCommunication());
		model.addAttribute(LIBELLES_LOGICIELS, tiersMapHelper.getAllLibellesLogiciels());
		model.addAttribute(CATEGORIES_IMPOT_SOURCE, tiersMapHelper.getMapCategorieImpotSource());
		model.addAttribute("command", view);
		return "tiers/edition/debiteur/edit";
	}

	@RequestMapping(value = "/edit.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	public String saveDebiteur(@ModelAttribute("command") DebiteurEditView view) throws Exception {
		controllerUtils.checkAccesDossierEnEcriture(view.getId());
		tiersEditManager.save(view);
		return "redirect:/tiers/visu.do?id=" + view.getId();
	}
}
