package ch.vd.uniregctb.declaration.source;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableMap;
import java.util.SortedSet;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.CategorieImpotSource;

public class DeterminerLRsEchuesResults extends JobResults<DeterminerLRsEchuesResults.InfoDebiteurAvecLrEchue, DeterminerLRsEchuesResults> {

	public static class InfoLrEchue {
		public final long id;
		public final RegDate dateDebut;
		public final RegDate dateFin;
		public final RegDate dateSommation;

		public InfoLrEchue(long id, RegDate dateDebut, RegDate dateFin, RegDate dateSommation) {
			this.id = id;
			this.dateDebut = dateDebut;
			this.dateFin = dateFin;
			this.dateSommation = dateSommation;
		}
	}

	public static class InfoDebiteurAvecLrEchue {
		public final long idDebiteur;
		private final NavigableMap<Integer, List<InfoLrEchue>> lrEchues = new TreeMap<>();

		public InfoDebiteurAvecLrEchue(long idDebiteur) {
			this.idDebiteur = idDebiteur;
		}

		public void addLrEchue(long id, RegDate dateDebut, RegDate dateFin, RegDate dateSommation) {
			final InfoLrEchue infoLrEchue = new InfoLrEchue(id, dateDebut, dateFin, dateSommation);
			final Integer key = dateDebut.year();
			final List<InfoLrEchue> list = getOrCreateMapEntry(key);
			list.add(infoLrEchue);
		}

		@NotNull
		private List<InfoLrEchue> getOrCreateMapEntry(Integer key) {
			return lrEchues.computeIfAbsent(key, k -> new LinkedList<>());
		}

		public List<InfoLrEchue> getLrEchues(int pf) {
			return lrEchues.get(pf);
		}

		public SortedSet<Integer> getPfConcernees() {
			return lrEchues.navigableKeySet();
		}
	}

	public abstract static class ResultDebiteur<T extends ResultDebiteur> implements Comparable<T> {

		public static final String EMPTY_STRING = "";

		public final long idDebiteur;
		public final String nomDebiteur;

		public ResultDebiteur(long idDebiteur, String nomDebiteur) {
			this.idDebiteur = idDebiteur;
			this.nomDebiteur = nomDebiteur;
		}

		@Override
		public int compareTo(@NotNull T o) {
			return Long.compare(idDebiteur, o.idDebiteur);
		}

	}

	public enum Raison {
		ENCORE_LR_A_EMETTRE_SUR_PF("Il reste au moins une LR à émettre sur la période fiscale."),
		EXCEPTION(EXCEPTION_DESCRIPTION);

		private final String description;

		Raison(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}
	}

	public abstract static class ResultDebiteurNonTraite<T extends ResultDebiteurNonTraite> extends ResultDebiteur<T> {
		protected ResultDebiteurNonTraite(long idDebiteur, String nomDebiteur) {
			super(idDebiteur, nomDebiteur);
		}

		public String getCommentaire() {
			return EMPTY_STRING;
		}

		public final String getDescriptionRaison() {
			final Raison raison = getRaison();
			if (raison != null) {
				return raison.getDescription();
			}
			else {
				return EMPTY_STRING;
			}
		}

		public abstract Raison getRaison();
	}

	public static class ResultDebiteurIgnoreResteLrAEmettre extends ResultDebiteurNonTraite<ResultDebiteurIgnoreResteLrAEmettre> {
		private final List<DateRange> periodesEncoreACouvrir;

		public ResultDebiteurIgnoreResteLrAEmettre(long idDebiteur, String nomDebiteur, List<DateRange> periodesEncoreACouvrir) {
			super(idDebiteur, nomDebiteur);
			this.periodesEncoreACouvrir = periodesEncoreACouvrir;
		}

		@Override
		public final Raison getRaison() {
			return Raison.ENCORE_LR_A_EMETTRE_SUR_PF;
		}

		@Override
		public String getCommentaire() {
			if (periodesEncoreACouvrir != null && !periodesEncoreACouvrir.isEmpty()) {
				final StringBuilder b = new StringBuilder();
				for (DateRange periode : periodesEncoreACouvrir) {
					if (b.length() > 0) {
						b.append(", ");
					}
					b.append('[').append(RegDateHelper.dateToDisplayString(periode.getDateDebut()));
					b.append(',').append(RegDateHelper.dateToDisplayString(periode.getDateFin())).append(']');
				}
				if (periodesEncoreACouvrir.size() > 1) {
					return String.format("Les périodes %s ne sont pas couvertes par les LR émises", b.toString());
				}
				else {
					return String.format("La période %s n'est pas couverte par les LR émises", b.toString());
				}
			}
			else {
				return EMPTY_STRING;
			}
		}
	}

