package ch.vd.uniregctb.adresse;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseGenerique.Source;

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

		private AdresseGeneriqueAdapterCallback(Boolean defaultSurcharge, Source sourceSurcharge) {
			this.defaultSurcharge = defaultSurcharge;
			this.sourceSurcharge = sourceSurcharge;
		}

		public AdresseGenerique adapt(AdresseGenerique range, RegDate debut, RegDate fin) {
			if (debut == null && fin == null && sourceSurcharge == null && defaultSurcharge == null) {
				return range; // optim
			}
			else {
				return new AdresseGeneriqueAdapter(range, debut, fin, sourceSurcharge, defaultSurcharge);
			}
		}
	}

	/**
	 * Extrait les adresses comprises dans la période spécifiée, et adapte si nécessaire les dates de début/fin des adresses retournées.
	 * <p>
	 * Exemple:
	 *
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
	 * @param adresses
	 *            une collection d'adresses triées par ordre chronologique.
	 * @param debut
	 *            la date début de la période à extraire, ou <b>null</b> pour spécifier la nuit des temps (Big Bang).
	 * @param fin
	 *            la date fin (inclus) de la période à extraire, ou <b>null</b> pour spécifier la fin des temps (Big Crunch).
	 * @return une liste d'adresses.
	 */
	public static List<AdresseGenerique> extract(List<AdresseGenerique> adresses, RegDate debut, RegDate fin) {
		return extract(adresses, debut, fin, null, null);
	}

	/**
	 * Extrait les adresses comprises dans la période spécifiée, et adapte si nécessaire les dates de début/fin des adresses retournées.
	 * <p>
	 * Exemple:
	 *
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
	 * @param adresses
	 *            une collection d'adresses triées par ordre chronologique.
	 * @param debut
	 *            la date début de la période à extraire, ou <b>null</b> pour spécifier la nuit des temps (Big Bang).
	 * @param fin
	 *            la date fin (inclus) de la période à extraire, ou <b>null</b> pour spécifier la fin des temps (Big Crunch).
	 * @param sourceSurcharge
	 *            valeur de surcharge pour la source des adresses extraite, ou <b>null</b> pour garder la source des adresses originelles.
	 * @param defaultSurcharge
	 *            valeur de surcharge pour le défaut des adresses extraite, ou <b>null</b> pour garder le défaut des adresses originelles.
	 * @return une liste d'adresses.
	 */
	public static List<AdresseGenerique> extract(List<AdresseGenerique> adresses, RegDate debut, RegDate fin, final Source sourceSurcharge,
			final Boolean defaultSurcharge) {

		return DateRangeHelper.extract(adresses, debut, fin, new AdresseGeneriqueAdapterCallback(defaultSurcharge, sourceSurcharge));
	}

	/**
	 * Surcharge la liste d'adresses spécifiées avec une secondes liste d'adresse.
	 * <p>
	 * Exemple:
	 *
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
	 *
	 * <b>Note:</b> les adresses annulées sont ignorées et retournées telles quelles dans la liste résultante.
	 *
	 * @param sourceSurcharge
	 *            valeur de surcharge pour les adresses surchargées, ou <b>null</b> pour garder la source des adresses originelles.
	 * @param defaultSurcharge
	 *            valeur de surcharge pour les adresses surchargées, ou <b>null</b> pour garder le défaut des adresses originelles.
	 * @return la liste des adresses combinées.
	 */
	public static List<AdresseGenerique> override(List<AdresseGenerique> adresses, List<AdresseGenerique> surcharges,
			final Source sourceSurcharge, final Boolean defaultSurcharge) {

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
			res = DateRangeHelper.override(nonAnnulees, surchargeNonAnnulees, new AdresseGeneriqueAdapterCallback(defaultSurcharge,
					sourceSurcharge));
			// on ajoute les adresses annulées à la fin
			res.addAll(annulees);
		}
		else {
			// pas d'adresse annulée -> on évite de créer des listes pour rien
			res = DateRangeHelper.override(adresses, surcharges, new AdresseGeneriqueAdapterCallback(defaultSurcharge, sourceSurcharge));
		}
		return res;

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
