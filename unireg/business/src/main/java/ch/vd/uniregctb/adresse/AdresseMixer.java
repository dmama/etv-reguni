package ch.vd.uniregctb.adresse;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.ProgrammingException;

import static ch.vd.uniregctb.adresse.AdresseGenerique.Source;

/**
 * Classe utilitaire <b>purement technique</b> dédiée à la manipulation des adresses génériques. Aucune régle métier ne doit être définie
 * dans cette classe.
 */
public class AdresseMixer {

	/**
	 * Callback spécialisé dans l'adaptation d'adresse génériques.
	 */
	private static final class AdresseGeneriqueAdapterCallback implements DateRangeHelper.AdapterCallback<AdresseGenerique> {
		private final Boolean defaultSurcharge;
		private final Source sourceSurcharge;
		private final boolean forceSurcharge;

		/**
		 * @param defaultSurcharge la valeur à appliquer aux adresses surchargées
		 * @param sourceSurcharge  la source à appliquer aux adresses surchargées
		 * @param forceSurcharge   <b>vrai</b> si les paramètres <i>defaultSurcharge</i> et <i>sourceSurcharge</i> doivent s'appliquer à toutes les adresses (surchargées ou non); <b>faux</b> si les
		 *                         paramètres <i>defaultSurcharge</i> et <i>sourceSurcharge</i> ne doivent s'appliquer qu'aux adresses surchargées.
		 */
		private AdresseGeneriqueAdapterCallback(Boolean defaultSurcharge, Source sourceSurcharge, boolean forceSurcharge) {
			this.defaultSurcharge = defaultSurcharge;
			this.sourceSurcharge = sourceSurcharge;
			this.forceSurcharge = forceSurcharge;
		}

		@Override
		public AdresseGenerique adapt(AdresseGenerique range, RegDate debut, RegDate fin) {
			if (forceSurcharge || (debut == null && fin == null)) {
				// si ces deux dates sont nulles, cela signifie que 'range' est une adresse de surcharge => on veut appliquer les valeurs 'source' et 'default' demandées dans ces cas-là.
				return new AdresseGeneriqueAdapter(range, debut, fin, sourceSurcharge, defaultSurcharge);
			}
			else {
				// [UNIREG-2927] si une des dates de début ou de fin est renseignée, cela signifie que 'range' est une adresse de base qui doit être adaptée pour laisser de la place à une adresse de surcharge => on ne veut pas changer les valeurs 'source' et 'default' dans ce cas-là.
				return new AdresseGeneriqueAdapter(range, debut, fin, null, null);
			}
		}
	}

	/**
	 * Extrait les adresses comprises dans la période spécifiée, et adapte si nécessaire les dates de début/fin des adresses retournées.
	 * <p/>
	 * Exemple:
	 * <p/>
	 * <pre>
	 *             +----------------------------------+-----------------------------------+--------------
	 * adresses    |-2000.01.01            2000.12.31-|-2001.01.01             2001.06.30-|-2001.07.01
	 *             +----------------------------------+-----------------------------------+--------------
	 *                      :                                                   :
	 * debut /fin           :-2000.08.20                             2001.05.20-:
	 *                      :                                                   :
	 *                      +-------------------------+-------------------------+
	 * résultat             |-2000.08.20   2000.12.31-|-2001.01.01   2001.05.20-|
	 *                      +-------------------------+-------------------------+
	 * </pre>
	 *
	 * @param adresses une collection d'adresses triées par ordre chronologique.
	 * @param debut    la date début de la période à extraire, ou <b>null</b> pour spécifier la nuit des temps (Big Bang).
	 * @param fin      la date fin (inclus) de la période à extraire, ou <b>null</b> pour spécifier la fin des temps (Big Crunch).
	 * @return une liste d'adresses.
	 */
	public static List<AdresseGenerique> extract(List<AdresseGenerique> adresses, RegDate debut, RegDate fin) {
		return extract(adresses, debut, fin, null, null);
	}

