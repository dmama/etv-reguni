package ch.vd.uniregctb.tiers.rattrapage.ancienshabitants;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import ch.vd.uniregctb.common.AbstractJobResults;
import ch.vd.uniregctb.tiers.PersonnePhysique;

public class RecuperationDonneesAnciensHabitantsResults extends AbstractJobResults<Long, RecuperationDonneesAnciensHabitantsResults> {

	public final int nbThreads;
	public final boolean forceEcrasement;
	public final boolean parents;
	public final boolean prenoms;
	public final boolean nomNaissance;

	private boolean interrupted = false;
	private final List<InfoIgnore> ignores = new LinkedList<>();
	private final List<InfoErreur> erreurs = new LinkedList<>();
	private final List<InfoTraite> traites = new LinkedList<>();

	public RecuperationDonneesAnciensHabitantsResults(int nbThreads, boolean forceEcrasement, boolean parents, boolean prenoms, boolean nomNaissance) {
		this.nbThreads = nbThreads;
		this.forceEcrasement = forceEcrasement;
		this.parents = parents;
		this.prenoms = prenoms;
		this.nomNaissance = nomNaissance;
	}

	public boolean isInterrupted() {
		return interrupted;
	}

	public void setInterrupted(boolean interrupted) {
		this.interrupted = interrupted;
	}

	public enum RaisonIgnorement {
		PAS_PERSONNE_PHYSIQUE("Pas (plus ?) une personne physique"),
		HABITANT("La personne physique est habitante"),
		JAMAIS_ETE_HABITANT("La personne physique est inconnue du registre civil"),
		RIEN_DANS_CIVIL("Pas d'information sur les noms des parents dans le registre civil"),
		VALEUR_DEJA_PRESENTE("Des valeurs sont déjà connues fiscalement et n'ont pas été écrasées");

		public final String description;

		RaisonIgnorement(String description) {
			this.description = description;
		}
	}

	public abstract static class Info implements Comparable<Info> {
		public final long noCtb;

		protected Info(long noCtb) {
			this.noCtb = noCtb;
		}

		@Override
		public final int compareTo(Info o) {
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
			return raison.description;
		}
	}

	public static class InfoErreur extends Info {
		private final Exception e;
		private InfoErreur(long noCtb, Exception e) {
			super(noCtb);
			this.e = e;
		}
		public String getMessage() {
			if (StringUtils.isBlank(e.getMessage())) {
				return e.getClass().getName();
			}
			else {
				return String.format("%s - %s", e.getClass().getName(), e.getMessage());
			}
		}
	}

	public static class InfoTraite extends Info {
		public final boolean majMere;
		public final String prenomsMere;
		public final String nomMere;
		public final boolean majPere;
		public final String prenomsPere;
		public final String nomPere;
		public final boolean majPrenoms;
		public final String tousPrenoms;
		public final boolean majNomNaissance;
		public final String nomNaissance;

		private InfoTraite(long noCtb, boolean majMere, String prenomsMere, String nomMere, boolean majPere, String prenomsPere, String nomPere, boolean majPrenoms, String tousPrenoms, boolean majNomNaissance, String nomNaissance) {
			super(noCtb);
			this.prenomsMere = prenomsMere;
			this.nomMere = nomMere;
			this.majMere = majMere;
			this.prenomsPere = prenomsPere;
			this.nomPere = nomPere;
			this.majPere = majPere;
			this.majPrenoms = majPrenoms;
			this.tousPrenoms = tousPrenoms;
			this.majNomNaissance = majNomNaissance;
			this.nomNaissance = nomNaissance;
		}
	}

	public void addCasTraite(PersonnePhysique pp, boolean majMere, boolean majPere, boolean majPrenoms, boolean majNomNaissance) {
		this.traites.add(new InfoTraite(pp.getNumero(), majMere, pp.getPrenomsMere(), pp.getNomMere(), majPere, pp.getPrenomsPere(), pp.getNomPere(), majPrenoms, pp.getTousPrenoms(), majNomNaissance, pp.getNomNaissance()));
	}

	public void addIgnore(long id, RaisonIgnorement raison) {
		this.ignores.add(new InfoIgnore(id, raison));
	}

	@Override
	public void addErrorException(Long element, Exception e) {
		this.erreurs.add(new InfoErreur(element, e));
	}

	@Override
	public void addAll(RecuperationDonneesAnciensHabitantsResults right) {
		this.ignores.addAll(right.ignores);
		this.erreurs.addAll(right.erreurs);
		this.traites.addAll(right.traites);
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

	public List<InfoTraite> getTraites() {
		return traites;
	}
}
