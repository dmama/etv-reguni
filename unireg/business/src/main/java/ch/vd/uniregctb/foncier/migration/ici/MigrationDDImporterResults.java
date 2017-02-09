package ch.vd.uniregctb.foncier.migration.ici;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.StringRenderer;
import ch.vd.uniregctb.foncier.DegrevementICI;
import ch.vd.uniregctb.foncier.DonneesLoiLogement;
import ch.vd.uniregctb.foncier.DonneesUtilisation;
import ch.vd.uniregctb.foncier.migration.MigrationKey;

public class MigrationDDImporterResults {

	/**
	 * Heure de démarrage du job (à la milliseconde près).
	 */
	public final long startTime = System.currentTimeMillis();

	/**
	 * Heure d'arrêt du job (à la milliseconde près).
	 */
	public long endTime = 0;

	private boolean interrompu = false;

	private int nbLignes = 0;
	private final List<LigneInfo> lignesEnErreur = new LinkedList<>();
	private final List<Ignore> donneesIgnorees = new LinkedList<>();
	private final List<Erreur> erreurs = new LinkedList<>();
	private final List<Traite> traites = new LinkedList<>();
	private int nbThreads;

	public static final class LigneInfo {
		public final int index;
		public final String message;

		public LigneInfo(int index, String message) {
			this.index = index;
			this.message = message;
		}

		public int getIndex() {
			return index;
		}

		public String getMessage() {
			return message;
		}

		@Override
		public String toString() {
			return "LigneInfo{" +
					"index=" + index +
					", message='" + message + '\'' +
					'}';
		}
	}

	public static class Ignore {
		private final MigrationKey key;
		private final String message;

		public Ignore(MigrationKey key, String message) {
			this.key = key;
			this.message = message;
		}

		public MigrationKey getKey() {
			return key;
		}

		public String getMessage() {
			return message;
		}

		public String getContexte() {
			return null;
		}
	}

	public static class LigneIgnoree extends Ignore {
		public final MigrationDD dd;

		public LigneIgnoree(MigrationDD dd, String message) {
			super(new MigrationKey(dd), message);
			this.dd = dd;
		}

		@Override
		public String getContexte() {
			return dd.toString();
		}
	}

	public static class Erreur {
		private final String message;

		public Erreur(String message) {
			this.message = message;
		}

		public String getMessage() {
			return message;
		}

		public String getContexte() {
			return null;
		}
	}

	public static class ErreurDonnees extends Erreur {
		private final MigrationKey key;

		public ErreurDonnees(MigrationKey key, String message) {
			super(message);
			this.key = key;
		}

		@Override
		public String getContexte() {
			return key.toString();
		}
	}

	public static final class ErreurContribuable extends Erreur {
		private final long noContribuable;

		public ErreurContribuable(long noContribuable, String message) {
			super(message);
			this.noContribuable = noContribuable;
		}

		@Override
		public String getContexte() {
			return String.format("Contribuable %d", noContribuable);
		}
	}

	private static final StringRenderer<BigDecimal> PERCENT_RENDERER = StringRenderer.withDefaultIfNull(StringUtils.EMPTY, bg -> String.format("%s%%", bg.toPlainString()));

	private static final StringRenderer<DonneesUtilisation> DONNNEES_UTILISATION_RENDERER = StringRenderer.withDefaultIfNull("{}",
	                                                                                                                         du -> "{" +
			                                                                                                                         "revenu=" + du.getRevenu() +
			                                                                                                                         ", volume=" + du.getVolume() +
			                                                                                                                         ", surface=" + du.getSurface() +
			                                                                                                                         ", pourcentage=" + PERCENT_RENDERER.toString(du.getPourcentage()) +
			                                                                                                                         ", pourcentageArrete=" + PERCENT_RENDERER.toString(du.getPourcentageArrete()) +
			                                                                                                                         '}');

