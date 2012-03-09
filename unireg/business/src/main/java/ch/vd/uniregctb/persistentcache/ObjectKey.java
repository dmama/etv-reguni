package ch.vd.uniregctb.persistentcache;

import java.io.Serializable;

public interface ObjectKey extends Serializable {
	public long getId();
	public String getComplement();
}
