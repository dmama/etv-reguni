package ch.vd.uniregctb.tiers.timeline;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseEnvoi;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
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
import ch.vd.uniregctb.tiers.timeline.TiersTimelineView.Table;

/**
 * Contrôleur pour l'affichage de l'historique des fors fiscaux et des assujettissements d'un contribuable
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@Controller
public class TiersTimelineController {

	private final Logger LOGGER = Logger.getLogger(TiersTimelineController.class);

	public final static String ID_PARAMETER = "id";
	public final static String FOR_PRINT = "print";
	public final static String TITLE = "title";
	public final static String DESCRIPTION = "description";

	private TiersDAO dao;
	private AdresseService adresseService;
	private TiersService tiersService;
	private AssujettissementService assujettissementService;
	private PeriodeImpositionService periodeImpositionService;

	@RequestMapping(value = "/tiers/timeline.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String index(Model mav,
	                    @RequestParam(ID_PARAMETER) Long id,
	                    @RequestParam(value = FOR_PRINT, required = false) Boolean forPrint,
	                    @RequestParam(value = TITLE, required = false) String title,
	                    @RequestParam(value = DESCRIPTION, required = false) String description) throws AccessDeniedException {

		final TiersTimelineView bean = new TiersTimelineView();
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
	 * Remplit les structures de données nécessaire à l'affichage de l'historique du tiers
	 */
	private void fillTimeline(TiersTimelineView bean) {

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
		final List<ForGestion> forsGestion = tiersService.getForsGestionHisto(tiers);

		// Extraction de l'assujettissement
		List<Assujettissement> assujettissements = new ArrayList<Assujettissement>();
		if (tiers instanceof Contribuable) {
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
		if (tiers instanceof Contribuable) {
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
		ranges.addAll(assujettissements);
		ranges.addAll(periodesImposition);
		ranges.addAll(forsGestion);

		final List<DateRange> periodes = new ArrayList<DateRange>();
		final List<RegDate> boundaries = TimelineHelper.extractBoundaries(ranges);
		RegDate previous = null;
		for (RegDate current : boundaries) {
			if (previous != null) {
				periodes.add(new DateRangeHelper.Range(previous, (current == null ? null : current.getOneDayBefore())));
			}
			previous = current;
		}

		final Table table = bean.getTable();
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
		for (ForGestion fg : forsGestion) {
			table.addForGestion(fg);
		}

		// Renseignement des assujettissements
		for (Assujettissement a : assujettissements) {
			table.addAssujettissement(a);
		}

		// Renseignement des périodes d'imposition
		for (PeriodeImposition p : periodesImposition) {
			table.addPeriodeImposition(p);
		}

		// Renseignement de l'adresse
		try {
			AdresseEnvoi adresseEnvoi = adresseService.getAdresseEnvoi(tiers, null, TypeAdresseFiscale.COURRIER, false);
			bean.setAdresse(adresseEnvoi);
		}
		catch (AdresseException e) {
			LOGGER.warn("Résolution des adresses pour le tiers [" + tiers.getNumero() + "] impossible.", e);
		}
	}

	public void setTiersDao(TiersDAO dao) {
		this.dao = dao;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
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
