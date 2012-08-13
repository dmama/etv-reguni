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

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.tiers.ForGestion;
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

//	private final Logger LOGGER = Logger.getLogger(ForsTimelineController.class);

	public final static String ID_PARAMETER = "id";
	public final static String FOR_PRINT = "print";
	public final static String TITLE = "title";
	public final static String DESCRIPTION = "description";

	private TiersDAO dao;
	private TiersService tiersService;
	private AssujettissementService assujettissementService;
	private PeriodeImpositionService periodeImpositionService;

	@RequestMapping(value = "/fors/timeline.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String index(Model mav,
	                    @RequestParam(ID_PARAMETER) Long id,
	                    @RequestParam(value = "showForsGestion", required = false, defaultValue = "true") boolean showForsGestion,
	                    @RequestParam(value = "showAssujettissements", required = false, defaultValue = "true") boolean showAssujettissements,
	                    @RequestParam(value = "showPeriodesImposition", required = false, defaultValue = "false") boolean showPeriodesImposition,
	                    @RequestParam(value = FOR_PRINT, required = false) Boolean forPrint,
	                    @RequestParam(value = TITLE, required = false) String title,
	                    @RequestParam(value = DESCRIPTION, required = false) String description) throws AccessDeniedException {

		final ForsTimelineView bean = new ForsTimelineView(showForsGestion, showAssujettissements, showPeriodesImposition);
		ControllerUtils.checkAccesDossierEnLecture(id);
		bean.setTiersId(id);

		if (forPrint != null) {
			bean.setForPrint(forPrint);
		}

		if (title != null) {
			bean.setTitle(title);
		}

		if (description != null) {
			bean.setDescription(description);
		}

		fillTimeline(bean);
		mav.addAttribute("command", bean);

		return "tiers/visualisation/fiscal/timeline";
	}

	/**
	 * Remplit les structures de données nécessaire à l'affichage de l'historique des fors d'un tiers
	 */
	private void fillTimeline(ForsTimelineView bean) {

		Long id = bean.getTiersId();
		if (id == null) {
			return;
		}

		Tiers tiers = dao.get(id);
		if (tiers == null) {
			return;
		}

		// Extraction des fors fiscaux
		final List<ForFiscal> forsFiscaux = tiers.getForsFiscauxNonAnnules(true);

		// Extraction des fors de gestion
		final List<ForGestion> forsGestion = bean.isShowForsGestion() ? tiersService.getForsGestionHisto(tiers) : Collections.<ForGestion>emptyList();

		// Extraction de l'assujettissement
		List<Assujettissement> assujettissements = new ArrayList<Assujettissement>();
		if (bean.isShowAssujettissements() && tiers instanceof Contribuable) {
			final Contribuable contribuable = (Contribuable) tiers;
			final RegDate debutActivite = contribuable.getDateDebutActivite();
			if (debutActivite != null) {
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
		List<PeriodeImposition> periodesImposition = new ArrayList<PeriodeImposition>();
		if (bean.isShowPeriodesImposition() && tiers instanceof Contribuable) {
			final Contribuable contribuable = (Contribuable) tiers;
			final RegDate debutActivite = contribuable.getDateDebutActivite();
			if (debutActivite != null) {
				try {
					final List<PeriodeImposition> list = periodeImpositionService.determine(contribuable, null);
					if (list != null) {
						periodesImposition.addAll(list);
					}
				}
				catch (AssujettissementException e) {
					bean.addException(e);
				}
			}
		}

		// Calcul des différents ranges de l'axe du temps
		final List<DateRange> ranges = new ArrayList<DateRange>();
		ranges.addAll(forsFiscaux);
		ranges.addAll(forsGestion);
		ranges.addAll(assujettissements);
		ranges.addAll(periodesImposition);

		final List<DateRange> periodes = buildPeriodes(ranges);

		final TimelineTable table = bean.getTable();
		table.setPeriodes(periodes);

		// Renseignement des fors fiscaux
		for (ForFiscal f : forsFiscaux) {
			if (f.isPrincipal()) {
				final ForFiscalPrincipal fp = (ForFiscalPrincipal) f;
				table.addForPrincipal(fp);
			}
			else if (f instanceof ForFiscalRevenuFortune) {
				final ForFiscalRevenuFortune fs = (ForFiscalRevenuFortune) f;
				table.addForSecondaire(fs);
			}
		}

		// Renseignement des fors de gestion
		if (bean.isShowForsGestion()) {
			for (ForGestion fg : forsGestion) {
				table.addForGestion(fg);
			}
		}

		// Renseignement des assujettissements
		if (bean.isShowAssujettissements()) {
			for (Assujettissement a : assujettissements) {
				table.addAssujettissement(a);
			}
		}

		// Renseignement des périodes d'imposition
		if (bean.isShowPeriodesImposition()) {
			for (PeriodeImposition p : periodesImposition) {
				table.addPeriodeImposition(p);
			}
		}
	}

	private static List<DateRange> buildPeriodes(List<DateRange> ranges) {
		final List<DateRange> periodes = new ArrayList<DateRange>();
		final List<RegDate> boundaries = TimelineHelper.extractBoundaries(ranges);
		RegDate previous = null;
		for (RegDate current : boundaries) {
			if (previous != null) {
				periodes.add(new Periode(previous, (current == null ? null : current.getOneDayBefore())));
			}
			previous = current;
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
}
