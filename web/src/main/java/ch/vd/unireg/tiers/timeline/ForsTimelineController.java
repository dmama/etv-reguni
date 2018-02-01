package ch.vd.uniregctb.tiers.timeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.registre.base.date.DateConstants;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.metier.assujettissement.SourcierPur;
import ch.vd.uniregctb.metier.piis.PeriodeImpositionImpotSource;
import ch.vd.uniregctb.metier.piis.PeriodeImpositionImpotSourceService;
import ch.vd.uniregctb.metier.piis.PeriodeImpositionImpotSourceServiceException;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.tiers.ForGestion;
import ch.vd.uniregctb.tiers.NatureTiers;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Contrôleur pour l'affichage de l'historique des fors fiscaux et des assujettissements d'un contribuable
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@Controller
public class ForsTimelineController {

//	private final Logger LOGGER = LoggerFactory.getLogger(ForsTimelineController.class);

	public static final String ID_PARAMETER = "id";
	public static final String FOR_PRINT = "print";
	public static final String TITLE = "title";
	public static final String DESCRIPTION = "description";

	private TiersDAO dao;
	private TiersService tiersService;
	private AssujettissementService assujettissementService;
	private PeriodeImpositionService periodeImpositionService;
	private PeriodeImpositionImpotSourceService periodeImpositionImpotSourceService;
	private ControllerUtils controllerUtils;
	private RegDate bigBangPersonnesPhysiques;
	private RegDate bigBangPersonnesMorales;

	@RequestMapping(value = "/fors/timeline-debug.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String indexDebug(Model mav,
	                         @RequestParam(ID_PARAMETER) Long id,
	                         @RequestParam(value = "invertedTime", defaultValue = "true") boolean invertedTime,
	                         @RequestParam(value = "showForsGestion", defaultValue = "true") boolean showForsGestion,
	                         @RequestParam(value = "showAssujettissementsSource", defaultValue = "false") boolean showAssujettissementsSource,
	                         @RequestParam(value = "showAssujettissementsRole", defaultValue = "false") boolean showAssujettissementsRole,
	                         @RequestParam(value = "showAssujettissements", defaultValue = "true") boolean showAssujettissements,
	                         @RequestParam(value = "showPeriodesImposition", defaultValue = "false") boolean showPeriodesImposition,
	                         @RequestParam(value = "showPeriodesImpositionIS", defaultValue = "false") boolean showPeriodesImpositionIS,
	                         @RequestParam(value = FOR_PRINT, required = false) Boolean forPrint,
	                         @RequestParam(value = TITLE, required = false) String title,
	                         @RequestParam(value = DESCRIPTION, required = false) String description) throws AccessDeniedException {

		if (forPrint != null && forPrint) { // on veut voir tous les assujettissements dans la vue pour impression
			showAssujettissementsSource = true;
			showAssujettissementsRole = true;
		}

		return commonTimeline(mav, id, invertedTime, showForsGestion, showAssujettissementsSource, showAssujettissementsRole, showAssujettissements, showPeriodesImposition, showPeriodesImpositionIS,
		                      forPrint, title, description, true);
	}

	@RequestMapping(value = "/fors/timeline.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String index(Model mav,
	                    @RequestParam(ID_PARAMETER) Long id,
	                    @RequestParam(value = "invertedTime", defaultValue = "true") boolean invertedTime,
	                    @RequestParam(value = "showForsGestion", defaultValue = "true") boolean showForsGestion,
	                    @RequestParam(value = "showAssujettissements", defaultValue = "true") boolean showAssujettissements,
	                    @RequestParam(value = "showPeriodesImposition", defaultValue = "false") boolean showPeriodesImposition,
	                    @RequestParam(value = "showPeriodesImpositionIS", defaultValue = "false") boolean showPeriodesImpositionIS,
	                    @RequestParam(value = FOR_PRINT, required = false) Boolean forPrint,
	                    @RequestParam(value = TITLE, required = false) String title,
	                    @RequestParam(value = DESCRIPTION, required = false) String description) throws AccessDeniedException {

		return commonTimeline(mav, id, invertedTime, showForsGestion, false, false, showAssujettissements, showPeriodesImposition, showPeriodesImpositionIS, forPrint, title, description, false);
	}

	private RegDate determineBigBang(boolean debugMode, Tiers tiers) {
		if (debugMode) {
			return DateConstants.DEFAULT_VALIDITY_RANGE.getDateDebut();
		}
		if (tiers instanceof ContribuableImpositionPersonnesMorales) {
			return bigBangPersonnesMorales;
		}
		return bigBangPersonnesPhysiques;
	}

