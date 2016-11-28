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

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.registrefoncier.BatimentRF;
import ch.vd.uniregctb.registrefoncier.key.BatimentRFKey;

public class MockBatimentRFDAO implements BatimentRFDAO {

	private final List<BatimentRF> db = new ArrayList<>();

	@Nullable
	@Override
	public BatimentRF find(@NotNull BatimentRFKey key) {
		return db.stream()
				.filter(b -> Objects.equals(b.getMasterIdRF(), key.getMasterIdRF()))
				.findFirst()
				.orElse(null);
	}

	@NotNull
	@Override
	public Set<String> findActifs() {
		return db.stream()
				.filter(b -> b.getImplantations().stream()
						.filter(i -> i.isValidAt(null))
						.count() > 0)
				.map(BatimentRF::getMasterIdRF)
				.collect(Collectors.toSet());
	}

	@Override
	public List<BatimentRF> getAll() {
		return db;
	}

	@Override
	public BatimentRF get(Long id) {
		return db.stream()
				.filter(b -> Objects.equals(b.getId(), id))
				.findFirst()
				.orElse(null);
	}

	@Override
	public boolean exists(Long id) {
		return get(id) != null;
	}

	@Override
	public boolean exists(Long id, FlushMode flushModeOverride) {
		throw new NotImplementedException();
	}

	@Override
	public BatimentRF save(BatimentRF object) {
		db.add(object);
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
	public Iterator<BatimentRF> iterate(String query) {
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
