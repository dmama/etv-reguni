package ch.vd.uniregctb.migration.pm;

import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;

public interface MigrationResultInitialization extends MigrationResultContextManipulation {

	/**
	 * Enregistre un traitement a effectuer avant la fin de la transaction
	 * @param dataClass classe de la donnée postée (on n'acceptera qu'un seul enregistrement par classe !)
	 * @param consolidationPhaseIndicator indicateur de l'emplacement de cette consolidation dans la grande liste des consolidations
	 * @param keyExtractor extracteur de la clé de regroupement pour les données postées
	 * @param dataMerger fusionneur des données associées à une clé postée plusieurs fois
	 * @param consolidator opération finale à effectuée sur les données consolidées
	 * @param <D> type des données postées et traitées
	 */
	<D> void registerPreTransactionCommitCallback(Class<D> dataClass,
	                                              int consolidationPhaseIndicator,
	                                              Function<? super D, ?> keyExtractor,
	                                              BinaryOperator<D> dataMerger,
	                                              Consumer<? super D> consolidator);
}
