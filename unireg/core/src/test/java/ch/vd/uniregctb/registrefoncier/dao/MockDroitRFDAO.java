package ch.vd.uniregctb.registrefoncier.dao;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.FlushMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.key.DroitRFKey;

public class MockDroitRFDAO implements DroitRFDAO {

	private final List<DroitRF> db = new ArrayList<>();

	@Override
	public @Nullable DroitRF find(@NotNull DroitRFKey key) {
		return db.stream()
				.filter(a -> Objects.equals(a.getMasterIdRF(), key.getMasterIdRF()))
				.findFirst()
				.orElse(null);
	}

	@Nullable
	@Override
	public DroitRF findActive(@NotNull DroitRFKey key) {
		throw new NotImplementedException();
	}

	@Override
	public @NotNull List<DroitRF> findForAyantDroit(long tiersRFId, boolean fetchSituationsImmeuble) {
		throw new NotImplementedException();
	}

	@NotNull
	@Override
	public Set<String> findIdsServitudesActives() {
		return db.stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(a -> a.isValidAt(RegDate.get()))
				.map(DroitRF::getMasterIdRF)
				.collect(Collectors.toSet());
	}

	@Override
	public List<DroitRF> getAll() {
		return db;
	}

	@Override
	public DroitRF get(Long id) {
		return db.stream()
				.filter(a -> Objects.equals(a.getId(), id))
				.findFirst()
				.orElse(null);
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
		this.db.add(object);
		object.setId((long) db.size());
		return object;
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
