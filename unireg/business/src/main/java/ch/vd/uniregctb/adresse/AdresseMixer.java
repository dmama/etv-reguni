package ch.vd.uniregctb.adresse;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;

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
	public static List<AdresseGenerique> override(List<AdresseGenerique> adresses, List<AdresseGenerique> surcharges, final Source sourceSurcharge, final Boolean defaultSurcharge) {

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
			for (AdresseGenerique a : adresses) {
				if (a.isAnnule()) {
					annulee = true;
					break;
				}
			}
		}
		return annulee;
	}
}
