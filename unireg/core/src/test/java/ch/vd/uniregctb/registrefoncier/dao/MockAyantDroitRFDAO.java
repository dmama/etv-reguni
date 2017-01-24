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
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRFMembreInfo;
import ch.vd.uniregctb.registrefoncier.TiersRF;
import ch.vd.uniregctb.registrefoncier.key.AyantDroitRFKey;

public class MockAyantDroitRFDAO implements AyantDroitRFDAO {

	private final List<AyantDroitRF> db = new ArrayList<>();

	@Override
	public List<AyantDroitRF> getAll() {
		return db;
	}

	@Override
	public AyantDroitRF get(Long id) {
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
	public AyantDroitRF save(AyantDroitRF object) {
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
		return db.stream()
				.filter(a -> Objects.equals(a.getIdRF(), key.getIdRF()))
				.findFirst()
				.orElse(null);
	}

	@Override
	public Set<String> findAvecDroitsActifs() {
		return db.stream()
				.filter(a -> !a.getDroits().isEmpty())
				.map(AyantDroitRF::getIdRF)
				.collect(Collectors.toSet());
	}

	@Nullable
	@Override
	public CommunauteRFMembreInfo getCommunauteMembreInfo(long communauteId) {
		throw new NotImplementedException();
	}

	@Nullable
	@Override
	public Long getContribuableIdFor(@NotNull TiersRF tiersRF) {
		throw new NotImplementedException();
	}
}
