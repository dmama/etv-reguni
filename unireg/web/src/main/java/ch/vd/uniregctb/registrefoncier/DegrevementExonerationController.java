package ch.vd.uniregctb.registrefoncier;

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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.common.ContribuableNotFoundException;
import ch.vd.uniregctb.common.EntrepriseNotFoundException;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.foncier.AllegementFoncier;
import ch.vd.uniregctb.foncier.DegrevementICI;
import ch.vd.uniregctb.foncier.DemandeDegrevementICI;
import ch.vd.uniregctb.foncier.ExonerationIFONC;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.view.ChoixImmeubleView;
import ch.vd.uniregctb.tiers.view.ImmeubleView;

@Controller
@RequestMapping(value = "/degrevement-exoneration")
public class DegrevementExonerationController {

	private RegistreFoncierService registreFoncierService;
	private HibernateTemplate hibernateTemplate;
	private ServiceInfrastructureService infraService;

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

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/immeubles.do", method = RequestMethod.GET)
	@ResponseBody
	public ChoixImmeubleView getImmeublesSurCommune(@RequestParam(value = "ctb") long idCtb,
	                                                @RequestParam(value = "ofsCommune") int ofsCommune,
	                                                @RequestParam(value = "noParcelle", required = false) Integer noParcelle,
	                                                @RequestParam(value = "index1", required = false) Integer index1,
	                                                @RequestParam(value = "index2", required = false) Integer index2,
	                                                @RequestParam(value = "index3", required = false) Integer index3) {

		final Contribuable ctb = hibernateTemplate.get(Contribuable.class, idCtb);
		if (ctb == null) {
			throw new ContribuableNotFoundException(idCtb);
		}

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

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/visu.do", method = RequestMethod.GET)
	public String showDetailDegrevementsExonerations(Model model,
	                                                 @RequestParam(value = "idCtb") long idContribuable,
	                                                 @RequestParam(value = "idImmeuble") long idImmeuble) {

		final Entreprise entreprise = hibernateTemplate.get(Entreprise.class, idContribuable);
		if (entreprise == null) {
			throw new EntrepriseNotFoundException(idContribuable);
		}

		final ImmeubleRF immeuble = hibernateTemplate.get(ImmeubleRF.class, idImmeuble);
		if (immeuble == null) {
			throw new ObjectNotFoundException("Immeuble inconnu avec l'identifiant " + idImmeuble);
		}

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

		final Comparator<DateRange> dateRangeComparator = new DateRangeComparator<>(DateRangeComparator.CompareOrder.DESCENDING);

		//noinspection unchecked
		final List<DegrevementICI> degrevements = (List<DegrevementICI>) allegements.getOrDefault(DegrevementICI.class, Collections.emptyList());
		final List<DegrevementICIView> viewsDegrevements = degrevements.stream()
				.map(DegrevementICIView::new)
				.sorted(dateRangeComparator)
				.collect(Collectors.toList());

		//noinspection unchecked
		final List<ExonerationIFONC> exonerations = (List<ExonerationIFONC>) allegements.getOrDefault(ExonerationIFONC.class, Collections.emptyList());
		final List<ExonerationIFONCView> viewsExonerations = exonerations.stream()
				.map(ExonerationIFONCView::new)
				.sorted(dateRangeComparator)
				.collect(Collectors.toList());

		// l'immeuble lui-même
		final ResumeImmeubleView immeubleView = new ResumeImmeubleView(immeuble, null, registreFoncierService);

		model.addAttribute("idContribuable", idContribuable);
		model.addAttribute("demandesDegrevement", demandes);
		model.addAttribute("degrevements", viewsDegrevements);
		model.addAttribute("exonerations", viewsExonerations);
		model.addAttribute("immeuble", immeubleView);
		return "tiers/visualisation/pm/detail-degrevement-exoneration";
	}
}
