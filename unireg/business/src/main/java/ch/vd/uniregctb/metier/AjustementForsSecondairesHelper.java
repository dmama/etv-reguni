package ch.vd.uniregctb.metier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * @author Raphaël Marmier, 2016-03-04, <raphael.marmier@vd.ch>
 */
public class AjustementForsSecondairesHelper {

	/**
	 * Recalcul les fors secondaires en fonction des domiciles et fourni un résultat relatif aux for existant.
	 *
	 * @param tousLesDomicilesVD les domiciles vaudois à considérer dans le calcul
	 * @param tousLesForsFiscauxSecondairesParCommune les fors secondaires existant
	 * @param dateAuPlusTot une date de commencement au plus tôt, qui coupe s'il le faut les débuts des fors secondaires calculés.
	 * @return les ajustements résultant du calcul
	 * @throws MetierServiceException
	 */
	@NotNull
	public static AjustementForsSecondairesResult getResultatAjustementForsSecondaires(Map<Integer, List<Domicile>> tousLesDomicilesVD,
	                                                                                   Map<Integer, List<ForFiscalSecondaire>> tousLesForsFiscauxSecondairesParCommune, RegDate dateAuPlusTot) throws
			MetierServiceException {
		final List<ForFiscalSecondaire> aAnnulerResultat = new ArrayList<>();
		final List<AjustementForsSecondairesResult.ForAFermer> aFermerResultat = new ArrayList<>();
		final List<ForFiscalSecondaire> aCreerResultat = new ArrayList<>();

		/* Lors d'une création, une date au plus tôt signifie qu'on ne doit rien couper qui existe déjà! Sécurité -> à revoir l'utilité. */
		if (dateAuPlusTot != null) {
			for (Map.Entry<Integer, List<ForFiscalSecondaire>> entry : tousLesForsFiscauxSecondairesParCommune.entrySet()) {
				final ForFiscalSecondaire existant = DateRangeHelper.rangeAt(entry.getValue(), dateAuPlusTot.getOneDayBefore());
				if (existant != null) {
					throw new MetierServiceException(String.format("Une date au plus tôt %s est précisée pour le recalcul des fors secondaires. Mais " +
							                                                       "au moins un for secondaire valide débutant antiérieurement a été trouvé sur la commune %s. " +
							                                                       "Début %s%s. Impossible de continuer. Veuillez signaler l'erreur.",
					                                                       RegDateHelper.dateToDisplayString(dateAuPlusTot),
					                                                       existant.getNumeroOfsAutoriteFiscale(),
					                                                       RegDateHelper.dateToDisplayString(existant.getDateDebut()),
					                                                       existant.getDateFin() != null ? " , fin " + RegDateHelper.dateToDisplayString(existant.getDateFin()) : ""
					));
				}
			}
		}

		List<ForFiscalSecondaire> aCreer = new ArrayList<>();

		// Déterminer la liste des communes sur laquelle on travaille
		Set<Integer> communes = new HashSet<>(tousLesDomicilesVD.keySet());
		communes.addAll(tousLesForsFiscauxSecondairesParCommune.keySet());

		for (Integer noOfsCommune : communes) {

			List<Domicile> domiciles = tousLesDomicilesVD.get(noOfsCommune);

			/*
			   Fusion des ranges qui se chevauchent pour obtenir la liste des ranges tels qu'on les veut, les candidats.
			   Ensuite de quoi on détermine ceux à annuler et ceux à créer pour la commune en cours.
			 */
			List<DateRange> rangesCandidatsPourCommune;
			if (domiciles != null && !domiciles.isEmpty()) {
				rangesCandidatsPourCommune = DateRangeHelper.merge(domiciles);
			} else {
				rangesCandidatsPourCommune = Collections.emptyList();
			}

			final List<ForFiscalSecondaire> forFiscalSecondaires = tousLesForsFiscauxSecondairesParCommune.get(noOfsCommune);

			// Determiner les fors à annuler dans la base Unireg (ils sont devenus redondant)
			if (forFiscalSecondaires != null) {
				for (ForFiscalSecondaire forExistant : forFiscalSecondaires) {
					// Rechercher dans les nouveaux projetés
					boolean aConserver = false;
					DateRange rangeCandidatAEnlever = null;
					for (DateRange rangeCandidat : rangesCandidatsPourCommune) {
						if (DateRangeHelper.equals(forExistant, rangeCandidat)) {
							aConserver = true;
						} else {
							// Cas du for à fermer
							if (forExistant.getDateDebut() == rangeCandidat.getDateDebut()
									&& forExistant.getDateFin() == null && rangeCandidat.getDateFin() != null) {
								aFermerResultat.add(new AjustementForsSecondairesResult.ForAFermer(forExistant, rangeCandidat.getDateFin()));
								rangeCandidatAEnlever = rangeCandidat;
								aConserver = true;
							}
						}
					}
					if (!aConserver) {
						aAnnulerResultat.add(forExistant);
					}
					if (rangeCandidatAEnlever != null) {
						rangesCandidatsPourCommune.remove(rangeCandidatAEnlever);
					}
				}
			}

			// Determiner les fors à créer dans la base Unireg
			for (DateRange rangesCandidat : rangesCandidatsPourCommune) {
				// Recherche dans les anciens fors, pour la commune en cours
				boolean existe = false;
				if (forFiscalSecondaires != null) {
					for (ForFiscalSecondaire forExistant : forFiscalSecondaires) {
						if (DateRangeHelper.equals(rangesCandidat, forExistant)) {
							existe = true;
						}
					}
				}
				if (!existe) {
					aCreer.add(new ForFiscalSecondaire(rangesCandidat.getDateDebut(), MotifFor.DEBUT_EXPLOITATION,
					                                   rangesCandidat.getDateFin(), rangesCandidat.getDateFin() != null ? MotifFor.FIN_EXPLOITATION : null,
					                                   noOfsCommune, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.ETABLISSEMENT_STABLE));
				}
			}
		}


		for (ForFiscalSecondaire forACreer : aCreer) {
			// Cas du for précédant entièrement la dateAuPlusTot
			if (dateAuPlusTot != null && forACreer.getDateFin() != null && dateAuPlusTot.isAfter(forACreer.getDateFin())) {
				continue;
			}
			// Cas de la dateAuPlusTot qui tombe au milieu d'un for en cours
			else if (dateAuPlusTot != null && forACreer.isValidAt(dateAuPlusTot)) {
				forACreer = new ForFiscalSecondaire(dateAuPlusTot, forACreer.getMotifOuverture(), forACreer.getDateFin(), forACreer.getMotifFermeture(),
				                                    forACreer.getNumeroOfsAutoriteFiscale(), forACreer.getTypeAutoriteFiscale(), forACreer.getMotifRattachement());
			}
			aCreerResultat.add(forACreer);
		}

		return new AjustementForsSecondairesResult(aAnnulerResultat, aFermerResultat, aCreerResultat);
	}

}
