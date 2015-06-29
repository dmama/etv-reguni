package ch.vd.uniregctb.indexer.messageidentification;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuableCriteria;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuableEtatFilter;
import ch.vd.uniregctb.evenement.identification.contribuable.TypeDemande;

public interface GlobalMessageIdentificationSearcher {

	 static final int MAX_RESULTS = 1000;

	/**
	 * Récupère les données indexées en fonction des critères fournis
	 * @param criteria critères de recherche
	 * @param typesDemande les types de demandes recherchés (<code>null</code> si pas de critère sur les types)
	 * @param etatFilter filtre à appliquer sur les états visibles
	 * @param pagination pagination pour la liste des résultats
	 * @return liste des message trouvés correspondants à la pagination souhaitée
	 */
	List<MessageIdentificationIndexedData> search(IdentificationContribuableCriteria criteria, @Nullable TypeDemande[] typesDemande, IdentificationContribuableEtatFilter etatFilter, ParamPagination pagination);

	/**
	 * @param criteria critères de recherche
	 * @param typesDemande les types de demandes recherchés (<code>null</code> si pas de critère sur les types)
	 * @param etatFilter filtre à appliquer sur les états visibles
	 * @return le nombre total de messages d'identification correspondant aux critères donnés
	 */
	int count(IdentificationContribuableCriteria criteria, @Nullable TypeDemande[] typesDemande, IdentificationContribuableEtatFilter etatFilter);
}
