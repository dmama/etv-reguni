package ch.vd.unireg.registrefoncier.dao;

import javax.persistence.FlushModeType;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.registrefoncier.SurfaceAuSolRF;
import ch.vd.unireg.registrefoncier.key.SurfaceAuSolRFKey;

public class MockSurfaceAuSolRFDAO implements SurfaceAuSolRFDAO {
	@Nullable
	@Override
	public SurfaceAuSolRF findActive(@NotNull SurfaceAuSolRFKey key) {
		throw new NotImplementedException("");
	}

	@Override
	public List<SurfaceAuSolRF> getAll() {
		throw new NotImplementedException("");
	}

	@Override
	public SurfaceAuSolRF get(Long id) {
		throw new NotImplementedException("");
	}

	@Override
	public boolean exists(Long id) {
		throw new NotImplementedException("");
	}

	@Override
	public boolean exists(Long id, FlushModeType flushModeOverride) {
		throw new NotImplementedException("");
	}

	@Override
	public SurfaceAuSolRF save(SurfaceAuSolRF object) {
		throw new NotImplementedException("");
	}

	@Override
	public Object saveObject(Object object) {
		throw new NotImplementedException("");
	}

	@Override
	public void remove(Long id) {
		throw new NotImplementedException("");
	}

	@Override
	public void removeAll() {
		throw new NotImplementedException("");
	}

	@Override
	public Iterator<SurfaceAuSolRF> iterate(String query) {
		throw new NotImplementedException("");
	}

	@Override
	public int getCount(Class<?> clazz) {
		throw new NotImplementedException("");
	}

	@Override
	public void clearSession() {
		throw new NotImplementedException("");
	}

	@Override
	public void evict(Object o) {
		throw new NotImplementedException("");
	}
}
