package ch.vd.uniregctb.declaration.ordinaire.pm;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.AbstractJobResults;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.metier.assujettissement.CategorieEnvoiDIPM;

public class EnvoiDIsPMResults extends AbstractJobResults<Long, EnvoiDIsPMResults> {

	private final List<DiEnvoyee> envoyees = new LinkedList<>();
	private final List<Erreur> erreurs = new LinkedList<>();
	private final List<TacheIgnoree> ignorees = new LinkedList<>();
	private int nbContribuablesVus = 0;

	private final RegDate dateTraitement;
	private final int nbThreads;
	private final CategorieEnvoiDIPM categorieEnvoi;
	private final int periodeFiscale;
	private final RegDate dateLimiteBouclements;
	private final Integer nbMaxEnvois;

	public boolean interrompu;

	public EnvoiDIsPMResults(RegDate dateTraitement, int nbThreads, CategorieEnvoiDIPM categorieEnvoi, int periodeFiscale, RegDate dateLimiteBouclements, @Nullable Integer nbMaxEnvois) {
		this.dateTraitement = dateTraitement;
		this.nbThreads = nbThreads;
		this.categorieEnvoi = categorieEnvoi;
		this.periodeFiscale = periodeFiscale;
		this.dateLimiteBouclements = dateLimiteBouclements;
		this.nbMaxEnvois = nbMaxEnvois;
	}

	public enum ErrorType {
		EXCEPTION(JobResults.EXCEPTION_DESCRIPTION),
		COLLISION_DI("Une déclaration existe déjà mais ne correspond pas à celle calculée.");

		private final String description;

		ErrorType(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}
	}

	public enum IgnoreType {
		DECLARATION_EXISTANTE("Une déclaration existe déjà pour la période."),
		BOUCLEMENT_TROP_RECENT("La date de bouclement est trop récente pour être déjà prise en compte."),
		BOUCLEMENT_FUTUR("La date de bouclement est dans le futur de la date de traitement.");

		private final String description;

		IgnoreType(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}
	}

	public static abstract class Info {
		final long noCtb;

		public Info(long noCtb) {
			this.noCtb = noCtb;
		}

		public long getNoCtb() {
			return noCtb;
		}
	}

	public static abstract class DateRangeInfo extends Info implements DateRange {
		final RegDate dateDebut;
		final RegDate dateFin;

		public DateRangeInfo(long noCtb, RegDate dateDebut, RegDate dateFin) {
			super(noCtb);
			this.dateDebut = dateDebut;
			this.dateFin = dateFin;
		}

		@Override
		public RegDate getDateDebut() {
			return dateDebut;
		}

		@Override
		public RegDate getDateFin() {
			return dateFin;
		}
	}

	public static class DiEnvoyee extends DateRangeInfo implements Comparable<DiEnvoyee> {

		public DiEnvoyee(long noCtb, RegDate dateDebut, RegDate dateFin) {
			super(noCtb, dateDebut, dateFin);
		}

		@Override
		public int compareTo(@NotNull DiEnvoyee o) {
			int comparison = Long.compare(noCtb, o.noCtb);
			if (comparison == 0) {
				comparison = DateRangeComparator.compareRanges(this, o);
			}
			return comparison;
		}
	}

	public static class Erreur extends Info implements Comparable<Erreur> {
		final ErrorType type;
		final String detail;

		public Erreur(long noCtb, ErrorType type, @Nullable String detail) {
			super(noCtb);
			this.type = type;
			this.detail = StringUtils.trimToNull(detail);
		}

		public ErrorType getType() {
			return type;
		}

		@Nullable
		public String getDetail() {
			return detail;
		}

		@Override
		public int compareTo(@NotNull Erreur o) {
			return Long.compare(noCtb, o.noCtb);
		}
	}

	public static class TacheIgnoree extends DateRangeInfo implements Comparable<TacheIgnoree> {
		final IgnoreType type;

		public TacheIgnoree(long noCtb, RegDate dateDebut, RegDate dateFin, IgnoreType type) {
			super(noCtb, dateDebut, dateFin);
			this.type = type;
		}

		@Override
		public int compareTo(@NotNull TacheIgnoree o) {
			int comparison = Long.compare(noCtb, o.noCtb);
			if (comparison == 0) {
				comparison = DateRangeComparator.compareRanges(this, o);
			}
			return comparison;
		}

		public IgnoreType getType() {
			return type;
		}
	}

	@Override
	public void addErrorException(Long noCtb, Exception e) {
		erreurs.add(new Erreur(noCtb, ErrorType.EXCEPTION, e.getMessage()));
	}

	public void addCollisionAvecDi(long noCtb, String message) {
		erreurs.add(new Erreur(noCtb, ErrorType.COLLISION_DI, message));
	}

	public void addDiEnvoyee(long noCtb, RegDate dateDebut, RegDate dateFin) {
		envoyees.add(new DiEnvoyee(noCtb, dateDebut, dateFin));
	}

	public void addTacheIgnoreeDeclarationExistante(long noCtb, RegDate dateDebut, RegDate dateFin) {
		ignorees.add(new TacheIgnoree(noCtb, dateDebut, dateFin, IgnoreType.DECLARATION_EXISTANTE));
	}

	public void addTacheIgnoreeBouclementTropRecent(long noCtb, RegDate dateDebut, RegDate dateFin) {
		ignorees.add(new TacheIgnoree(noCtb, dateDebut, dateFin, IgnoreType.BOUCLEMENT_TROP_RECENT));
	}

	public void addTacheIgnoreeBouclementFutur(long noCtb, RegDate dateDebut, RegDate dateFin) {
		ignorees.add(new TacheIgnoree(noCtb, dateDebut, dateFin, IgnoreType.BOUCLEMENT_FUTUR));
	}

	public void addLotContribuablesVus(int tailleLot) {
		nbContribuablesVus += tailleLot;
	}

	@Override
	public void addAll(EnvoiDIsPMResults right) {
		envoyees.addAll(right.envoyees);
		erreurs.addAll(right.erreurs);
		ignorees.addAll(right.ignorees);
		nbContribuablesVus += right.nbContribuablesVus;
	}

	@Override
	public void end() {
		Collections.sort(envoyees);
		Collections.sort(erreurs);
		Collections.sort(ignorees);
		super.end();
	}

	public List<DiEnvoyee> getEnvoyees() {
		return envoyees;
	}

	public List<Erreur> getErreurs() {
		return erreurs;
	}

	public List<TacheIgnoree> getIgnorees() {
		return ignorees;
	}

	public RegDate getDateTraitement() {
		return dateTraitement;
	}

	public int getNbThreads() {
		return nbThreads;
	}

	public CategorieEnvoiDIPM getCategorieEnvoi() {
		return categorieEnvoi;
	}

	public int getPeriodeFiscale() {
		return periodeFiscale;
	}

	public RegDate getDateLimiteBouclements() {
		return dateLimiteBouclements;
	}

	public Integer getNbMaxEnvois() {
		return nbMaxEnvois;
	}

	public int getNbContribuablesVus() {
		return nbContribuablesVus;
	}
}
