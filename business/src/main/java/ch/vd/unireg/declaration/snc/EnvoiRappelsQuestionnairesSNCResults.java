package ch.vd.unireg.declaration.snc;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.unireg.common.AbstractJobResults;
import ch.vd.unireg.declaration.EtatDeclaration;
import ch.vd.unireg.declaration.IdentifiantDeclaration;
import ch.vd.unireg.declaration.QuestionnaireSNC;

public class EnvoiRappelsQuestionnairesSNCResults extends AbstractJobResults<IdentifiantDeclaration, EnvoiRappelsQuestionnairesSNCResults> {

	private final RegDate dateTraitement;
	private final Integer nbMaxEnvois;
	private final Integer periodeFiscale;

	private boolean interrupted = false;

	public EnvoiRappelsQuestionnairesSNCResults(RegDate dateTraitement, @Nullable Integer periodeFiscale, @Nullable Integer nbMaxEnvois) {
		this.dateTraitement = dateTraitement;
		this.periodeFiscale = periodeFiscale;
		this.nbMaxEnvois = nbMaxEnvois;
	}

	/**
	 * Classe de base des informations reportées
	 */
	public static abstract class Info {
		public final long noCtb;
		public Info(long noCtb) {
			this.noCtb = noCtb;
		}
	}

	/**
	 * Classe de rapport d'un rappel émis
	 */
	public static class RappelEmis extends Info {
		public final int pf;
		public RappelEmis(long noCtb, int pf) {
			super(noCtb);
			this.pf = pf;
		}
	}

	public enum CauseIgnorance {
		ETAT_NON_EMIS,
		QUESTIONNAIRE_DEVRAIT_ETRE_ANNULE,
		DELAI_ADMINISTRATIF_NON_ECHU
	}

	/**
	 * Classe de rapport d'un rappel non-émis pour une raison métier légitime
	 */
	public static class RappelIgnore extends Info {
		public final int pf;
		public final CauseIgnorance cause;
		public final String detail;
		public RappelIgnore(long noCtb, int pf, CauseIgnorance cause, @Nullable String detail) {
			super(noCtb);
			this.pf = pf;
			this.cause = cause;
			this.detail = detail;
		}
	}

	public enum CauseErreur {
		EXCEPTION
	}

	public static class ErreurTraitement extends Info {
		public final Integer pf;
		public final CauseErreur cause;
		public final String detail;
		public ErreurTraitement(long noCtb, @Nullable Integer pf, CauseErreur cause, String detail) {
			super(noCtb);
			this.pf = pf;
			this.cause = cause;
			this.detail = detail;
		}
	}

	private final List<RappelEmis> emis = new LinkedList<>();
	private final List<RappelIgnore> ignores = new LinkedList<>();
	private final List<ErreurTraitement> erreurs = new LinkedList<>();

	public int getNombreRappelsEmis() {
		return emis.size();
	}

	@Override
	public void addAll(EnvoiRappelsQuestionnairesSNCResults right) {
		emis.addAll(right.emis);
		ignores.addAll(right.ignores);
		erreurs.addAll(right.erreurs);
	}

	@Override
	public void end() {
		final Comparator<Info> comparator = Comparator.comparingLong(i -> i.noCtb);
		final Comparator<RappelEmis> comparatorEmis = (o1, o2) -> {
			int comparison = Long.compare(o1.noCtb, o2.noCtb);
			if (comparison == 0) {
				comparison = Integer.compare(o1.pf, o2.pf);
			}
			return comparison;
		};
		emis.sort(comparatorEmis);
		ignores.sort(comparator);
		erreurs.sort(comparator);
		super.end();
	}

	@Override
	public void addErrorException(IdentifiantDeclaration element, Exception e) {
		erreurs.add(new ErreurTraitement(element.getNumeroTiers(), null, CauseErreur.EXCEPTION, ExceptionUtils.extractCallStack(e)));
	}

	public void addQuestionnaireNonEmis(QuestionnaireSNC questionnaire, EtatDeclaration dernierEtat) {
		ignores.add(new RappelIgnore(questionnaire.getTiers().getNumero(), questionnaire.getPeriode().getAnnee(), CauseIgnorance.ETAT_NON_EMIS, String.format("Etat trouvé : %s", dernierEtat != null ? dernierEtat.getEtat().name() : "vide")));
	}

	public void addRappelIgnoreCarQuestionnaireDevraitEtreAnnule(QuestionnaireSNC questionnaire) {
		ignores.add(new RappelIgnore(questionnaire.getTiers().getNumero(), questionnaire.getPeriode().getAnnee(), CauseIgnorance.QUESTIONNAIRE_DEVRAIT_ETRE_ANNULE, null));
	}

	public void addRappelIgnoreDelaiAdministratifNonEchu(QuestionnaireSNC questionnaire) {
		ignores.add(new RappelIgnore(questionnaire.getTiers().getNumero(), questionnaire.getPeriode().getAnnee(), CauseIgnorance.DELAI_ADMINISTRATIF_NON_ECHU, null));
	}

	public void addRappelEmis(QuestionnaireSNC questionnaire) {
		emis.add(new RappelEmis(questionnaire.getTiers().getNumero(), questionnaire.getPeriode().getAnnee()));
	}

	public boolean wasInterrupted() {
		return interrupted;
	}

	public void setInterrupted(boolean interrupted) {
		this.interrupted = interrupted;
	}

	public RegDate getDateTraitement() {
		return dateTraitement;
	}

	@Nullable
	public Integer getPeriodeFiscale() {
		return periodeFiscale;
	}

	@Nullable
	public Integer getNbMaxEnvois() {
		return nbMaxEnvois;
	}

	public List<RappelEmis> getRappelsEmis() {
		return emis;
	}

	public List<RappelIgnore> getIgnores() {
		return ignores;
	}

	public List<ErreurTraitement> getErreurs() {
		return erreurs;
	}
}