	public static class ResultErreurDebiteur extends ResultDebiteurNonTraite<ResultErreurDebiteur> {
		private final Exception e;

		public ResultErreurDebiteur(long idDebiteur, String nomDebiteur, Exception e) {
			super(idDebiteur, nomDebiteur);
			this.e = e;
		}

		@Override
		public Raison getRaison() {
			return Raison.EXCEPTION;
		}

		@Override
		public String getCommentaire() {
			if (StringUtils.isBlank(e.getMessage())) {
				return e.getClass().getName();
			}
			else {
				return e.getMessage();
			}
		}
	}

	public static class ResultLrEchue extends ResultDebiteur<ResultLrEchue> implements Comparable<ResultLrEchue> {
		public final CategorieImpotSource categorieImpotSource;
		public final RegDate debutPeriode;
		public final RegDate finPeriode;

		public ResultLrEchue(long idDebiteur, CategorieImpotSource categorieImpotSource, String nomDebiteur, RegDate debutPeriode, RegDate finPeriode) {
			super(idDebiteur, nomDebiteur);
			this.categorieImpotSource = categorieImpotSource;
			this.debutPeriode = debutPeriode;
			this.finPeriode = finPeriode;
		}

		@Override
		public int compareTo(@NotNull ResultLrEchue o) {
			int compare = super.compareTo(o);
			if (compare == 0) {
				compare = debutPeriode.compareTo(o.debutPeriode);
			}
			return compare;
		}
	}

	/**
	 * Concatène toutes les chaînes de la liste en une seule chaîne, en utilisant le séparateur donné entre chacune d'entre elles
	 */
	private static String concat(List<String> elts, String separator) {
		final StringBuilder b = new StringBuilder();
		boolean first = true;
		for (String elt : elts) {
			if (!first) {
				b.append(separator);
			}
			b.append(elt);
			first = false;
		}
		return b.toString();
	}

	private String getNomDebiteur(DebiteurPrestationImposable dpi) {
		final List<String> raisonSociale = tiersService.getRaisonSociale(dpi);
		return concat(raisonSociale, " ");
	}

	private final Integer periodeFiscale;
	private final RegDate dateTraitement;
	private final TiersService tiersService;

	private boolean interrompu;
	private int nbDebiteursAnalyses = 0;
	public final List<ResultLrEchue> lrEchues = new ArrayList<>();
	public final List<ResultDebiteurNonTraite> ignores = new ArrayList<>();
	public final List<ResultErreurDebiteur> erreurs = new ArrayList<>();

	public DeterminerLRsEchuesResults(Integer periodeFiscale, RegDate dateTraitement, TiersService tiersService, AdresseService adresseService) {
		super(tiersService, adresseService);
		this.periodeFiscale = periodeFiscale;
		this.dateTraitement = dateTraitement;
		this.tiersService = tiersService;
	}

	@Override
	public void addErrorException(InfoDebiteurAvecLrEchue element, Exception e) {
		erreurs.add(new ResultErreurDebiteur(element.idDebiteur, "", e));
	}

	@Override
	public void addAll(DeterminerLRsEchuesResults right) {
		lrEchues.addAll(right.lrEchues);
		ignores.addAll(right.ignores);
		erreurs.addAll(right.erreurs);
		nbDebiteursAnalyses += right.nbDebiteursAnalyses;
	}

	@Override
	public void end() {
		Collections.sort(lrEchues);
		Collections.sort(ignores);
		Collections.sort(erreurs);
		super.end();
	}

	public boolean isInterrompu() {
		return interrompu;
	}

	public int getNbDebiteursAnalyses() {
		return nbDebiteursAnalyses;
	}

	public void newDebiteurAnalyse() {
		++ nbDebiteursAnalyses;
	}

	public void setInterrompu(boolean interrompu) {
		this.interrompu = interrompu;
	}

	public Integer getPeriodeFiscale() {
		return periodeFiscale;
	}

	public RegDate getDateTraitement() {
		return dateTraitement;
	}

	public void addDebiteurIgnoreResteLrAEmettre(DebiteurPrestationImposable dpi, List<DateRange> aEmettre) {
		ignores.add(new ResultDebiteurIgnoreResteLrAEmettre(dpi.getId(), getNomDebiteur(dpi), aEmettre));
	}

	public void addLrEchue(DebiteurPrestationImposable dpi, DeclarationImpotSource lr) {
		lrEchues.add(new ResultLrEchue(dpi.getId(), dpi.getCategorieImpotSource(), getNomDebiteur(dpi), lr.getDateDebut(), lr.getDateFin()));
	}
}
