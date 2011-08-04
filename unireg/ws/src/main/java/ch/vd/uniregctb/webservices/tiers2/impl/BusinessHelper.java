package ch.vd.uniregctb.webservices.tiers2.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.webservices.tiers2.data.TiersPart;

public class BusinessHelper {

	private static final Logger LOGGER = Logger.getLogger(BusinessHelper.class);

	/**
	 * @return retourne la raison sociale du débiteur spécifié.
	 */
	public static String getRaisonSociale(final DebiteurPrestationImposable debiteur, RegDate date, AdresseService service) {

		if (date == null) {
			date = RegDate.get();
		}

		String raison = "";
		try {
			List<String> list = service.getNomCourrier(debiteur, date, false);
			if (!list.isEmpty()) {
				raison = list.get(0); // on ignore joyeusement une éventuelle deuxième ligne
			}
		}
		catch (AdresseException e) {
			// Si on a une exception on renvoie une raison sociale nulle
			raison = "";
		}
		return raison;
	}

	/**
	 * Précharge l'individu pour la personne physique et les parts spécifiés.
	 *
	 * @param pp      une personne physique
	 * @param parts   des parts
	 * @param context le context d'exécution
	 */
	public static void warmIndividus(PersonnePhysique pp, Set<TiersPart> parts, Context context) {

		if (pp != null && pp.isHabitantVD() && context.serviceCivilService.isWarmable()) {
			final Long noInd = pp.getNumeroIndividu();
			try {
				final AttributeIndividu[] attributs = determineAttributs(parts);
				context.serviceCivilService.getIndividu(noInd, null, attributs);
			}
			catch (Exception e) {
				LOGGER.warn("Impossible de précharger l'individu [" + noInd + "].", e);
			}
		}
	}

	/**
	 * Précharge les individus pour le ménage commun et les parts spécifiés.
	 *
	 * @param menage  un ménage commun
	 * @param parts   des parts
	 * @param context le context d'exécution
	 */
	public static void warmIndividus(MenageCommun menage, Set<TiersPart> parts, Context context) {

		if (context.serviceCivilService.isWarmable() && parts != null && parts.contains(TiersPart.COMPOSANTS_MENAGE)) {
			final Set<Long> ids = new HashSet<Long>();
			for (RapportEntreTiers rapport : menage.getRapportsObjet()) {
				if (!rapport.isAnnule() && rapport instanceof AppartenanceMenage) {
					final AppartenanceMenage appartenance = (AppartenanceMenage) rapport;
					ids.add(appartenance.getSujetId());
				}
			}
			if (!ids.isEmpty()) {
				warmIndividus(ids, parts, context);
			}
		}

	}

	/**
	 * Précharge les individus pour le ids de tiers et les parts spécifiés.
	 *
	 * @param ids     des ids de tiers
	 * @param parts   des parts
	 * @param context le context d'exécution
	 */
	public static void warmIndividus(Set<Long> ids, Set<TiersPart> parts, Context context) {
		final Set<Long> numerosIndividus = context.tiersDAO.getNumerosIndividu(ids, true);
		if (!numerosIndividus.isEmpty()) { // on peut tomber sur une plage de tiers ne contenant pas d'habitant
			try {
				final AttributeIndividu[] attributs = determineAttributs(parts);
				// date=null => parce qu'on s'intéresse à l'historique complete de l'individu
				context.serviceCivilService.getIndividus(numerosIndividus, null, attributs); // chauffe le cache
			}
			catch (Exception e) {
				LOGGER.warn("Impossible de précharger le lot d'individus [" + numerosIndividus + "].", e);
			}
		}
	}

	/**
	 * Détermine les attributs de l'individu devant être préchargée pour éviter de faire plus d'un appel au service civil.
	 *
	 * @param parts les parts du tiers correspondant devant être renseignées.
	 * @return un tableau d'attributs
	 */
	private static AttributeIndividu[] determineAttributs(Set<TiersPart> parts) {
		final AttributeIndividu[] attributs;
		if (parts != null && (parts.contains(TiersPart.ADRESSES) || parts.contains(TiersPart.ADRESSES_ENVOI))) {
			attributs = new AttributeIndividu[]{AttributeIndividu.PERMIS, AttributeIndividu.ADRESSES};
		}
		else {
			attributs = new AttributeIndividu[]{AttributeIndividu.PERMIS};
		}
		return attributs;
	}
}
