package ch.vd.uniregctb.registrefoncier.dataimport.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
		return dataEquals(estimation, get(amtlicheBewertung));
	}

	public static boolean dataEquals(@Nullable EstimationRF left, @Nullable EstimationRF right) {
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
						left.isEnRevision() == right.isEnRevision();
			}
	}

	@NotNull
	public static EstimationRF newEstimationRF(@NotNull AmtlicheBewertung amtlicheBewertung) {
		final EstimationRF estimation = new EstimationRF();
		estimation.setMontant(amtlicheBewertung.getAmtlicherWert());
		estimation.setReference(amtlicheBewertung.getProtokollNr());
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
	 * Détermine les dates de fin métier d'une collection d'estimations fiscales. Les estimations fiscales spécifiées
	 * doivent appartenir au même immeuble et leur périodes de validité ne pas se chevaucher.
	 *
	 * @param estimations une collection d'estimations fiscales à mettre-à-jour
	 */
	public static void determineDatesFinMetier(@NotNull Collection<EstimationRF> estimations) {

		final List<EstimationRF> list = new ArrayList<>(estimations);
		list.sort(new DateRangeComparator<>(DateRangeComparator.CompareOrder.ASCENDING));

		// algorithme naïf : la date de début métier de l'estimation fiscale suivante est utilisée pour déduire la date de fin métier précédante.
		EstimationRF previous = null;
		for (EstimationRF current : list) {
			final RegDate dateDebutMetier = current.getDateDebutMetier();
			if (previous != null && dateDebutMetier != null) {
				previous.setDateFinMetier(dateDebutMetier.getOneDayBefore());
			}
			previous = current;
		}

		// si la dernière estimation fiscale est fermée, on met aussi une date de fin métier
		if (previous != null && previous.getDateFin() != null) {
			previous.setDateFinMetier(previous.getDateFin());
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