	/**
	 * Extrait les adresses comprises dans la période spécifiée, et adapte si nécessaire les dates de début/fin des adresses retournées.
	 * <p/>
	 * Exemple:
	 * <p/>
	 * <pre>
	 *             +----------------------------------+-----------------------------------+--------------
	 * adresses    |-2000.01.01            2000.12.31-|-2001.01.01             2001.06.30-|-2001.07.01
	 *             +----------------------------------+-----------------------------------+--------------
	 *                      :                                                   :
	 * debut /fin           :-2000.08.20                             2001.05.20-:
	 *                      :                                                   :
	 *                      +-------------------------+-------------------------+
	 * résultat             |-2000.08.20   2000.12.31-|-2001.01.01   2001.05.20-|
	 *                      +-------------------------+-------------------------+
	 * </pre>
	 *
	 * @param adresses         une collection d'adresses triées par ordre chronologique.
	 * @param debut            la date début de la période à extraire, ou <b>null</b> pour spécifier la nuit des temps (Big Bang).
	 * @param fin              la date fin (inclus) de la période à extraire, ou <b>null</b> pour spécifier la fin des temps (Big Crunch).
	 * @param sourceSurcharge  valeur de surcharge pour la source des adresses extraite, ou <b>null</b> pour garder la source des adresses originelles.
	 * @param defaultSurcharge valeur de surcharge pour le défaut des adresses extraite, ou <b>null</b> pour garder le défaut des adresses originelles.
	 * @return une liste d'adresses.
	 */
	public static List<AdresseGenerique> extract(List<AdresseGenerique> adresses, RegDate debut, RegDate fin, final Source sourceSurcharge, final Boolean defaultSurcharge) {
		return DateRangeHelper.extract(adresses, debut, fin, new AdresseGeneriqueAdapterCallback(defaultSurcharge, sourceSurcharge, true));
	}

	/**
	 * Extrait les adresses comprises dans les périodes spécifiées, et adapte si nécessaire les dates de début/fin des adresses retournées.
	 * <p/>
	 * Exemple:
	 * <p/>
	 * <pre>
	 *             +-----------------------------+   +-------------------------+            +-------------------------+
	 * adresses    |-2000.01.01       2001.12.31-|   |-2002.05.01   2002.08.15-|            |-2004.01.01   2004.03.31-|
	 *             +-----------------------------+   +-------------------------+            +-------------------------+
	 *             :                                                                        :                         :
	 *             +-------------------------+                                    +--------------------------------------------------+
	 * ranges      |-2000.01.01   2000.08.19-|                                    |-2003.01.01                            2005.08.31-|
	 *             +-------------------------+                                    +--------------------------------------------------+
	 *             :                         :                                              :                         :
	 *             +-------------------------+                                              +-------------------------+
	 * résultat    |-2000.01.01   2000.08.19-|                                              |-2004.01.01   2004.38.31-|
	 *             +-------------------------+                                              +-------------------------+
	 *
	 * @param adresses         une collection d'adresses triées par ordre chronologique.
	 * @param ranges           les périodes à extraire
	 * @param sourceSurcharge  valeur de surcharge pour la source des adresses extraite, ou <b>null</b> pour garder la source des adresses originelles.
	 * @param defaultSurcharge valeur de surcharge pour le défaut des adresses extraite, ou <b>null</b> pour garder le défaut des adresses originelles.
	 * @return une liste d'adresses.
	 */
	public static List<AdresseGenerique> extract(List<AdresseGenerique> adresses, List<? extends DateRange> ranges, final Source sourceSurcharge, final Boolean defaultSurcharge) {
		return DateRangeHelper.extract(adresses, ranges, new AdresseGeneriqueAdapterCallback(defaultSurcharge, sourceSurcharge, true));
	}

