package ch.vd.uniregctb.xml;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.TiersService;

public class BusinessHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(BusinessHelper.class);

	/**
	 * @return retourne la raison sociale du débiteur spécifié.
	 */
	public static String getDebtorName(final DebiteurPrestationImposable debiteur, TiersService service) {

		final String raison;
		final List<String> list = service.getRaisonSociale(debiteur);
		if (!list.isEmpty())  {
			final StringBuilder b = new StringBuilder();
			for (String elt : list) {
				if (b.length() > 0) {
					b.append(" ");
				}
				b.append(elt);
			}
			raison = b.toString();
		}
		else {
			raison = StringUtils.EMPTY;
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
	public static void warmIndividusV1(PersonnePhysique pp, Set<ch.vd.unireg.xml.party.v1.PartyPart> parts, Context context) {

		if (pp != null && pp.isHabitantVD() && context.serviceCivilService.isWarmable()) {
			final Long noInd = pp.getNumeroIndividu();
			try {
				final AttributeIndividu[] attributs = determineAttributsV1(parts);
				context.serviceCivilService.getIndividu(noInd, null, attributs);
			}
			catch (Exception e) {
				LOGGER.warn("Impossible de précharger l'individu [" + noInd + "].", e);
			}
		}
	}

	/**
	 * Précharge l'individu pour la personne physique et les parts spécifiés.
	 *
	 * @param pp      une personne physique
	 * @param parts   des parts
	 * @param context le context d'exécution
	 */
	public static void warmIndividusV2(PersonnePhysique pp, Set<ch.vd.unireg.xml.party.v2.PartyPart> parts, Context context) {

		if (pp != null && pp.isHabitantVD() && context.serviceCivilService.isWarmable()) {
			final Long noInd = pp.getNumeroIndividu();
			try {
				final AttributeIndividu[] attributs = determineAttributsV2(parts);
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
	public static void warmIndividusV1(MenageCommun menage, Set<ch.vd.unireg.xml.party.v1.PartyPart> parts, Context context) {

		if (context.serviceCivilService.isWarmable() && parts != null && parts.contains(ch.vd.unireg.xml.party.v1.PartyPart.HOUSEHOLD_MEMBERS)) {
			final Set<Long> ids = new HashSet<>();
			for (RapportEntreTiers rapport : menage.getRapportsObjet()) {
				if (!rapport.isAnnule() && rapport instanceof AppartenanceMenage) {
					final AppartenanceMenage appartenance = (AppartenanceMenage) rapport;
					ids.add(appartenance.getSujetId());
				}
			}
			if (!ids.isEmpty()) {
				warmIndividusV1(ids, parts, context);
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
	public static void warmIndividusV2(MenageCommun menage, Set<ch.vd.unireg.xml.party.v2.PartyPart> parts, Context context) {

		if (context.serviceCivilService.isWarmable() && parts != null && parts.contains(ch.vd.unireg.xml.party.v2.PartyPart.HOUSEHOLD_MEMBERS)) {
			final Set<Long> ids = new HashSet<>();
			for (RapportEntreTiers rapport : menage.getRapportsObjet()) {
				if (!rapport.isAnnule() && rapport instanceof AppartenanceMenage) {
					final AppartenanceMenage appartenance = (AppartenanceMenage) rapport;
					ids.add(appartenance.getSujetId());
				}
			}
			if (!ids.isEmpty()) {
				warmIndividusV2(ids, parts, context);
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
	public static void warmIndividusV1(Set<Long> ids, Set<ch.vd.unireg.xml.party.v1.PartyPart> parts, Context context) {
		final Set<Long> numerosIndividus = context.tiersDAO.getNumerosIndividu(ids, true);
		if (!numerosIndividus.isEmpty()) { // on peut tomber sur une plage de tiers ne contenant pas d'habitant
			try {
				final AttributeIndividu[] attributs = determineAttributsV1(parts);
				// date=null => parce qu'on s'intéresse à l'historique complete de l'individu
				context.serviceCivilService.getIndividus(numerosIndividus, null, attributs); // chauffe le cache
			}
			catch (Exception e) {
				LOGGER.warn("Impossible de précharger le lot d'individus [" + numerosIndividus + "].", e);
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
	public static void warmIndividusV2(Set<Long> ids, Set<ch.vd.unireg.xml.party.v2.PartyPart> parts, Context context) {
		final Set<Long> numerosIndividus = context.tiersDAO.getNumerosIndividu(ids, true);
		if (!numerosIndividus.isEmpty()) { // on peut tomber sur une plage de tiers ne contenant pas d'habitant
			try {
				final AttributeIndividu[] attributs = determineAttributsV2(parts);
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
	private static AttributeIndividu[] determineAttributsV1(Set<ch.vd.unireg.xml.party.v1.PartyPart> parts) {
		final AttributeIndividu[] attributs;
		if (parts != null && parts.contains(ch.vd.unireg.xml.party.v1.PartyPart.ADDRESSES)) {
			attributs = new AttributeIndividu[]{AttributeIndividu.PERMIS, AttributeIndividu.ADRESSES};
		}
		else {
			attributs = new AttributeIndividu[]{AttributeIndividu.PERMIS};
		}
		return attributs;
	}

	/**
	 * Détermine les attributs de l'individu devant être préchargée pour éviter de faire plus d'un appel au service civil.
	 *
	 * @param parts les parts du tiers correspondant devant être renseignées.
	 * @return un tableau d'attributs
	 */
	private static AttributeIndividu[] determineAttributsV2(Set<ch.vd.unireg.xml.party.v2.PartyPart> parts) {
		final AttributeIndividu[] attributs;
		if (parts != null && parts.contains(ch.vd.unireg.xml.party.v2.PartyPart.ADDRESSES)) {
			attributs = new AttributeIndividu[]{AttributeIndividu.PERMIS, AttributeIndividu.ADRESSES};
		}
		else {
			attributs = new AttributeIndividu[]{AttributeIndividu.PERMIS};
		}
		return attributs;
	}
}
