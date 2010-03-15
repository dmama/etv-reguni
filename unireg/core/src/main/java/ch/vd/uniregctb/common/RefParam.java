package ch.vd.uniregctb.common;

/**
 * Cette classe permet de passer par référence (in/out) un paramètre à une méthode java.
 */
public class RefParam<T> {

	public T ref;

	RefParam() {
	}

	RefParam(T ref) {
		this.ref = ref;
	}
}