	private static final StringRenderer<DonneesLoiLogement> DONNEES_LL_RENDERER = StringRenderer.withDefaultIfNull("{}",
	                                                                                                               ll -> "{" +
			                                                                                                               "dateOctroi=" + RegDateHelper.dateToDisplayString(ll.getDateOctroi()) +
			                                                                                                               ", dateEcheance=" + RegDateHelper.dateToDisplayString(ll.getDateEcheance()) +
			                                                                                                               ", pourcentageCaractereSocial=" + PERCENT_RENDERER.toString(ll.getPourcentageCaractereSocial()) +
			                                                                                                               '}');

	public static class Traite {
		private final MigrationKey key;
		private final String descriptionDegrevement;

		public Traite(DegrevementICI degrevement, MigrationKey key) {
			this.key = key;
			this.descriptionDegrevement = buildDescription(degrevement);
		}

		private static String buildDescription(DegrevementICI degrevement) {
			return "{" +
					"dateDebut=" + RegDateHelper.dateToDashString(degrevement.getDateDebut()) +
					", immeuble=" + degrevement.getImmeuble().getId() +
					", locatif=" + DONNNEES_UTILISATION_RENDERER.toString(degrevement.getLocation()) +
					", propreUsage=" + DONNNEES_UTILISATION_RENDERER.toString(degrevement.getPropreUsage()) +
					", caractSocial=" + DONNEES_LL_RENDERER.toString(degrevement.getLoiLogement()) +
					"}";
		}

		public MigrationKey getKey() {
			return key;
		}

		public String getDescriptionDegrevement() {
			return descriptionDegrevement;
		}
	}

	public MigrationDDImporterResults(int nbThreads) {
		this.nbThreads = nbThreads;
	}

	public void addAll(MigrationDDImporterResults right) {
		nbLignes += right.nbLignes;
		lignesEnErreur.addAll(right.lignesEnErreur);
		donneesIgnorees.addAll(right.donneesIgnorees);
		erreurs.addAll(right.erreurs);
		traites.addAll(right.traites);
	}

	public void end() {
		this.endTime = System.currentTimeMillis();
	}

	public int incNbLignes() {
		return ++nbLignes;
	}

	public void addDegrevementTraite(DegrevementICI degrevement, MigrationKey key) {
		traites.add(new Traite(degrevement, key));
	}

	public void addLineEnErreur(int index, String message) {
		lignesEnErreur.add(new LigneInfo(index, message));
	}

	public void addLigneIgnoree(MigrationDD migrationDD, String message) {
		donneesIgnorees.add(new LigneIgnoree(migrationDD, message));
	}

	public void addDonneeDegrevementVide(Map.Entry<MigrationKey, ValeurDegrevement> dd) {
		donneesIgnorees.add(new Ignore(dd.getKey(), "Aucune valeur de dégrèvement disponible pour la PF " + dd.getValue().getPeriodeFiscale()));
	}

	public void addDegrevementIgnoreValeurPlusRecente(MigrationKey key, RegDate dateDebut, RegDate dateDebutPlusRecente) {
		donneesIgnorees.add(new Ignore(key, "Valeur plus récente disponible (" + RegDateHelper.dateToDisplayString(dateDebutPlusRecente) + " au lieu de " + RegDateHelper.dateToDisplayString(dateDebut) + ")"));
	}

	public void addErreur(Map.Entry<MigrationKey, ValeurDegrevement> dd, String message) {
		erreurs.add(new ErreurDonnees(dd.getKey(), message));
	}

	public void addContribuableEnErreur(long noContribuable, String message) {
		erreurs.add(new ErreurContribuable(noContribuable, message));
	}

	public void setInterrompu(boolean interrompu) {
		this.interrompu = interrompu;
	}

	public int getNbThreads() {
		return nbThreads;
	}

	public int getNbLignes() {
		return nbLignes;
	}

	public List<LigneInfo> getLignesEnErreur() {
		return lignesEnErreur;
	}

	/**
	 * @return la liste des lignes d'input ignorées
	 */
	public List<Ignore> getDonneesIgnorees() {
		return donneesIgnorees;
	}

	public boolean isInterrompu() {
		return interrompu;
	}

	public long getStartTime() {
		return startTime;
	}

	public long getEndTime() {
		return endTime;
	}

	public List<Erreur> getErreurs() {
		return erreurs;
	}

	public List<Traite> getTraites() {
		return traites;
	}
}

