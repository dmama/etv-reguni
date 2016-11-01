package ch.vd.uniregctb.etiquette;

import javax.validation.Valid;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateEditor;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.view.EtiquetteTiersView;

@Controller
@RequestMapping(value = "/etiquette")
public class EtiquetteTiersController {

	private HibernateTemplate hibernateTemplate;
	private SecurityProviderInterface securityProvider;
	private ControllerUtils controllerUtils;
	private EtiquetteService etiquetteService;

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	public void setEtiquetteService(EtiquetteService etiquetteService) {
		this.etiquetteService = etiquetteService;
	}

	private void checkAccess() {
		if (!SecurityHelper.isGranted(securityProvider, Role.GEST_ETIQUETTES)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec pour la gestion des étiquettes d'un tiers.");
		}
	}

	private void checkTiersType(Tiers tiers) {
		// pour le moment, on n'édite les étiquettes que sur les personnes physiques
		if (!(tiers instanceof PersonnePhysique)) {
			throw new AccessDeniedException("Cette modification n'est autorisée que sur les personnes physiques.");
		}
	}

	@InitBinder(value = "addCommand")
	public void initBinderAddCommand(WebDataBinder binder) {
		binder.setValidator(new AddEtiquetteTiersViewValidator());
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false, RegDateHelper.StringFormat.DISPLAY));
	}

	@InitBinder(value = "editCommand")
	public void initBinderEditCommand(WebDataBinder binder) {
		binder.setValidator(new EditEtiquetteTiersViewValidator());
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false, RegDateHelper.StringFormat.DISPLAY));
	}

	@NotNull
	private Tiers getTiers(Long tiersId) {
		// accès au tiers
		final Tiers tiers = hibernateTemplate.get(Tiers.class, tiersId);
		if (tiers == null) {
			throw new TiersNotFoundException(tiersId);
		}

		// pour le moment, on n'édite les étiquettes que sur les personnes physiques
		checkTiersType(tiers);
		return tiers;
	}

	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@RequestMapping(value = "/edit-list.do", method = RequestMethod.GET)
	public String showEditList(Model model, @RequestParam("tiersId") long tiersId) {
		checkAccess();
		controllerUtils.checkAccesDossierEnEcriture(tiersId);

		// accès au tiers et à ses étiquettes
		final Tiers tiers = getTiers(tiersId);
		final List<EtiquetteTiersView> views = tiers.getEtiquettes().stream()
				.map(EtiquetteTiersView::new)
				.sorted(new AnnulableHelper.AnnulesApresWrappingComparator<>(Comparator.comparing(EtiquetteTiersView::getDateDebut).reversed()))
				.collect(Collectors.toList());

		model.addAttribute("tiersId", tiersId);
		model.addAttribute("etiquettes", views);
		return "tiers/edition/etiquette/edit-etiquettes";
	}

	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@RequestMapping(value = "/add.do", method = RequestMethod.GET)
	public String showAdd(Model model, @RequestParam("tiersId") long tiersId) {
		checkAccess();
		controllerUtils.checkAccesDossierEnEcriture(tiersId);
		return showAdd(model, new AddEtiquetteTiersView(tiersId));
	}

	/**
	 * @param clazz classe du tiers concerné
	 * @param oldCode [optionnel] code d'une étiquette qui doit absolument être dans la map
	 * @return la map code -> libellé des étiquettes utilisables
	 */
	private Map<String, String> getLibelleMap(Class<? extends Tiers> clazz, @Nullable String oldCode) {

		// on prend de base les étiquettes prévues pour le tiers en question
		final List<Etiquette> allowed = etiquetteService.getAllEtiquettes().stream()
				.filter(etiq -> !etiq.isAnnule())
				.filter(Etiquette::isActive)
				.filter(etiq -> etiq.getTypeTiers().isForClass(clazz))
				.collect(Collectors.toCollection(LinkedList::new));

		// éventuellement, il faut rajouter l'étiquette correspondant à l'étiquette existante (= pour avoir l'option de ne pas changer)
		if (oldCode != null) {
			final Etiquette oldEtiquette = etiquetteService.getEtiquette(oldCode);
			if (oldEtiquette != null) {
				allowed.add(oldEtiquette);
			}
		}

		// tri + création d'une map code -> libellé
		return allowed.stream()
				.sorted(Comparator.comparing(Etiquette::getLibelle))
				.collect(Collectors.toMap(Etiquette::getCode,
				                          Etiquette::getLibelle,
				                          (e1, e2) -> e1,           // le cas où le "oldCode" était un code déjà autorisé...
				                          LinkedHashMap::new));
	}

	private String showAdd(Model model, AddEtiquetteTiersView view) {
		final Tiers tiers = getTiers(view.getTiersId());
		model.addAttribute("addCommand", view);
		model.addAttribute("libelles", getLibelleMap(tiers.getClass(), null));
		return "tiers/edition/etiquette/add";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "add.do", method = RequestMethod.POST)
	public String add(Model model, @Valid @ModelAttribute("addCommand") AddEtiquetteTiersView view, BindingResult bindingResult) {
		checkAccess();
		if (bindingResult.hasErrors()) {
			return showAdd(model, view);
		}
		controllerUtils.checkAccesDossierEnEcriture(view.getTiersId());
		final Tiers tiers = getTiers(view.getTiersId());

		final Etiquette etiquette = etiquetteService.getEtiquette(view.getCodeEtiquette());
		if (etiquette == null) {
			throw new ObjectNotFoundException(String.format("Pas d'étiquette avec le code '%s'", view.getCodeEtiquette()));
		}

		final EtiquetteTiers etiquetteTiers = new EtiquetteTiers(view.getDateDebut(), view.getDateFin(), etiquette);
		etiquetteTiers.setCommentaire(view.getCommentaire());
		tiers.addEtiquette(etiquetteTiers);
		return "redirect:/etiquette/edit-list.do?tiersId=" + view.getTiersId();
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "cancel.do", method = RequestMethod.POST)
	public String cancel(@RequestParam("idEtiquette") long idEtiquetteTiers) {
		checkAccess();

		final EtiquetteTiers etiquetteTiers = hibernateTemplate.get(EtiquetteTiers.class, idEtiquetteTiers);
		if (etiquetteTiers == null) {
			throw new ObjectNotFoundException("Pas d'étiquette de tiers avec l'identifiant " + idEtiquetteTiers);
		}

		final Tiers tiers = etiquetteTiers.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(tiers.getNumero());
		checkTiersType(tiers);

		etiquetteTiers.setAnnule(true);
		return "redirect:/etiquette/edit-list.do?tiersId=" + tiers.getNumero();
	}

	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@RequestMapping(value = "edit-item.do", method = RequestMethod.GET)
	public String showEditItem(Model model, @RequestParam("idEtiquette") long idEtiquetteTiers) {
		checkAccess();

		final EtiquetteTiers etiquetteTiers = hibernateTemplate.get(EtiquetteTiers.class, idEtiquetteTiers);
		if (etiquetteTiers == null) {
			throw new ObjectNotFoundException("Pas d'étiquette de tiers avec l'identifiant " + idEtiquetteTiers);
		}

		final Tiers tiers = etiquetteTiers.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(tiers.getNumero());
		checkTiersType(tiers);

		return showEditItem(model, etiquetteTiers.getEtiquette().getCode(), new EditEtiquetteTiersView(etiquetteTiers));
	}

	private String showEditItem(Model model, @Nullable String oldCodeEtiquette, EditEtiquetteTiersView view) {
		final Tiers tiers = getTiers(view.getTiersId());
		model.addAttribute("editCommand", view);
		model.addAttribute("libelles", getLibelleMap(tiers.getClass(), oldCodeEtiquette));
		return "tiers/edition/etiquette/edit";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "edit-item.do", method = RequestMethod.POST)
	public String editItem(Model model, @Valid @ModelAttribute("editCommand") EditEtiquetteTiersView view, BindingResult bindingResult) {
		checkAccess();

		final EtiquetteTiers etiquetteTiers = etiquetteService.getEtiquetteTiers(view.getEtiquetteTiersId());
		if (etiquetteTiers == null) {
			throw new ObjectNotFoundException("Pas d'étiquette de tiers avec l'identifiant " + view.getEtiquetteTiersId());
		}

		if (bindingResult.hasErrors()) {
			return showEditItem(model, etiquetteTiers.getEtiquette().getCode(), view);
		}

		final Tiers tiers = etiquetteTiers.getTiers();
		checkTiersType(tiers);
		controllerUtils.checkAccesDossierEnEcriture(tiers.getNumero());

		final Etiquette newEtiquette = etiquetteService.getEtiquette(view.getCodeEtiquette());
		if (newEtiquette == null) {
			throw new ObjectNotFoundException(String.format("Pas d'étiquette avec le code '%s'", view.getCodeEtiquette()));
		}

		// au moindre changement, on annule et on remplace
		if (!Objects.equals(etiquetteTiers.getCommentaire(), view.getCommentaire())
				|| !Objects.equals(etiquetteTiers.getDateDebut(), view.getDateDebut())
				|| !Objects.equals(etiquetteTiers.getDateFin(), view.getDateFin())
				|| newEtiquette != etiquetteTiers.getEtiquette()) {

			final EtiquetteTiers nouvelleInstance = etiquetteTiers.duplicate();
			nouvelleInstance.setDateDebut(view.getDateDebut());
			nouvelleInstance.setDateFin(view.getDateFin());
			nouvelleInstance.setEtiquette(newEtiquette);

			final String commentaireEpure = StringUtils.trimToNull(view.getCommentaire());
			final String commentaire = commentaireEpure != null && commentaireEpure.length() > LengthConstants.ETIQUETTE_TIERS_COMMENTAIRE ? commentaireEpure.substring(0, LengthConstants.ETIQUETTE_TIERS_COMMENTAIRE) : commentaireEpure;
			nouvelleInstance.setCommentaire(commentaire);

			etiquetteTiers.setAnnule(true);
			tiers.addEtiquette(nouvelleInstance);
		}
		else {
			Flash.message("Aucun changement détecté.");
		}
		return "redirect:/etiquette/edit-list.do?tiersId=" + tiers.getNumero();
	}
}
