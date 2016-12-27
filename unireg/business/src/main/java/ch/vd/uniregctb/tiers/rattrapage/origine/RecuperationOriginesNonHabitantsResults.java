package ch.vd.uniregctb.tiers.rattrapage.origine;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.common.AbstractJobResults;
import ch.vd.uniregctb.tiers.OriginePersonnePhysique;

public class RecuperationOriginesNonHabitantsResults extends AbstractJobResults<Long, RecuperationOriginesNonHabitantsResults> {

	private final int nbThreads;
	private final boolean dryRun;

	private boolean interrupted = false;

	private final List<InfoIgnore> ignores = new LinkedList<>();
	private final List<InfoErreur> erreurs = new LinkedList<>();
	private final List<InfoTraitement> traites = new LinkedList<>();

	public RecuperationOriginesNonHabitantsResults(int nbThreads, boolean dryRun) {
		this.nbThreads = nbThreads;
		this.dryRun = dryRun;
	}

	public int getNbThreads() {
		return nbThreads;
	}

	public boolean isDryRun() {
		return dryRun;
	}

	public boolean isInterrupted() {
		return interrupted;
	}

	public void setInterrupted(boolean interrupted) {
		this.interrupted = interrupted;
	}

	public enum RaisonIgnorement {
		HABITANT_VD("Habitant vaudois"),
		AUCUNE_ORIGINE_CIVILE_CONNUE("Ancient habitant pour lequel le registre civil ne connait pas d'origine"),
		NON_HABITANT_SANS_LIBELLE_ORIGINE("Aucune donnée"),
		PAS_PERSONNE_PHYSIQUE("Pas une personne physique"),
		VALEUR_DEJA_PRESENTE("Valeur déjà présente");

		private final String libelle;

		RaisonIgnorement(String libelle) {
			this.libelle = libelle;
		}

		public String getLibelle() {
			return libelle;
		}
	}

	public abstract static class Info implements Comparable<Info> {
		public final long noCtb;

		protected Info(long noCtb) {
			this.noCtb = noCtb;
		}

		@Override
		public final int compareTo(@NotNull Info o) {
			return Long.compare(noCtb, o.noCtb);
		}
	}

	public static class InfoIgnore extends Info {
		protected final RaisonIgnorement raison;
		private InfoIgnore(long noCtb, RaisonIgnorement raison) {
			super(noCtb);
			this.raison = raison;
		}
		public String getMessage() {
			return raison.getLibelle();
		}
	}

	public static class InfoErreur extends Info {
		private final String message;
		private InfoErreur(long noCtb, Exception e) {
			super(noCtb);
			this.message = buildExceptionMessage(e);
		}
		private InfoErreur(long noCtb, String message) {
			super(noCtb);
			this.message = message;
		}
		private static String buildExceptionMessage(Exception e) {
			if (StringUtils.isBlank(e.getMessage())) {
				return e.getClass().getName();
			}
			else {
				return String.format("%s - %s", e.getClass().getName(), e.getMessage());
			}
		}
		public String getMessage() {
			return message;
		}
	}

	public static final class InfoTraitement extends Info {
		private final String libelle;
		private final String sigleCanton;

		private InfoTraitement(long noCtb, String libelle, String sigleCanton) {
			super(noCtb);
			this.libelle = libelle;
			this.sigleCanton = sigleCanton;
		}

		public String getLibelle() {
			return libelle;
		}

		public String getSigleCanton() {
			return sigleCanton;
		}
	}

	public void addIgnore(Long element, RaisonIgnorement raison) {
		ignores.add(new InfoIgnore(element, raison));
	}

	public void addErreurCommuneInconnue(long noCtb, String libelle) {
		erreurs.add(new InfoErreur(noCtb, String.format("Pas de commune trouvée avec le libellé '%s'", libelle)));
	}

	public void addTraite(long noCtb, OriginePersonnePhysique origine) {
		traites.add(new InfoTraitement(noCtb, origine.getLibelle(), origine.getSigleCanton()));
	}

	@Override
	public void addErrorException(Long element, Exception e) {
		erreurs.add(new InfoErreur(element, e));
	}

	@Override
	public void addAll(RecuperationOriginesNonHabitantsResults right) {
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

	public List<InfoIgnore> getIgnores() {
		return ignores;
	}

	public List<InfoErreur> getErreurs() {
		return erreurs;
	}

	public List<InfoTraitement> getTraites() {
		return traites;
	}
}
