package ch.vd.uniregctb.registrefoncier;

import java.util.ArrayList;

import org.jetbrains.annotations.NotNull;

/**
 * Liste spécialisée pour contenir des données en relation avec un immeuble.
 */
public class ListImmeuble<T> extends ArrayList<T> {

	@NotNull
	private final String idRF;

	public ListImmeuble(@NotNull String idRF) {
		this.idRF = idRF;
	}

	public ListImmeuble(@NotNull String idRF, int initialCapacity) {
		super(initialCapacity);
		this.idRF = idRF;
	}

	@NotNull
	public String getIdRF() {
		return idRF;
	}
}
