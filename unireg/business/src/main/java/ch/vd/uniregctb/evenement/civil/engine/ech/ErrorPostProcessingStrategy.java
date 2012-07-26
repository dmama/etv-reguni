package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchBasicInfo;

/**
 * Interface implémentée par les stratégies de post-processing du reliquat des événements
 * non encore traités d'une queue pour un individu suite à la levée d'une erreur plus tôt dans la queue.
 * <p/>
 * Le framework va d'abord appeler {@link #doCollectPhase(java.util.List, ch.vd.uniregctb.evenement.civil.engine.ech.ErrorPostProcessingStrategy.CustomDataHolder)},
 * charge à la stratégie de collecter les éléments nécessaire à son traitement (à placer dans le paramètre <b>customData</b>).
 * <p/>
 * Ensuite, c'est la méthode {@link #doFinalizePhase(Object)} qui sera appelée en repassant l'objet constitué dans le premier appel.
 * @param <T> type de la donnée partagée entre les phase de collecte et de finalisation
 */
public interface ErrorPostProcessingStrategy<T> {

	/**
	 * Structure de donnée passée lors de la phase de collecte pour permettre l'utilisation de paramètre <i>out</i>.
	 * @param <T> type de la donnée stockée
	 */
	public static final class CustomDataHolder<T> {
		@Nullable
		public T member;
	}

	/**
	 * @return <code>true</code> si le {@link #doCollectPhase(java.util.List, ch.vd.uniregctb.evenement.civil.engine.ech.ErrorPostProcessingStrategy.CustomDataHolder)} doit être appelé dans une transaction, <code>false</code> sinon
	 */
	boolean needsTransactionOnCollectPhase();

	/**
	 * Appelé dans la première phase avec la collection complète des données sur les événements de la queue d'erreur (l'événement
	 * en erreur à la tête de la queue ne fait pas partie de la liste)
	 * @param remainingEvents les événements à regarder
	 * @param customData porte de sortie pour une donnée à faire passer en phase de finalisation
	 * @return une liste contenant les événements non-traités par cette stratégie (ils seront pris en compte par les stratégies suivantes)
	 */
	@NotNull
	List<EvenementCivilEchBasicInfo> doCollectPhase(List<EvenementCivilEchBasicInfo> remainingEvents, CustomDataHolder<T> customData);

	/**
	 * @return <code>true</code> si le {@link #doFinalizePhase(Object)} doit être appelé dans une transaction, <code>false</code> sinon
	 */
	boolean needsTransactionOnFinalizePhase();

	/**
	 * Appelé dans la phase de finalisation
	 * @param customData donnée fournie par la phase de collecte
	 */
	void doFinalizePhase(T customData);

}
