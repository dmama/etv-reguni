package ch.vd.uniregctb.tiers;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ch.vd.uniregctb.adresse.AdresseException;
import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.DateRangeHelper.Range;
import ch.vd.uniregctb.adresse.AdresseEnvoi;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.tiers.view.TiersTimelineView;
import ch.vd.uniregctb.tiers.view.TiersTimelineView.Table;

/**
 * Contrôleur pour l'affichage de l'historique des fors fiscaux et des assujettissements d'un contribuable
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class TiersTimelineController extends AbstractTiersController {

	private final Logger LOGGER = Logger.getLogger(TiersTimelineController.class);

	public final static String ID_PARAMETER = "id";

	private TiersDAO dao;

	private AdresseService adresseService;
	private TiersService tiersService;

	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		TiersTimelineView bean = (TiersTimelineView) super.formBackingObject(request);

		Long id = extractLongParam(request, ID_PARAMETER);
		if (id != null) {
			checkAccesDossierEnLecture(id);
			bean.setTiersId(id);
		}

		fillTimeline(bean);
		return bean;
	}

	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {

		TiersTimelineView bean = (TiersTimelineView) command;
		checkAccesDossierEnLecture(bean.getTiersId());
		fillTimeline(bean);

		return super.onSubmit(request, response, command, errors);
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
				final int anneeCourante = RegDate.get().year();
				final Range range = new Range(debutActivite, RegDate.get(anneeCourante, 12, 31));
				try {
					assujettissements = Assujettissement.determine(contribuable, range, true);
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
				final int anneeCourante = RegDate.get().year();
				for (int annee = debutActivite.year(); annee <= anneeCourante; ++annee) {

					List<PeriodeImposition> list = null;
					try {
						list = PeriodeImposition.determine(contribuable, annee);
						if (list != null) {
							periodesImposition.addAll(list);
						}
					}
					catch (AssujettissementException e) {
						bean.addException(e);
					}
				}
			}
		}

		// Calcul des différents ranges de l'axe du temps
		final List<DateRange> ranges = new ArrayList<DateRange>();
		ranges.addAll(forsFiscaux);
		ranges.addAll(assujettissements);
		ranges.addAll(periodesImposition);
		ranges.addAll(forsGestion);
		final List<DateRange> periodes = DateRangeHelper.projectRanges(ranges);

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
			AdresseEnvoi adresseEnvoi = adresseService.getAdresseEnvoi(tiers, null, false);
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
}
