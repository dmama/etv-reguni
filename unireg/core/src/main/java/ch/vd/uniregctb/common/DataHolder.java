package ch.vd.uniregctb.common;

/**
 * Classe simple qui contient une donnée.
 * 
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class DataHolder<T> {

	private T data;

	public DataHolder() {
	}

	public DataHolder(T data) {
		this.data = data;
	}

	public T get() {
		return data;
	}

	public void set(T data) {
		this.data = data;
	}
}
