package ch.vd.uniregctb.listes.assujettis;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.ListesResults;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.MotifAssujettissement;
import ch.vd.uniregctb.metier.assujettissement.SourcierPur;
import ch.vd.uniregctb.metier.assujettissement.TypeAssujettissement;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Classe de résultats du batch d'extraction des assujettis d'une période fiscale
 */
public class ListeAssujettisResults extends ListesResults<ListeAssujettisResults> {

	private final int anneeFiscale;
	private final boolean avecSourciersPurs;
	private final boolean seulementAssujettisFinAnnee;

	private final List<InfoCtbAssujetti> assujettis = new LinkedList<>();
	private final List<InfoCtbIgnore> ignores = new LinkedList<>();
	private int nbCtbAssujettis = 0;
	private final boolean withForcedCtbs;
	private final AssujettissementService assujettissementService;

	public abstract static class InfoCtb<T extends InfoCtb> implements Comparable<T> {
		public final long noCtb;

		public InfoCtb(long noCtb) {
			this.noCtb = noCtb;
		}

		@Override
		public int compareTo(@NotNull T o) {
			return Long.compare(noCtb, o.noCtb);
		}
	}

	public static class InfoCtbAssujetti extends InfoCtb<InfoCtbAssujetti> {
		public final TypeAssujettissement typeAssujettissement;
		public final RegDate debutAssujettissement;
		public final RegDate finAssujettissement;
		public final MotifAssujettissement motifDebut;
		public final MotifAssujettissement motifFin;

		public InfoCtbAssujetti(long noCtb, TypeAssujettissement typeAssujettissement, RegDate debutAssujettissement, RegDate finAssujettissement, MotifAssujettissement motifDebut, MotifAssujettissement motifFin) {
			super(noCtb);
			this.typeAssujettissement = typeAssujettissement;
			this.debutAssujettissement = debutAssujettissement;
			this.finAssujettissement = finAssujettissement;
			this.motifDebut = motifDebut;
			this.motifFin = motifFin;
		}

		@Override
		public int compareTo(@NotNull InfoCtbAssujetti o) {
			int comparison = super.compareTo(o);
			if (comparison == 0) {
				if (debutAssujettissement != o.debutAssujettissement) {
					if (debutAssujettissement == null) {
						comparison = -1;
					}
					else if (o.debutAssujettissement == null) {
						comparison = 1;
					}
					else {
						comparison = debutAssujettissement.compareTo(o.debutAssujettissement);
					}
				}
			}
			return comparison;
		}
	}

	public enum CauseIgnorance {
		SOURCIER_PUR("Sourcier pur"),
		NON_ASSUJETTI_FIN_PERIODE("Non assujetti en fin de période fiscale"),
		NON_ASSUJETTI("Non assujetti"),
		NON_ASSUJETTI_MAIS_MENAGE_ASSUJETTI("Non assujetti avec ménage assujetti");

		public final String description;

		CauseIgnorance(String description) {
			this.description = description;
		}
	}

	public static class InfoCtbIgnore extends InfoCtb<InfoCtbIgnore> {
		public final CauseIgnorance cause;

		public InfoCtbIgnore(long noCtb, CauseIgnorance cause) {
			super(noCtb);
			this.cause = cause;
		}
	}

	public ListeAssujettisResults(RegDate dateTraitement, int nombreThreads, int anneeFiscale, boolean avecSourciersPurs, boolean seulementAssujettisFinAnnee,
	                              boolean withForcedCtbs, TiersService tiersService,
	                              AssujettissementService assujettissementService, AdresseService adresseService) {
		super(dateTraitement, nombreThreads, tiersService, adresseService);
		this.anneeFiscale = anneeFiscale;
		this.avecSourciersPurs = avecSourciersPurs;
		this.seulementAssujettisFinAnnee = seulementAssujettisFinAnnee;
		this.withForcedCtbs = withForcedCtbs;
		this.assujettissementService = assujettissementService;
	}