	private String commonTimeline(Model mav, Long id, boolean invertedTime, boolean showForsGestion,
	                              boolean showAssujettissementsSource, boolean showAssujettissementsRole,
	                              boolean showAssujettissements, boolean showPeriodesImposition,
	                              boolean showPeriodesImpositionIS,
	                              Boolean forPrint, String title, String description,
	                              boolean debugMode) throws AccessDeniedException {

		controllerUtils.checkAccesDossierEnLecture(id);

		final Tiers tiers = id == null ? null : dao.get(id);
		final RegDate bigBang = determineBigBang(debugMode, tiers);
		final ForsTimelineView bean = new ForsTimelineView(invertedTime, showForsGestion, showAssujettissementsSource, showAssujettissementsRole, showAssujettissements, showPeriodesImposition,
		                                                   showPeriodesImpositionIS, bigBang);

		if (forPrint != null) {
			bean.setForPrint(forPrint);
		}

		if (title != null) {
			bean.setTitle(title);
		}

		if (description != null) {
			bean.setDescription(description);
		}

		if (tiers != null) {
			fillTimeline(bean, tiers, bigBang);
		}
		mav.addAttribute("command", bean);
		mav.addAttribute("debugAssujettissement", debugMode);

		return "tiers/visualisation/fiscal/timeline";
	}

	/**
	 * Remplit les structures de données nécessaire à l'affichage de l'historique des fors d'un tiers
	 */
	private void fillTimeline(ForsTimelineView bean, Tiers tiers, RegDate bigBang) {

		bean.setTiersId(tiers.getNumero());

		// [SIFISC-11149] on veut pouvoir masquer des trucs selon le type de tiers
		final NatureTiers natureTiers = tiers.getNatureTiers();
		bean.setNatureTiers(natureTiers);

		// Extraction des fors fiscaux
		final List<ForFiscal> forsFiscaux = tiers.getForsFiscauxNonAnnules(true);

		// Extraction des fors de gestion
		final List<ForGestion> forsGestion = bean.isShowForsGestion() ? tiersService.getForsGestionHisto(tiers) : Collections.emptyList();

		// Extraction de l'assujettissement
		final List<SourcierPur> assujettissementsSource = new ArrayList<>();
		final List<Assujettissement> assujettissementsRole = new ArrayList<>();
		final List<Assujettissement> assujettissements = new ArrayList<>();

		if (tiers instanceof ContribuableImpositionPersonnesPhysiques) {
			final ContribuableImpositionPersonnesPhysiques contribuable = (ContribuableImpositionPersonnesPhysiques) tiers;
			if (bean.isShowAssujettissementsSource()) {
				try {
					final List<SourcierPur> list = assujettissementService.determineSource(contribuable);
					if (list != null) {
						assujettissementsSource.addAll(list);
					}
				}
				catch (AssujettissementException e) {
					bean.addException(e);
				}
			}
			if (bean.isShowAssujettissementsRole()) {
				try {
					final List<Assujettissement> list = assujettissementService.determineRole(contribuable);
					if (list != null) {
						assujettissementsRole.addAll(list);
					}
				}
				catch (AssujettissementException e) {
					bean.addException(e);
				}
			}
		}
		if (tiers instanceof Contribuable) {
			if (bean.isShowAssujettissements()) {
				final Contribuable contribuable = (Contribuable) tiers;
				try {
					final List<Assujettissement> list = assujettissementService.determine(contribuable);
					if (list != null) {
						assujettissements.addAll(list);
					}
				}
				catch (AssujettissementException e) {
					bean.addException(e);
				}
			}
		}

		// Extraction des périodes d'imposition
		final List<PeriodeImposition> periodesImposition = new ArrayList<>();
		final List<PeriodeImpositionImpotSource> periodesImpositionIS = new ArrayList<>();

		if (bean.isShowPeriodesImposition() && tiers instanceof Contribuable) {
			final Contribuable contribuable = (Contribuable) tiers;
			final RegDate debutActivite = contribuable.getDateDebutActivite();
			if (debutActivite != null) {
				try {
					final List<PeriodeImposition> list = periodeImpositionService.determine(contribuable);
					if (list != null) {
						periodesImposition.addAll(list);
					}
				}
				catch (AssujettissementException e) {
					bean.addException(e);
				}
			}
		}
		if (bean.isShowPeriodesImpositionIS() && tiers instanceof PersonnePhysique) {
			final PersonnePhysique pp = (PersonnePhysique) tiers;
			try {
				periodesImpositionIS.addAll(periodeImpositionImpotSourceService.determine(pp));
			}
			catch (PeriodeImpositionImpotSourceServiceException e) {
				bean.addException(new AssujettissementException(e));
			}
		}

		// Calcul des différents ranges de l'axe du temps
		final List<DateRange> ranges = new ArrayList<>();
		ranges.addAll(filter(forsFiscaux, bigBang));
		ranges.addAll(filter(forsGestion, bigBang));
		ranges.addAll(filter(assujettissementsSource, bigBang));
		ranges.addAll(filter(assujettissementsRole, bigBang));
		ranges.addAll(filter(assujettissements, bigBang));
		ranges.addAll(filter(periodesImposition, bigBang));
		ranges.addAll(filter(periodesImpositionIS, bigBang));

		final List<DateRange> periodes = buildPeriodes(ranges);

		final TimelineTable table = bean.getTable();
		table.setPeriodes(periodes);

		// Renseignement des fors fiscaux
		for (ForFiscal f : forsFiscaux) {
			if (compare(f, bigBang) >= 0) {
				if (f.isPrincipal()) {
					table.addForPrincipal(f);
				}
				else if (f instanceof ForFiscalRevenuFortune) {
					table.addForSecondaire(f);
				}
			}
		}

		// Renseignement des fors de gestion
		if (bean.isShowForsGestion()) {
			for (ForGestion fg : forsGestion) {
				if (compare(fg, bigBang) >= 0) {
					table.addForGestion(fg);
				}
			}
		}

		// Renseignement des assujettissements source
		if (bean.isShowAssujettissementsSource()) {
			for (Assujettissement a : assujettissementsSource) {
				if (compare(a, bigBang) >= 0) {
					table.addAssujettissementSource(a);
				}
			}
		}

		// Renseignement des assujettissements rôle
		if (bean.isShowAssujettissementsRole()) {
			for (Assujettissement a : assujettissementsRole) {
				if (compare(a, bigBang) >= 0) {
					table.addAssujettissementRole(a);
				}
			}
		}

		// Renseignement des assujettissements
		if (bean.isShowAssujettissements()) {
			for (Assujettissement a : assujettissements) {
				if (compare(a, bigBang) >= 0) {
					table.addAssujettissement(a);
				}
			}
		}

		// Renseignement des périodes d'imposition
		if (bean.isShowPeriodesImposition()) {
			for (PeriodeImposition p : periodesImposition) {
				if (compare(p, bigBang) >= 0) {
					table.addPeriodeImposition(p);
				}
			}
		}

		// Renseignement des périodes d'imposition IS
		if (bean.isShowPeriodesImpositionIS()) {
			for (PeriodeImpositionImpotSource p : periodesImpositionIS) {
				if (compare(p, bigBang) >= 0) {
					table.addPeriodeImpositionIS(p);
				}
			}
		}
	}

