package ch.vd.uniregctb.tiers.timeline;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.adresse.AdresseCouche;
import ch.vd.uniregctb.adresse.AdresseEnvoi;

/**
 * Form backing object pour la page de visualisation de l'historique des adresses d'un contribuable
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class AdresseTimelineView {

	// paramètres du formulaire
	private Long tiersId = null;

	// données découlant des paramètres
	private AdresseEnvoi adresseEnvoi;
	private final List<Table> tables = new ArrayList<Table>();
	private final List<Exception> exceptions = new ArrayList<Exception>();

	/**
	 * Représente une ligne dans la table
	 */
	public static class Row {
		public Map<AdresseCouche, TimelineCell> columns = new EnumMap<AdresseCouche, TimelineCell>(AdresseCouche.class);
		public final DateRange periode;

		public Row(DateRange periode, List<AdresseCouche> cols) {
			this.periode = periode;
			for (AdresseCouche c : cols) {
				columns.put(c, TimelineCell.FILLER);
			}
		}

		public DateRange getPeriode() {
			return periode;
		}

		public Collection<TimelineCell> getColumns() {
			return columns.values();
		}

		public void setCell(AdresseCouche couche, TimelineCell cell) {
			columns.put(couche, cell);
		}

		public TimelineCell getCell(AdresseCouche couche) {
			return columns.get(couche);
		}
	}

	/**
	 * Représente la table contenant la "timeline"
	 */
	public static class Table {
		public final String nom;
		public final List<Row> rows = new ArrayList<Row>();
		public List<AdresseCouche> columns;

		public Table(String nom) {
			this.nom = nom;
		}

		public String getNom() {
			return nom;
		}

		public List<Row> getRows() {
			return rows;
		}

		public List<AdresseCouche> getColumns() {
			return columns;
		}

		public void setColumns(List<AdresseCouche> columns) {
			this.columns = columns;
		}

		/**
		 * Renseigne l'axe du temps (= les périodes minimales)
		 */
		public void setPeriodes(List<DateRange> periodes) {
			if (columns == null) {
				throw new IllegalArgumentException("Les colonnes doivent être renseignées en premier");
			}
			rows.clear();
			for (DateRange p : periodes) {
				rows.add(new Row(p, columns));
			}
		}

		/**
		 * Ajoute un range dans la colonne qui correspond à la couche spécifiée.
		 */
		public void addAdresse(AdresseCouche couche, DateRange range) {
			TimelineCell c = new TimelineCell(range);
			int longueur = 0;
			for (Row r : rows) {
				if (RegDateHelper.equals(range.getDateDebut(), r.periode.getDateDebut())) {
					if (r.getCell(couche) != TimelineCell.FILLER) {
						throw new IllegalArgumentException();
					}
					r.setCell(couche, c);
					longueur++;
				}
				else if (DateRangeHelper.within(r.periode, range)) {
					if (r.getCell(couche) != TimelineCell.FILLER) {
						throw new IllegalArgumentException();
					}
					r.setCell(couche, TimelineCell.SPAN);
					longueur++;
				}
			}
			c.longueurAffichage = longueur;
		}
	}

	public Long getTiersId() {
		return tiersId;
	}

	public void setTiersId(Long tiersId) {
		this.tiersId = tiersId;
	}

	public AdresseEnvoi getAdresseEnvoi() {
		return adresseEnvoi;
	}

	public void setAdresseEnvoi(AdresseEnvoi adresseEnvoi) {
		this.adresseEnvoi = adresseEnvoi;
	}

	public void addTable(Table table) {
		tables.add(table);
	}

	public List<Table> getTables() {
		return tables;
	}

	public void addException(Exception e) {
		exceptions.add(e);
	}

	public List<Exception> getExceptions() {
		return exceptions;
	}


}
