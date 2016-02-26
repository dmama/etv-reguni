package ch.vd.uniregctb.migration.pm.engine.collector;

import ch.vd.uniregctb.migration.pm.MigrationResultContextManipulation;
import ch.vd.uniregctb.migration.pm.mapping.IdMapping;

@FunctionalInterface
public interface NeutralizedLinkAction {

	/**
	 * Action à lancer sur un lien dont au moins une extrémité a été neutralisée... il n'est pas prévu de pouvoir
	 * ré-enregistrer des demandes de création de lien depuis cette méthode
	 * @param link le lien en question
	 * @param neutralizationReason la portée de la neutralisation
	 * @param mr une collecteur de messages de suivi et
	 * @param idMapper mapping des identifiants RegPM -> Unireg
	 */
	void execute(EntityLinkCollector.EntityLink link,
	             EntityLinkCollector.NeutralizationReason neutralizationReason,
	             MigrationResultContextManipulation mr,
	             IdMapping idMapper);
}
