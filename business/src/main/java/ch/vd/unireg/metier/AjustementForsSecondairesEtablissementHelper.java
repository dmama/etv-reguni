package ch.vd.unireg.metier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.tiers.ForFiscalPrincipalPM;
import ch.vd.unireg.tiers.ForFiscalSecondaire;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;

/**
 * @author Raphaël Marmier, 2016-03-04, <raphael.marmier@vd.ch>
 */
public class AjustementForsSecondairesEtablissementHelper {

	/**
	 * Recalcul les fors secondaires en fonction des domiciles et fourni un résultat relatif aux for existant.
	 *
	 * @param tousLesDomicilesVD les domiciles vaudois à considérer dans le calcul
	 * @param tousLesForsFiscauxSecondairesEtablissementParCommune les fors secondaires existant
	 * @param forsFiscauxPrincipaux les fors principaux actifs. Nécessaires pour déterminer le genre d'impôt à appliquer. Doivent couvrir toute la période
	 * @param dateDebutFiscal la date précisant le début fiscal de l'entreprise, le début des fors secondaires est ajusté à cette date
	 * @return les ajustements résultant du calcul
	 * @throws MetierServiceException
	 */
	@NotNull
	public static AjustementForsSecondairesResult getResultatAjustementForsSecondaires(Map<Integer, List<Domicile>> tousLesDomicilesVD,
	                                                                                   Map<Integer, List<ForFiscalSecondaire>> tousLesForsFiscauxSecondairesEtablissementParCommune,
	                                                                                   List<ForFiscalPrincipalPM> forsFiscauxPrincipaux,
			                                                                           RegDate dateDebutFiscal) throws MetierServiceException {
		final List<ForFiscalSecondaire> aAnnulerResultat = new ArrayList<>();
		final List<AjustementForsSecondairesResult.ForAFermer> aFermerResultat = new ArrayList<>();
		final List<ForFiscalSecondaire> aCreerResultat = new ArrayList<>();
		final List<ForFiscalSecondaire> nonCouvertsResultat = new ArrayList<>();

		/* Par sécurité, vérifier qu'on ne coupe rien qui existe déjà avec la date de début fiscal. */
		if (dateDebutFiscal != null) {
			for (Map.Entry<Integer, List<ForFiscalSecondaire>> entry : tousLesForsFiscauxSecondairesEtablissementParCommune.entrySet()) {
				final ForFiscalSecondaire existant = DateRangeHelper.rangeAt(entry.getValue(), dateDebutFiscal.getOneDayBefore());
				if (existant != null) {
					throw new MetierServiceException(String.format("Une date de début fiscal au %s est précisée pour le recalcul des fors secondaires. Mais " +
							                                                       "un for secondaire existant en base débute avant cette date: commune %s, " +
							                                                       "début %s%s. Impossible de continuer. Veuillez signaler l'erreur.",
					                                                       RegDateHelper.dateToDisplayString(dateDebutFiscal),
					                                                       existant.getNumeroOfsAutoriteFiscale(),
					                                                       RegDateHelper.dateToDisplayString(existant.getDateDebut()),
					                                                       existant.getDateFin() != null ? " , fin " + RegDateHelper.dateToDisplayString(existant.getDateFin()) : ""
					));
				}
			}
		} else {
			throw new IllegalArgumentException("La date de début fiscale manque. Impossible de recalculer les fors secondaires.");
		}

		// Déterminer la liste des communes sur laquelle on travaille
		Set<Integer> communes = new HashSet<>(tousLesDomicilesVD.keySet());
		communes.addAll(tousLesForsFiscauxSecondairesEtablissementParCommune.keySet());

		for (Integer noOfsCommune : communes) {

			List<Domicile> domiciles = tousLesDomicilesVD.get(noOfsCommune);

			/*
			   On utilise l'historique des domiciles pour établir le périodes qui doivent être couverte par un for secondaire.
			   Ensuite de quoi on détermine ceux à annuler et ceux à créer pour la commune en cours.
			 */
			List<ForFiscalSecondaire> forCandidatsPourCommune;
			List<ForFiscalSecondaire> nonCouvertsPourCommune;

			if (domiciles != null && !domiciles.isEmpty()) {
				forCandidatsPourCommune = new ArrayList<>();
				nonCouvertsPourCommune = new ArrayList<>();
				// Garantir que les périodes ne sont pas fragmentées.
				final List<DateRange> mergedRanges = DateRangeHelper.merge(domiciles);
				for (DateRange periodeDomicile : mergedRanges) {

					// Adapter le premier for secondaire au début effectif dicté par la date de début fiscal, le cas échéant.
					final RegDate dateDebutRange;
					if (periodeDomicile.getDateDebut().isBefore(dateDebutFiscal)) {
						dateDebutRange = dateDebutFiscal;
					} else {
						dateDebutRange = periodeDomicile.getDateDebut();
					}
					/**
					 * Créer le for fiscal secondaire modèle correspondant à la période de domicile visée.
					 */
					final ForFiscalSecondaire forSecondaireSansGenre = new ForFiscalSecondaire(dateDebutRange, MotifFor.DEBUT_EXPLOITATION,
					                                                                           periodeDomicile.getDateFin(), periodeDomicile.getDateFin() != null ? MotifFor.FIN_EXPLOITATION : null,
					                                                                           noOfsCommune, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.ETABLISSEMENT_STABLE);
					forSecondaireSansGenre.setGenreImpot(null);

					// Générer les fors secondaires avec genre d'impôt correspondant au for fiscal secondaire modèle, à partir des fors principaux.
					final List<ForFiscalSecondaire> resultatCandidats = appliquerGenreImpot(forSecondaireSansGenre, forsFiscauxPrincipaux);
					forCandidatsPourCommune.addAll(resultatCandidats);

					// Déterminer et réinjecter les fors secondaires sans genre correspondant aux périodes non couvertes par un for principal
					final List<ForFiscalSecondaire> nonCouverts = DateRangeHelper.subtract(forSecondaireSansGenre, resultatCandidats,  new DateRangeHelper.AdapterCallback<ForFiscalSecondaire>() {
						@Override
						public ForFiscalSecondaire adapt(ForFiscalSecondaire range, RegDate debut, RegDate fin) {
							final RegDate dateOuverture = debut != null ? debut : range.getDateDebut();
							MotifFor motifOuverture = range.getMotifOuverture();
							if (debut != null && debut.isAfter(range.getDateDebut())) {
								motifOuverture = MotifFor.CHGT_MODE_IMPOSITION;
							}
							final RegDate dateFermeture = fin != null ? fin : range.getDateFin();
							MotifFor motifFermeture = range.getMotifFermeture();
							if (motifFermeture == null || fin != null && fin.isBefore(range.getDateFin())) {
								motifFermeture = MotifFor.CHGT_MODE_IMPOSITION;
							}
							final ForFiscalSecondaire nouveauForFiscalSecondaire =
									new ForFiscalSecondaire(dateOuverture, motifOuverture, dateFermeture, dateFermeture != null ? motifFermeture : null,
									                        range.getNumeroOfsAutoriteFiscale(), range.getTypeAutoriteFiscale(), range.getMotifRattachement());
							nouveauForFiscalSecondaire.setGenreImpot(null);

							return nouveauForFiscalSecondaire;
						}
					});
					forCandidatsPourCommune.addAll(nonCouverts);
					forCandidatsPourCommune.sort(new DateRangeComparator<>());

					// Les non couverts sont aussi retournés séparément
					nonCouvertsPourCommune.addAll(nonCouverts);
				}
			} else {
				forCandidatsPourCommune = Collections.emptyList();
				nonCouvertsPourCommune = Collections.emptyList();
			}



			final List<ForFiscalSecondaire> forFiscalSecondaires = tousLesForsFiscauxSecondairesEtablissementParCommune.get(noOfsCommune);

			/*
			 Determiner les fors à annuler dans la base Unireg (ils sont devenus redondant).
			 On compare non seulement la correspondance à la période, mais aussi au genre d'impôt et au motifs d'ouverture et de fermeture.
			  */
			if (forFiscalSecondaires != null) {
				for (ForFiscalSecondaire forExistant : forFiscalSecondaires) {
					// Rechercher dans les nouveaux projetés
					boolean aConserver = false;
					ForFiscalSecondaire forCandidatAEnlever = null;
					for (ForFiscalSecondaire forCandidat : forCandidatsPourCommune) {
						if (forExistant.getGenreImpot() == forCandidat.getGenreImpot()) {
							if (DateRangeHelper.equals(forExistant, forCandidat)) {
								aConserver = true;
							} else {
								// Cas du for à fermer
								if (forExistant.getDateDebut() == forCandidat.getDateDebut()
										&& forExistant.getDateFin() == null && forCandidat.getDateFin() != null) {
									aFermerResultat.add(new AjustementForsSecondairesResult.ForAFermer(forExistant, forCandidat.getDateFin()));
									forCandidatAEnlever = forCandidat;
									aConserver = true;
								}
							}
						}
					}
					if (!aConserver) {
						aAnnulerResultat.add(forExistant);
					}
					if (forCandidatAEnlever != null) {
						forCandidatsPourCommune.remove(forCandidatAEnlever);
					}
				}
			}

			// Determiner les fors à créer dans la base Unireg
			for (ForFiscalSecondaire forCandidat : forCandidatsPourCommune) {
				// Recherche dans les anciens fors, pour la commune en cours
				boolean existe = false;
				if (forFiscalSecondaires != null) {
					for (ForFiscalSecondaire forExistant : forFiscalSecondaires) {
						if (forExistant.getGenreImpot() == forCandidat.getGenreImpot() && DateRangeHelper.equals(forCandidat, forExistant)) {
							existe = true;
						}
					}
				}
				if (!existe) {
					aCreerResultat.add(forCandidat);
				}
			}

			// Ajouter la liste des périodes non couvertes
			nonCouvertsResultat.addAll(nonCouvertsPourCommune);
		}

		return new AjustementForsSecondairesResult(aAnnulerResultat, aFermerResultat, aCreerResultat, nonCouvertsResultat);
	}

