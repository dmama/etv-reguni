package ch.vd.unireg.registrefoncier.dao;

import javax.persistence.FlushModeType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.registrefoncier.CommuneRF;
import ch.vd.unireg.registrefoncier.key.CommuneRFKey;

public class MockCommuneRFDAO implements CommuneRFDAO {

	private final List<CommuneRF> db = new ArrayList<>();

	public MockCommuneRFDAO() {
	}

	public MockCommuneRFDAO(CommuneRF... communes) {
		for (int i = 0; i < communes.length; i++) {
			final CommuneRF commune = communes[i];
			commune.setId((long) i + 1);
			db.add(commune);
		}
	}

	@Nullable
	@Override
	public CommuneRF findActive(@NotNull CommuneRFKey communeRFKey) {
		return db.stream()
				.filter(c -> c.getNoRf() == communeRFKey.getNoRF())
				.filter(c -> c.isValidAt(null))
				.findFirst()
				.orElse(null);
	}

	@Override
	public List<CommuneRF> getAll() {
		return db;
	}

	@Override
	public CommuneRF get(Long id) {
		return db.stream()
				.filter(c -> Objects.equals(c.getId(), id))
				.findFirst()
				.orElse(null);
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
	public CommuneRF save(CommuneRF commune) {
		commune.setId((long) db.size() + 1);
		db.add(commune);
		return commune;
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
	public Iterator<CommuneRF> iterate(String query) {
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
