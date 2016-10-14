package ch.vd.uniregctb.declaration.snc;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.uniregctb.common.AbstractJobResults;
import ch.vd.uniregctb.declaration.QuestionnaireSNC;

public class EnvoiQuestionnairesSNCEnMasseResults extends AbstractJobResults<Long, EnvoiQuestionnairesSNCEnMasseResults> {

	private final int periodeFiscale;
	private final RegDate dateTraitement;
	private final Integer nbMaxEnvois;

	public static abstract class Info {
		public final long noCtb;
		public Info(long noCtb) {
			this.noCtb = noCtb;
		}
	}

	public static class QuestionnaireEnvoye extends Info {
		public QuestionnaireEnvoye(long noCtb) {
			super(noCtb);
		}
	}

	public enum CauseIgnorance {
		TACHE_ENVOI_DISPARUE,
		QUESTIONNAIRE_DEJA_EXISTANT
	}

	public static class ContribuableIgnore extends Info {
		public final CauseIgnorance cause;
		public ContribuableIgnore(long noCtb, CauseIgnorance cause) {
			super(noCtb);
			this.cause = cause;
		}
	}

	public enum CauseErreur {
		TIERS_PAS_ENTREPRISE,
		EXCEPTION
	}

	public static class TraitementEnErreur extends Info {
		public final CauseErreur cause;
		public final String details;
		public TraitementEnErreur(long noCtb, CauseErreur cause, String details) {
			super(noCtb);
			this.cause = cause;
			this.details = details;
		}
	}

	private final List<QuestionnaireEnvoye> envoyes = new LinkedList<>();
	private final List<ContribuableIgnore> ignores = new LinkedList<>();
	private final List<TraitementEnErreur> erreurs = new LinkedList<>();
	private boolean interrupted = false;

	public EnvoiQuestionnairesSNCEnMasseResults(int periodeFiscale, RegDate dateTraitement, @Nullable Integer nbMaxEnvois) {
		this.periodeFiscale = periodeFiscale;
		this.dateTraitement = dateTraitement;
		this.nbMaxEnvois = nbMaxEnvois;
	}

	public void addQuestionnaireEnvoye(QuestionnaireSNC questionnaire) {
		envoyes.add(new QuestionnaireEnvoye(questionnaire.getTiers().getNumero()));
	}

	public void addIgnoreTacheDisparue(Long element) {
		ignores.add(new ContribuableIgnore(element, CauseIgnorance.TACHE_ENVOI_DISPARUE));
	}

	public void addIgnoreQuestionnaireExistant(Long element) {
		ignores.add(new ContribuableIgnore(element, CauseIgnorance.QUESTIONNAIRE_DEJA_EXISTANT));
	}

	public void addErrorWrongPartyType(Long element) {
		erreurs.add(new TraitementEnErreur(element, CauseErreur.TIERS_PAS_ENTREPRISE, null));
	}

	@Override
	public void addErrorException(Long element, Exception e) {
		erreurs.add(new TraitementEnErreur(element, CauseErreur.EXCEPTION, ExceptionUtils.extractCallStack(e)));
	}

	@Override
	public void addAll(EnvoiQuestionnairesSNCEnMasseResults right) {
		this.envoyes.addAll(right.envoyes);
		this.ignores.addAll(right.ignores);
		this.erreurs.addAll(right.erreurs);
	}

	@Override
	public void end() {
		final Comparator<Info> comparateur = (i1, i2) -> Long.compare(i1.noCtb, i2.noCtb);
		Collections.sort(envoyes, comparateur);
		Collections.sort(ignores, comparateur);
		Collections.sort(erreurs, comparateur);
		super.end();
	}

	public int getPeriodeFiscale() {
		return periodeFiscale;
	}

	public RegDate getDateTraitement() {
		return dateTraitement;
	}

	@Nullable
	public Integer getNbMaxEnvois() {
		return nbMaxEnvois;
	}

	public int getNombreTiersInspectes() {
		return getNombreEnvoyes() + getNombreErreurs() + getNombreIgnores();
	}

	public List<QuestionnaireEnvoye> getEnvoyes() {
		return envoyes;
	}

	public int getNombreEnvoyes() {
		return envoyes.size();
	}

	public List<ContribuableIgnore> getIgnores() {
		return ignores;
	}

	public int getNombreIgnores() {
		return ignores.size();
	}

	public List<TraitementEnErreur> getErreurs() {
		return erreurs;
	}

	public int getNombreErreurs() {
		return erreurs.size();
	}

	public boolean wasInterrupted() {
		return interrupted;
	}

	public void setInterrupted(boolean interrupted) {
		this.interrupted = interrupted;
	}
}
