package ch.vd.uniregctb.registrefoncier.dao;

import java.util.Iterator;
import java.util.List;

import org.hibernate.FlushMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.key.DroitRFKey;

public class MockDroitRFDAO implements DroitRFDAO {
	@Nullable
	@Override
	public DroitRF findActive(@NotNull DroitRFKey key) {
		throw new NotImplementedException();
	}

	@Override
	public @NotNull List<DroitRF> findForAyantDroit(long tiersRFId, boolean fetchSituationsImmeuble) {
		throw new NotImplementedException();
	}

	@Override
	public List<DroitRF> getAll() {
		throw new NotImplementedException();
	}

	@Override
	public DroitRF get(Long id) {
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
	public DroitRF save(DroitRF object) {
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
	public Iterator<DroitRF> iterate(String query) {
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
}
