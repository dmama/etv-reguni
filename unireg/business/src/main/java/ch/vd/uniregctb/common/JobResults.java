package ch.vd.uniregctb.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.tiers.Tiers;

/**
 * Classe de base des containers de résultats pour les rapports d'exécution des batchs
 */
public abstract class JobResults<E, R extends JobResults> implements BatchResults<E, R> {

	public static final String EXCEPTION_DESCRIPTION = "Une exception est apparue pendant le traitement du contribuable, veuillez en informer le chef de projet Unireg";

	/**
	 * Classe de base des informations dumpées dans les rapports d'exécution
	 */
	public static abstract class Info {
		public final long noCtb;
		public final Integer officeImpotID;
		public final String details;
		public final String nomCtb;

		public Info(long noCtb, Integer officeImpotID, String details) {
			this.noCtb = noCtb;
			this.officeImpotID = officeImpotID;
			this.details = details;
			this.nomCtb = getNom(noCtb);
		}

		public abstract String getDescriptionRaison();
	}

	/**
	 * Comparator utilisable pour trier les "info" dans l'ordre croissant du numéro de contribuable
	 * @param <T>
	 */
	public static class CtbComparator<T extends Info> implements Comparator<T> {
		@Override
		public int compare(T o1, T o2) {
			return (int) (o1.noCtb - o2.noCtb);
		}
	}

	private static AdresseService adresseService;
	private static HibernateTemplate hibernateTemplate;

	public void setAdresseService(AdresseService adresseService) {
		JobResults.adresseService = adresseService;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		JobResults.hibernateTemplate = hibernateTemplate;
	}

	protected static void checkServices() {
		Assert.notNull(adresseService);
		Assert.notNull(hibernateTemplate);
	}

	/**
	 * Retourne le nom et le prénom du contribuable spécifié. S'il s'agit d'un contribuable ménage commun et que les deux parties sont connues, la liste retournée contient les deux noms des parties.
	 *
	 * @param noCtb le numéro de contribuable
	 * @return une liste avec 1 nom (majorité des cas) ou 2 noms (contribuables ménage commun)
	 */
	private static List<String> getNoms(long noCtb) {

		List<String> noms;

		final Tiers tiers = hibernateTemplate.get(Tiers.class, noCtb);
		if (tiers == null) {
			noms = Collections.emptyList();
		}
		else {
			try {
				noms = adresseService.getNomCourrier(tiers, null, false);
			}
			catch (Exception e) {
				noms = new ArrayList<String>(1);
				noms.add(e.getMessage()); // rien de mieux à faire ici
			}
		}

		return noms;
	}

	/**
	 * Retourne le nom et prénom du contribuable (ou des deux parties dans le cas d'un ménage commun)
	 *
	 * @param noCtb le numéro de contribuable
	 * @return le nom et le prénom du contribuable.
	 */
	private static String getNom(long noCtb) {

		final String nom;

		final List<String> noms = getNoms(noCtb);
		if (noms.size() == 1) { // 90% des cas
			nom = noms.get(0);
		}
		else if (noms.size() > 1) {
			final StringBuilder b = new StringBuilder();
			for (String part : noms) {
				if (StringUtils.isNotBlank(part)) {
					if (b.length() > 0) {
						b.append(", ");
					}
					b.append(StringUtils.trimToEmpty(part));
				}
			}
			nom = b.toString();
		}
		else {
			nom = null;
		}

		return StringUtils.trimToEmpty(nom);
	}

	/**
	 * Heure de démarrage du job (à la milliseconde près).
	 */
	public final long startTime;

	/**
	 * Heure d'arrêt du job (à la milliseconde près).
	 */
	public long endTime = 0;

	public JobResults() {
		this.startTime = System.currentTimeMillis();
	}

	public void end() {
		this.endTime = System.currentTimeMillis();
	}
}