	private static List<DateRange> filter(List<? extends DateRange> source, RegDate bigBang) {
		if (source == null || source.isEmpty()) {
			return Collections.emptyList();
		}
		final List<DateRange> result = new ArrayList<>(source.size());
		for (DateRange range : source) {
			final int comparison = compare(range, bigBang);
			if (comparison == 0) {
				result.add(new DateRangeHelper.Range(bigBang, range.getDateFin()));
			}
			else if (comparison > 0) {
				result.add(range);
			}
		}
		return result;
	}

	/**
	 *
	 * @param range range to locate relatively to the bigBang date
	 * @param bigBang reference date
	 * @return -1 if the range is completely before the bigBang date, 0 if the bigBang date lies within the range (but does not start it), and +1 if the range is complelety after the bigBang date
	 */
	private static int compare(DateRange range, RegDate bigBang) {
		if (RegDateHelper.isBefore(range.getDateDebut(), bigBang, NullDateBehavior.EARLIEST)) {
			if (RegDateHelper.isAfterOrEqual(range.getDateFin(), bigBang, NullDateBehavior.LATEST)) {
				return 0;
			}
			else {
				return -1;
			}
		}
		else {
			return 1;
		}
	}

	private static List<DateRange> buildPeriodes(List<DateRange> ranges) {
		final List<DateRange> periodes = new ArrayList<>();
		final List<RegDate> boundaries = TimelineHelper.extractBoundaries(ranges);
		RegDate previous = null;
		boolean secondLoopOrLater = false;
		for (RegDate current : boundaries) {
			if (secondLoopOrLater) {
				periodes.add(new Periode(previous, (current == null ? null : current.getOneDayBefore())));
			}
			previous = current;
			secondLoopOrLater = true;
		}
		return periodes;
	}

	@SuppressWarnings("UnusedDeclaration")
	public static class Periode extends DateRangeHelper.Range {

		public Periode(RegDate debut, RegDate fin) {
			super(debut, fin);
		}

		public String getDateDebutLabel() {
			return toDayMonth(getDateDebut());
		}

		public String getDateFinLabel() {
			final RegDate date = getDateFin();
			if (date == null) {
				return "...";
			}
			return toDayMonth(date);
		}

		private String toDayMonth(RegDate date) {
			return String.format("%02d.%02d", date.day(), date.month());
		}
	}

	public void setTiersDao(TiersDAO dao) {
		this.dao = dao;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setAssujettissementService(AssujettissementService assujettissementService) {
		this.assujettissementService = assujettissementService;
	}

	public void setPeriodeImpositionService(PeriodeImpositionService periodeImpositionService) {
		this.periodeImpositionService = periodeImpositionService;
	}

	public void setPeriodeImpositionImpotSourceService(PeriodeImpositionImpotSourceService periodeImpositionImpotSourceService) {
		this.periodeImpositionImpotSourceService = periodeImpositionImpotSourceService;
	}

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	public void setParametreAppService(ParametreAppService params) {
		this.bigBangPersonnesPhysiques = RegDate.get(params.getPremierePeriodeFiscalePersonnesPhysiques(), 1, 1);
		this.bigBangPersonnesMorales = RegDate.get(params.getPremierePeriodeFiscalePersonnesMorales(), 1, 1);
	}
}
