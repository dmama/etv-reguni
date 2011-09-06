package ch.vd.uniregctb.declaration.ordinaire;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.tiers.NatureTiers;

public class ImportCodesSegmentResults extends JobResults<ContribuableAvecCodeSegment, ImportCodesSegmentResults> {

	private static final String IGNORE_DEJA_BON_SEGMENT = "Le contribuable est déjà assigné au bon segment";

	public static abstract class Info implements Comparable<Info> {
		public final long noTiers;

		public Info(long noTiers) {
			this.noTiers = noTiers;
		}

		@Override
		public int compareTo(Info o) {
			return noTiers < o.noTiers ? -1 : (noTiers > o.noTiers ? 1 : 0);
		}
	}

	public static enum ErreurType {
		EXCEPTION(EXCEPTION_DESCRIPTION),
		CTB_INCONNU("Le contribuable est inconnu"),
		TIERS_PAS_CONTRIBUABLE("Le tiers n'est pas un contribuable"),
		CTB_SANS_DECLARATION("Le contribuable ne possède encore aucune déclaration"),
		MAUVAIS_TYPE_DECLARATION("Les déclarations du contribuable ne sont pas du bon type");

		private final String description;

		private ErreurType(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}
	}

	public static class Erreur extends Info {
		public final ErreurType type;
		public final String details;

		public Erreur(long noTiers, ErreurType type, @Nullable String details) {
			super(noTiers);
			this.type = type;
			this.details = details;
		}
	}

	public static class Traite extends Info {
		public final int codeSegment;

		public Traite(long noTiers, int codeSegment) {
			super(noTiers);
			this.codeSegment = codeSegment;
		}
	}

	public static class Ignore extends Info {
		public final String cause;

		public Ignore(long noTiers, String cause) {
			super(noTiers);
			this.cause = cause;
		}
	}

	private final List<Traite> traites = new LinkedList<Traite>();
	private final List<Erreur> erreurs = new LinkedList<Erreur>();
	private final List<Ignore> ignores = new LinkedList<Ignore>();
	private boolean interrompu = false;

	@Override
	public void addErrorException(ContribuableAvecCodeSegment element, Exception e) {
		erreurs.add(new Erreur(element.getNoContribuable(), ErreurType.EXCEPTION, getExceptionMessage(e)));
	}

	private String getExceptionMessage(Exception e) {
		final String msg;
		if (StringUtils.isNotBlank(e.getMessage())) {
			msg = e.getMessage();
		}
		else {
			msg = e.getClass().getName();
		}
		return String.format("%s - %s", msg, Arrays.toString(e.getStackTrace()));
	}

	@Override
	public void addAll(ImportCodesSegmentResults right) {
		traites.addAll(right.traites);
		erreurs.addAll(right.erreurs);
		ignores.addAll(right.ignores);
	}

	@Override
	public void end() {
		Collections.sort(traites);
		Collections.sort(erreurs);
		Collections.sort(ignores);
		super.end();
	}

	public int getNombreTiersAnalyses() {
		return traites.size() + erreurs.size() + ignores.size();
	}

	public void addErrorCtbInconnu(long noCtb) {
		erreurs.add(new Erreur(noCtb, ErreurType.CTB_INCONNU, null));
	}

	public void addErrorPasUnContribuable(long tiersId, NatureTiers natureTiers) {
		erreurs.add(new Erreur(tiersId, ErreurType.TIERS_PAS_CONTRIBUABLE, natureTiers.name()));
	}

	public void addErrorCtbSansDeclaration(long ctbId) {
		erreurs.add(new Erreur(ctbId, ErreurType.CTB_SANS_DECLARATION, null));
	}

	public void addErrorCtbAvecMauvaisTypeDeDeclaration(long ctbId, String declarationClazz) {
		erreurs.add(new Erreur(ctbId, ErreurType.MAUVAIS_TYPE_DECLARATION, declarationClazz));
	}

	public void addCtbTraite(long noCtb, int codeSegment) {
		traites.add(new Traite(noCtb, codeSegment));
	}

	public void addCtbIgnoreDejaBonCode(long noCtb) {
		ignores.add(new Ignore(noCtb, IGNORE_DEJA_BON_SEGMENT));
	}

	public boolean isInterrompu() {
		return interrompu;
	}

	public void setInterrompu(boolean interrompu) {
		this.interrompu = interrompu;
	}

	public List<Traite> getTraites() {
		return traites;
	}

	public List<Erreur> getErreurs() {
		return erreurs;
	}

	public List<Ignore> getIgnores() {
		return ignores;
	}
}
