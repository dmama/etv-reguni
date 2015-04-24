package ch.vd.uniregctb.migration.pm.engine;

import java.util.List;
import java.util.Map;

import ch.vd.registre.base.date.DateRange;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCommune;
import ch.vd.uniregctb.tiers.Tiers;

/**
 * Classe de base des données collectées pour construire les fors secondaires
 */
public abstract class ForsSecondairesData {

	final AbstractEntityMigrator.KeyedSupplier<? extends Tiers> entiteJuridiqueSupplier;
	final Map<RegpmCommune, List<DateRange>> communes;

	protected ForsSecondairesData(AbstractEntityMigrator.KeyedSupplier<? extends Tiers> entiteJuridiqueSupplier,
	                              Map<RegpmCommune, List<DateRange>> communes) {
		this.entiteJuridiqueSupplier = entiteJuridiqueSupplier;
		this.communes = communes;
	}

	/**
	 * Classe des données collectées pour construire les fors "immeuble"
	 */
	public static final class Immeuble extends ForsSecondairesData {
		public Immeuble(AbstractEntityMigrator.KeyedSupplier<? extends Tiers> entiteJuridiqueSupplier, Map<RegpmCommune, List<DateRange>> communes) {
			super(entiteJuridiqueSupplier, communes);
		}
	}

	/**
	 * Classe des données collectées pour construire les fors "activité" (= établissement)
	 */
	public static final class Activite extends ForsSecondairesData {
		public Activite(AbstractEntityMigrator.KeyedSupplier<? extends Tiers> entiteJuridiqueSupplier, Map<RegpmCommune, List<DateRange>> communes) {
			super(entiteJuridiqueSupplier, communes);
		}
	}
}
