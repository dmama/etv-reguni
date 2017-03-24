package ch.vd.uniregctb.registrefoncier;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
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
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.common.ContribuableNotFoundException;
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.common.EntrepriseNotFoundException;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.foncier.AllegementFoncier;
import ch.vd.uniregctb.foncier.DegrevementICI;
import ch.vd.uniregctb.foncier.DemandeDegrevementICI;
import ch.vd.uniregctb.foncier.ExonerationIFONC;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.view.ChoixImmeubleView;
import ch.vd.uniregctb.tiers.view.ImmeubleView;
import ch.vd.uniregctb.utils.RegDateEditor;

@Controller
@RequestMapping(value = "/degrevement-exoneration")
public class DegrevementExonerationController {

	public static final String VISU_CRITERIA_SESSION_NAME = "degExoVisuCriteria";

	private RegistreFoncierService registreFoncierService;
	private HibernateTemplate hibernateTemplate;
	private ServiceInfrastructureService infraService;
	private SecurityProviderInterface securityProviderInterface;
	private ControllerUtils controllerUtils;

	private static final Comparator<ImmeubleView> IMMEUBLE_VIEW_COMPARATOR = Comparator.comparing(ImmeubleView::getNoParcelle)
			.thenComparing(Comparator.comparing(ImmeubleView::getIndex1, Comparator.nullsFirst(Comparator.naturalOrder())))
			.thenComparing(Comparator.comparing(ImmeubleView::getIndex2, Comparator.nullsFirst(Comparator.naturalOrder())))
			.thenComparing(Comparator.comparing(ImmeubleView::getIndex3, Comparator.nullsFirst(Comparator.naturalOrder())));

	public void setRegistreFoncierService(RegistreFoncierService registreFoncierService) {
		this.registreFoncierService = registreFoncierService;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void setSecurityProviderInterface(SecurityProviderInterface securityProviderInterface) {
		this.securityProviderInterface = securityProviderInterface;
	}

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/immeubles.do", method = RequestMethod.GET)
	@ResponseBody
	public ChoixImmeubleView getImmeublesSurCommune(HttpSession session,
	                                                @RequestParam(value = "ctb") long idCtb,
	                                                @RequestParam(value = "ofsCommune") int ofsCommune,
	                                                @RequestParam(value = "noParcelle", required = false) Integer noParcelle,
	                                                @RequestParam(value = "index1", required = false) Integer index1,
	                                                @RequestParam(value = "index2", required = false) Integer index2,
	                                                @RequestParam(value = "index3", required = false) Integer index3) {

		final Contribuable ctb = getTiers(Contribuable.class, idCtb, ContribuableNotFoundException::new);

		// on récupère tous les immeubles concernés une fois, et on agrège les périodes des droits pour chacun d'eux

		final Map<Long, ImmeubleRF> immeublesParId = new HashMap<>();
		final Map<Long, List<DateRange>> droitsParImmeuble = registreFoncierService.getDroitsForCtb(ctb, true).stream()
				.filter(AnnulableHelper::nonAnnule)
				.peek(droit -> immeublesParId.put(droit.getImmeuble().getId(), droit.getImmeuble()))
				.collect(Collectors.toMap(droit -> droit.getImmeuble().getId(),
				                          droit -> Collections.singletonList(droit.getRangeMetier()),
				                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList())));

		final Set<Integer> numerosParcelles = new TreeSet<>();
		final Set<Integer> numerosIndex1 = new TreeSet<>();
		final Set<Integer> numerosIndex2 = new TreeSet<>();
		final Set<Integer> numerosIndex3 = new TreeSet<>();
		final List<ImmeubleView> views = new ArrayList<>(immeublesParId.size());
		for (ImmeubleRF immeuble : immeublesParId.values()) {
			//noinspection OptionalGetWithoutIsPresent
			final DateRange periode = droitsParImmeuble.get(immeuble.getId()).stream()
					.reduce((r1, r2) -> new DateRangeHelper.Range(RegDateHelper.minimum(r1.getDateDebut(), r2.getDateDebut(), NullDateBehavior.EARLIEST),
					                                              RegDateHelper.maximum(r1.getDateFin(), r2.getDateFin(), NullDateBehavior.LATEST)))
					.get();

			final SituationRF situation = getSituationSurCommune(immeuble, ofsCommune);
			if (situation != null) {
				// dans la liste de sortie ?
				if (matchIndices(situation, noParcelle, index1, index2, index3)) {
					final Long estimationFiscale = registreFoncierService.getEstimationFiscale(immeuble, periode.getDateFin());
					final String nature = ImmeubleHelper.getNatureImmeuble(immeuble, periode.getDateFin(), Integer.MAX_VALUE);
					final ImmeubleView view = new ImmeubleView(immeuble.getId(),
					                                           periode.getDateDebut(),
					                                           periode.getDateFin(),
					                                           situation.getNoParcelle(),
					                                           situation.getIndex1(),
					                                           situation.getIndex2(),
					                                           situation.getIndex3(),
					                                           estimationFiscale,
					                                           nature);
					views.add(view);
				}

				// même si l'immeuble n'est pas dans la liste de sortie parce qu'il ne satisfait pas aux critères
				// autour de la parcelle, il faut quand-même conserver ses valeurs autour de la parcelle, justement...
				numerosParcelles.add(situation.getNoParcelle());
				Optional.ofNullable(situation.getIndex1()).ifPresent(numerosIndex1::add);
				Optional.ofNullable(situation.getIndex2()).ifPresent(numerosIndex2::add);
				Optional.ofNullable(situation.getIndex3()).ifPresent(numerosIndex3::add);
			}
		}
		views.sort(IMMEUBLE_VIEW_COMPARATOR);

		// placement en session des derniers critères utilisés
		session.setAttribute(VISU_CRITERIA_SESSION_NAME, new DegrevementExonerationVisuSessionData(idCtb, ofsCommune, noParcelle, index1, index2, index3));

		// construction de la vue et retour
		return new ChoixImmeubleView(views, numerosParcelles, numerosIndex1, numerosIndex2, numerosIndex3);
	}

