package ch.vd.unireg.evenement.fiscal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

/**
 * Quelques méthodes utilitaires autour des événements fiscaux envoyés par Unireg
 */
public abstract class EvenementFiscalHelper {

	private static final String UNIREG = "unireg";

	/**
	 * Liste des extracteurs de BusinessUser depuis une chaîne de caractères (= <i>a priori</i> principal au moment de la création de l'événement fiscal)
	 */
	private static final List<Pair<Pattern, Function<Matcher, String>>> BUSINESS_USER_EXTRACTORS = buildBusinessUserExtractors();

	/**
	 * Construction de la liste des extracteurs de BusinessUser depuis une chaîne de caractères. <br/>
	 * <b>A mettre à jour régulièrement</b> en fonction des nouveaux visas <i>inventés</i> ou <i>construits</i> ajoutés dans l'application
	 */
	@NotNull
	private static List<Pair<Pattern, Function<Matcher, String>>> buildBusinessUserExtractors() {
		final List<Pair<Pattern, Function<Matcher, String>>> extractors = new ArrayList<>();
		extractors.add(Pair.of(Pattern.compile("EvtCivil-[0-9]+"), matcher -> UNIREG));
		extractors.add(Pair.of(Pattern.compile("EvtOrganisation-[0-9]+"), matcher -> UNIREG));
		extractors.add(Pair.of(Pattern.compile("JMS-.+"), matcher -> UNIREG));
		extractors.add(Pair.of(Pattern.compile("Récupération-démarrage"), matcher -> UNIREG));
		extractors.add(Pair.of(Pattern.compile("ReqDes-(UT-)?[0-9]+"), matcher -> UNIREG));
		extractors.add(Pair.of(Pattern.compile(".+-reqdes"), matcher -> UNIREG));
		extractors.add(Pair.of(Pattern.compile("ReqDesEvent"), matcher -> UNIREG));
		extractors.add(Pair.of(Pattern.compile(Pattern.quote("[system]")), matcher -> UNIREG));
		extractors.add(Pair.of(Pattern.compile(Pattern.quote("[cron]")), matcher -> UNIREG));
		extractors.add(Pair.of(Pattern.compile(Pattern.quote("[Batch WS]")), matcher -> UNIREG));
		extractors.add(Pair.of(Pattern.compile("(.+)-(recalculParentes|SuperGra|recalculTaches|auto-mvt)"), matcher -> getBusinessUser(matcher.group(1))));
		extractors.add(Pair.of(Pattern.compile(".+Job"), matcher -> UNIREG));
		extractors.add(Pair.of(Pattern.compile("AutoSynchro(Parentes)?"), matcher -> UNIREG));
		return Collections.unmodifiableList(extractors);
	}

	/**
	 * @param principal chaîne de caractères (en général la valeur du principal au moment de la création de l'événement fiscal)
	 * @return la valeur à mettre dans le champ "BusinessUser" de l'événement fiscal envoyé dans l'ESB
	 */
	public static String getBusinessUser(String principal) {
		for (Pair<Pattern, Function<Matcher, String>> extractor : BUSINESS_USER_EXTRACTORS) {
			final Matcher matcher = extractor.getLeft().matcher(principal);
			if (matcher.matches()) {
				return extractor.getRight().apply(matcher);
			}
		}
		return principal;
	}
}
