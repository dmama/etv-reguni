package ch.vd.uniregctb.registrefoncier.dataimport.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.capitastra.grundstueck.AmtlicheBewertung;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.registrefoncier.EstimationRF;

public abstract class EstimationRFHelper {

	private static final List<Pair<Pattern, ToIntFunction<Matcher>>> ANNEE_ESTIMATION_FISCALE_PATTERNS = buildPatternsAnneeEstimationFiscale();

	private EstimationRFHelper() {
	}

	public static boolean dataEquals(@Nullable EstimationRF estimation, @Nullable AmtlicheBewertung amtlicheBewertung) {
		return dataEquals(estimation, get(amtlicheBewertung), false);
	}

	public static boolean dataEquals(@Nullable EstimationRF left, @Nullable EstimationRF right, boolean ignoreRevisionFlag) {
		if (left == null && right == null) {
			return true;
		}
		else //noinspection SimplifiableIfStatement
			if (left == null || right == null) {
				return false;
			}
			else {
				return Objects.equals(left.getMontant(), right.getMontant()) &&
						Objects.equals(left.getReference(), right.getReference()) &&
						Objects.equals(left.getDateInscription(), right.getDateInscription()) &&
						(left.isEnRevision() == right.isEnRevision() || ignoreRevisionFlag);
			}
	}

	@NotNull
	public static EstimationRF newEstimationRF(@NotNull AmtlicheBewertung amtlicheBewertung) {
		final EstimationRF estimation = new EstimationRF();
		estimation.setMontant(amtlicheBewertung.getAmtlicherWert());
		estimation.setReference(amtlicheBewertung.getProtokollNr());
		estimation.setAnneeReference(determineAnneeReference(amtlicheBewertung.getProtokollNr()));
		estimation.setDateInscription(amtlicheBewertung.getProtokollDatum());
		estimation.setDateDebutMetier(determineDateDebutMetier(amtlicheBewertung.getProtokollNr(), amtlicheBewertung.getProtokollDatum()));
		estimation.setDateFinMetier(null);  // à calculer plus tard
		final Boolean gueltig = amtlicheBewertung.isProtokollGueltig();
		estimation.setEnRevision(gueltig == null || !gueltig);
		return estimation;
	}

	@Nullable
	public static EstimationRF get(@Nullable AmtlicheBewertung amtlicheBewertung) {
		if (amtlicheBewertung == null) {
			return null;
		}
		return newEstimationRF(amtlicheBewertung);
	}

	/**
	 * [SIFISC-23478] Détermine l'année de référence.
	 *
	 * @param reference le code de référence de l'estimation fiscale
	 * @return l'année déduite; ou <b>null</b> si la référence est nulle ou inutilisable.
	 */
	public static Integer determineAnneeReference(@Nullable String reference) {
		// note : on ne tient pas compte de la date d'inscription volontairement
		return getAnneeReference(reference, null).orElse(null);
	}

	/**
	 * Détermine la date de début métier. Par définition, il s'agit de la date de référence de l'estimation fiscal rapportée au premier janvier.
	 *
	 * @param reference       le code de référence de l'estimation fiscale
	 * @param dateInscription la date d'inscription de l'estimation fiscale (utilisée comme deuxième choix si la référence n'est pas utilisable)
	 * @return la date de début métier trouvée; ou <b>null</b> si aucune donneé n'est utilisable.
	 */
	@Nullable
	public static RegDate determineDateDebutMetier(@Nullable String reference, @Nullable RegDate dateInscription) {
		return getAnneeReference(reference, dateInscription)
				.map(year -> RegDate.get(year, 1, 1))   // SIFISC-22995
				.orElse(null);
	}