	private static boolean matchIndices(SituationRF situation, @Nullable Integer noParcelle, @Nullable Integer index1, @Nullable Integer index2, @Nullable Integer index3) {
		return (noParcelle == null || noParcelle == situation.getNoParcelle())
				&& (index1 == null || Objects.equals(index1, situation.getIndex1()))
				&& (index2 == null || Objects.equals(index2, situation.getIndex2()))
				&& (index3 == null || Objects.equals(index3, situation.getIndex3()));
	}

	@Nullable
	private static SituationRF getSituationSurCommune(ImmeubleRF immeuble, int noOfsCommune) {
		return immeuble.getSituations().stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(situation -> situation.getCommune().getNoOfs() == noOfsCommune)
				.max(Comparator.naturalOrder())
				.orElse(null);
	}

	@NotNull
	private <T extends Tiers> T getTiers(Class<T> clazz, long id, Function<Long, ? extends TiersNotFoundException> exceptionGenerator) {
		final T tiers = hibernateTemplate.get(clazz, id);
		if (tiers == null) {
			throw exceptionGenerator.apply(id);
		}
		return tiers;
	}

	@NotNull
	private ImmeubleRF getImmeuble(long id) {
		final ImmeubleRF immeuble = hibernateTemplate.get(ImmeubleRF.class, id);
		if (immeuble == null) {
			throw new ObjectNotFoundException("Immeuble inconnu avec l'identifiant " + id);
		}
		return immeuble;
	}

