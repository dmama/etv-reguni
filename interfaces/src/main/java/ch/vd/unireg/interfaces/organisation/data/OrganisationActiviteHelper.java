package ch.vd.unireg.interfaces.organisation.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.CollectionsUtils;

/**
 * Helper spécialisé dans le calcul de l'activité des entreprises et établissements.
 *
 * Fonctions {@link OrganisationActiviteHelper#isActif} et {@link OrganisationActiviteHelper#activite} déplacées de {@link OrganisationHelper} le 2017-08-14.
 *
 * @author Raphaël Marmier, 2017-08-14, <raphael.marmier@vd.ch>
 */
public class OrganisationActiviteHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(OrganisationActiviteHelper.class);

	/**
	 * <p>
	 *     Détermine la plage d'activité d'une organisation, c'est à dire la période à laquelle on la considère en existence au sens de
	 *     "non radiée" RC et/ou IDE et REE.
	 * </p>
	 * <p>
	 *     La date de commencement est déterminée par la date d'inscription au RC, si inscription il y a, ou par la date du premier événement
	 *     RCEnt rapportant l'inscription à l'IDE ou au REE, indifférament. Postulat: Une inscription à l'IDE ou au REE ouvre nécessairement
	 *     une période d'activité (pas d'ouverture inactive). Or. dans le cas du REE, ce n'est pas si sûr.
	 * </p>
	 * <p>
	 *     La date de fin est déterminée par la date de radiation au RC, pour les établissements obligatoirement inscrits au RC, ou par la
	 *     date de fin à l'IDE ou au REE.
	 * </p>
	 * <p>
	 *     NOTES:
	 *     <ul>
	 *         <li>
	 *             Les organisations radiées puis réinscrites sont considérées comme actives durant toutes la période ou elles
	 *             ont été radiées, car on ne détermine ici que le commencement et la fin de l'activité. C'est une limitation de l'algorithme.
	 *         </li>
	 *         <li>
	 *             Lorsque la radiation du RC est liée à celle de l'IDE (c'est à dire que l'IDE ne fait qu'enregistrer l'état de fait
	 *             au RC pour les organisation obligatoirement inscrites au RC), c'est la date du RC qui est utilisée. Actuellement,
	 *             le seuil {@link OrganisationHelper#NB_JOURS_TOLERANCE_DE_DECALAGE_RC} est utilisé pour deviner ce cas de figure. Ainsi,
	 *             si la radiation du registre IDE ne suit pas celle du RC dans les n jours de la tolérance, on considère qu'il s'agit de
	 *             deux faits distinct (ex: association sans obligation de s'inscrire au RC).
	 *
	 *         </li>
	 *         <li>
	 *             La radiation au RC est récupérée lors de l'analyse des données IDE et/ou REE. En cas d'absence de celles-ci, la radiation au RC serait
	 *             manquée. Cette situation ne pourrait survenir qu'à la suite d'un problème technique, car l'inscription à l'IDE est
	 *             automatique pour toute entité inscrite au RC.
	 *         </li>
	 *         <li>
	 *             La date de radiation de l'IDE ou du REE peut être déterminée de manière erronée lorsque l'entité non inscrite au RC subit
	 *             des mutations post-radiation.
	 *         </li>
	 *     </ul>
	 * </p>
	 *
	 * @param site le site concernée
	 * @return la période où le site a été en activité (sous forme de liste, mais en l'état de l'implémentation, elle est toujours unique).
	 */
	public static List<DateRange> activite(SiteOrganisation site) {

		// trouvons la première information d'inscription
		InscriptionRC first = null;
		final List<DateRanged<InscriptionRC>> inscriptions = site.getDonneesRC().getInscription();
		if (inscriptions != null) {
			for (DateRanged<InscriptionRC> inscription : inscriptions) {
				if (inscription.getPayload().isInscrit()) {
					first = inscription.getPayload();
					break;
				}
			}
		}

		final RegDate dateCreation;
		if (first == null) {
			dateCreation = site.getNom().get(0).getDateDebut(); // Postulat: une organisation commence toujours par être active. (pas d'inscription en inactivité)(attention REE est-ce toujours vrai?)
		}
		else if (first.getDateInscriptionVD() != null && first.getDateInscriptionCH() == null) {
			// Cas de la date RC vide quand la date RC VD est renseignée. Impossible de déterminer quoi que ce soit dans ce cas
			throw new RuntimeException(String.format("Impossible de trouver la date d'inscription au RC pour le site %s", site.getNumeroSite()));
		}
		else {
			dateCreation = RegDateHelper.minimum(site.getNom().get(0).getDateDebut(), first.getDateInscriptionCH(), NullDateBehavior.LATEST);
		}

		/* Une période théorique d'activité continue non terminée. */
		final DateRange activite = new DateRangeHelper.Range(dateCreation, null);

/*  == On peut omettre, car les inscrites au RC sont obligatoirement à l'IDE et la date est gérée avec ce dernier. ==

		final List<DateRange> datesRadiation = new ArrayList<>();
		List<DateRange> nonRadiation = Collections.emptyList();
		if (!site.getDonneesRC().getDateRadiation().isEmpty()) {
			for (DateRanged<RegDate> dateRadiation : site.getDonneesRC().getDateRadiation()) {
				final RegDate dateEffective = dateRadiation.getPayload();
				if (dateEffective != dateRadiation.getDateDebut()) {
					datesRadiation.add(new DateRangeHelper.Range(dateEffective, dateRadiation.getDateFin()));
				} else {
					datesRadiation.add(dateRadiation);
				}
			}
			nonRadiation = DateRangeHelper.subtract(activite, datesRadiation);
		}

		final List<DateRange> datesRadiationVd = new ArrayList<>();
		List<DateRange> nonRadiationVd = Collections.emptyList();
		if (!site.getDonneesRC().getDateRadiationVd().isEmpty()) {
			for (DateRanged<RegDate> dateRadiationVd : site.getDonneesRC().getDateRadiationVd()) {
				final RegDate dateEffectiveVd = dateRadiationVd.getPayload();
				if (dateEffectiveVd != dateRadiationVd.getDateDebut()) {
					datesRadiationVd.add(new DateRangeHelper.Range(dateEffectiveVd, dateRadiationVd.getDateFin()));
				} else {
					datesRadiationVd.add(dateRadiationVd);
				}
			}
			nonRadiationVd = DateRangeHelper.subtract(activite, datesRadiationVd);
		}
*/

		final List<DateRanged<StatusRegistreIDE>> periodesStatusIde = site.getDonneesRegistreIDE().getStatus();
		final List<DateRange> datesRadieIde = new ArrayList<>();
		List<DateRange> activiteIde = Collections.emptyList();
		if (periodesStatusIde != null && !periodesStatusIde.isEmpty()) {
			final DateRanged<StatusRegistreIDE> dernierePeriode = CollectionsUtils.getLastElement(periodesStatusIde); // FIXME: Postulat excessif: la dernière période n'est pas nécessairement celle de la radiation.
			final StatusRegistreIDE status = dernierePeriode.getPayload();
			if (dernierePeriode.getDateFin() == null && (status == StatusRegistreIDE.RADIE || status == StatusRegistreIDE.DEFINITIVEMENT_RADIE)) {
				final RegDate dateRadiationRC = site.getDateRadiationRC(dernierePeriode.getDateDebut());
				if (dateRadiationRC != null && !dateRadiationRC.isBefore(dernierePeriode.getDateDebut().addDays( - OrganisationHelper.NB_JOURS_TOLERANCE_DE_DECALAGE_RC))) {
					datesRadieIde.add(new DateRangeHelper.Range(dateRadiationRC.getOneDayAfter(), dernierePeriode.getDateFin()));
				} else {
					datesRadieIde.add(dernierePeriode);
				}
			}
			activiteIde = DateRangeHelper.subtract(activite, datesRadieIde); // Compromis. Si par erreur il est radié à l'IDE mais pas au RC, on va quand même mettre la date de fin de l'IDE, ce qui serait faux.
		}

		final List<DateRanged<InscriptionREE>> periodesStatusRee = site.getDonneesREE().getInscriptionREE();
		final List<DateRange> datesRadieRee = new ArrayList<>();
		List<DateRange> activiteRee = Collections.emptyList();
		if (periodesStatusRee != null && !periodesStatusRee.isEmpty()) {
			final DateRanged<InscriptionREE> dernierePeriode = CollectionsUtils.getLastElement(periodesStatusRee); // FIXME: Postulat excessif: la dernière période n'est pas nécessairement celle de la radiation.
			final StatusREE status = dernierePeriode.getPayload().getStatus();
			if (dernierePeriode.getDateFin() == null && status == StatusREE.RADIE) { // et status == StatusREE.INACTIF aussi (??) contrairement au commentaire au bas du SIFISC-18739
				final RegDate dateRadiationRC = site.getDateRadiationRC(dernierePeriode.getDateDebut());
				if (dateRadiationRC != null && !dateRadiationRC.isBefore(dernierePeriode.getDateDebut().addDays( - OrganisationHelper.NB_JOURS_TOLERANCE_DE_DECALAGE_RC))) {
					datesRadieRee.add(new DateRangeHelper.Range(dateRadiationRC.getOneDayAfter(), dernierePeriode.getDateFin()));
				} else {
					datesRadieRee.add(dernierePeriode);
				}
			}
			activiteRee = DateRangeHelper.subtract(activite, datesRadieRee); // Compromis. Si par erreur il est radié à l'IDE mais pas au RC, on va quand même mettre la date de fin de l'IDE, ce qui serait faux.
		}

		// TODO FIXME Inclure la notion de mort par transfert! Cas du changement de propriétaire remonté par Madame Viquerat.

		final List<DateRange> tousActivite = new ArrayList<>();
		if (activiteIde.isEmpty() && activiteRee.isEmpty()) {
			tousActivite.add(activite);
		}
		else {
			tousActivite.addAll(activiteIde); // Plage d'activité à l'IDE
			tousActivite.addAll(activiteRee); // Plage d'activité au REE
		}

		final List<DateRange> activiteDeterminee = DateRangeHelper.merge(tousActivite);
		if (activiteDeterminee.size() > 1) {
			LOGGER.warn(String.format("Le calcul de l'activité pour le site RCEnt %d a retourné plus d'une période.", site.getNumeroSite()));
		}
		return activiteDeterminee;
	}

	/**
	 * Dire si un site est globallement actif, c'est à dire qu'il a une existence à la date fournie chez au
	 * moins un fournisseur primaire (RC, IDE). Etre actif signifie être inscrit et non radié.
	 *
	 * @param site le site
	 * @param date la date pour laquelle on veut connaitre la situation du site
	 * @return true si actif, false sinon
	 */
	public static boolean isActif(SiteOrganisation site, RegDate date) {
		if (OrganisationHelper.isConnuInscritAuRC(site, date) && !OrganisationHelper.isRadieDuRC(site, date)) {
			return true;
		}
		else if (OrganisationHelper.isInscritIDE(site, date) && !OrganisationHelper.isRadieIDE(site, date)) {
			return true;
		}
		else if (OrganisationHelper.isConnuInscritREE(site, date) && !OrganisationHelper.isRadieREE(site, date)) {
			return true;
		}
		return false;
	}
}
