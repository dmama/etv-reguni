package ch.vd.uniregctb.tiers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseEnvoi;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
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
	public final static String FOR_PRINT = "print";
	public final static String TITLE = "title";
	public final static String DESCRIPTION = "description";

	private TiersDAO dao;

	private AdresseService adresseService;
	private TiersService tiersService;

	private PlatformTransactionManager transactionManager;

	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		final TiersTimelineView bean = (TiersTimelineView) super.formBackingObject(request);

		final Long id = extractLongParam(request, ID_PARAMETER);
		if (id != null) {
			checkAccesDossierEnLecture(id);
			bean.setTiersId(id);
		}

		final Boolean forPrint = extractBooleanParam(request, FOR_PRINT);
		if (forPrint != null){
			bean.setForPrint(forPrint);
		}

		final String title = request.getParameter(TITLE);
		if (title != null) {
			bean.setTitle(title);
		}

		final String description = request.getParameter(DESCRIPTION);
		if (description != null) {
			bean.setDescription(description);
		}

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				fillTimeline(bean);
				return null;
			}
		});

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
	 * A partir d'une liste de ranges (qui peuvent se recouvrir), détermine la liste triées par ordre croissant des dates de début et de fin
	 * des ranges. Chaque date n'apparaît qu'une fois dans la liste.
	 * <p>
	 * <b>Note:</b> Si au moins un des ranges possède une date de début nulle, la première date de la liste est nulle. De même, si au moins
	 * un des ranges possède une date de fin nulle, la dernière date de la liste est nulle.
	 *
	 * <pre>
	 * Ranges d'entrées:
	 * +----------------------+                           +----------------+
	 * ¦x                    y¦                           ¦u              v¦
	 * +----------------------+                           +----------------+
	 *                      +----------------------+              +----------------+
	 *                      ¦y                    z¦              ¦r              s¦
	 *                      +----------------------+              +----------------+
	 * .
	 * Dates de sortie: x, y, y+1, z+1, u, r, v+1, s+1
	 * </pre>
	 */
	public static List<RegDate> extractBoundaries(Collection<? extends DateRange> ranges) {

		boolean dateDebutNull = false;
		boolean dateFinNull = false;

		// Détermination de l'ensemble des dates
		final SortedSet<RegDate> dates = new TreeSet<RegDate>();
		for (DateRange r : ranges) {
			final RegDate dateDebut = r.getDateDebut();
			if (dateDebut == null) {
				dateDebutNull = true;
			}
			else {
				dates.add(dateDebut);
			}
			RegDate dateFin = r.getDateFin();
			if (dateFin == null) {
				dateFinNull = true;
			}
			else {
				dates.add(dateFin.getOneDayAfter());
			}
		}

		// Transforme l'ensemble en liste + traitement des valeurs nulles
		List<RegDate> list = new ArrayList<RegDate>(dates);
		if (dateDebutNull) {
			list.add(0, null);
		}
		if (dateFinNull) {
			list.add(null);
		}

		return list;
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
					assujettissements = Assujettissement.determine(contribuable);
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
		
		final List<DateRange> periodes = new ArrayList<DateRange>();
		final List<RegDate> boundaries = extractBoundaries(ranges);
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

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}
}