	/**
	 * Détermine les dates de fin métier d'une collection d'estimations fiscales. Les estimations fiscales spécifiées doivent appartenir au même immeuble.
	 * En cas de chevauchement, les estimations fiscales les plus anciennes sont adaptées ou annulées pour que l'ensemble de la liste reste valide.
	 *
	 * @param estimations        une collection d'estimations fiscales à mettre-à-jour
	 * @param fermetureListener  un listener qui reçoit les estimations fiscales fermées
	 * @param annulationListener un listener qui reçoit les estimations fiscales annulées
	 */
	public static void determineDatesFinMetier(@NotNull Collection<EstimationRF> estimations, @Nullable Consumer<EstimationRF> fermetureListener, @Nullable Consumer<EstimationRF> annulationListener) {

		final List<EstimationRF> list = new ArrayList<>(estimations);
		list.sort(new DateRangeComparator<>(DateRangeComparator.CompareOrder.ASCENDING));

		// Note : on n'utilise pas la méthode DateRangeHelper.override() ici parce que :
		//  - les estimations fiscales sont reçues selon un certain ordre (= l'ordre des dates techniques) qui peut être différent de l'ordre métier (= l'ordre des dates métier) ;
		//  - il faut respecter l'ordre donné par les dates techniques dans l'application des surcharges mais utiliser les dates métiers ;
		//  - on travaille sur des entités Hibernate qui sont liées à la session courante.

		// principe de base : la date de début métier de l'estimation fiscale suivante est utilisée pour déduire la date de fin métier précédante.
		// [SIFISC-24311] chaque nouvelle estimation fiscale peut surcharger une ou plusieurs estimations fiscales précédentes (la date de début métier est
		// déduite du code de l'estimation fiscale qui est une valeur en saisie libre), il faut donc le cas échéant adapter ou annuler les estimations fiscales concernées.
		final List<EstimationRF> actives = new ArrayList<>(estimations.size());
		for (final EstimationRF newEstimation : list) {
			if (newEstimation.isAnnule()) {
				continue;
			}
			final RegDate newDebut = newEstimation.getDateDebutMetier();

			for (int j = actives.size() - 1; j >= 0; j--) {
				final EstimationRF currentActive = actives.get(j);
				final RegDate activeDebut = currentActive.getDateDebutMetier();
				final RegDate activeFin = currentActive.getDateFinMetier();

				if (newDebut == null || (activeDebut != null && activeDebut.isAfterOrEqual(newDebut))) {
					// la nouvelle estimation surcharge *complétement* l'estimation active courante : on l'annule et la retire de la liste des actives
					currentActive.setAnnule(true);
					actives.remove(j);
					// on signale que l'estimation a été annulée
					if (annulationListener != null) {
						annulationListener.accept(currentActive);
					}
				}
				else if (activeFin == null || activeFin.isAfterOrEqual(newDebut)) {
					// la nouvelle estimation surcharge *partiellement* l'estimation active courante : on met-à-jour la date de fin
					currentActive.setDateFinMetier(newDebut.getOneDayBefore());
					// on signale que l'estimation a été fermée
					if (fermetureListener != null) {
						fermetureListener.accept(currentActive);
					}
				}
				else {
					// rien à faire
				}
			}
			actives.add(newEstimation);
		}

		// si la dernière estimation fiscale est fermée (parce que - par exemple - dans le nouvel import,
		// l'immeuble ne possède plus d'estimation fiscale), on met aussi une date de fin métier
		if (!actives.isEmpty()) {
			final EstimationRF last = actives.get(actives.size() - 1);
			if (last != null && last.getDateFin() != null) {
				last.setDateFinMetier(last.getDateFin());
				// on signale que l'estimation a été fermée
				if (fermetureListener != null) {
					fermetureListener.accept(last);
				}
			}
		}
	}

	@NotNull
	private static List<Pair<Pattern, ToIntFunction<Matcher>>> buildPatternsAnneeEstimationFiscale() {
		final List<Pair<Pattern, ToIntFunction<Matcher>>> list = new ArrayList<>();
		list.add(Pair.of(Pattern.compile("(?:EF|RF|RG)\\s*(\\d{4})", Pattern.CASE_INSENSITIVE), EstimationRFHelper::groupOneToInt));
		list.add(Pair.of(Pattern.compile("(?:EF|RF|RG)\\s*(\\d{2})", Pattern.CASE_INSENSITIVE), EstimationRFHelper::groupOneToIntPlusSiecle));
		list.add(Pair.of(Pattern.compile("(\\d{4})"), EstimationRFHelper::groupOneToInt));
		list.add(Pair.of(Pattern.compile("\\d{1,2}\\.\\d{1,2}\\.(\\d{4})"), EstimationRFHelper::groupOneToInt));
		list.add(Pair.of(Pattern.compile("(\\d{4})\\s*(?:RF|RG|RP|rév\\.|T|T\\.|enrévision|§|\\.)", Pattern.CASE_INSENSITIVE), EstimationRFHelper::groupOneToInt));
		return Collections.unmodifiableList(list);
	}

	private static int groupOneToInt(Matcher matcher) {
		return Integer.valueOf(matcher.group(1));
	}

	private static int groupOneToIntPlusSiecle(Matcher matcher) {
		final int anneeSansSiecle = groupOneToInt(matcher);
		return anneeSansSiecle + (RegDate.get().year() / 100) * 100 - (anneeSansSiecle > 50 ? 100 : 0);
	}

	/**
	 * Détermine et retourne l'année de référence d'une estimation fiscale.
	 *
	 * @param reference       le code de référence de l'estimation fiscale
	 * @param dateInscription la date d'inscription de l'estimation fiscale (utilisée comme deuxième choix si la référence n'est pas utilisable)
	 * @return l'année de la date de référence trouvée; ou <b>null</b> si aucune donneé n'est utilisable.
	 */
	@NotNull
	public static Optional<Integer> getAnneeReference(@Nullable String reference, @Nullable RegDate dateInscription) {
		// on cherche d'abord dans la référence, et si on ne trouve rien d'interprétable, on se rabat sur la date d'estimation
		// (et on ajoute 1 à l'année)
		final String ref = StringUtils.trimToNull(reference);
		if (ref != null) {
			final Optional<Integer> fromReference = ANNEE_ESTIMATION_FISCALE_PATTERNS.stream()
					.map(pair -> Pair.of(pair.getKey().matcher(ref), pair.getValue()))
					.filter(pair -> pair.getKey().matches())
					.map(pair -> pair.getValue().applyAsInt(pair.getKey()))
					.findFirst();
			if (fromReference.isPresent()) {
				return fromReference;
			}
		}

		// si la date d'estimation fiscale est remplie, allons-y, sinon, tant pis
		return Optional.ofNullable(dateInscription).map(RegDate::year);
	}
}
