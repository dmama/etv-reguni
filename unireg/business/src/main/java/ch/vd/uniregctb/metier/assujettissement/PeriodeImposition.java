package ch.vd.uniregctb.metier.assujettissement;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;

/**
 * Classe de base abstraite représentant une période d'imposition.
 * <p/>
 * <b>Note:</b> la différence entre une période d'imposition et un assujettissement (voir {@link Assujettissement}) est subtile, mais bien
 * réelle.
 * <ul>
 * <li>Un <b>assujettissement</b> représente le type de contribuable tel que calculé en appliquant les règles fiscales au plus près de leurs
 * définition, sans tenir compte du contexte d'utilisation.</li>
 * <li>Une <b>période d'imposition</b> est une notion qui représente la période et le type de contribuable, mais limité à une période
 * fiscale et en tenant compte d'un context orienté "déclaration d'impôt".</li>
 * </ul>
 * <p/>
 * Dans la plupart des cas, les deux notions sont confondues; mais pas dans tous.
 * <p/>
 * <b>Exemple où les deux notions sont différentes:</b> le cas du contribuable vaudois qui part hors-Suisse en gardant un immeuble dans le
 * canton. Dans ce cas, on a:
 * <ul>
 * <li><b>assujettissements:</b> un assujettissement VaudoisOrdinaire pour la période du 1er janvier à la date du départ; et un second
 * assujettissement HorsSuisse pour le reste de l'année.</li>
 * <li><b>période d'imposition:</b> une seule période couvrant toute l'année avec le type de document DECLARATION_IMPOT_COMPLETE car les
 * deux assujettissements VaudoisOrdinaire et HorsSuisse génèrent le même type de DI au final.</li>
 * </ul>
 */
public abstract class PeriodeImposition implements CollatableDateRange<PeriodeImposition> {

	/**
	 * Description des différents cas intéressants de fermeture de la période d'imposition
	 */
	public enum CauseFermeture {
		/**
		 * si la période d'imposition se termine à cause d'un décès (ou d'un veuvage).
		 */
		VEUVAGE_DECES,

		/**
		 * si la période d'imposition (HS) se termine pour cause de fin d'activité indépendante ou de vente d'immeuble
		 */
		FIN_ASSUJETTISSEMENT_HS,

		/**
		 * si la période d'imposition se termine pour une autre raison (c'est bien-sûr le cas le plus fréquent)
		 */
		AUTRE
	}

	/**
	 * Date de début de la période de déclaration, s'il y a une déclaration
	 */
	private final RegDate debut;

	/**
	 * Date de fin de la période de déclaration, s'il y a une déclaration
	 */
	private final RegDate fin;

	/**
	 * Contribuable concerné par l'imposition
	 */
	private final Contribuable contribuable;

	/**
	 * <code>true</code> si la déclaration n'est émise que sur demande du contribuable (cas des forfaitaires hors-Suisse, par exemple...)
	 */
	private final boolean declarationOptionnelle;

	/**
	 * <code>true</code> si la déclaration correspondante à la période d'imposition est remplacée par une note à l'administration fiscale d'un autre canton (= la DI n'est émise).
	 */
	private final boolean declarationRemplaceeParNote;

	/**
	 * Cause pour laquelle la période d'imposition se termine
	 */
	private final CauseFermeture causeFermeture;

	/**
	 * Code segment fourni par la taxation pour la déclaration suivante
	 */
	private final Integer codeSegment;



	public PeriodeImposition(RegDate debut, RegDate fin, Contribuable contribuable, boolean declarationOptionnelle, boolean declarationRemplaceeParNote,
	                         CauseFermeture causeFermeture, Integer codeSegment) {
		if (debut == null || fin == null) {
			throw new IllegalArgumentException("Une période d'imposition est toujours bornée : " + DateRangeHelper.toDisplayString(debut, fin));
		}
		if (contribuable == null) {
			throw new IllegalArgumentException("Une période d'imposition est toujours liée à un contribuable !");
		}

		this.debut = debut;
		this.fin = fin;
		this.contribuable = contribuable;
		this.declarationOptionnelle = declarationOptionnelle;
		this.declarationRemplaceeParNote = declarationRemplaceeParNote;
		this.causeFermeture = causeFermeture;
		this.codeSegment = codeSegment;
	}

