package ch.vd.uniregctb.documentfiscal;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.AbstractJobResults;

public class RappelLettresBienvenueResults extends AbstractJobResults<Long, RappelLettresBienvenueResults> {

	public final RegDate dateTraitement;

	public enum RaisonIgnorement {
		LETTRE_DEJA_RETOURNEE("La lettre de bienvenue a déjà été retournée."),
		LETTRE_DEJA_RAPPELEE("Un rappel a déjà eu lieu."),
		DELAI_ADMINISTRATIF_NON_ECHU("Délai administratif non-échu.")
		;

		public final String libelle;

		RaisonIgnorement(String libelle) {
			this.libelle = libelle;
		}
	}

	public static class Ignore implements Comparable<Ignore> {

		public final long noCtb;
		public final RegDate dateEnvoi;
		public final RaisonIgnorement raison;

		public Ignore(long noCtb, RegDate dateEnvoi, RaisonIgnorement raison) {
			this.noCtb = noCtb;
			this.dateEnvoi = dateEnvoi;
			this.raison = raison;
		}

		@Override
		public int compareTo(@NotNull Ignore o) {
			int comparison = Long.compare(noCtb, o.noCtb);
			if (comparison == 0) {
				comparison = NullDateBehavior.EARLIEST.compare(dateEnvoi, o.dateEnvoi);
			}
			return comparison;
		}
	}

	public static class EnErreur implements Comparable<EnErreur> {

		public final Long noCtb;
		public final long idLettre;
		public final String msg;

		public EnErreur(Long noCtb, long idLettre, String msg) {
			this.noCtb = noCtb;
			this.idLettre = idLettre;
			this.msg = msg;
		}

		@Override
		public int compareTo(@NotNull EnErreur o) {
			if (noCtb == null) {
				if (o.noCtb == null) {
					return Long.compare(idLettre, o.idLettre);
				}
				else {
					return 1;
				}
			}
			else if (o.noCtb == null) {
				return -1;
			}
			else {
				int comparison = Long.compare(noCtb, o.noCtb);
				if (comparison == 0) {
					comparison = Long.compare(idLettre, o.idLettre);
				}
				return comparison;
			}
		}
	}

	public static class Traite implements Comparable<Traite> {

		public final long noCtb;
		public final RegDate dateEnvoiLettre;

		public Traite(long noCtb, RegDate dateEnvoiLettre) {
			this.noCtb = noCtb;
			this.dateEnvoiLettre = dateEnvoiLettre;
		}

		@Override
		public int compareTo(@NotNull Traite o) {
			int comparison = Long.compare(noCtb, o.noCtb);
			if (comparison == 0) {
				comparison = NullDateBehavior.EARLIEST.compare(dateEnvoiLettre, o.dateEnvoiLettre);
			}
			return comparison;
		}
	}

	private final List<Ignore> ignores = new LinkedList<>();
	private final List<EnErreur> erreurs = new LinkedList<>();
	private final List<Traite> traites = new LinkedList<>();
	private boolean interrompu = false;

	public RappelLettresBienvenueResults(RegDate dateTraitement) {
		this.dateTraitement = dateTraitement;
	}

	public void addLettreIgnoree(long idContribuable, RegDate dateEnvoi, RaisonIgnorement raison) {
		ignores.add(new Ignore(idContribuable, dateEnvoi, raison));
	}

	public void addRappelEnvoye(long idContribuable, RegDate dateEnvoiLettre) {
		traites.add(new Traite(idContribuable, dateEnvoiLettre));
	}

	public void addRappelErreur(long idContribuable, long idLettre, String message) {
		erreurs.add(new EnErreur(idContribuable, idLettre, message));
	}

	@Override
	public void addErrorException(Long idLettre, Exception e) {
		erreurs.add(new EnErreur(null, idLettre, e.getMessage()));
	}

	@Override
	public void addAll(RappelLettresBienvenueResults right) {
		ignores.addAll(right.ignores);
		erreurs.addAll(right.erreurs);
		traites.addAll(right.traites);
	}

	@Override
	public void end() {
		Collections.sort(ignores);
		Collections.sort(erreurs);
		Collections.sort(traites);
		super.end();
	}

	public void setInterrompu() {
		interrompu = true;
	}

	public boolean isInterrompu() {
		return interrompu;
	}

	public List<Ignore> getIgnores() {
		return ignores;
	}

	public List<EnErreur> getErreurs() {
		return erreurs;
	}

	public List<Traite> getTraites() {
		return traites;
	}
}