	/**
	 * Appliquer le genre d'impot en vigueur sur le/les fors fiscaux principaux au for secondaire, en le fragmentant si nécessaire pour tenir compte des changements
	 * de genre d'impot.
	 *
	 * ATTENTION: dans le cas ou le for fiscal ne couvre pas entièrement le for secondaire, seule la partie couverte sera générée et ajouté au résultat. Les périodes
	 *            non couvertes sont silencieusement ignorées.
	 *
	 * @param forFiscalSecondaireModele Le for fiscal secondaire cible
	 * @param forsFiscauxPrincipaux La liste des fors principaux
	 * @return la liste des fors fiscaux à créer
	 */
	private static List<ForFiscalSecondaire> appliquerGenreImpot(final ForFiscalSecondaire forFiscalSecondaireModele, final List<ForFiscalPrincipalPM> forsFiscauxPrincipaux) {
		if (forsFiscauxPrincipaux == null || forsFiscauxPrincipaux.isEmpty()) {
			return Collections.emptyList();
		}

		List<ForFiscalSecondaire> resultat = new ArrayList<>();
		// Extraire les fors principaux correspondant à la période du for secondaire. On en a besoin pour arriver à distinguer le début/fin d'activité du changements de genre d'impôt. Voir ci-dessous.
		final List<ForFiscalPrincipalPM> forsPrincipauxPourForSecondairesACreer =
				DateRangeHelper.extract(forsFiscauxPrincipaux, forFiscalSecondaireModele.getDateDebut(), forFiscalSecondaireModele.getDateFin(), new DateRangeHelper.AdapterCallback<ForFiscalPrincipalPM>() {
					@Override
					public ForFiscalPrincipalPM adapt(ForFiscalPrincipalPM range, RegDate debut, RegDate fin) {
						ForFiscalPrincipalPM nouveauForFiscalPrincipalPM = new ForFiscalPrincipalPM(
								debut != null ? debut : range.getDateDebut(),
								range.getMotifOuverture(),
								fin != null ? fin : range.getDateFin(),
								range.getMotifFermeture(),
								range.getNumeroOfsAutoriteFiscale(),
								range.getTypeAutoriteFiscale(),
								range.getMotifRattachement()
						);
						nouveauForFiscalPrincipalPM.setGenreImpot(range.getGenreImpot());
						return nouveauForFiscalPrincipalPM;
					}
				});

		// Classer les fors principaux par genre d'impot
		Map<GenreImpot, List<ForFiscalPrincipalPM>> forPrincipauxParGenre = indexForPrincipauxParGenre(forsPrincipauxPourForSecondairesACreer);

		// Gérérer les fors secondaires pour chaque genre d'impôt.
		for (Map.Entry<GenreImpot, List<ForFiscalPrincipalPM>> genreEntry : forPrincipauxParGenre.entrySet()) {
			// Eliminer toute fragmentation artificielle.
			final List<DateRange> mergedRanges = DateRangeHelper.merge(genreEntry.getValue());

			// On extrait les fors secondaires correspondant aux périodes de for principal du genre en cours.
			final GenreImpot genreImpot = genreEntry.getKey();
			resultat.addAll(DateRangeHelper.extract(Collections.singletonList(forFiscalSecondaireModele), mergedRanges, new DateRangeHelper.AdapterCallback<ForFiscalSecondaire>() {
				@Override
				public ForFiscalSecondaire adapt(ForFiscalSecondaire range, RegDate debut, RegDate fin) {
					final RegDate dateOuverture = debut != null ? debut : range.getDateDebut();
					MotifFor motifOuverture = range.getMotifOuverture();
					if (debut != null && debut.isAfter(range.getDateDebut())) {
						motifOuverture = MotifFor.CHGT_MODE_IMPOSITION;
					}
					final RegDate dateFermeture = fin != null ? fin : range.getDateFin();
					MotifFor motifFermeture = range.getMotifFermeture();
					if (motifFermeture == null || fin != null && fin.isBefore(range.getDateFin())) {
						motifFermeture = MotifFor.CHGT_MODE_IMPOSITION;
					}
					final ForFiscalSecondaire nouveauForFiscalSecondaire =
							new ForFiscalSecondaire(dateOuverture, motifOuverture, dateFermeture, dateFermeture != null ? motifFermeture : null,
							                        range.getNumeroOfsAutoriteFiscale(), range.getTypeAutoriteFiscale(), range.getMotifRattachement());
					nouveauForFiscalSecondaire.setGenreImpot(genreImpot);

					return nouveauForFiscalSecondaire;
				}
			}));
		}
		resultat.sort(new DateRangeComparator<>(DateRangeComparator.CompareOrder.ASCENDING));
		return resultat;
	}

	/**
	 * Classer les fors fiscaux selon leur genre d'impôt.
	 * @param forsfiscaux la liste des fors
	 * @return une map de fors fiscaux indexée par genre
	 */
	@NotNull
	private static Map<GenreImpot, List<ForFiscalPrincipalPM>> indexForPrincipauxParGenre(List<ForFiscalPrincipalPM> forsfiscaux) {
		final Map<GenreImpot, List<ForFiscalPrincipalPM>> forFiscauxParGenre = new EnumMap<>(GenreImpot.class);
		for (ForFiscalPrincipalPM ffp : forsfiscaux) {
			final List<ForFiscalPrincipalPM> forFiscals = forFiscauxParGenre.computeIfAbsent(ffp.getGenreImpot(), k -> new ArrayList<>());
			forFiscals.add(ffp);
		}
		return forFiscauxParGenre;
	}
}