	/**
	 * @return la période fiscale (sous la forme d'un numéro d'année) à laquelle cette période d'imposition est liée
	 */
	public final int getPeriodeFiscale() {
		// c'est l'année de la fin de la période
		return fin.year();
	}

	@Override
	public final boolean isCollatable(PeriodeImposition next) {
		// on accepte les ranges qui se touchent *et* ceux qui se chevauchent, ceci parce que les périodes d'impositions peuvent être plus
		// larges que les assujettissement sous-jacents (cas des HorsCanton et HorsSuisse) et qu'il s'agit de pouvoir les
		// collater malgré tout (mais jamais sur deux périodes fiscales différentes).
		return fin.getOneDayAfter().isAfterOrEqual(next.getDateDebut()) && getPeriodeFiscale() == next.getPeriodeFiscale() && isCompatibleWith(next);
	}

	/**
	 * @param next la période d'imposition qui vient après (les dates ont déjà été vérifiées)
	 * @return si oui ou non la période courante et compatible avec la suivante (= si, les dates jouant, elles pourraient être fusionnées ensemble)
	 */
	protected abstract boolean isCompatibleWith(PeriodeImposition next);

	@NotNull
	@Override
	public RegDate getDateDebut() {
		return debut;
	}

	@NotNull
	@Override
	public RegDate getDateFin() {
		return fin;
	}

	@NotNull
	public Contribuable getContribuable() {
		return contribuable;
	}

	/**
	 * @return le type de contribuable, (VD, HC, HS...) concerné par cette période d'imposition
	 */
	public abstract TypeContribuable getTypeContribuable();

	/**
	 * @return le type de document de la déclaration concernée par cette période d'imposition
	 */
	public abstract TypeDocument getTypeDocumentDeclaration();

	public boolean isDeclarationOptionnelle() {
		return declarationOptionnelle;
	}

	public boolean isDeclarationRemplaceeParNote() {
		return declarationRemplaceeParNote;
	}

	/**
	 * @return <code>true</code> si la période correspond à celle d'un diplomate suisse sans immeuble
	 */
	public abstract boolean isDiplomateSuisseSansImmeuble();

	/**
	 * @return <code>true</code> si aucune des condition d'optionalité ne sont remplies pour la déclaration
	 */
	public boolean isDeclarationMandatory() {
		return !declarationOptionnelle && !declarationRemplaceeParNote;
	}

	/**
	 * @return <code>true</code> si la période d'imposition se termine avant la fin de la période fiscale
	 */
	public boolean isFermetureAnticipee() {
		return fin.isBefore(getDernierJourPourPeriodeFiscale());
	}

	/**
	 * @return le dernier jour comptant pour la période fiscale (31.12.pf pour les PP, fin de l'exercice commercial courant pour les PM)
	 */
	@NotNull
	protected abstract RegDate getDernierJourPourPeriodeFiscale();

	public CauseFermeture getCauseFermeture() {
		return causeFermeture;
	}

	public Integer getCodeSegment() {
		return codeSegment;
	}

	@Override
	public final String toString() {
		return String.format("%s{%s}", getClass().getSimpleName(), dumpValuesToString(getDisplayValues()));
	}

	/**
	 * @return une map (ordonnée) des données à afficher pour cette période d'imposition
	 */
	public final Map<String, String> getDisplayValues() {
		final Map<String, String> values = new LinkedHashMap<>();
		fillDisplayValues(values);
		return values;
	}

	/**
	 * Remplit la map (ordonnée) avec les éléments à afficher pour cette période d'imposition
	 * @param map map à remplir
	 */
	protected void fillDisplayValues(@NotNull Map<String, String> map) {
		map.put("debut", RegDateHelper.dateToDisplayString(debut));
		map.put("fin", RegDateHelper.dateToDisplayString(fin));
		map.put("periodeFiscale", String.valueOf(getPeriodeFiscale()));
		map.put("declarationOptionnelle", String.valueOf(declarationOptionnelle));
		map.put("declarationRemplaceeParNote", String.valueOf(declarationRemplaceeParNote));
	}

	@NotNull
	private static String dumpValuesToString(@NotNull Map<String, String> map) {
		final StringBuilder b = new StringBuilder();
		for (Map.Entry<String, String> entry : map.entrySet()) {
			if (b.length() > 0) {
				b.append(", ");
			}
			b.append(entry.getKey()).append("=").append(entry.getValue());
		}
		return b.toString();
	}

}