	@Override
	public void addContribuable(Contribuable ctb) throws AssujettissementException {

		final List<Assujettissement> assujettissements = assujettissementService.determine(ctb, anneeFiscale);
		final CauseIgnorance causeIgnorance = determineCauseIgnorance(assujettissements);

		if (causeIgnorance != null) {
			// si on a donné une liste de contribuables en entrée, on cherche éventuellement
			// un ménage assujetti selon les mêmes conditions associé à la personne physique ignorée
			CauseIgnorance causeIgnoranceEffective = causeIgnorance;
			if (ctb instanceof PersonnePhysique && withForcedCtbs) {
				final PersonnePhysique pp = (PersonnePhysique) ctb;
				final List<EnsembleTiersCouple> couples = tiersService.getEnsembleTiersCouple(pp, anneeFiscale);
				if (couples != null && !couples.isEmpty()) {
					final Map<Long, MenageCommun> menages = couples.stream()
							.map(EnsembleTiersCouple::getMenage)
							.collect(Collectors.toMap(MenageCommun::getNumero, Function.identity(), (m1, m2) -> m1));
					for (MenageCommun menage : menages.values()) {
						final List<Assujettissement> assujMenage = assujettissementService.determine(menage, anneeFiscale);
						final CauseIgnorance causeIgnoranceMenage = determineCauseIgnorance(assujMenage);
						if (causeIgnoranceMenage == null) {
							causeIgnoranceEffective = CauseIgnorance.NON_ASSUJETTI_MAIS_MENAGE_ASSUJETTI;
							break;
						}
					}
				}
			}

			ignores.add(new InfoCtbIgnore(ctb.getNumero(), causeIgnoranceEffective));
		}
		else {
			++ nbCtbAssujettis;
			for (Assujettissement a : assujettissements) {
				assujettis.add(new InfoCtbAssujetti(ctb.getNumero(), a.getType(), a.getDateDebut(), a.getDateFin(), a.getMotifFractDebut(), a.getMotifFractFin()));
			}
		}
	}

	@Nullable
	private CauseIgnorance determineCauseIgnorance(List<Assujettissement> assujettissements) {
		CauseIgnorance causeIgnorance = null;
		if (assujettissements == null || assujettissements.isEmpty()) {
			causeIgnorance = CauseIgnorance.NON_ASSUJETTI;
		}
		else {

			// si on ne doit pas tenir compte des assujettissement source pure, on les enlève maintenant
			if (!avecSourciersPurs) {
				assujettissements.removeIf(SourcierPur.class::isInstance);

				// s'il ne reste plus rien, c'est que le contribuable était toujours sourcier pur...
				if (assujettissements.isEmpty()) {
					causeIgnorance = CauseIgnorance.SOURCIER_PUR;
				}
			}

			// si le contribuable n'est pas assujetti en fin d'année et qu'il ne faut pas prendre en compte ces assujettis-là,
			// alors c'est le moment de s'arrêter
			if (seulementAssujettisFinAnnee && causeIgnorance == null) {
				final RegDate finAnnee = RegDate.get(anneeFiscale, 12, 31);
				final Assujettissement assujettissementFinAnnee = DateRangeHelper.rangeAt(assujettissements, finAnnee);
				if (assujettissementFinAnnee == null) {
					causeIgnorance = CauseIgnorance.NON_ASSUJETTI_FIN_PERIODE;
				}
			}
		}
		return causeIgnorance;
	}

	@Override
	public void addAll(ListeAssujettisResults sources) {
		super.addAll(sources);
		nbCtbAssujettis += sources.nbCtbAssujettis;
		assujettis.addAll(sources.assujettis);
		ignores.addAll(sources.ignores);
	}

	@Override
	public void sort() {
		super.sort();
		Collections.sort(assujettis);
		Collections.sort(ignores);
	}

	@Override
	public void addTiersEnErreur(Tiers tiers) {
		// ne fait rien en particulier
	}

	public int getAnneeFiscale() {
		return anneeFiscale;
	}

	public boolean isAvecSourciersPurs() {
		return avecSourciersPurs;
	}

	public boolean isSeulementAssujettisFinAnnee() {
		return seulementAssujettisFinAnnee;
	}

	public int getNbCtbAssujettis() {
		return nbCtbAssujettis;
	}

	public int getNbAssujettissements() {
		return assujettis.size();
	}

	public int getNbCtbIgnores() {
		return ignores.size();
	}

	public int getNbContribuablesInspectes() {
		return nbCtbAssujettis + getNbCtbIgnores() + getListeErreurs().size();
	}

	public List<InfoCtbAssujetti> getAssujettis() {
		return assujettis;
	}

	public List<InfoCtbIgnore> getIgnores() {
		return ignores;
	}
}
