package ch.vd.uniregctb.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Classe de base des containers de résultats pour les rapports d'exécution des batchs
 */
public abstract class JobResults<E, R extends JobResults<E, R>> extends AbstractJobResults<E, R> {

	public static final String EXCEPTION_DESCRIPTION = "Une exception est apparue pendant le traitement du tiers, veuillez en informer le chef de projet Unireg";

	/**
	 * Classe de base des informations dumpées dans les rapports d'exécution
	 */
	public abstract static class Info {
		public final long noCtb;
		public final Integer officeImpotID;
		public final String details;
		public final String nomCtb;

		public Info(long noCtb, @Nullable Integer officeImpotID, String details, String nomCtb) {
			this.noCtb = noCtb;
			this.officeImpotID = officeImpotID;
			this.details = details;
			this.nomCtb = nomCtb;
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
			return Long.compare(o1.noCtb, o2.noCtb);
		}
	}

	private final AdresseService adresseService;
	private final TiersService tiersService;

	/**
	 * Retourne le nom et le prénom ou la désignation du tiers spécifié.
	 * S'il s'agit d'un contribuable ménage commun et que les deux parties sont connues, la liste retournée contient les deux noms des parties.
	 *
	 * @param tiers le tiers
	 * @return une liste avec 1 nom (majorité des cas) ou 2 noms (contribuables ménage commun); ou plus pour les tiers débiteurs / entreprises
	 */
	private List<String> getNoms(Tiers tiers) {
		List<String> noms;
		if (tiers == null) {
			noms = Collections.emptyList();
		}
		else {
			try {
				noms = adresseService.getNomCourrier(tiers, null, false);
			}
			catch (Exception e) {
				noms = new ArrayList<>(1);
				noms.add(e.getMessage()); // rien de mieux à faire ici
			}
		}

		return noms;
	}

	/**
	 * Retourne le nom et prénom ou la désignation du tiers (ou des deux parties dans le cas d'un ménage commun)
	 *
	 * @param noTiers le numéro du tiers
	 * @return le nom et le prénom, ou la désignation du tiers.
	 */
	protected String getNom(@Nullable Long noTiers) {
		if (noTiers == null) {
			return StringUtils.EMPTY;
		}
		final Tiers tiers = tiersService.getTiers(noTiers);
		return getNom(tiers);
	}

	/**
	 * Retourne le nom et prénom ou la désignation du tiers (ou des deux parties dans le cas d'un ménage commun)
	 *
	 * @param tiers le tiers
	 * @return le nom et le prénom, ou la désignation du tiers.
	 */
	protected String getNom(@Nullable Tiers tiers) {
		if (tiers == null) {
			return StringUtils.EMPTY;
		}
		final String nom;

		final List<String> noms = getNoms(tiers);
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

	protected JobResults(TiersService tiersService, AdresseService adresseService) {
		this.tiersService = tiersService;
		this.adresseService = adresseService;
	}
}
