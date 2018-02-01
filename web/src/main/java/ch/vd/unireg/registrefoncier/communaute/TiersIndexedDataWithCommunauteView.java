package ch.vd.unireg.registrefoncier.communaute;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.indexer.tiers.TiersIndexedData;
import ch.vd.unireg.tiers.TiersIndexedDataView;

public class TiersIndexedDataWithCommunauteView extends TiersIndexedDataView {

	/**
	 * Le nombre de modèles de communauté dont fait partie le contribuable
	 */
	private final long modelesCount;

	public TiersIndexedDataWithCommunauteView(@NotNull TiersIndexedData data, long modelesCount) {
		super(data);
		this.modelesCount = modelesCount;
	}

	public long getModelesCount() {
		return modelesCount;
	}
}
