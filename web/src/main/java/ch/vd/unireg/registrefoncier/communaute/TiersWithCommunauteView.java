package ch.vd.uniregctb.registrefoncier.communaute;

import java.util.List;

import org.jetbrains.annotations.NotNull;

public class TiersWithCommunauteView {

	private final long ctbId;
	@NotNull
	private final List<ModeleCommunauteForTiersView> modeles;

	public TiersWithCommunauteView(long ctbId, @NotNull List<ModeleCommunauteForTiersView> modeles) {
		this.ctbId = ctbId;
		this.modeles = modeles;
	}

	public long getCtbId() {
		return ctbId;
	}

	@NotNull
	public List<ModeleCommunauteForTiersView> getModeles() {
		return modeles;
	}
}
