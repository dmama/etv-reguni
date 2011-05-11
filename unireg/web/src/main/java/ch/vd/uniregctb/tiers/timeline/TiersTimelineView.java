package ch.vd.uniregctb.tiers.timeline;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.Assert;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.adresse.AdresseEnvoi;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;

/**
 * Form backing object pour la page de visualisation de l'historique des fors fiscaux et assujettissements d'un contribuable
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class TiersTimelineView {

	// paramètres du formulaire
	private Long tiersId = null;

	// données découlant des paramètres
	private AdresseEnvoi adresse;
	private final Table table = new Table();
	private final List<AssujettissementException> exceptions = new ArrayList<AssujettissementException>();

	private boolean forPrint = false;
	private String title;
	private String description;

	/**
	 * Représente une ligne dans la table
	 */
	public static class Row {
		public final DateRange periode;
		public TimelineCell forPrincipal;
		public final List<TimelineCell> forsSecondaires = new ArrayList<TimelineCell>(1);
		public TimelineCell forGestion;
		public TimelineCell assujettissement;
		public TimelineCell periodeImposition;

		public Row(DateRange periode) {
			this.periode = periode;
			this.forPrincipal = TimelineCell.FILLER;
			this.forsSecondaires.add(TimelineCell.FILLER);
			this.forGestion = TimelineCell.FILLER;
			this.assujettissement = TimelineCell.FILLER;
			this.periodeImposition = TimelineCell.FILLER;
		}

		public DateRange getPeriode() {
			return periode;
		}

		public TimelineCell getForPrincipal() {
			return forPrincipal;
		}

		public List<TimelineCell> getForsSecondaires() {
			return forsSecondaires;
		}

		public TimelineCell getForGestion() {
			return forGestion;
		}

		public TimelineCell getAssujettissement() {
			return assujettissement;
		}

		public TimelineCell getPeriodeImposition() {
			return periodeImposition;
		}
	}

	/**
	 * Représente la table contenant la "timeline"
	 */
	public static class Table {
		public final List<Row> rows = new ArrayList<Row>();

		public List<Row> getRows() {
			return rows;
		}

		public int getForsSecondairesSize() {
			if (rows.isEmpty()) {
				return 1;
			}
			else {
				return rows.get(0).forsSecondaires.size();
			}
		}

		/**
		 * Renseigne l'axe du temps (= les périodes minimales)
		 */
		public void setPeriodes(List<DateRange> periodes) {
			rows.clear();
			for (DateRange p : periodes) {
				rows.add(new Row(p));
			}
		}

		/**
		 * Ajoute un range dans la colonne "fors principaux"
		 */
		public void addForPrincipal(DateRange range) {
			TimelineCell c = new TimelineCell(range);
			int longueur = 0;
			for (Row r : rows) {
				if (RegDateHelper.equals(range.getDateDebut(), r.periode.getDateDebut())) {
					Assert.isTrue(r.forPrincipal == TimelineCell.FILLER);
					r.forPrincipal = c;
					longueur++;
				}
				else if (DateRangeHelper.within(r.periode, range)) {
					Assert.isTrue(r.forPrincipal == TimelineCell.FILLER);
					r.forPrincipal = TimelineCell.SPAN;
					longueur++;
				}
			}
			c.longueurAffichage = longueur;
		}

		/**
		 * Ajoute un range dans la colonne "fors gestion"
		 */
		public void addForGestion(DateRange range) {
			TimelineCell c = new TimelineCell(range);
			int longueur = 0;
			for (Row r : rows) {
				if (RegDateHelper.equals(range.getDateDebut(), r.periode.getDateDebut())) {
					Assert.isTrue(r.forGestion == TimelineCell.FILLER);
					r.forGestion = c;
					longueur++;
				}
				else if (DateRangeHelper.within(r.periode, range)) {
					Assert.isTrue(r.forGestion == TimelineCell.FILLER);
					r.forGestion = TimelineCell.SPAN;
					longueur++;
				}
			}
			c.longueurAffichage = longueur;
		}

		/**
		 * Ajoute un range dans la colonne "assujettissements"
		 */
		public void addAssujettissement(DateRange range) {
			TimelineCell c = new TimelineCell(range);
			int longueur = 0;
			for (Row r : rows) {
				if (RegDateHelper.equals(range.getDateDebut(), r.periode.getDateDebut())) {
					Assert.isTrue(r.assujettissement == TimelineCell.FILLER);
					r.assujettissement = c;
					longueur++;
				}
				else if (DateRangeHelper.within(r.periode, range)) {
					Assert.isTrue(r.assujettissement == TimelineCell.FILLER);
					r.assujettissement = TimelineCell.SPAN;
					longueur++;
				}
			}
			c.longueurAffichage = longueur;
		}

		/**
		 * Ajoute un range dans la colonne "périodes d'imposition"
		 */
		public void addPeriodeImposition(DateRange range) {
			TimelineCell c = new TimelineCell(range);
			int longueur = 0;
			for (Row r : rows) {
				if (RegDateHelper.equals(range.getDateDebut(), r.periode.getDateDebut())) {
					Assert.isTrue(r.periodeImposition == TimelineCell.FILLER);
					r.periodeImposition = c;
					longueur++;
				}
				else if (DateRangeHelper.within(r.periode, range)) {
					Assert.isTrue(r.periodeImposition == TimelineCell.FILLER);
					r.periodeImposition = TimelineCell.SPAN;
					longueur++;
				}
			}
			c.longueurAffichage = longueur;
		}

		/**
		 * Ajoute un range dans la colonne "fors secondaires". Note: il est possible d'avoir plusieurs fors secondaires valides pour une
		 * période donnée.
		 */
		public void addForSecondaire(DateRange range) {

			// recherche de l'emplacement
			int debut = -1;
			int fin = -1;
			int levels = 0;
			for (int i = 0; i < rows.size(); ++i) {
				Row r = rows.get(i);
				levels = r.forsSecondaires.size();
				if (RegDateHelper.equals(range.getDateDebut(), r.periode.getDateDebut())) {
					debut = i;
					fin = i;
				}
				else if (RegDateHelper.equals(range.getDateFin(), r.periode.getDateFin())) {
					fin = i;
					break;
				}
			}

			// recherche du niveau où placer le for
			int level = 0;
			boolean collision = false;
			for (; level < levels; ++level) {
				collision = false;
				for (int i = debut; i <= fin; ++i) {
					Row r = rows.get(i);
					if (r.forsSecondaires.get(level) != TimelineCell.FILLER) {
						collision = true;
						break;
					}
				}
				if (!collision) {
					break;
				}
			}

			// ajout d'un niveau supplémentaire si nécessaire
			if (collision) {
				addForSecondaireLevel();
			}

			// ajout du for
			TimelineCell c = new TimelineCell(range);

			for (int i = debut; i <= fin; ++i) {
				Row r = rows.get(i);
				Assert.isTrue(r.forsSecondaires.get(level) == TimelineCell.FILLER);
				if (i == debut) {
					r.forsSecondaires.set(level, c);
				}
				else {
					r.forsSecondaires.set(level, TimelineCell.SPAN);
				}
			}

			c.longueurAffichage = fin - debut + 1;
		}

		/**
		 * Ajoute un niveau à tous les collections contenant les fors secondaires
		 */
		private void addForSecondaireLevel() {
			for (Row r : rows) {
				r.forsSecondaires.add(TimelineCell.FILLER);
			}
		}
	}

	public Long getTiersId() {
		return tiersId;
	}

	public void setTiersId(Long tiersId) {
		this.tiersId = tiersId;
	}

	public boolean isForPrint() {
		return forPrint;
	}

	public void setForPrint(boolean forPrint) {
		this.forPrint = forPrint;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public AdresseEnvoi getAdresse() {
		return adresse;
	}

	public void setAdresse(AdresseEnvoi adresse) {
		this.adresse = adresse;
	}

	public Table getTable() {
		return table;
	}

	public void addException(AssujettissementException e) {
		exceptions.add(e);
	}

	public List<AssujettissementException> getExceptions() {
		return exceptions;
	}


}
