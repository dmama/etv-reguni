package ch.vd.uniregctb.registrefoncier.dao;

import java.util.Iterator;
import java.util.List;

import org.hibernate.FlushMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.key.AyantDroitRFKey;

public class MockAyantDroitRFDAO implements AyantDroitRFDAO {
	@Override
	public List<AyantDroitRF> getAll() {
		throw new NotImplementedException();
	}

	@Override
	public AyantDroitRF get(Long id) {
		throw new NotImplementedException();
	}

	@Override
	public boolean exists(Long id) {
		throw new NotImplementedException();
	}

	@Override
	public boolean exists(Long id, FlushMode flushModeOverride) {
		throw new NotImplementedException();
	}

	@Override
	public AyantDroitRF save(AyantDroitRF object) {
		throw new NotImplementedException();
	}

	@Override
	public Object saveObject(Object object) {
		throw new NotImplementedException();
	}

	@Override
	public void remove(Long id) {
		throw new NotImplementedException();
	}

	@Override
	public void removeAll() {
		throw new NotImplementedException();
	}

	@Override
	public Iterator<AyantDroitRF> iterate(String query) {
		throw new NotImplementedException();
	}

	@Override
	public int getCount(Class<?> clazz) {
		throw new NotImplementedException();
	}

	@Override
	public void clearSession() {
		throw new NotImplementedException();
	}

	@Override
	public void evict(Object o) {
		throw new NotImplementedException();
	}

	@Nullable
	@Override
	public AyantDroitRF find(@NotNull AyantDroitRFKey key) {
		throw new NotImplementedException();
	}
}
