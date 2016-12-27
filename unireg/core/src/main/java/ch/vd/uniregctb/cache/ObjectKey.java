package ch.vd.uniregctb.cache;

import java.io.Serializable;

public interface ObjectKey extends Serializable {
	long getId();
	String getComplement();
}
