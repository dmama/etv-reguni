package ch.vd.uniregctb.common;

import org.jetbrains.annotations.Nullable;

/**
 * Classe simple qui contient une donnée.
 * 
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class DataHolder<T> {

	private T data;

	public DataHolder() {
	}

	public DataHolder(@Nullable T data) {
		this.data = data;
	}

	@Nullable
	public T get() {
		return data;
	}

	public void set(@Nullable T data) {
		this.data = data;
	}
}
