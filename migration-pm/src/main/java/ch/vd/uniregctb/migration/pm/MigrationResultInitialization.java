package ch.vd.uniregctb.migration.pm;

import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmIndividu;
import ch.vd.uniregctb.migration.pm.utils.EntityKey;

public interface MigrationResultInitialization extends MigrationResultContextManipulation {

	/**
	 * Enregistre un traitement a effectuer avant la fin de la transaction
	 * @param dataClass classe de la donnée postée (on n'acceptera qu'un seul enregistrement par classe !)
	 * @param consolidationPhaseIndicator indicateur de l'emplacement de cette consolidation dans la grande liste des consolidations
	 * @param keyExtractor extracteur de la clé de regroupement pour les données postées
	 * @param dataMerger fusionneur des données associées à une clé postée plusieurs fois
	 * @param consolidator opération finale à effectuer sur les données consolidées
	 * @param <D> type des données postées et traitées
	 */
	<D> void registerPreTransactionCommitCallback(Class<D> dataClass,
	                                              ConsolidationPhase consolidationPhaseIndicator,
	                                              Function<? super D, ?> keyExtractor,
	                                              BinaryOperator<D> dataMerger,
	                                              Consumer<? super D> consolidator);

	/**
	 * Enregistre une méthode d'extraction de données depuis les données RegPM (l'idée est de ne la calculer qu'une seule fois,
	 * ces extracteurs ne seront appelés qu'une seule fois par instance de graphe) utilisable ensuite au travers de la méthode
	 * {@link MigrationResultProduction#getExtractedData(Class, EntityKey)}
	 * @param dataClass classe discriminante pour la donnée à extraire (une donnée par classe et entité)
	 * @param entrepriseExtractor l'extracteur à utiliser si cette données est extraite d'une entreprise
	 * @param etablissementExtractor l'extracteur à utiliser si cette données est extraite d'un établissement
	 * @param individuExtractor l'extracteur à utiliser si cette données est extraite d'un individu
	 * @param <D> le type de la donnée extraite
	 */
	<D> void registerDataExtractor(Class<D> dataClass,
	                               @Nullable Function<? super RegpmEntreprise, ? extends D> entrepriseExtractor,
	                               @Nullable Function<? super RegpmEtablissement, ? extends D> etablissementExtractor,
	                               @Nullable Function<? super RegpmIndividu, ? extends D> individuExtractor);

}
