package ch.vd.uniregctb.registrefoncier;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.common.ContribuableNotFoundException;
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.common.Duplicable;
import ch.vd.uniregctb.common.EditiqueErrorHelper;
import ch.vd.uniregctb.common.EntrepriseNotFoundException;
import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.RetourEditiqueControllerHelper;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.documentfiscal.AutreDocumentFiscalException;
import ch.vd.uniregctb.documentfiscal.AutreDocumentFiscalService;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueResultatErreur;
import ch.vd.uniregctb.editique.EditiqueResultatReroutageInbox;
import ch.vd.uniregctb.foncier.AllegementFoncier;
import ch.vd.uniregctb.foncier.DegrevementICI;
import ch.vd.uniregctb.foncier.DemandeDegrevementICI;
import ch.vd.uniregctb.foncier.DonneesLoiLogement;
import ch.vd.uniregctb.foncier.ExonerationIFONC;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
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
	private ParametreAppService parametreAppService;
	private AutreDocumentFiscalService autreDocumentFiscalService;
	private RetourEditiqueControllerHelper retourEditiqueControllerHelper;

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

	public void setParametreAppService(ParametreAppService parametreAppService) {
		this.parametreAppService = parametreAppService;
	}

	public void setAutreDocumentFiscalService(AutreDocumentFiscalService autreDocumentFiscalService) {
		this.autreDocumentFiscalService = autreDocumentFiscalService;
	}

	public void setRetourEditiqueControllerHelper(RetourEditiqueControllerHelper retourEditiqueControllerHelper) {
		this.retourEditiqueControllerHelper = retourEditiqueControllerHelper;
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

		controllerUtils.checkAccesDossierEnLecture(idCtb);
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
					final EstimationRF estimationFiscale = registreFoncierService.getEstimationFiscale(immeuble, periode.getDateFin());
					final String nature = ImmeubleHelper.getNatureImmeuble(immeuble, periode.getDateFin(), Integer.MAX_VALUE);
					final ImmeubleView view = new ImmeubleView(immeuble.getId(),
					                                           periode.getDateDebut(),
					                                           periode.getDateFin(),
					                                           situation.getNoParcelle(),
					                                           situation.getIndex1(),
					                                           situation.getIndex2(),
					                                           situation.getIndex3(),
					                                           estimationFiscale != null ? estimationFiscale.getMontant() : null,
					                                           estimationFiscale != null ? estimationFiscale.getReference() : null,
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

	@NotNull
	private ExonerationIFONC getExoneration(long id) {
		final ExonerationIFONC exoneration = hibernateTemplate.get(ExonerationIFONC.class, id);
		if (exoneration == null) {
			throw new ObjectNotFoundException("L'identifiant " + id + " ne correspond à aucune donnée d'exonération connue.");
		}
		return exoneration;
	}

	@NotNull
	private DemandeDegrevementICI getDemandeDegrevement(long id) {
		final DemandeDegrevementICI demande = hibernateTemplate.get(DemandeDegrevementICI.class, id);
		if (demande == null) {
			throw new ObjectNotFoundException("L'identifiant " + id + " ne correspond à aucun formulaire de demande de dégrèvement connu.");
		}
		return demande;
	}

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/visu.do", method = RequestMethod.GET)
	public String showDetailDegrevementsExonerations(Model model,
	                                                 @RequestParam(value = "idCtb") long idContribuable,
	                                                 @RequestParam(value = "idImmeuble") long idImmeuble) {

		controllerUtils.checkAccesDossierEnLecture(idContribuable);
		final Entreprise entreprise = getTiers(Entreprise.class, idContribuable, EntrepriseNotFoundException::new);
		final ImmeubleRF immeuble = getImmeuble(idImmeuble);

		// trouvons maintenant les liens entre les deux
		// 0. les droits
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

		// les droits qui lient cette entreprise et cet immeuble
		final List<DroitView> viewsDroits = buildListeDroits(entreprise, immeuble);

		// l'immeuble lui-même
		final ResumeImmeubleView immeubleView = new ResumeImmeubleView(immeuble, null, registreFoncierService);

		model.addAttribute("idContribuable", idContribuable);
		model.addAttribute("demandesDegrevement", demandes);
		model.addAttribute("degrevements", viewsDegrevements);
		model.addAttribute("exonerations", viewsExonerations);
		model.addAttribute("immeuble", immeubleView);
		model.addAttribute("droits", viewsDroits);
		return "tiers/visualisation/pm/degrevement-exoneration/detail-degrevement-exoneration";
	}

	private List<DroitView> buildListeDroits(Entreprise entreprise, ImmeubleRF immeuble) {
		return registreFoncierService.getDroitsForCtb(entreprise).stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(dt -> dt.getImmeuble() == immeuble)
				.sorted(Comparator.reverseOrder())
				.map(DroitView::new)
				.collect(Collectors.toList());
	}

	/**
	 * @param existing la collection des éléments existants
	 * @return la liste des périodes de début utilisables (celles correspondant aux éléments existants ont été indiquées comme non-utilisables)
	 */
	private List<PeriodeFiscaleView> buildPeriodesDebutAutorisees(Collection<? extends AllegementFoncier> existing) {
		final Set<Integer> used = existing.stream()
				.filter(AnnulableHelper::nonAnnule)
				.map(AllegementFoncier::getDateDebut)
				.map(RegDate::year)
				.collect(Collectors.toSet());
		return buildPeriodesAutoriseesSauf(used);
	}

	/**
	 * @param existing la collection des demandes de dégrèvement existantes
	 * @return la liste des périodes fiscales utilisables (celles correspondant aux éléments existants sont indiquées comme non-utilisables)
	 */
	private List<PeriodeFiscaleView> buildPeriodesFiscalesAutorisees(Collection<DemandeDegrevementICI> existing) {
		final Set<Integer> used = existing.stream()
				.filter(AnnulableHelper::nonAnnule)
				.map(DemandeDegrevementICI::getPeriodeFiscale)
				.collect(Collectors.toSet());
		return buildPeriodesAutoriseesSauf(used);
	}

	/**
	 * @param used ensemble des périodes à exclure
	 * @return une liste des périodes fiscales (dans l'ordre décroissant) depuis la première période fiscale des périodes morale (par défaut 2009)
	 * jusqu'à l'année prochaine, toutes associées à un flag d'utilisabilité
	 */
	private List<PeriodeFiscaleView> buildPeriodesAutoriseesSauf(Set<Integer> used) {
		final int first = parametreAppService.getPremierePeriodeFiscalePersonnesMorales();
		final int last = RegDate.get().year() + 1;
		final List<PeriodeFiscaleView> list = new ArrayList<>(last - first + 1);
		for (int i = last ; i >= first ; -- i) {
			list.add(new PeriodeFiscaleView(i, used.contains(i)));
		}
		return list;
	}

	/**
	 * @param contribuable un contribuable
	 * @param immeuble un immeuble
	 * @param clazz la class des allègements fonciers qui nous intéressent
	 * @param excluded l'allègement foncier dont il ne faut pas tenir compte
	 * @param avecAnnules <code>true</code> si on veut les annulés aussi
	 * @param <T> le type d'allègement foncier considéré
	 * @return la liste des allègements fonciers non-annulés de même classe qui lient l'entreprise à l'immeuble
	 */
	private <T extends AllegementFoncier> List<T> getAllegementsFonciers(Contribuable contribuable, ImmeubleRF immeuble, Class<T> clazz, @Nullable T excluded, boolean avecAnnules) {
		return getAllegementsFonciers(contribuable, immeuble.getId(), clazz, excluded, avecAnnules);
	}

	/**
	 * @param contribuable un contribuable
	 * @param idImmeuble l'identifiant technique d'un immeuble
	 * @param clazz la class des allègements fonciers qui nous intéressent
	 * @param excluded l'allègement foncier dont il ne faut pas tenir compte
	 * @param avecAnnules <code>true</code> si on veut les annulés aussi
	 * @param <T> le type d'allègement foncier considéré
	 * @return la liste des allègements fonciers non-annulés de même classe qui lient l'entreprise à l'immeuble
	 */
	private <T extends AllegementFoncier> List<T> getAllegementsFonciers(Contribuable contribuable, long idImmeuble, Class<T> clazz, @Nullable T excluded, boolean avecAnnules) {
		return contribuable.getAllegementsFonciers().stream()
				.filter(af -> avecAnnules || !af.isAnnule())
				.filter(af -> excluded == null || af != excluded)
				.filter(af -> clazz.isAssignableFrom(af.getClass()))
				.filter(af -> af.getImmeuble().getId() == idImmeuble)
				.map(af -> (T) af)
				.collect(Collectors.toList());
	}

	/**
	 * @param entreprise une entreprise
	 * @param immeuble un immeuble
	 * @param excluded la demande de dégrèvement dont il ne faut pas tenir compte
	 * @param avecAnnulees <code>true</code> si on veut les demandes annulées aussi
	 * @return la liste des demandes de dégrèvement qui lient l'entreprise à l'immeuble
	 */
	private List<DemandeDegrevementICI> getDemandesDegrevement(Entreprise entreprise, ImmeubleRF immeuble, DemandeDegrevementICI excluded, boolean avecAnnulees) {
		return getDemandesDegrevement(entreprise, immeuble.getId(), excluded, avecAnnulees);
	}

	/**
	 * @param entreprise une entreprise
	 * @param idImmeuble l'identifiant technique d'un immeuble
	 * @param excluded la demande de dégrèvement dont il ne faut pas tenir compte
	 * @param avecAnnulees <code>true</code> si on veut les demandes annulées aussi
	 * @return la liste des demandes de dégrèvement qui lient l'entreprise à l'immeuble
	 */
	private List<DemandeDegrevementICI> getDemandesDegrevement(Entreprise entreprise, long idImmeuble, DemandeDegrevementICI excluded, boolean avecAnnulees) {
		return entreprise.getAutresDocumentsFiscaux(DemandeDegrevementICI.class, false, avecAnnulees).stream()
				.filter(dd -> excluded == null || dd != excluded)
				.filter(dd -> dd.getImmeuble().getId() == idImmeuble)
				.collect(Collectors.toList());
	}

	/**
	 * Méthode générique pour gérer l'édition d'allègement foncier et en particulier
	 * le changement de période fiscale de début par rapport à un existant
	 * @param ctb contribuable
	 * @param autres les autres allègements fonciers de même type
	 * @param editedEntity l'entité en cours d'édition (ne doit pas faire partie de la liste des "autres")
	 * @param newRange nouvelle période de validité de l'entité en cours d'édition
	 * @param <T> le type d'allègement foncier
	 */
	private static <T extends AllegementFoncier & Duplicable<T>> void computeEditionInfluenceOnOthers(Contribuable ctb,
	                                                                                                  List<T> autres,
	                                                                                                  T editedEntity,
	                                                                                                  AbstractYearRangeView newRange) {

		// il faut adapter les dates de fin entités existantes, éventuellement, et aussi celle de l'entité modifiée
		// - recalculer la date de fin de l'entité modifiée
		// - recalculer la date de fin de la précédente si on bouge la date de début de l'entité modifiée
		// - recalculer les dates de fin des existantes

		final Integer pfFin = autres.stream()
				.filter(af -> af.getDateDebut().isAfter(newRange.getDateDebut()))
				.map(AllegementFoncier::getDateDebut)
				.min(Comparator.naturalOrder())
				.map(RegDate::getOneDayBefore)
				.map(RegDate::year)
				.orElse(null);
		newRange.setAnneeFin(pfFin);

		if (newRange.getDateDebut() != editedEntity.getDateDebut()) {
			final List<T> copies = new ArrayList<>(autres.size());
			autres.stream()
					.filter(af -> af.getDateFin() == editedEntity.getDateDebut().getOneDayBefore())
					.forEach(af -> {
						final T copy = af.duplicate();
						af.setAnnule(true);

						// calcul de la nouvelle date de fin
						final RegDate nouvelleDateFin = Stream.concat(autres.stream().map(AllegementFoncier::getDateDebut), Stream.of(newRange.getDateDebut()))
								.filter(date -> date.isAfter(af.getDateDebut()))
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
				.map(af -> Pair.of(af, DateRangeHelper.intersection(af, newRange)))
				.filter(pair -> pair.getRight() != null)
				.forEach(pair -> {
					final T af = pair.getLeft();
					final RegDate nouvelleDateFin = pair.getRight().getDateDebut().getOneDayBefore();
					if (af.getDateFin() == null || af.getId() == null) {
						// fermeture simple sur les données simplement ouvertes ou sur les données en cours de création
						af.setDateFin(nouvelleDateFin);
					}
					else {
						final T copy = af.duplicate();
						af.setAnnule(true);
						copy.setDateFin(nouvelleDateFin);
						ctb.addAllegementFoncier(copy);
					}
				});
	}

	/**
	 * Méthode générique pour gérer l'ajout d'allègement foncier par rapport aux dates de validité des allègements existants de même type
	 * @param ctb contribuable
	 * @param preexisting les allègements fonciers de même type déjà existants
	 * @param newRange période de validité de la nouvelle entité
	 * @param <T> le type d'allègement foncier
	 */
	private static <T extends AllegementFoncier & Duplicable<T>> void computeAdditionInfluenceOnOthers(Contribuable ctb,
	                                                                                                   List<T> preexisting,
	                                                                                                   AbstractYearRangeView newRange) {

		// il faut adapter les dates de fin des entités existantes, éventuellement, et aussi celle de la nouvelle
		// - d'abord la nouvelle : prendre l'entité existante postérieure et assigner la date de fin de la nouvelle entité à la veille de la date de début de celui-ci
		// - puis les existantes, pour les arrêter si nécessaire à la veille de la date de début de la nouvelle

		final Integer pfFin = preexisting.stream()
				.filter(af -> af.getDateDebut().isAfter(newRange.getDateDebut()))
				.map(AllegementFoncier::getDateDebut)
				.min(Comparator.naturalOrder())
				.map(RegDate::getOneDayBefore)
				.map(RegDate::year)
				.orElse(null);
		newRange.setAnneeFin(pfFin);

		preexisting.stream()
				.map(af -> Pair.of(af, DateRangeHelper.intersection(af, newRange)))
				.filter(pair -> pair.getRight() != null)
				.forEach(pair -> {
					final T af = pair.getLeft();
					final RegDate nouvelleDateFin = pair.getRight().getDateDebut().getOneDayBefore();
					if (af.getDateFin() == null) {
						// fermeture simple
						af.setDateFin(nouvelleDateFin);
					}
					else {
						final T copy = af.duplicate();
						af.setAnnule(true);
						copy.setDateFin(nouvelleDateFin);
						ctb.addAllegementFoncier(copy);
					}
				});
	}

	//////////////////////////////////
	//                              //
	// Edition des dégrèvements ICI //
	//                              //
	//////////////////////////////////

	@RequestMapping(value = "/edit-degrevements.do", method = RequestMethod.GET)
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	public String editDegrevements(Model model,
	                               @RequestParam(value = "idContribuable") long idContribuable,
	                               @RequestParam(value = "idImmeuble") long idImmeuble) {

		controllerUtils.checkAccesDossierEnEcriture(idContribuable);
		final Entreprise entreprise = getTiers(Entreprise.class, idContribuable, EntrepriseNotFoundException::new);
		final ImmeubleRF immeuble = getImmeuble(idImmeuble);

		final List<DegrevementICIView> degrevements = getAllegementsFonciers(entreprise, immeuble, DegrevementICI.class, null, true).stream()
				.map(DegrevementICIView::new)
				.sorted(new AnnulableHelper.AnnulableDateRangeComparator<>(true))
				.collect(Collectors.toList());

		final ResumeImmeubleView immeubleView = new ResumeImmeubleView(immeuble, null, registreFoncierService);

		// les droits qui lient cette entreprise et cet immeuble
		final List<DroitView> viewsDroits = buildListeDroits(entreprise, immeuble);

		model.addAttribute("idContribuable", idContribuable);
		model.addAttribute("degrevements", degrevements);
		model.addAttribute("immeuble", immeubleView);
		model.addAttribute("droits", viewsDroits);
		return "tiers/edition/pm/degrevement-exoneration/edit-degrevements";
	}

	@RequestMapping(value = "/cancel-degrevement.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	public String cancelDegrevement(@RequestParam("id") long idDegrevement) {
		if (!SecurityHelper.isGranted(securityProviderInterface, Role.DEGREVEMENTS_ICI)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits d'accès suffisants pour effectuer cette opération.");
		}

		final DegrevementICI degrevement = getDegrevement(idDegrevement);
		final Contribuable contribuable = degrevement.getContribuable();
		controllerUtils.checkAccesDossierEnEcriture(contribuable.getNumero());

		degrevement.setAnnule(true);
		getAllegementsFonciers(contribuable, degrevement.getImmeuble(), DegrevementICI.class, degrevement, false).stream()
				.filter(deg -> deg.getDateFin() == degrevement.getDateDebut().getOneDayBefore())
				.forEach(deg -> {
					final DegrevementICI copy = deg.duplicate();
					deg.setAnnule(true);
					copy.setDateFin(degrevement.getDateFin());      // on rallonge le dégrèvement précédent s'il existe
					contribuable.addAllegementFoncier(copy);
				});

		return "redirect:/degrevement-exoneration/edit-degrevements.do?idContribuable=" + contribuable.getNumero() + "&idImmeuble=" + degrevement.getImmeuble().getId();
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
		controllerUtils.checkAccesDossierEnEcriture(idContribuable);
		final Entreprise entreprise = getTiers(Entreprise.class, idContribuable, EntrepriseNotFoundException::new);
		return showAddDegrevement(model,
		                          getAllegementsFonciers(entreprise, idImmeuble, DegrevementICI.class, null, false),
		                          new AddDegrevementView(entreprise.getNumero(), idImmeuble));
	}

	private String showAddDegrevement(Model model, Collection<DegrevementICI> autres, AddDegrevementView view) {
		model.addAttribute("periodesDebut", buildPeriodesDebutAutorisees(autres));
		model.addAttribute("idContribuable", view.getIdContribuable());
		model.addAttribute("immeuble", new ResumeImmeubleView(getImmeuble(view.getIdImmeuble()), null, registreFoncierService));
		model.addAttribute("addDegrevementCommand", view);
		model.addAttribute("degrevementNonIntegrable", Boolean.FALSE);
		return "tiers/edition/pm/degrevement-exoneration/add-degrevement";
	}

	@RequestMapping(value = "/add-degrevement.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	public String doAddDegrevement(Model model,
	                               @Valid @ModelAttribute("addDegrevementCommand") AddDegrevementView view,
	                               BindingResult bindingResult) {

		if (!SecurityHelper.isGranted(securityProviderInterface, Role.DEGREVEMENTS_ICI)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits d'accès suffisants pour effectuer cette opération.");
		}

		controllerUtils.checkAccesDossierEnEcriture(view.getIdContribuable());
		final Entreprise entreprise = getTiers(Entreprise.class, view.getIdContribuable(), EntrepriseNotFoundException::new);
		if (bindingResult.hasErrors()) {
			return showAddDegrevement(model,
			                          getAllegementsFonciers(entreprise, view.getIdImmeuble(), DegrevementICI.class, null, false),
			                          view);
		}

		final ImmeubleRF immeuble = getImmeuble(view.getIdImmeuble());

		// on ne doit pas pouvoir réutiliser la période de début de validité d'une donnée existante
		final List<DegrevementICI> autres = getAllegementsFonciers(entreprise, immeuble, DegrevementICI.class, null, false);
		if (autres.stream().anyMatch(deg -> deg.getDateDebut().year() == view.getAnneeDebut())) {
			bindingResult.rejectValue("anneeDebut", "error.degexo.degrevement.periode.debut.deja.utilisee");
			return showAddDegrevement(model,
			                          getAllegementsFonciers(entreprise, immeuble, DegrevementICI.class, null, false),
			                          view);
		}

		// mise en place par rapport aux autres
		computeAdditionInfluenceOnOthers(entreprise, autres, view);

		// création d'une nouvelle entité
		final DegrevementICI degrevement = new DegrevementICI();
		degrevement.setImmeuble(immeuble);
		degrevement.setDateDebut(view.getDateDebut());
		degrevement.setDateFin(view.getDateFin());
		degrevement.setLocation(view.getLocation());
		degrevement.setPropreUsage(view.getPropreUsage());
		degrevement.setLoiLogement(cleanupLoiLogement(view.getLoiLogement()));
		degrevement.setNonIntegrable(Boolean.FALSE);        // ré-initialisation du flag dans tous les cas
		entreprise.addAllegementFoncier(degrevement);

		return "redirect:edit-degrevements.do?idContribuable=" + view.getIdContribuable() + "&idImmeuble=" + view.getIdImmeuble();
	}

	private static DonneesLoiLogement cleanupLoiLogement(DonneesLoiLogement source) {
		if (source == null || source.getControleOfficeLogement() == null || !source.getControleOfficeLogement()) {
			return new DonneesLoiLogement(Boolean.FALSE, null, null, null);
		}
		else {
			return source;
		}
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
		model.addAttribute("degrevementNonIntegrable", degrevement.isNonIntegrable());
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
		final List<DegrevementICI> autres = getAllegementsFonciers(ctb, immeuble, DegrevementICI.class, degrevement, false);
		if (autres.stream().anyMatch(deg -> deg.getDateDebut().year() == view.getAnneeDebut())) {
			bindingResult.rejectValue("anneeDebut", "error.degexo.degrevement.periode.debut.deja.utilisee");
			return showEditDegrevement(model, degrevement, view);
		}

		// mise en place par rapport aux autres
		computeEditionInfluenceOnOthers(ctb, autres, degrevement, view);

		degrevement.setDateDebut(view.getDateDebut());
		degrevement.setDateFin(view.getDateFin());
		degrevement.setLocation(view.getLocation());
		degrevement.setPropreUsage(view.getPropreUsage());
		degrevement.setLoiLogement(cleanupLoiLogement(view.getLoiLogement()));
		degrevement.setNonIntegrable(Boolean.FALSE);        // ré-initialisation du flag dans tous les cas

		return "redirect:edit-degrevements.do?idContribuable=" + ctb.getNumero() + "&idImmeuble=" + degrevement.getImmeuble().getId();
	}

	////////////////////////////////////
	//                                //
	// Edition des exonérations IFONC //
	//                                //
	////////////////////////////////////

	@RequestMapping(value = "/edit-exonerations.do", method = RequestMethod.GET)
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	public String editExonerations(Model model,
	                               @RequestParam(value = "idContribuable") long idContribuable,
	                               @RequestParam(value = "idImmeuble") long idImmeuble) {

		controllerUtils.checkAccesDossierEnEcriture(idContribuable);
		final Entreprise entreprise = getTiers(Entreprise.class, idContribuable, EntrepriseNotFoundException::new);
		final ImmeubleRF immeuble = getImmeuble(idImmeuble);

		final List<ExonerationIFONCView> exonerations = getAllegementsFonciers(entreprise, immeuble, ExonerationIFONC.class, null, true).stream()
				.map(ExonerationIFONCView::new)
				.sorted(new AnnulableHelper.AnnulableDateRangeComparator<>(true))
				.collect(Collectors.toList());

		final ResumeImmeubleView immeubleView = new ResumeImmeubleView(immeuble, null, registreFoncierService);

		// les droits qui lient cette entreprise et cet immeuble
		final List<DroitView> viewsDroits = buildListeDroits(entreprise, immeuble);

		model.addAttribute("idContribuable", idContribuable);
		model.addAttribute("exonerations", exonerations);
		model.addAttribute("immeuble", immeubleView);
		model.addAttribute("droits", viewsDroits);
		return "tiers/edition/pm/degrevement-exoneration/edit-exonerations";
	}

	@RequestMapping(value = "/cancel-exoneration.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	public String cancelExoneration(@RequestParam("id") long idExoneration) {
		if (!SecurityHelper.isGranted(securityProviderInterface, Role.EXONERATIONS_IFONC)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits d'accès suffisants pour effectuer cette opération.");
		}

		final ExonerationIFONC exoneration = getExoneration(idExoneration);
		final Contribuable contribuable = exoneration.getContribuable();
		controllerUtils.checkAccesDossierEnEcriture(exoneration.getContribuable().getNumero());

		exoneration.setAnnule(true);
		getAllegementsFonciers(contribuable, exoneration.getImmeuble(), ExonerationIFONC.class, exoneration, false).stream()
				.filter(exo -> exo.getDateFin() == exoneration.getDateDebut().getOneDayBefore())
				.forEach(exo -> {
					final ExonerationIFONC copy = exo.duplicate();
					exo.setAnnule(true);
					copy.setDateFin(exoneration.getDateFin());      // on rallonge l'exonération précédente si elle existe
					contribuable.addAllegementFoncier(copy);
				});

		return "redirect:/degrevement-exoneration/edit-exonerations.do?idContribuable=" + contribuable.getNumero() + "&idImmeuble=" + exoneration.getImmeuble().getId();
	}

	@InitBinder(value = "addExonerationCommand")
	public void initAddExonerationCommandBinder(WebDataBinder binder) {
		binder.setValidator(new AbstractEditExonerationViewValidator());
	}

	@RequestMapping(value = "/add-exoneration.do", method = RequestMethod.GET)
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	public String addExoneration(Model model,
	                             @RequestParam(value = "idContribuable") long idContribuable,
	                             @RequestParam(value = "idImmeuble") long idImmeuble) {
		controllerUtils.checkAccesDossierEnEcriture(idContribuable);
		final Entreprise entreprise = getTiers(Entreprise.class, idContribuable, EntrepriseNotFoundException::new);
		return showAddExoneration(model,
		                          getAllegementsFonciers(entreprise, idImmeuble, ExonerationIFONC.class, null, false),
		                          new AddExonerationView(entreprise.getNumero(), idImmeuble));
	}

	private String showAddExoneration(Model model, Collection<ExonerationIFONC> autres, AddExonerationView view) {
		model.addAttribute("periodesDebut", buildPeriodesDebutAutorisees(autres));
		model.addAttribute("idContribuable", view.getIdContribuable());
		model.addAttribute("immeuble", new ResumeImmeubleView(getImmeuble(view.getIdImmeuble()), null, registreFoncierService));
		model.addAttribute("addExonerationCommand", view);
		return "tiers/edition/pm/degrevement-exoneration/add-exoneration";
	}

	@RequestMapping(value = "/add-exoneration.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	public String doAddExoneration(Model model,
	                               @Valid @ModelAttribute("addExonerationCommand") AddExonerationView view,
	                               BindingResult bindingResult) {

		if (!SecurityHelper.isGranted(securityProviderInterface, Role.EXONERATIONS_IFONC)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits d'accès suffisants pour effectuer cette opération.");
		}

		controllerUtils.checkAccesDossierEnEcriture(view.getIdContribuable());
		final Entreprise entreprise = getTiers(Entreprise.class, view.getIdContribuable(), EntrepriseNotFoundException::new);
		if (bindingResult.hasErrors()) {
			return showAddExoneration(model,
			                          getAllegementsFonciers(entreprise, view.getIdImmeuble(), ExonerationIFONC.class, null, false),
			                          view);
		}

		final ImmeubleRF immeuble = getImmeuble(view.getIdImmeuble());

		// on ne doit pas pouvoir réutiliser la période de début de validité d'une donnée existante
		final List<ExonerationIFONC> autres = getAllegementsFonciers(entreprise, immeuble, ExonerationIFONC.class, null, false);
		if (autres.stream().anyMatch(exo -> exo.getDateDebut().year() == view.getAnneeDebut())) {
			bindingResult.rejectValue("anneeDebut", "error.degexo.exoneration.periode.debut.deja.utilisee");
			return showAddExoneration(model,
			                          getAllegementsFonciers(entreprise, immeuble, ExonerationIFONC.class, null, false),
			                          view);
		}

		// mise en place par rapport aux autres
		computeAdditionInfluenceOnOthers(entreprise, autres, view);

		// création d'une nouvelle entité
		final ExonerationIFONC exoneration = new ExonerationIFONC();
		exoneration.setImmeuble(immeuble);
		exoneration.setDateDebut(view.getDateDebut());
		exoneration.setDateFin(view.getDateFin());
		exoneration.setPourcentageExoneration(view.getPourcentageExoneration());
		entreprise.addAllegementFoncier(exoneration);

		return "redirect:edit-exonerations.do?idContribuable=" + view.getIdContribuable() + "&idImmeuble=" + view.getIdImmeuble();
	}

	@InitBinder(value = "editExonerationCommand")
	public void initEditExonerationCommandBinder(WebDataBinder binder) {
		binder.setValidator(new AbstractEditExonerationViewValidator());
	}

	@RequestMapping(value = "/edit-exoneration.do", method = RequestMethod.GET)
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	public String editExoneration(Model model,
	                              @RequestParam("id") long idExoneration) {

		final ExonerationIFONC exoneration = getExoneration(idExoneration);
		return showEditExoneration(model, exoneration, new EditExonerationView(exoneration));
	}

	private String showEditExoneration(Model model, ExonerationIFONC exoneration, EditExonerationView view) {
		model.addAttribute("idContribuable", exoneration.getContribuable().getNumero());
		model.addAttribute("immeuble", new ResumeImmeubleView(exoneration.getImmeuble(), null, registreFoncierService));
		model.addAttribute("editExonerationCommand", view);
		return "tiers/edition/pm/degrevement-exoneration/edit-exoneration";
	}

	private String showEditExoneration(Model model, EditExonerationView view) {
		final ExonerationIFONC exoneration = getExoneration(view.getIdExoneration());
		return showEditExoneration(model, exoneration, view);
	}

	@RequestMapping(value = "/edit-exoneration.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	public String doEditExoneration(Model model,
	                                @Valid @ModelAttribute("editExonerationCommand") EditExonerationView view,
	                                BindingResult bindingResult) {

		if (bindingResult.hasErrors()) {
			return showEditExoneration(model, view);
		}

		if (!SecurityHelper.isGranted(securityProviderInterface, Role.EXONERATIONS_IFONC)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits d'accès suffisants pour effectuer cette opération.");
		}

		final ExonerationIFONC exoneration = getExoneration(view.getIdExoneration());
		final Contribuable ctb = exoneration.getContribuable();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getNumero());

		// on ne doit pas pouvoir réutiliser la période de début de validité d'une donnée existante
		final ImmeubleRF immeuble = exoneration.getImmeuble();
		final List<ExonerationIFONC> autres = getAllegementsFonciers(ctb, immeuble, ExonerationIFONC.class, exoneration, false);
		if (autres.stream().anyMatch(deg -> deg.getDateDebut().year() == view.getAnneeDebut())) {
			bindingResult.rejectValue("anneeDebut", "error.degexo.exoneration.periode.debut.deja.utilisee");
			return showEditExoneration(model, exoneration, view);
		}

		// mise en place par rapport aux autres
		computeEditionInfluenceOnOthers(ctb, autres, exoneration, view);

		exoneration.setDateDebut(view.getDateDebut());
		exoneration.setDateFin(view.getDateFin());
		exoneration.setPourcentageExoneration(view.getPourcentageExoneration());

		return "redirect:edit-exonerations.do?idContribuable=" + ctb.getNumero() + "&idImmeuble=" + exoneration.getImmeuble().getId();
	}

	///////////////////////////////////////////////////////////
	//                                                       //
	// Edition des formulaires de demande de dégrèvement ICI //
	//                                                       //
	///////////////////////////////////////////////////////////

	@RequestMapping(value = "/edit-demandes-degrevement.do", method = RequestMethod.GET)
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	public String editDemandesDegrevement(Model model,
	                                      @RequestParam(value = "idContribuable") long idContribuable,
	                                      @RequestParam(value = "idImmeuble") long idImmeuble) {

		controllerUtils.checkAccesDossierEnEcriture(idContribuable);
		final Entreprise entreprise = getTiers(Entreprise.class, idContribuable, EntrepriseNotFoundException::new);
		final ImmeubleRF immeuble = getImmeuble(idImmeuble);

		final List<DemandeDegrevementICIView> demandes = getDemandesDegrevement(entreprise, immeuble, null, true).stream()
				.map(dd -> new DemandeDegrevementICIView(dd, infraService))
				.sorted(new AnnulableHelper.AnnulesApresWrappingComparator<>(Comparator.comparingInt(DemandeDegrevementICIView::getPeriodeFiscale).reversed()))
				.collect(Collectors.toList());

		final ResumeImmeubleView immeubleView = new ResumeImmeubleView(immeuble, null, registreFoncierService);

		// les droits qui lient cette entreprise et cet immeuble
		final List<DroitView> viewsDroits = buildListeDroits(entreprise, immeuble);

		model.addAttribute("idContribuable", idContribuable);
		model.addAttribute("demandesDegrevement", demandes);
		model.addAttribute("immeuble", immeubleView);
		model.addAttribute("droits", viewsDroits);
		return "tiers/edition/pm/degrevement-exoneration/edit-demandes-degrevement";
	}

	@RequestMapping(value = "/cancel-demande-degrevement.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	public String cancelDemandeDegrevement(@RequestParam(value = "id") long idDemande) {

		if (!SecurityHelper.isGranted(securityProviderInterface, Role.DEMANDES_DEGREVEMENT_ICI)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits d'accès suffisants pour effectuer cette opération.");
		}

		final DemandeDegrevementICI demande = getDemandeDegrevement(idDemande);
		final Entreprise entreprise = demande.getEntreprise();
		controllerUtils.checkAccesDossierEnEcriture(entreprise.getNumero());

		demande.setAnnule(true);
		return "redirect:/degrevement-exoneration/edit-demandes-degrevement.do?idContribuable=" + entreprise.getNumero() + "&idImmeuble=" + demande.getImmeuble().getId();
	}

	@InitBinder(value = "addDemandeDegrevementCommand")
	public void initAddDemandeDegrevementCommandBinder(WebDataBinder binder) {
		binder.setValidator(new AddDemandeDegrevementViewValidator());
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false, RegDateHelper.StringFormat.DISPLAY));
	}

	@RequestMapping(value = "/add-demande-degrevement.do", method = RequestMethod.GET)
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	public String addDemandeDegrevement(Model model,
	                                    @RequestParam(value = "idContribuable") long idContribuable,
	                                    @RequestParam(value = "idImmeuble") long idImmeuble) {

		controllerUtils.checkAccesDossierEnEcriture(idContribuable);
		final Entreprise entreprise = getTiers(Entreprise.class, idContribuable, EntrepriseNotFoundException::new);
		final AddDemandeDegrevementView view = new AddDemandeDegrevementView(entreprise.getNumero(), idImmeuble);
		view.setDelaiRetour(RegDate.get().addDays(parametreAppService.getDelaiRetourDemandeDegrevementICI()));
		return showAddDemandeDegrevement(model,
		                                 getDemandesDegrevement(entreprise, idImmeuble, null, false),
		                                 view);
	}

	private String showAddDemandeDegrevement(Model model, List<DemandeDegrevementICI> autres, AddDemandeDegrevementView view) {
		model.addAttribute("periodes", buildPeriodesFiscalesAutorisees(autres));
		model.addAttribute("idContribuable", view.getIdContribuable());
		model.addAttribute("immeuble", new ResumeImmeubleView(getImmeuble(view.getIdImmeuble()), null, registreFoncierService));
		model.addAttribute("addDemandeDegrevementCommand", view);
		return "tiers/edition/pm/degrevement-exoneration/add-demande-degrevement";
	}

	@RequestMapping(value = "/add-demande-degrevement.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	public String doAddDemandeDegrevement(Model model,
	                                      @Valid @ModelAttribute(value = "addDemandeDegrevementCommand") AddDemandeDegrevementView view,
	                                      BindingResult bindingResult) throws IOException {

		if (!SecurityHelper.isGranted(securityProviderInterface, Role.DEMANDES_DEGREVEMENT_ICI)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits d'accès suffisants pour effectuer cette opération.");
		}

		controllerUtils.checkAccesDossierEnEcriture(view.getIdContribuable());
		final Entreprise entreprise = getTiers(Entreprise.class, view.getIdContribuable(), EntrepriseNotFoundException::new);
		if (bindingResult.hasErrors()) {
			return showAddDemandeDegrevement(model,
			                                 getDemandesDegrevement(entreprise, view.getIdImmeuble(), null, false),
			                                 view);
		}

		final ImmeubleRF immeuble = getImmeuble(view.getIdImmeuble());

		// on ne doit pas pouvoir réutiliser la période de début de validité d'une donnée existante
		final List<DemandeDegrevementICI> autres = getDemandesDegrevement(entreprise, immeuble, null, false);
		if (autres.stream().anyMatch(exo -> Objects.equals(exo.getPeriodeFiscale(), view.getPeriodeFiscale()))) {
			bindingResult.rejectValue("periodeFiscale", "error.demande.degrevement.periode.fiscale.deja.utilisee");
			return showAddDemandeDegrevement(model,
			                                 getDemandesDegrevement(entreprise, immeuble, null, false),
			                                 view);
		}

		final EditiqueResultat resultat;
		try {
			resultat = autreDocumentFiscalService.envoyerDemandeDegrevementICIOnline(entreprise, immeuble, view.getPeriodeFiscale(), RegDate.get(), view.getDelaiRetour());
		}
		catch (AutreDocumentFiscalException e) {
			throw new ActionException("Impossoble d'imprimer le formulaire de demande de dégrèvement ICI", e);
		}

		final String redirect = String.format("redirect:/degrevement-exoneration/edit-demandes-degrevement.do?idContribuable=%d&idImmeuble=%d",
	                                          entreprise.getNumero(),
	                                          immeuble.getId());

		final RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatReroutageInbox> inbox = res -> redirect;

		final RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatErreur> erreur = res -> {
			Flash.error(EditiqueErrorHelper.getMessageErreurEditique(res));
			return redirect;
		};

		return retourEditiqueControllerHelper.traiteRetourEditiqueAfterRedirect(resultat,
		                                                                        "formulaire-demande-dégrèvement",
		                                                                        redirect,
		                                                                        inbox,
		                                                                        null,
		                                                                        erreur);
	}

	@InitBinder(value = "editDemandeDegrevementCommand")
	public void initEditDemandeDegrevementCommandBinder(WebDataBinder binder) {
		binder.setValidator(new EditDemandeDegrevementViewValidator());
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false, RegDateHelper.StringFormat.DISPLAY));
	}

	@RequestMapping(value = "/edit-demande-degrevement.do", method = RequestMethod.GET)
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	public String editDemandeDegrevement(Model model, @RequestParam(value = "id") long idDemande) {
		final DemandeDegrevementICI demande = getDemandeDegrevement(idDemande);
		return showEditDemandeDegrevement(model, demande, new EditDemandeDegrevementView(demande));
	}

	private String showEditDemandeDegrevement(Model model, DemandeDegrevementICI demande, EditDemandeDegrevementView view) {
		model.addAttribute("idContribuable", demande.getEntreprise().getNumero());
		model.addAttribute("immeuble", new ResumeImmeubleView(demande.getImmeuble(), null, registreFoncierService));
		model.addAttribute("editDemandeDegrevementCommand", view);
		return "tiers/edition/pm/degrevement-exoneration/edit-demande-degrevement";
	}

	private String showEditDemandeDegrevement(Model model, EditDemandeDegrevementView view) {
		final DemandeDegrevementICI demande = getDemandeDegrevement(view.getIdDemandeDegrevement());
		return showEditDemandeDegrevement(model, demande, view);
	}

	@RequestMapping(value = "/edit-demande-degrevement.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	public String doEditDemandeDegrevement(Model model,
	                                       @Valid @ModelAttribute(value = "editDemandeDegrevementCommand") EditDemandeDegrevementView view,
	                                       BindingResult bindingResult) {

		if (bindingResult.hasErrors()) {
			return showEditDemandeDegrevement(model, view);
		}

		if (!SecurityHelper.isGranted(securityProviderInterface, Role.DEMANDES_DEGREVEMENT_ICI)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits d'accès suffisants pour effectuer cette opération.");
		}

		final DemandeDegrevementICI demande = getDemandeDegrevement(view.getIdDemandeDegrevement());
		final Entreprise entreprise = demande.getEntreprise();
		final ImmeubleRF immeuble = demande.getImmeuble();
		controllerUtils.checkAccesDossierEnEcriture(entreprise.getNumero());

		// on ne doit pas pouvoir réutiliser la période fiscale d'une donnée existante
		final List<DemandeDegrevementICI> autres = getDemandesDegrevement(entreprise, immeuble, demande, false);
		if (autres.stream().anyMatch(exo -> Objects.equals(exo.getPeriodeFiscale(), view.getPeriodeFiscale()))) {
			bindingResult.rejectValue("periodeFiscale", "error.demande.degrevement.periode.fiscale.deja.utilisee");
			return showEditDemandeDegrevement(model, demande, view);
		}

		// la date de retour ne doit pas être avant la date d'émission
		if (view.getDateRetour() != null && view.getDateRetour().isBefore(demande.getDateEnvoi())) {
			bindingResult.rejectValue("dateRetour", "error.date.retour.anterieure.date.emission");
			return showEditDemandeDegrevement(model, demande, view);
		}

		demande.setDateRetour(view.getDateRetour());
		return "redirect:/degrevement-exoneration/edit-demandes-degrevement.do?idContribuable=" + entreprise.getNumero() + "&idImmeuble=" + immeuble.getId();
	}
}
