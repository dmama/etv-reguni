package ch.vd.uniregctb.registrefoncier;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.JobResults;

public class ImportImmeublesResults extends JobResults<String, ImportImmeublesResults> {

	private static final Pattern NO_IMMEUBLE_PATTERN = Pattern.compile(".*?\"([0-9]*/[0-9]*)\"");

	public enum ErreurType {
		EXCEPTION(EXCEPTION_DESCRIPTION),
		CTB_INCONNU("Le contribuable n'existe pas"),
		BAD_NUMERO("Le numéro est vide"),
		BAD_CTB_TYPE("Le type de contribuable est incorrect"),
		BAD_GENRE_PERSONNE("Le genre de personne est malformé"),
		BAD_DATE_NO_CTB("Le numéro de contribuable est malformé"),
		BAD_URL("L'url est malformée"),
		BAD_DATE_MODIF("La date de modification est malformée"),
		BAD_DATE_DEBUT("La date de début est malformée"),
		BAD_DATE_FIN("La date de fin est malformée"),
		BAD_EF("L'estimation fiscale est malformée"),
		BAD_GENRE_PROP("Le genre de propriété est malformé"),
		BAD_PART_PROP("La part de propriété est malformée"),
		CTB_MENAGE_COMMUN("Le contribuable est un ménage commun"),
		BAD_NOM_COMMUNE("Le nom de la commune est vide"),
		BAD_NATURE("La nature de l'immeuble est vide");

		private final String description;

		private ErreurType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public enum IgnoreType {
		CTB_NULL("Le contribuable n'est pas renseigné"),
		CTB_ENTREPRISE("Le contribuable est une entreprise");

		private final String description;

		private IgnoreType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public enum AVerifierType {
		TYPE_INCOHERENT_TRAITE("Incohérence des types de contribuable (traitement effectué)."),
		TYPE_INCOHERENT_NON_TRAITE("Incohérence des types de contribuable (traitement non-effectué).");

		private final String description;

		private AVerifierType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public static class Import {
		public final String noImmeuble;
		public final Long noContribuable;

		public Import(String noImmeuble, Long noContribuable) {
			this.noImmeuble = noImmeuble;
			this.noContribuable = noContribuable;
		}

		public String getNoImmeuble() {
			return noImmeuble;
		}

		public Long getNoContribuable() {
			return noContribuable;
		}
	}

	public static class Erreur {
		public final String noImmeuble;
		public final ErreurType raison;
		public final String details;

		public Erreur(String noImmeuble, ErreurType raison) {
			this.noImmeuble = noImmeuble;
			this.raison = raison;
			this.details = null;
		}

		public Erreur(String noImmeuble, ErreurType raison, String details) {
			this.noImmeuble = noImmeuble;
			this.raison = raison;
			this.details = details;
		}

		public String getNoImmeuble() {
			return noImmeuble;
		}

		public String getDescriptionRaison() {
			return raison.description;
		}

		public String getDetails() {
			return details;
		}
	}

	public static class Ignore {
		public final String noImmeuble;
		public final IgnoreType raison;

		public Ignore(String noImmeuble, IgnoreType raison) {
			this.noImmeuble = noImmeuble;
			this.raison = raison;
		}

		public String getNoImmeuble() {
			return noImmeuble;
		}

		public String getDescriptionRaison() {
			return raison.description;
		}
	}

	public static class AVerifier {
		public final String noImmeuble;
		public final AVerifierType raison;
		public final String details;

		public AVerifier(String noImmeuble, AVerifierType raison, String details) {
			this.noImmeuble = noImmeuble;
			this.raison = raison;
			this.details = details;
		}

		public String getNoImmeuble() {
			return noImmeuble;
		}

		public String getDescriptionRaison() {
			return raison.description;
		}

		public String getDetails() {
			return details;
		}
	}

	public final RegDate dateTraitement = RegDate.get();
	public int nbLignes;
	public final List<Import> traites = new ArrayList<Import>();
	public final List<Ignore> ignores = new ArrayList<Ignore>();
	public final List<AVerifier> averifier = new ArrayList<AVerifier>();
	public final List<Erreur> erreurs = new ArrayList<Erreur>();
	public boolean interrompu;

	public void incNbLignes() {
		++nbLignes;
	}

	public void addTraite(String numero, Long numeroCtb) {
		traites.add(new Import(numero, numeroCtb));
	}

	@Override
	public void addErrorException(String line, Exception e) {
		final String noImmeuble = extractNoImmeubleSafe(line);
		erreurs.add(new Erreur(noImmeuble, ErreurType.EXCEPTION, e.getMessage()));
	}

	private String extractNoImmeubleSafe(String line) {
		String noImmeuble = "<n/a>";
		if (line != null) {
			final Matcher m = NO_IMMEUBLE_PATTERN.matcher(line);
			if (m.find()) {
				if (m.groupCount() > 0) {
					noImmeuble = m.group(1);
				}
			}
		}
		return noImmeuble;
	}

	public void addError(String noImmeuble, ErreurType type, String details) {
		erreurs.add(new Erreur(noImmeuble, type, details));
	}

	public void addIgnore(String noImmeuble, IgnoreType raison) {
		ignores.add(new Ignore(noImmeuble, raison));
	}

	public void addAVerifier(String noImmeuble, AVerifierType raison, String details) {
		averifier.add(new AVerifier(noImmeuble, raison, details));
	}

	@Override
	public void addAll(ImportImmeublesResults right) {
		nbLignes += right.nbLignes;
		traites.addAll(right.traites);
		ignores.addAll(right.ignores);
		averifier.addAll(right.averifier);
		erreurs.addAll(right.erreurs);
	}

	public boolean isInterrompu() {
		return interrompu;
	}

	public void setInterrompu(boolean interrompu) {
		this.interrompu = interrompu;
	}

	public int getNbImmeubles() {
		return nbLignes;
	}
}