	/**
	 * Surcharge la liste d'adresses spécifiées avec une secondes liste d'adresse.
	 * <p/>
	 * Exemple:
	 * <p/>
	 * <pre>
	 *             +-------------------------------------------+---------------------------------------------+------------------
	 * adresses    |-2000.01.01         (A)         2000.12.31-|-2001.01.01          (B)          2001.06.30-|-2001.07.01  (C)
	 *             +-------------------------------------------+---------------------------------------------+------------------
	 *                                           :                             :
	 *                                           +-----------------------------+
	 * surcharges                                |-2000.08.20  (D)  2001.03.31-|
	 *                                           +-----------------------------+
	 *                                           :                             :
	 *             +-----------------------------+-----------------------------+------------------------------------------------
	 * résultat    |-2000.01.01  (A)  2000.08.19-|-2000.08.20  (D)  2001.03.31-|-2001.04.01  (B)  2001.06.30-|-2001.07.01  (C)
	 *             +-----------------------------+-----------------------------+------------------------------------------------
	 * </pre>
	 * <p/>
	 * <b>Note:</b> les adresses annulées sont ignorées et retournées telles quelles dans la liste résultante.
	 *
	 * @param adresses         les adresse base
	 * @param surcharges       les adresses à surcharger sur les adresses de base
	 * @param sourceSurcharge  valeur de surcharge pour les adresses surchargées, ou <b>null</b> pour garder la source des adresses originelles.
	 * @param defaultSurcharge valeur de surcharge pour les adresses surchargées, ou <b>null</b> pour garder le défaut des adresses originelles.
	 * @return la liste des adresses combinées.
	 */
	public static List<AdresseGenerique> override(List<AdresseGenerique> adresses, List<AdresseGenerique> surcharges, @Nullable final Source sourceSurcharge, @Nullable final Boolean defaultSurcharge) {

		final boolean annulee = hasAdresseAnnulee(adresses) || hasAdresseAnnulee(surcharges);

		final List<AdresseGenerique> res;

		if (annulee) {
			// [UNIREG-888] Si on a des adresses annulées, il faut les extraire des listes avant d'appliquer l'override parce que l'adresse
			// mixer ne connaît la notion d'annulation.
			final List<AdresseGenerique> nonAnnulees = new ArrayList<AdresseGenerique>();
			final List<AdresseGenerique> surchargeNonAnnulees = new ArrayList<AdresseGenerique>();
			final List<AdresseGenerique> annulees = new ArrayList<AdresseGenerique>();

			if (adresses != null) {
				for (AdresseGenerique a : adresses) {
					if (a.isAnnule()) {
						annulees.add(a);
					}
					else {
						nonAnnulees.add(a);
					}
				}
			}
			if (surcharges != null) {
				for (AdresseGenerique a : surcharges) {
					if (a.isAnnule()) {
						annulees.add(a);
					}
					else {
						surchargeNonAnnulees.add(a);
					}
				}
			}

			// override sans les adresses annulées
			res = DateRangeHelper.override(nonAnnulees, surchargeNonAnnulees, new AdresseGeneriqueAdapterCallback(defaultSurcharge, sourceSurcharge, false));
			// on ajoute les adresses annulées à la fin
			res.addAll(annulees);
		}
		else {
			// pas d'adresse annulée -> on évite de créer des listes pour rien
			res = DateRangeHelper.override(adresses, surcharges, new AdresseGeneriqueAdapterCallback(defaultSurcharge, sourceSurcharge, false));
		}
		return res;

	}

	/**
	 * Découpe un ou plusieurs plages dans une liste d'adresses.
	 * <p/>
	 * Exemple:
	 * <p/>
	 * <pre>
	 *             +--------------------------------------+---------------------------------------------------+       +-----------------
	 * adresses    |-2000.01.01                2000.12.31-|-2001.01.01                             2001.12.31-|       |-2003.01.01
	 *             +--------------------------------------+---------------------------------------------------+       +-----------------
	 *             :
	 *             :                         +-------------------------+                         +-------------------------+
	 * ranges      :                         |-2000.08.20   2001.04.30-|                         |-2001.09.01   2003.06.30-|
	 *             :                         +-------------------------+                         +-------------------------+
	 *             :                         :                         :                         :                         :
	 *             +-------------------------+                         +-------------------------+                         +--------------
	 * résultat    |-2000.01.01   2000.08.19-|                         |-2001.05.01   2001.08.31-|                         |-2003.07.01
	 *             +-------------------------+                         +-------------------------+                         +--------------
	 * </pre>
	 *
	 * @param adresses une liste d'adresses
	 * @param ranges   les plages de validité à découper dans la liste d'adresse
	 * @return une liste d'adresse
	 */
	public static List<AdresseGenerique> subtract(List<AdresseGenerique> adresses, List<DateRange> ranges) {
		return DateRangeHelper.subtract(adresses, ranges, new AdresseGeneriqueAdapterCallback(null, null, false));
	}

	private static boolean hasAdresseAnnulee(List<AdresseGenerique> adresses) {
		boolean annulee = false;
		if (adresses != null) {
			for (int i = 0, adressesSize = adresses.size(); i < adressesSize; i++) {
				final AdresseGenerique a = adresses.get(i);
				if (a.isAnnule()) {
					annulee = true;
					break;
				}
			}
		}
		return annulee;
	}

