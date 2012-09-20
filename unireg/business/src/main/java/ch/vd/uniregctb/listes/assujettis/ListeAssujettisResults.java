package ch.vd.uniregctb.listes.assujettis;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.ListesResults;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.SourcierPur;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.MotifFor;

/**
 * Classe de résultats du batch d'extraction des assujettis d'une période fiscale
 */
public class ListeAssujettisResults extends ListesResults<ListeAssujettisResults> {

	private final int anneeFiscale;
	private final boolean avecSourciersPurs;
	private final boolean seulementAssujettisFinAnnee;

	private final List<InfoCtbAssujetti> assujettis = new LinkedList<InfoCtbAssujetti>();
	private final List<InfoCtbIgnore> ignores = new LinkedList<InfoCtbIgnore>();
	private int nbCtbAssujettis = 0;
	private final AssujettissementService assujettissementService;

	public static abstract class InfoCtb<T extends InfoCtb> implements Comparable<T> {
		public final long noCtb;

		public InfoCtb(long noCtb) {
			this.noCtb = noCtb;
		}

		@Override
		public int compareTo(T o) {
			return (noCtb < o.noCtb ? -1 : (noCtb > o.noCtb ? 1 : 0));
		}
	}

	public static class InfoCtbAssujetti extends InfoCtb<InfoCtbAssujetti> {
		public final String typeAssujettissement;
		public final RegDate debutAssujettissement;
		public final RegDate finAssujettissement;
		public final MotifFor motifDebut;
		public final MotifFor motifFin;

		public InfoCtbAssujetti(long noCtb, String typeAssujettissement, RegDate debutAssujettissement, RegDate finAssujettissement, MotifFor motifDebut, MotifFor motifFin) {
			super(noCtb);
			this.typeAssujettissement = typeAssujettissement;
			this.debutAssujettissement = debutAssujettissement;
			this.finAssujettissement = finAssujettissement;
			this.motifDebut = motifDebut;
			this.motifFin = motifFin;
		}

		@Override
		public int compareTo(InfoCtbAssujetti o) {
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

	public static enum CauseIgnorance {
		SOURCIER_PUR("Sourcier pur"),
		NON_ASSUJETTI_FIN_PERIODE("Non assujetti en fin de période fiscale"),
		NON_ASSUJETTI("Non assujetti");

		public final String description;

		private CauseIgnorance(String description) {
			this.description = description;
		}
	}

	public static class InfoCtbIgnore extends InfoCtb {
		public final CauseIgnorance cause;

		public InfoCtbIgnore(long noCtb, CauseIgnorance cause) {
			super(noCtb);
			this.cause = cause;
		}
	}

	public ListeAssujettisResults(RegDate dateTraitement, int nombreThreads, int anneeFiscale, boolean avecSourciersPurs, boolean seulementAssujettisFinAnnee, TiersService tiersService,
	                              AssujettissementService assujettissementService) {
		super(dateTraitement, nombreThreads, tiersService);
		this.anneeFiscale = anneeFiscale;
		this.avecSourciersPurs = avecSourciersPurs;
		this.seulementAssujettisFinAnnee = seulementAssujettisFinAnnee;
		this.assujettissementService = assujettissementService;
	}

	@Override
	public void addContribuable(Contribuable ctb) throws AssujettissementException {

		CauseIgnorance causeIgnorance = null;

		final List<Assujettissement> assujettissements = assujettissementService.determine(ctb, anneeFiscale);
		if (assujettissements == null || assujettissements.isEmpty()) {
			causeIgnorance = CauseIgnorance.NON_ASSUJETTI;
		}
		else {

			// si on ne doit pas tenir compte des assujettissement source pure, on les enlève maintenant
			if (!avecSourciersPurs) {
				final Iterator<Assujettissement> iterator = assujettissements.iterator();
				while (iterator.hasNext()) {
					final Assujettissement assujettissement = iterator.next();
					if (assujettissement instanceof SourcierPur) {
						iterator.remove();
					}
				}

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

		if (causeIgnorance != null) {
			ignores.add(new InfoCtbIgnore(ctb.getNumero(), causeIgnorance));
		}
		else {
			++ nbCtbAssujettis;
			for (Assujettissement a : assujettissements) {
				assujettis.add(new InfoCtbAssujetti(ctb.getNumero(), a.getDescription(), a.getDateDebut(), a.getDateFin(), a.getMotifFractDebut(), a.getMotifFractFin()));
			}
		}
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
