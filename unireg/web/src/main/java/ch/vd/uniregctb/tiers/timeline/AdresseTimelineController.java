package ch.vd.uniregctb.tiers.timeline;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

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
import ch.vd.uniregctb.adresse.AdresseCouche;
import ch.vd.uniregctb.adresse.AdresseEnvoi;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdresseSandwich;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdressesFiscalesSandwich;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.timeline.AdresseTimelineView.Table;

/**
 * Contrôleur pour l'affichage de l'historique des adresses d'un contribuable
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@Controller
public class AdresseTimelineController {

	private final Logger LOGGER = Logger.getLogger(AdresseTimelineController.class);

	public final static String ID_PARAMETER = "id";

	private TiersDAO dao;
	private AdresseService adresseService;

	@RequestMapping(value = "/adresses/timeline.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String index(Model mav, @RequestParam(ID_PARAMETER) Long id) throws AccessDeniedException {

		final AdresseTimelineView bean = new AdresseTimelineView();
		ControllerUtils.checkAccesDossierEnLecture(id);
		bean.setTiersId(id);

		fillTimeline(bean);
		mav.addAttribute("command", bean);

		return "tiers/visualisation/adresse/timeline";
	}

	/**
	 * A partir d'une liste de ranges (qui peuvent se recouvrir), détermine la liste triées par ordre croissant des dates de début et de fin des ranges. Chaque date n'apparaît qu'une fois dans la liste.
	 * <p/>
	 * <b>Note:</b> Si au moins un des ranges possède une date de début nulle, la première date de la liste est nulle. De même, si au moins un des ranges possède une date de fin nulle, la dernière date
	 * de la liste est nulle.
	 * <p/>
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
	private void fillTimeline(AdresseTimelineView bean) {

		Long id = bean.getTiersId();
		if (id == null) {
			return;
		}

		Tiers tiers = dao.get(id);
		if (tiers == null) {
			return;
		}

		final AdressesFiscalesSandwich sandwich;
		try {
			sandwich = adresseService.getAdressesFiscalesSandwich(tiers, false);
		}
		catch (AdresseException e) {
			bean.addException(e);
			return;
		}

		filleTable(bean, sandwich.courrier, "Courrier");
		filleTable(bean, sandwich.domicile, "Domicile");
		filleTable(bean, sandwich.representation, "Représentation");
		filleTable(bean, sandwich.poursuite, "Poursuite");
		filleTable(bean, sandwich.poursuiteAutreTiers, "Poursuite Autre Tiers");

		// Renseignement de l'adresse
		try {
			AdresseEnvoi adresseEnvoi = adresseService.getAdresseEnvoi(tiers, null, TypeAdresseFiscale.COURRIER, false);
			bean.setAdresseEnvoi(adresseEnvoi);
		}
		catch (AdresseException e) {
			LOGGER.warn("Résolution des adresses pour le tiers [" + tiers.getNumero() + "] impossible.", e);
		}
	}

	private void filleTable(AdresseTimelineView bean, AdresseSandwich adresses, String type) {

		// Calcul des différents ranges de l'axe du temps
		final SortedSet<AdresseCouche> columns = new TreeSet<AdresseCouche>();
		final List<DateRange> ranges = new ArrayList<DateRange>();
		for (AdresseSandwich.Couche couche : adresses.decortique()) {
			final Collection<? extends DateRange> col = removeAdressesAnnulees(couche.getAdresses());
			if (!col.isEmpty()) {
				ranges.addAll(col);
				columns.add(couche.getType());
			}
		}
		ranges.addAll(removeAdressesAnnulees(adresses.emballe()));
		columns.add(AdresseCouche.RESULTAT);

		final List<DateRange> periodes = new ArrayList<DateRange>();
		final List<RegDate> boundaries = extractBoundaries(ranges);
		for (int i = 1; i < boundaries.size(); i++) {
			RegDate previous = boundaries.get(i - 1);
			RegDate current = boundaries.get(i);
			periodes.add(new DateRangeHelper.Range(previous, (current == null ? null : current.getOneDayBefore())));
		}

		final Table table = new Table(type);
		table.setColumns(new ArrayList<AdresseCouche>(columns));
		table.setPeriodes(periodes);

		// Renseignement des différentes couches
		for (AdresseSandwich.Couche couche : adresses.decortique()) {
			for (AdresseGenerique adresse : couche.getAdresses()) {
				if (!adresse.isAnnule()) {
					table.addAdresse(couche.getType(), adresse);
				}
			}
		}
		for (AdresseGenerique adresse : adresses.emballe()) {
			if (!adresse.isAnnule()) {
				table.addAdresse(AdresseCouche.RESULTAT, adresse);
			}
		}

		bean.addTable(table);
	}

	private static Collection<? extends DateRange> removeAdressesAnnulees(List<AdresseGenerique> adresses) {
		if (adresses == null) {
			return null;
		}
		List<AdresseGenerique> list = new ArrayList<AdresseGenerique>(adresses.size());
		for (AdresseGenerique a : adresses) {
			if (!a.isAnnule()) {
				list.add(a);
			}
		}
		return list;
	}

	public void setTiersDao(TiersDAO dao) {
		this.dao = dao;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}
}