	/**
	 * Détermine les trous dans une collection d'adresses de base et calcule les adresses qui permettraient de boucher ces trous.
	 *
	 * @param adresses les adresses de base (qui peuvent posséder des trous)
	 * @param defaults une ou plusieurs collections d'adresses utilisées comme source pour boucher les trous. La première collection d'adresse sera utilisée prioritairement pour boucher les trous, puis
	 *                 les autres par ordre décroissant de priorité.
	 * @return une liste d'adresses qui peuvent être utilisées pour boucher les trous de la collection d'adresses de base; ou <b>null</b> si la collection de base ne possède aucun trou.
	 */
	public static List<AdresseGenerique> determineBoucheTrous(List<AdresseGenerique> adresses, List<AdresseGenerique>... defaults) {
		if (defaults == null || defaults.length == 0) {
			return null;
		}

		final boolean annulee = hasAdresseAnnulee(adresses);
		if (annulee) {
			// [SIFISC-1868] Si on a des adresses annulées, il faut les supprimer de la liste avant de déterminer les trous
			adresses = extractAdressesNonAnnulees(adresses);
		}

		if (DateRangeHelper.isFull(adresses)) {
			// il n'y a pas de trou dans le sandwich -> inutile d'essayer d'appliquer des valeurs par défaut
			return null;
		}

		// on calcule la vue à plat de la couche d'adresses par défaut
		List<AdresseGenerique> adressesDefault = new ArrayList<AdresseGenerique>();
		for (int i = defaults.length - 1; i >= 0; i--) {

			final List<AdresseGenerique> list = new ArrayList<AdresseGenerique>();
			for (AdresseGenerique a : defaults[i]) {
				if (a.isAnnule()) {
					// on ne prend pas en compte les adresses annulées comme défaut
					continue;
				}
				if (a.getSource().getType().isRepresentation()) {
					// [UNIREG-3025] on ne prend pas en compte les adresse de représentation comme défaut
					continue;
				}
				list.add(a);
			}

			if (!list.isEmpty()) {
				adressesDefault = AdresseMixer.override(adressesDefault, list, null, null);
			}
		}

		if (adressesDefault.isEmpty()) {
			return null;
		}

		// on détermine les trous dans le sandwich qui peuvent être comblés par les adresses par défaut
		return DateRangeHelper.subtract(adressesDefault, adresses, new DateRangeHelper.AdapterCallback<AdresseGenerique>() {
			@Override
			public AdresseGenerique adapt(AdresseGenerique range, RegDate debut, RegDate fin) {
				return new AdresseGeneriqueAdapter(range, debut, fin, null, null);
			}
		});
	}

	/**
	 * Crée une nouvelle liste avec les adresses non-annulées.
	 *
	 * @param adresses une liste d'adresse avec potentiellement des adresses annulées.
	 * @return une nouvelle liste qui ne contient que des adresses non-annulées.
	 */
	public static List<AdresseGenerique> extractAdressesNonAnnulees(@NotNull List<AdresseGenerique> adresses) {
		final List<AdresseGenerique> nonAnnulees = new ArrayList<AdresseGenerique>(adresses.size());
		for (AdresseGenerique a : adresses) {
			if (!a.isAnnule()) {
				nonAnnulees.add(a);
			}
		}
		return nonAnnulees;
	}

	/**
	 * Découpe les adresses spécifiées aux dates spécifiées. Chacune des dates spécifiées va splitter une adresse en deux de telle manière que la premier adresse s'arrête le jour précédent et que la
	 * seconde adresse commence le jour de la date considérée.
	 *
	 * @param adresses une liste d'adresses
	 * @param dates    une liste de dates
	 * @return la liste d'adresses découpées; ou la liste spécifiée en entrée si aucun découpage n'a eu lieu.
	 */
	public static List<AdresseGenerique> splitAt(@Nullable List<AdresseGenerique> adresses, @Nullable Set<RegDate> dates) {

		if (adresses == null || adresses.isEmpty() || dates == null || dates.isEmpty()) {
			return adresses;
		}

		final List<AdresseGenerique> list = new ArrayList<AdresseGenerique>(adresses);
		int count = 0;
		while (splitOnceAt(list, dates)) {
			if (++count > 1000) {
				throw new ProgrammingException();
			}
		}

		return list;
	}

	/**
	 * Découpe en deux la <b>première</b> des adresses spécifiées qui peut l'être à une des dates données et retourne immédiatement. Cette méthode ne découpe pas plus d'une adresse à la fois.
	 *
	 * @param adresses une liste d'adresses
	 * @param dates    une liste de dates
	 * @return <b>vrai</b> si une des adresses a été découpées; <b>faux</b> si aucune adresse n'a pu être découpée.
	 */
	private static boolean splitOnceAt(List<AdresseGenerique> adresses, Set<RegDate> dates) {
		for (int i = 0, adressesSize = adresses.size(); i < adressesSize; i++) {
			final AdresseGenerique a = adresses.get(i);
			for (RegDate date : dates) {
				if (a.isValidAt(date) && a.getDateDebut() != date) {
					// on remplace l'adresse par les deux adresses splittées
					adresses.remove(i);
					adresses.add(i, new AdresseGeneriqueAdapter(a, a.getDateDebut(), date.getOneDayBefore(), null));
					adresses.add(i + 1, new AdresseGeneriqueAdapter(a, date, a.getDateFin(), null));
					return true;
				}
			}
		}
		return false;
	}
}