	@NotNull
	private DegrevementICI getDegrevement(long id) {
		final DegrevementICI degrevement = hibernateTemplate.get(DegrevementICI.class, id);
		if (degrevement == null) {
			throw new ObjectNotFoundException("L'identifiant " + id + " ne correspond à aucune donnée de dégrèvement connue.");
		}
		return degrevement;
	}

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/visu.do", method = RequestMethod.GET)
	public String showDetailDegrevementsExonerations(Model model,
	                                                 @RequestParam(value = "idCtb") long idContribuable,
	                                                 @RequestParam(value = "idImmeuble") long idImmeuble) {

		final Entreprise entreprise = getTiers(Entreprise.class, idContribuable, EntrepriseNotFoundException::new);
		final ImmeubleRF immeuble = getImmeuble(idImmeuble);

		// trouvons maintenant les liens entre les deux
		// 1. les demandes de dégrèvement
		// 2. les données de dégrèvement
		// 3. les données d'exonération

		final List<DemandeDegrevementICIView> demandes = entreprise.getAutresDocumentsFiscaux(DemandeDegrevementICI.class, false, true).stream()
				.filter(demande -> demande.getImmeuble() == immeuble)
				.map(demande -> new DemandeDegrevementICIView(demande, infraService))
				.sorted(Comparator.comparingInt(DemandeDegrevementICIView::getPeriodeFiscale).reversed())
				.collect(Collectors.toList());

		final Map<Class<? extends AllegementFoncier>, List<? extends AllegementFoncier>> allegements = entreprise.getAllegementsFonciers().stream()
				.filter(allegement -> allegement.getImmeuble() == immeuble)
				.collect(Collectors.toMap(AllegementFoncier::getClass,
				                          Collections::singletonList,
				                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList())));

		//noinspection unchecked
		final List<DegrevementICI> degrevements = (List<DegrevementICI>) allegements.getOrDefault(DegrevementICI.class, Collections.emptyList());
		final List<DegrevementICIView> viewsDegrevements = degrevements.stream()
				.map(DegrevementICIView::new)
				.sorted(new AnnulableHelper.AnnulableDateRangeComparator<>(true))
				.collect(Collectors.toList());

		//noinspection unchecked
		final List<ExonerationIFONC> exonerations = (List<ExonerationIFONC>) allegements.getOrDefault(ExonerationIFONC.class, Collections.emptyList());
		final List<ExonerationIFONCView> viewsExonerations = exonerations.stream()
				.map(ExonerationIFONCView::new)
				.sorted(new AnnulableHelper.AnnulableDateRangeComparator<>(true))
				.collect(Collectors.toList());

		// l'immeuble lui-même
		final ResumeImmeubleView immeubleView = new ResumeImmeubleView(immeuble, null, registreFoncierService);

