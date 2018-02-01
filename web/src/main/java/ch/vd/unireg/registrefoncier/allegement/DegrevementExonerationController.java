package ch.vd.unireg.registrefoncier.allegement;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
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

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;
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
import ch.vd.unireg.common.ActionException;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.ContribuableNotFoundException;
import ch.vd.unireg.common.ControllerUtils;
import ch.vd.unireg.common.Duplicable;
import ch.vd.unireg.common.EditiqueErrorHelper;
import ch.vd.unireg.common.EntrepriseNotFoundException;
import ch.vd.unireg.common.Equalator;
import ch.vd.unireg.common.Flash;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.common.RetourEditiqueControllerHelper;
import ch.vd.unireg.common.TiersNotFoundException;
import ch.vd.unireg.documentfiscal.AjouterEtatAutreDocumentFiscalView;
import ch.vd.unireg.documentfiscal.AutreDocumentFiscalException;
import ch.vd.unireg.documentfiscal.AutreDocumentFiscalManager;
import ch.vd.unireg.documentfiscal.AutreDocumentFiscalService;
import ch.vd.unireg.documentfiscal.AutreDocumentFiscalView;
import ch.vd.unireg.documentfiscal.DelaiDocumentFiscal;
import ch.vd.unireg.documentfiscal.EditionDelaiAutreDocumentFiscalView;
import ch.vd.unireg.documentfiscal.EtatAutreDocumentFiscal;
import ch.vd.unireg.documentfiscal.EtatAutreDocumentFiscalRetourne;
import ch.vd.unireg.editique.EditiqueResultat;
import ch.vd.unireg.editique.EditiqueResultatErreur;
import ch.vd.unireg.editique.EditiqueResultatReroutageInbox;
import ch.vd.unireg.foncier.AllegementFoncier;
import ch.vd.unireg.foncier.DegrevementICI;
import ch.vd.unireg.foncier.DemandeDegrevementICI;
import ch.vd.unireg.foncier.DonneesLoiLogement;
import ch.vd.unireg.foncier.DonneesUtilisation;
import ch.vd.unireg.foncier.ExonerationIFONC;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.parametrage.DelaisService;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.registrefoncier.DroitRF;
import ch.vd.unireg.registrefoncier.EstimationRF;
import ch.vd.unireg.registrefoncier.ImmeubleHelper;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.RegistreFoncierService;
import ch.vd.unireg.registrefoncier.SituationRF;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.view.ChoixImmeubleView;
import ch.vd.unireg.tiers.view.ImmeubleView;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.utils.DecimalNumberEditor;
import ch.vd.unireg.utils.IntegerEditor;
import ch.vd.unireg.utils.RegDateEditor;
import ch.vd.unireg.utils.WebContextUtils;

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
	private MessageSource messageSource;
	private DelaisService delaisService;
	private SessionFactory sessionFactory;

	private Validator editionValidator;

	// Certaines fonctions sont génériques et peuvent être partagées
	private AutreDocumentFiscalManager autreDocumentFiscalManager;

	private static final Comparator<ImmeubleView> IMMEUBLE_VIEW_COMPARATOR = Comparator.comparing(ImmeubleView::getNoParcelle)
			.thenComparing(Comparator.comparing(ImmeubleView::getIndex1, Comparator.nullsFirst(Comparator.naturalOrder())))
			.thenComparing(Comparator.comparing(ImmeubleView::getIndex2, Comparator.nullsFirst(Comparator.naturalOrder())))
			.thenComparing(Comparator.comparing(ImmeubleView::getIndex3, Comparator.nullsFirst(Comparator.naturalOrder())));

	private static final Equalator<Integer> INTEGER_EQUALATOR = Objects::equals;
	private static final Equalator<Long> LONG_EQUALATOR = Objects::equals;
	private static final Equalator<RegDate> DATE_EQUALATOR = Objects::equals;
	private static final Equalator<Boolean> BOOLEAN_EQUALATOR = Objects::equals;
	private static final Equalator<BigDecimal> BIGDECIMAL_EQUALATOR = (d1, d2) -> Objects.equals(d1, d2) || (d1 != null && d2 != null && d1.compareTo(d2) == 0);

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

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public void setDelaisService(DelaisService delaisService) {
		this.delaisService = delaisService;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public void setAutreDocumentFiscalManager(AutreDocumentFiscalManager autreDocumentFiscalManager) {
		this.autreDocumentFiscalManager = autreDocumentFiscalManager;
	}

	public void setEditionValidator(Validator editionValidator) {
		this.editionValidator = editionValidator;
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
		final Map<Long, List<DateRange>> droitsParImmeuble = new HashMap<>();

		final List<DroitRF> droits = registreFoncierService.getDroitsForCtb(ctb, true, false, false);
		for (DroitRF droit : droits) {
			if (droit.isAnnule()) {
				continue;
			}
			droit.getImmeubleList().forEach(i -> {
				immeublesParId.put(i.getId(), i);
				droitsParImmeuble.merge(i.getId(), Collections.singletonList(droit.getRangeMetier()), ListUtils::union);
			});
		}

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
				.filter(situation -> situation.getNoOfsCommune() == noOfsCommune)
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
				.map(demande -> new DemandeDegrevementICIView(demande, infraService, messageSource))
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
		return registreFoncierService.getDroitsForCtb(entreprise, false, false).stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(dt -> dt.getImmeubleList().contains(immeuble))
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
	private <T extends AllegementFoncier> List<T> getAllegementsFonciers(ContribuableImpositionPersonnesMorales contribuable, ImmeubleRF immeuble, Class<T> clazz, @Nullable T excluded, boolean avecAnnules) {
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
	private <T extends AllegementFoncier> List<T> getAllegementsFonciers(ContribuableImpositionPersonnesMorales contribuable, long idImmeuble, Class<T> clazz, @Nullable T excluded, boolean avecAnnules) {
		return contribuable.getAllegementsFonciers().stream()
				.filter(af -> avecAnnules || !af.isAnnule())
				.filter(af -> excluded == null || af != excluded)
				.filter(af -> clazz.isAssignableFrom(af.getClass()))
				.filter(af -> af.getImmeuble().getId() == idImmeuble)
				.map(clazz::cast)
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
	private static <T extends AllegementFoncier & Duplicable<T>> void computeEditionInfluenceOnOthers(ContribuableImpositionPersonnesMorales ctb,
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
	private static <T extends AllegementFoncier & Duplicable<T>> void computeAdditionInfluenceOnOthers(ContribuableImpositionPersonnesMorales ctb,
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
		final ContribuableImpositionPersonnesMorales contribuable = degrevement.getContribuable();
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

	@InitBinder(value = {"addDegrevementCommand", "editDegrevementCommand"})
	public void initAddDegrevementCommandBinder(WebDataBinder binder) {
		binder.setValidator(new AbstractEditDegrevementViewValidator());
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false, RegDateHelper.StringFormat.DISPLAY));
		binder.registerCustomEditor(Integer.class, "location.revenu", new IntegerEditor(true));
		binder.registerCustomEditor(Integer.class, "location.volume", new IntegerEditor(true));
		binder.registerCustomEditor(Integer.class, "location.surface", new IntegerEditor(true));
		binder.registerCustomEditor(Integer.class, "propreUsage.revenu", new IntegerEditor(true));
		binder.registerCustomEditor(Integer.class, "propreUsage.volume", new IntegerEditor(true));
		binder.registerCustomEditor(Integer.class, "propreUsage.surface", new IntegerEditor(true));
		binder.registerCustomEditor(BigDecimal.class, "location.pourcentage", new DecimalNumberEditor(2));
		binder.registerCustomEditor(BigDecimal.class, "location.pourcentageArrete", new DecimalNumberEditor(2));
		binder.registerCustomEditor(BigDecimal.class, "propreUsage.pourcentage", new DecimalNumberEditor(2));
		binder.registerCustomEditor(BigDecimal.class, "propreUsage.pourcentageArrete", new DecimalNumberEditor(2));
		binder.registerCustomEditor(BigDecimal.class, "loiLogement.pourcentageCaractereSocial", new DecimalNumberEditor(2));
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
		final ContribuableImpositionPersonnesMorales ctb = degrevement.getContribuable();
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

		// [SIFISC-24412] modification avec ou sans passage par une annulation ?
		if (mayEditInPlace(degrevement, view)) {
			copyData(degrevement, view);
		}
		else {
			final DegrevementICI copy = degrevement.duplicate();
			degrevement.setAnnule(true);
			copyData(copy, view);
			ctb.addAllegementFoncier(copy);
		}

		return "redirect:edit-degrevements.do?idContribuable=" + ctb.getNumero() + "&idImmeuble=" + degrevement.getImmeuble().getId();
	}

	private static void copyData(DegrevementICI destination, EditDegrevementView source) {
		destination.setDateDebut(source.getDateDebut());
		destination.setDateFin(source.getDateFin());
		destination.setLocation(source.getLocation());
		destination.setPropreUsage(source.getPropreUsage());
		destination.setLoiLogement(cleanupLoiLogement(source.getLoiLogement()));
		destination.setNonIntegrable(Boolean.FALSE);        // ré-initialisation du flag dans tous les cas
	}

	/**
	 * [SIFISC-24412] on vérifie s'il faut annuler la précédente valeur du dégrèvement avant d'en générer une nouvelle
	 * (l'alternative étant de modifier directement le dégrèvement <i>in-place</i>)
	 * @param oldValue valeur stockée en base
	 * @param newValue nouvelles valeurs à stocker
	 * @return si oui ou non il faut passer par un cycle d'annulation / remplacement pour ces modifications
	 */
	private static boolean mayEditInPlace(DegrevementICI oldValue, EditDegrevementView newValue) {
		// on devra faire passer par l'annulation si l'une au moins des conditions suivantes est remplie :
		// - une valeur arrêtée préalablement définie (= non-vide) est modifiée
		// - une autre valeur est modifiée, quelle que soit sa valeur précédente (vide ou pas)

		// mais il ne faut pas oublier la date de début... si elle change, tout change !
		if (!INTEGER_EQUALATOR.test(oldValue.getDateDebut().year(), newValue.getAnneeDebut())) {
			return false;
		}

		// d'abord on regarde les données de location
		final DonneesUtilisation oldLocation = oldValue.getLocation() != null ? oldValue.getLocation() : new DonneesUtilisation();
		final DonneesUtilisation newLocation = newValue.getLocation();
		if (oldLocation.getPourcentageArrete() != null && !BIGDECIMAL_EQUALATOR.test(oldLocation.getPourcentageArrete(), newLocation.getPourcentageArrete())) {
			return false;
		}
		if (!LONG_EQUALATOR.test(oldLocation.getRevenu(), newLocation.getRevenu())
				|| !LONG_EQUALATOR.test(oldLocation.getSurface(), newLocation.getSurface())
				|| !LONG_EQUALATOR.test(oldLocation.getVolume(), newLocation.getVolume())
				|| !BIGDECIMAL_EQUALATOR.test(oldLocation.getPourcentage(), newLocation.getPourcentage())) {
			return false;
		}

		// puis les données de propre usage
		final DonneesUtilisation oldPropreUsage = oldValue.getPropreUsage() != null ? oldValue.getPropreUsage() : new DonneesUtilisation();
		final DonneesUtilisation newPropreUsage = newValue.getPropreUsage();
		if (oldPropreUsage.getPourcentageArrete() != null && !BIGDECIMAL_EQUALATOR.test(oldPropreUsage.getPourcentageArrete(), newPropreUsage.getPourcentageArrete())) {
			return false;
		}
		if (!LONG_EQUALATOR.test(oldPropreUsage.getRevenu(), newPropreUsage.getRevenu())
				|| !LONG_EQUALATOR.test(oldPropreUsage.getSurface(), newPropreUsage.getSurface())
				|| !LONG_EQUALATOR.test(oldPropreUsage.getVolume(), newPropreUsage.getVolume())
				|| !BIGDECIMAL_EQUALATOR.test(oldPropreUsage.getPourcentage(), newPropreUsage.getPourcentage())) {
			return false;
		}

		// puis enfin les données de la loi sur le logement
		final DonneesLoiLogement oldLoiLogement = oldValue.getLoiLogement() != null ? oldValue.getLoiLogement() : new DonneesLoiLogement(Boolean.FALSE, null, null, null);
		final DonneesLoiLogement newLoiLogement = cleanupLoiLogement(newValue.getLoiLogement());
		if (!BOOLEAN_EQUALATOR.test(oldLoiLogement.getControleOfficeLogement(), newLoiLogement.getControleOfficeLogement())
				|| !DATE_EQUALATOR.test(oldLoiLogement.getDateEcheance(), newLoiLogement.getDateEcheance())
				|| !DATE_EQUALATOR.test(oldLoiLogement.getDateOctroi(), newLoiLogement.getDateOctroi())
				|| !BIGDECIMAL_EQUALATOR.test(oldLoiLogement.getPourcentageCaractereSocial(), newLoiLogement.getPourcentageCaractereSocial())) {
			return false;
		}

		// ok, tout est identique (ou les valeurs arrêtées on juste été ajoutées)
		return true;
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
		final ContribuableImpositionPersonnesMorales contribuable = exoneration.getContribuable();
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
		binder.registerCustomEditor(BigDecimal.class, "pourcentageExoneration", new DecimalNumberEditor(2));
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
		binder.registerCustomEditor(BigDecimal.class, "pourcentageExoneration", new DecimalNumberEditor(2));
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
		final ContribuableImpositionPersonnesMorales ctb = exoneration.getContribuable();
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

		if (mayEditInPlace(exoneration, view)) {
			copyData(exoneration, view);
		}
		else {
			final ExonerationIFONC copy = exoneration.duplicate();
			exoneration.setAnnule(true);
			copyData(copy, view);
			ctb.addAllegementFoncier(copy);
		}

		return "redirect:edit-exonerations.do?idContribuable=" + ctb.getNumero() + "&idImmeuble=" + exoneration.getImmeuble().getId();
	}

	private static void copyData(ExonerationIFONC destination, EditExonerationView source) {
		destination.setDateDebut(source.getDateDebut());
		destination.setDateFin(source.getDateFin());
		destination.setPourcentageExoneration(source.getPourcentageExoneration());
	}

	/**
	 * @param oldValue la valeur précédemment stockée en base
	 * @param newValue la nouvelle valeur proposée par l'utilisateur
	 * @return si oui ou non on peut se permettre de procéder à une mise à jour "in-place"
	 */
	private static boolean mayEditInPlace(ExonerationIFONC oldValue, EditExonerationView newValue) {
		return INTEGER_EQUALATOR.test(oldValue.getDateDebut().year(), newValue.getAnneeDebut()) && BIGDECIMAL_EQUALATOR.test(oldValue.getPourcentageExoneration(), newValue.getPourcentageExoneration());
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
				.map(dd -> new DemandeDegrevementICIView(dd, infraService, messageSource))
				.sorted(new AnnulableHelper.AnnulesApresWrappingComparator<>(Comparator.comparingInt(DemandeDegrevementICIView::getPeriodeFiscale).reversed()))
				.collect(Collectors.toList());
		final Set<Integer> periodesActives = demandes.stream()    // les périodes fiscales pour lesquelles des demandes non-annulées existent déjà
				.filter(AnnulableHelper::nonAnnule)
				.map(AutreDocumentFiscalView::getPeriodeFiscale)
				.collect(Collectors.toSet());

		final ResumeImmeubleView immeubleView = new ResumeImmeubleView(immeuble, null, registreFoncierService);

		// les droits qui lient cette entreprise et cet immeuble
		final List<DroitView> viewsDroits = buildListeDroits(entreprise, immeuble);

		model.addAttribute("idContribuable", idContribuable);
		model.addAttribute("demandesDegrevement", demandes);
		model.addAttribute("immeuble", immeubleView);
		model.addAttribute("droits", viewsDroits);
		model.addAttribute("periodesActives", periodesActives);
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
		                                                                        true,
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
		return showEditDemandeDegrevement(model, demande, new EditDemandeDegrevementView(demande, infraService, messageSource));
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

	@InitBinder(value = "ajouterView")
	public void initAjouterDelaiBinder(WebDataBinder binder) {
		binder.setValidator(editionValidator);
		binder.registerCustomEditor(RegDate.class, "dateDemande", new RegDateEditor(false, false, false, RegDateHelper.StringFormat.DISPLAY));
		binder.registerCustomEditor(RegDate.class, "delaiAccordeAu", new RegDateEditor(true, false, false, RegDateHelper.StringFormat.DISPLAY));
		binder.registerCustomEditor(RegDate.class, "ancienDelaiAccorde", new RegDateEditor(true, false, false, RegDateHelper.StringFormat.DISPLAY));
	}
	@InitBinder(value = "ajouterQuittance")
	public void initAjouterQuittanceBinder(WebDataBinder binder) {
		binder.setValidator(editionValidator);
		binder.registerCustomEditor(RegDate.class, "dateRetour", new RegDateEditor(false, false, false, RegDateHelper.StringFormat.DISPLAY));
	}

	/**
	 * Affiche un écran qui permet de choisir les paramètres pour l'ajout d'une demande de délai
	 */
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/delai/ajouter.do", method = RequestMethod.GET)
	public String ajouterDelai(@RequestParam("id") long id,
	                               Model model) throws AccessDeniedException {

		if (!SecurityHelper.isGranted(securityProviderInterface, Role.DEMANDES_DEGREVEMENT_ICI)) {
			throw new AccessDeniedException("vous n'avez pas le droit d'ajouter un délai à une demande de dégrèvement ICI.");
		}

		final DemandeDegrevementICI doc = getDemandeDegrevement(id);

		final Entreprise ctb = (Entreprise) doc.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		final RegDate delaiAccordeAu = determineDateAccordDelaiParDefaut(doc.getDelaiAccordeAu());
		model.addAttribute("ajouterView", new EditionDelaiAutreDocumentFiscalView(doc, delaiAccordeAu));
		return "tiers/edition/pm/degrevement-exoneration/delai/ajouter";
	}

	/**
	 * [SIFISC-18869] la date par défaut du délai accordé (sursis ou pas) ne doit de toute façon pas être dans le passé
	 * @param delaiPrecedent la date actuelle du délai accordé
	 * @return la nouvelle date à proposer comme délai par défaut
	 */
	private RegDate determineDateAccordDelaiParDefaut(RegDate delaiPrecedent) {
		final RegDate delaiNormal = delaisService.getDateFinDelaiRetourDeclarationImpotPMEmiseManuellement(delaiPrecedent);
		return RegDateHelper.maximum(delaiNormal, RegDate.get(), NullDateBehavior.EARLIEST);
	}

	/**
	 * Ajoute un délai
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/delai/ajouter.do", method = RequestMethod.POST)
	public String ajouterDelai(@Valid @ModelAttribute("ajouterView") final EditionDelaiAutreDocumentFiscalView view,
	                                    BindingResult result, Model model, HttpServletResponse response) throws Exception {

		if (!SecurityHelper.isGranted(securityProviderInterface, Role.DEMANDES_DEGREVEMENT_ICI)) {
			throw new AccessDeniedException("vous n'avez pas le droit de gestion des delais d'une demande de dégrèvement ICI");
		}

		final Long id = view.getIdDocumentFiscal();

		if (result.hasErrors()) {
			final DemandeDegrevementICI documentFiscal = getDemandeDegrevement(id);
			view.resetDocumentInfo(documentFiscal);
			return "tiers/edition/pm/degrevement-exoneration/delai/ajouter";
		}

		// Vérifie les paramètres
		final DemandeDegrevementICI doc = getDemandeDegrevement(id);

		final Entreprise ctb = (Entreprise) doc.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		// On ajoute le délai
		final RegDate delaiAccordeAu = view.getDelaiAccordeAu();
		autreDocumentFiscalManager.saveNouveauDelai(id, view.getDateDemande(), delaiAccordeAu, EtatDelaiDocumentFiscal.ACCORDE);
		return "redirect:/degrevement-exoneration/edit-demande-degrevement.do?id=" + id;
	}

	/**
	 * Annule un délai
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/delai/annuler.do", method = RequestMethod.POST)
	public String annulerDelai(@RequestParam("id") long id) throws AccessDeniedException {

		if (!SecurityHelper.isGranted(securityProviderInterface, Role.DEMANDES_DEGREVEMENT_ICI)) {
			throw new AccessDeniedException("vous n'avez pas le droit de gestion des delais d'une demande de dégrèvement ICI.");
		}

		final DelaiDocumentFiscal delai = (DelaiDocumentFiscal) sessionFactory.getCurrentSession().get(DelaiDocumentFiscal.class, id);
		if (delai == null) {
			throw new IllegalArgumentException("Le délai n°" + id + " n'existe pas.");
		}

		final Entreprise ctb = (Entreprise) delai.getDocumentFiscal().getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		delai.setAnnule(true);

		return "redirect:/degrevement-exoneration/edit-demande-degrevement.do?id=" + delai.getDocumentFiscal().getId();
	}

	/**
	 * Affiche un écran qui permet de quittancer une demande de dégrèvement ICI.
	 */
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/etat/ajouter-quittance.do", method = RequestMethod.GET)
	public String ajouterEtat(@RequestParam("id") long id, Model model) throws AccessDeniedException {

		if (!SecurityHelper.isGranted(securityProviderInterface, Role.DEMANDES_DEGREVEMENT_ICI)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec de quittancement des demandes de dégrèvement ICI.");
		}

		final DemandeDegrevementICI doc = getDemandeDegrevement(id);

		final Entreprise ctb = (Entreprise) doc.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		AjouterEtatAutreDocumentFiscalView view = new AjouterEtatAutreDocumentFiscalView(doc, infraService, messageSource);
		if (view.getDateRetour() == null) {
			view.setDateRetour(RegDate.get());
		}

		model.addAttribute("ajouterQuittance", view);

		return "tiers/edition/pm/degrevement-exoneration/etat/ajouter-quittance";
	}

	/**
	 * Quittance d'un autre document fiscal avec suivi
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/etat/ajouter-quittance.do", method = RequestMethod.POST)
	public String ajouterEtat(@Valid @ModelAttribute("ajouterQuittance") final AjouterEtatAutreDocumentFiscalView view, BindingResult result, Model model) throws AccessDeniedException {

		if (!SecurityHelper.isGranted(securityProviderInterface, Role.DEMANDES_DEGREVEMENT_ICI)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec de quittancement des demandes de dégrèvement ICI.");
		}

		if (result.hasErrors()) {
			final DemandeDegrevementICI doc = getDemandeDegrevement(view.getId());
			view.resetDocumentInfo(doc, infraService, messageSource);
			return "tiers/edition/pm/degrevement-exoneration/etat/ajouter-quittance";
		}

		final DemandeDegrevementICI doc = getDemandeDegrevement(view.getId());

		final Entreprise ctb = (Entreprise) doc.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		// On quittance
		final boolean success = autreDocumentFiscalManager.quittanceDemandeDegrevement(doc.getId(), view.getDateRetour());
		if (success) {
			Flash.message(String.format("La demande de dégrèvement ICI n°%s a été quittancée avec succès.", FormatNumeroHelper.numeroCTBToDisplay(doc.getId())));
		}
		else {
			Flash.warning(String.format("La demande de dégrèvement ICI n°%s, étant déjà retournée en date du %s, n'a pas été quittancée à nouveau.",
			                            FormatNumeroHelper.numeroCTBToDisplay(doc.getId()), RegDateHelper.dateToDisplayString(doc.getDateRetour())));
		}

		return "redirect:/degrevement-exoneration/edit-demande-degrevement.do?id=" + doc.getId();
	}

	/**
	 * Annuler le quittancement spécifié.
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/etat/annuler-quittance.do", method = RequestMethod.POST)
	public String annulerQuittancement(@RequestParam("id") final long id) throws Exception {

		if (!SecurityHelper.isGranted(securityProviderInterface, Role.DEMANDES_DEGREVEMENT_ICI)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec de quittancement des demandes de dégrèvement ICI.");
		}

		// Vérifie les paramètres
		final EtatAutreDocumentFiscal etat = (EtatAutreDocumentFiscal) sessionFactory.getCurrentSession().get(EtatAutreDocumentFiscal.class, id);
		if (etat == null) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.etat.inexistant", null, WebContextUtils.getDefaultLocale()));
		}
		if (!(etat instanceof EtatAutreDocumentFiscalRetourne)) {
			throw new IllegalArgumentException("Seuls les quittancements peuvent être annulés.");
		}

		final DemandeDegrevementICI doc = getDemandeDegrevement(etat.getAutreDocumentFiscal().getId());
		final Entreprise ctb = (Entreprise) doc.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		// On annule le quittancement
		final EtatAutreDocumentFiscalRetourne retour = (EtatAutreDocumentFiscalRetourne) etat;
		retour.setAnnule(true);

		Flash.message("Le quittancement du " + RegDateHelper.dateToDisplayString(retour.getDateObtention()) + " a été annulé.");
		return "redirect:/degrevement-exoneration/edit-demande-degrevement.do?id=" + doc.getId();
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/annuler.do", method = RequestMethod.POST)
	public String annuler(@RequestParam("id") long id) throws AccessDeniedException {

		if (!SecurityHelper.isAnyGranted(securityProviderInterface, Role.DEMANDES_DEGREVEMENT_ICI)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec de consultation pour l'application Unireg");
		}

		final DemandeDegrevementICI doc = getDemandeDegrevement(id);

		// vérification des droits en écriture
		final Entreprise ctb = (Entreprise) doc.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		// annulation de l'autre document fiscal
		autreDocumentFiscalManager.annulerAutreDocumentFiscal(doc);

		return "redirect:/degrevement-exoneration/edit-demandes-degrevement.do?idContribuable=" + ctb.getId() + " &idImmeuble=" + doc.getImmeuble().getId();
	}

	/**
	 * Désannuler une demande de dégrèvement ICI.
	 *
	 * @param id l'id du document fiscal à désannuler
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/desannuler.do", method = RequestMethod.POST)
	public String desannuler(@RequestParam("id") long id) throws AccessDeniedException {

		if (!SecurityHelper.isGranted(securityProviderInterface, Role.DEMANDES_DEGREVEMENT_ICI)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec de désannulation des demandes de dégrèvement ICI.");
		}

		final DemandeDegrevementICI doc = getDemandeDegrevement(id);

		if (!doc.isAnnule()) {
			throw new IllegalArgumentException("La demande de dégrèvement ICI n°" + id + " n'est pas annulée.");
		}

		// vérification des droits en écriture
		final Entreprise ctb = (Entreprise) doc.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		// [SIFISC-27974] on vérifie qu'il n'y a pas une autre demande de dégrèvement déjà active pour la période fiscale considérée et l'immeuble considéré
		final List<DemandeDegrevementICI> demandesActives = ctb.getAutresDocumentsFiscaux(DemandeDegrevementICI.class, false, false);
		if (demandesActives.stream()
				.filter(d -> Objects.equals(d.getImmeuble().getId(), doc.getImmeuble().getId()))
				.anyMatch(d -> Objects.equals(d.getPeriodeFiscale(), doc.getPeriodeFiscale()))) {
			Flash.error("Impossible de désannuler la demande spécifiée car il existe déjà une demande active pour la période fiscale " + doc.getPeriodeFiscale());
		}
		else {
			// désannulation de l'autre document fiscal
			autreDocumentFiscalManager.desannulerAutreDocumentFiscal(doc);
		}

		return "redirect:/degrevement-exoneration/edit-demandes-degrevement.do?idContribuable=" + ctb.getId() + " &idImmeuble=" + doc.getImmeuble().getId();
	}

	/**
	 * Imprime un duplicata de demande de dégrèvement
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/duplicata.do", method = RequestMethod.POST)
	public String duplicataDemandeDegrevement(@RequestParam("id") long id,
	                                                   HttpServletResponse response) throws Exception {

		if (!SecurityHelper.isAnyGranted(securityProviderInterface, Role.DEMANDES_DEGREVEMENT_ICI)) {
			throw new AccessDeniedException("Vous ne possédez pas le droit IfoSec pour imprimer des duplicata de demande de dégrèvement.");
		}

		final DemandeDegrevementICI doc = getDemandeDegrevement(id);

		// vérification des droits en écriture
		final Entreprise ctb = (Entreprise) doc.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		final EditiqueResultat resultat = autreDocumentFiscalManager.envoieImpressionLocalDuplicataDemandeDegrevement(id);
		final DegrevementExonerationController.RedirectEditDemandeDegrevement inbox = new DegrevementExonerationController.RedirectEditDemandeDegrevement(id);
		final DegrevementExonerationController.RedirectEditDemandeDegrevementApresErreur erreur = new DegrevementExonerationController.RedirectEditDemandeDegrevementApresErreur(id, messageSource);
		return retourEditiqueControllerHelper.traiteRetourEditique(resultat, response, "lb", inbox, null, erreur);
	}

	private static class RedirectEditDemandeDegrevement implements RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatReroutageInbox> {
		private final long id;

		public RedirectEditDemandeDegrevement(long id) {
			this.id = id;
		}

		@Override
		public String doJob(EditiqueResultatReroutageInbox resultat) {
			return "redirect:/degrevement-exoneration/edit-demande-degrevement.do?id=" + id;
		}
	}

	private static class RedirectEditDemandeDegrevementApresErreur implements RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatErreur> {
		private final long id;
		private final MessageSource messageSource;

		public RedirectEditDemandeDegrevementApresErreur(long id, MessageSource messageSource) {
			this.id = id;
			this.messageSource = messageSource;
		}

		@Override
		public String doJob(EditiqueResultatErreur resultat) {
			final String message = messageSource.getMessage("global.error.communication.editique", null, WebContextUtils.getDefaultLocale());
			Flash.error(message);
			return "redirect:/degrevement-exoneration/edit-demande-degrevement.do?id=" + id;
		}
	}
}
