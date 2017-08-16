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
import ch.vd.uniregctb.common.CollectionsUtils;

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
	 *     Détermine la plage d'activité d'une organisation, c'est à dire la période à laquelle on la considère en
	 *     existence au sens de "non radiée" RC et/ou IDE.
	 * </p>
	 * <p>
	 *     NOTES:
	 *     <ul>
	 *         <li>
	 *             Les organisations radiées puis réinscrites sont considérées comme actives durant toutes la période ou elles
	 *             on été radiées.
	 *         </li>
	 *         <li>
	 *             Lorsque la radiation du RC est liée à celle de l'IDE (c'est à dire que l'IDE ne fait qu'enregistrer l'état de fait
	 *             au RC), c'est la date du RC qui est utilisée. Actuellement, le seuil {@link OrganisationHelper#NB_JOURS_TOLERANCE_DE_DECALAGE_RC} est
	 *             utilisé pour déterminer cela.
	 *         </li>
	 *         <li>
	 *             Le REE n'est pas encore supporté. Les éventuels établissements REE ne "meurent" pas.
	 *         </li>
	 *     </ul>
	 * </p>
	 *
	 * @param site le site concernée
	 * @return la période où le site a été en activité (sous forme de liste, mais il ne devrait y en avoir qu'une)
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
			dateCreation = site.getNom().get(0).getDateDebut();
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
		List<DateRange> nonRadiationIde = Collections.emptyList();
		if (periodesStatusIde != null && !periodesStatusIde.isEmpty()) {
			final DateRanged<StatusRegistreIDE> dernierePeriode = CollectionsUtils.getLastElement(periodesStatusIde);
			final StatusRegistreIDE status = dernierePeriode.getPayload();
			if (dernierePeriode.getDateFin() == null && (status == StatusRegistreIDE.RADIE || status == StatusRegistreIDE.DEFINITIVEMENT_RADIE)) {
				final RegDate dateRadiationRC = site.getDateRadiationRC(dernierePeriode.getDateDebut());
				if (dateRadiationRC != null && !dateRadiationRC.isBefore(dernierePeriode.getDateDebut().addDays( - OrganisationHelper.NB_JOURS_TOLERANCE_DE_DECALAGE_RC))) {
					datesRadieIde.add(new DateRangeHelper.Range(dateRadiationRC.getOneDayAfter(), dernierePeriode.getDateFin()));
				} else {
					datesRadieIde.add(dernierePeriode);
				}
			}
			nonRadiationIde = DateRangeHelper.subtract(activite, datesRadieIde);
		}

		// TODO FIXME Inclure la notion de mort par transfert! Cas du changement de propriétaire remonté par Madame Viquerat.

		// Infrastructure nécessaire pour ajouter le support de la mort REE
		final List<DateRange> tousActivite = new ArrayList<>();
		if (nonRadiationIde.isEmpty()) {
			tousActivite.add(activite);
		} else {
			tousActivite.addAll(nonRadiationIde);
		}

		final List<DateRange> combinesNonRadies = DateRangeHelper.merge(tousActivite);

		if (combinesNonRadies.size() > 1) {
			LOGGER.warn(String.format("Le calcul de l'activité pour le site RCEnt %d a retourné plus d'une période.", site.getNumeroSite()));
		}
		return combinesNonRadies;
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