		model.addAttribute("idContribuable", idContribuable);
		model.addAttribute("demandesDegrevement", demandes);
		model.addAttribute("degrevements", viewsDegrevements);
		model.addAttribute("exonerations", viewsExonerations);
		model.addAttribute("immeuble", immeubleView);
		return "tiers/visualisation/pm/degrevement-exoneration/detail-degrevement-exoneration";
	}

	@RequestMapping(value = "/edit-degrevements.do", method = RequestMethod.GET)
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	public String editDegrevements(Model model,
	                               @RequestParam(value = "idContribuable") long idContribuable,
	                               @RequestParam(value = "idImmeuble") long idImmeuble) {

		final Entreprise entreprise = getTiers(Entreprise.class, idContribuable, EntrepriseNotFoundException::new);
		final ImmeubleRF immeuble = getImmeuble(idImmeuble);

		final List<DegrevementICIView> degrevements = entreprise.getAllegementsFonciers().stream()
				.filter(allegement -> allegement.getImmeuble() == immeuble)
				.filter(allegement -> allegement instanceof DegrevementICI)
				.map(allegement -> (DegrevementICI) allegement)
				.map(DegrevementICIView::new)
				.sorted(new AnnulableHelper.AnnulableDateRangeComparator<>(true))
				.collect(Collectors.toList());

		final ResumeImmeubleView immeubleView = new ResumeImmeubleView(immeuble, null, registreFoncierService);

		model.addAttribute("idContribuable", idContribuable);
		model.addAttribute("degrevements", degrevements);
		model.addAttribute("immeuble", immeubleView);
		return "tiers/edition/pm/degrevement-exoneration/edit-degrevements";
	}

	@RequestMapping(value = "/cancel-degrevement.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	public String cancelDegrevement(@RequestParam("id") long idDegrevement) {
		if (!SecurityHelper.isGranted(securityProviderInterface, Role.DEGREVEMENTS_ICI)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits d'accès suffisants pour effectuer cette opération.");
		}

		final DegrevementICI degrevement = getDegrevement(idDegrevement);
		degrevement.setAnnule(true);
		return "redirect:/degrevement-exoneration/edit-degrevements.do?idContribuable=" + degrevement.getContribuable().getNumero() + "&idImmeuble=" + degrevement.getImmeuble().getId();
	}

	@InitBinder(value = "addDegrevementCommand")
	public void initAddDegrevementCommandBinder(WebDataBinder binder) {
		binder.setValidator(new AbstractEditDegrevementViewValidator());
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false, RegDateHelper.StringFormat.DISPLAY));
	}

	@RequestMapping(value = "/add-degrevement.do", method = RequestMethod.GET)
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	public String addDegrevement(Model model,
	                             @RequestParam(value = "idContribuable") long idContribuable,
	                             @RequestParam(value = "idImmeuble") long idImmeuble) {
		final Entreprise entreprise = getTiers(Entreprise.class, idContribuable, EntrepriseNotFoundException::new);
		return showAddDegrevement(model, new AddDegrevementView(entreprise.getNumero(), idImmeuble));
	}

	private String showAddDegrevement(Model model, AddDegrevementView view) {
		model.addAttribute("idContribuable", view.getIdContribuable());
		model.addAttribute("immeuble", new ResumeImmeubleView(getImmeuble(view.getIdImmeuble()), null, registreFoncierService));
		model.addAttribute("addDegrevementCommand", view);
		return "tiers/edition/pm/degrevement-exoneration/add-degrevement";
	}

	@RequestMapping(value = "/add-degrevement.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	public String doAddDegrevement(Model model,
	                               @Valid @ModelAttribute("addDegrevementCommand") AddDegrevementView view,
	                               BindingResult bindingResult) {

		if (bindingResult.hasErrors()) {
			return showAddDegrevement(model, view);
		}

		if (!SecurityHelper.isGranted(securityProviderInterface, Role.DEGREVEMENTS_ICI)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits d'accès suffisants pour effectuer cette opération.");
		}

		controllerUtils.checkAccesDossierEnEcriture(view.getIdContribuable());
		final Entreprise entreprise = getTiers(Entreprise.class, view.getIdContribuable(), EntrepriseNotFoundException::new);
		final ImmeubleRF immeuble = getImmeuble(view.getIdImmeuble());

		// on ne doit pas pouvoir réutiliser la période de début de validité d'une donnée existante
		final List<DegrevementICI> autres = entreprise.getAllegementsFonciersNonAnnulesTries(DegrevementICI.class).stream()
				.filter(deg -> deg.getImmeuble() == immeuble)
				.collect(Collectors.toList());
		if (autres.stream().anyMatch(deg -> deg.getDateDebut().year() == view.getPfDebut())) {
			bindingResult.rejectValue("pfDebut", "error.degexo.degrevement.periode.debut.deja.utilisee");
			return showAddDegrevement(model, view);
		}

		// il faut adapter les dates de fin des dégrèvements existant, éventuellement, et aussi celle du nouveau
		// - d'abord le nouveau : prendre le dégrèvement existant postérieur est assigner la date de fin du nouveau à la veille de la date de début de celui-ci
		// - puis les existants, pour les arrêter si nécessaire à la veille de la date de début du nouveau
		autres.stream()
				.filter(deg -> deg.getDateDebut().isAfter(view.getDateDebut()))
				.map(DegrevementICI::getDateDebut)
				.min(Comparator.naturalOrder())
				.ifPresent(nextDebut -> view.setPfFin(nextDebut.getOneDayBefore().year()));
		autres.stream()
				.map(deg -> Pair.of(deg, DateRangeHelper.intersection(deg, view)))
				.filter(pair -> pair.getRight() != null)
				.forEach(pair -> {
					final DegrevementICI deg = pair.getLeft();
					final RegDate nouvelleDateFin = pair.getRight().getDateDebut().getOneDayBefore();
					if (deg.getDateFin() == null) {
						// fermeture simple
						deg.setDateFin(nouvelleDateFin);
					}
					else {
						final DegrevementICI copy = deg.duplicate();
						deg.setAnnule(true);
						copy.setDateFin(nouvelleDateFin);
						entreprise.addAllegementFoncier(copy);
					}
				});

		// création d'une nouvelle entité
		final DegrevementICI degrevement = new DegrevementICI();
		degrevement.setImmeuble(immeuble);
		degrevement.setDateDebut(view.getDateDebut());
		degrevement.setDateFin(view.getDateFin());
		degrevement.setLocation(view.getLocation());
		degrevement.setPropreUsage(view.getPropreUsage());
		degrevement.setLoiLogement(view.getLoiLogement());
		entreprise.addAllegementFoncier(degrevement);

		return "redirect:edit-degrevements.do?idContribuable=" + view.getIdContribuable() + "&idImmeuble=" + view.getIdImmeuble();
	}

	@InitBinder(value = "editDegrevementCommand")
	public void initEditDegrevementCommandBinder(WebDataBinder binder) {
		binder.setValidator(new AbstractEditDegrevementViewValidator());
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false, RegDateHelper.StringFormat.DISPLAY));
	}

	@RequestMapping(value = "/edit-degrevement.do", method = RequestMethod.GET)
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	public String editDegrevement(Model model,
	                              @RequestParam("id") long idDegrevement) {

		final DegrevementICI degrevement = getDegrevement(idDegrevement);
		return showEditDegrevement(model, degrevement, new EditDegrevementView(degrevement));
	}

	private String showEditDegrevement(Model model, DegrevementICI degrevement, EditDegrevementView view) {
		model.addAttribute("idContribuable", degrevement.getContribuable().getNumero());
		model.addAttribute("immeuble", new ResumeImmeubleView(degrevement.getImmeuble(), null, registreFoncierService));
		model.addAttribute("editDegrevementCommand", view);
		return "tiers/edition/pm/degrevement-exoneration/edit-degrevement";
	}

	private String showEditDegrevement(Model model, EditDegrevementView view) {
		final DegrevementICI degrevement = getDegrevement(view.getIdDegrevement());
		return showEditDegrevement(model, degrevement, view);
	}

	@RequestMapping(value = "/edit-degrevement.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	public String doEditDegrevement(Model model,
	                                @Valid @ModelAttribute("editDegrevementCommand") EditDegrevementView view,
	                                BindingResult bindingResult) {

		if (bindingResult.hasErrors()) {
			return showEditDegrevement(model, view);
		}

		if (!SecurityHelper.isGranted(securityProviderInterface, Role.DEGREVEMENTS_ICI)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits d'accès suffisants pour effectuer cette opération.");
		}

		final DegrevementICI degrevement = getDegrevement(view.getIdDegrevement());
		final Contribuable ctb = degrevement.getContribuable();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getNumero());

		// on ne doit pas pouvoir réutiliser la période de début de validité d'une donnée existante
		final ImmeubleRF immeuble = degrevement.getImmeuble();
		final List<DegrevementICI> autres = ctb.getAllegementsFonciersNonAnnulesTries(DegrevementICI.class).stream()
				.filter(deg -> deg.getImmeuble() == immeuble)
				.filter(deg -> deg != degrevement)              // on ne prend que les autres !!!
				.collect(Collectors.toList());
		if (autres.stream().anyMatch(deg -> deg.getDateDebut().year() == view.getPfDebut())) {
			bindingResult.rejectValue("pfDebut", "error.degexo.degrevement.periode.debut.deja.utilisee");
			return showEditDegrevement(model, degrevement, view);
		}

		// il faut adapter les dates de fin des dégrèvements existant, éventuellement, et aussi celle du modifié
		// - recalculer la date de fin de l'entité modifiée
		// - recalculer la date de fin du précédent si on bouge la date de début de l'entité modifiée
		// - recalculer les dates de fin des existants
		view.setPfFin(null);        // ré-initialisation de la donnée
		autres.stream()
				.filter(deg -> deg.getDateDebut().isAfter(view.getDateDebut()))
				.map(DegrevementICI::getDateDebut)
				.min(Comparator.naturalOrder())
				.ifPresent(nextDebut -> view.setPfFin(nextDebut.getOneDayBefore().year()));
		if (view.getDateDebut() != degrevement.getDateDebut()) {
			final List<DegrevementICI> copies = new ArrayList<>(autres.size());
			autres.stream()
					.filter(deg -> deg.getDateFin() == degrevement.getDateDebut().getOneDayBefore())
					.forEach(deg -> {
						final DegrevementICI copy = deg.duplicate();
						deg.setAnnule(true);

						// calcul de la nouvelle date de fin
						final RegDate nouvelleDateFin = Stream.concat(autres.stream().map(DegrevementICI::getDateDebut), Stream.of(view.getDateDebut()))
								.filter(date -> date.isAfter(deg.getDateDebut()))
								.min(Comparator.naturalOrder())
								.map(RegDate::getOneDayBefore)
								.orElse(null);

						copy.setDateFin(nouvelleDateFin);
						ctb.addAllegementFoncier(copy);
						copies.add(copy);
					});
			autres.addAll(copies);
		}
		autres.stream()
				.filter(AnnulableHelper::nonAnnule)
				.map(deg -> Pair.of(deg, DateRangeHelper.intersection(deg, view)))
				.filter(pair -> pair.getRight() != null)
				.forEach(pair -> {
					final DegrevementICI deg = pair.getLeft();
					final RegDate nouvelleDateFin = pair.getRight().getDateDebut().getOneDayBefore();
					if (deg.getDateFin() == null || deg.getId() == null) {
						// fermeture simple sur les données simplement ouvertes ou sur les données en cours de création
						deg.setDateFin(nouvelleDateFin);
					}
					else {
						final DegrevementICI copy = deg.duplicate();
						deg.setAnnule(true);
						copy.setDateFin(nouvelleDateFin);
						ctb.addAllegementFoncier(copy);
					}
				});

		degrevement.setDateDebut(view.getDateDebut());
		degrevement.setDateFin(view.getDateFin());
		degrevement.setLocation(view.getLocation());
		degrevement.setPropreUsage(view.getPropreUsage());
		degrevement.setLoiLogement(view.getLoiLogement());

		return "redirect:edit-degrevements.do?idContribuable=" + ctb.getNumero() + "&idImmeuble=" + degrevement.getImmeuble().getId();
	}

	@RequestMapping(value = "/edit-demandes-degrevement.do", method = RequestMethod.GET)
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	public String editDemandesDegrevement(Model model,
	                                      @RequestParam(value = "idContribuable") long idContribuable,
	                                      @RequestParam(value = "idImmeuble") long idImmeuble) {
		throw new NotImplementedException("Pas encore implémenté !");
	}

	@RequestMapping(value = "/edit-exonerations.do", method = RequestMethod.GET)
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	public String editExonerations(Model model,
	                               @RequestParam(value = "idContribuable") long idContribuable,
	                               @RequestParam(value = "idImmeuble") long idImmeuble) {
		throw new NotImplementedException("Pas encore implémenté !");
	}
}
