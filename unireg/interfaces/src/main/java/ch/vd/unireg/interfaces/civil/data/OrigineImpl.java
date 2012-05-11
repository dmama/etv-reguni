package ch.vd.unireg.interfaces.civil.data;

import java.io.Serializable;

import org.apache.commons.lang.StringUtils;

public class OrigineImpl implements Origine, Serializable {

	private static final long serialVersionUID = -4733215913274172897L;

	private final String nomLieu;

	public static OrigineImpl get(ch.vd.registre.civil.model.Origine target) {
		if (target == null) {
			return null;
		}
		return new OrigineImpl(target);
	}

	private OrigineImpl(ch.vd.registre.civil.model.Origine target) {
		this.nomLieu = StringUtils.trimToEmpty(target.getNomCommune());
	}

	@Override
	public String getNomLieu() {
		return nomLieu;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final OrigineImpl origine = (OrigineImpl) o;
		return !(nomLieu != null ? !nomLieu.equals(origine.nomLieu) : origine.nomLieu != null);
	}

	@Override
	public int hashCode() {
		return nomLieu != null ? nomLieu.hashCode() : 0;
	}
}
