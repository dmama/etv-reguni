package ch.vd.uniregctb.registrefoncier.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.FlushMode;
import org.hibernate.NonUniqueResultException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.key.ImmeubleRFKey;

public class MockImmeubleRFDAO implements ImmeubleRFDAO {

	private final List<ImmeubleRF> db = new ArrayList<>();

	public MockImmeubleRFDAO() {
	}

	public MockImmeubleRFDAO(ImmeubleRF... immeubles) {
		Arrays.stream(immeubles).forEach(this::save);
	}

	@Nullable
	@Override
	public ImmeubleRF find(@NotNull ImmeubleRFKey key) {
		return db.stream()
				.filter(i -> Objects.equals(i.getIdRF(), key.getIdRF()))
				.findFirst()
				.orElse(null);
	}

	@NotNull
	@Override
	public Set<String> findWithActiveSurfacesAuSol() {
		return db.stream()
				.filter(i -> i.getSurfacesAuSol().stream()
						.filter(s -> s.isValidAt(null))
						.count() > 0)
				.map(ImmeubleRF::getIdRF)
				.collect(Collectors.toSet());
	}

	@NotNull
	@Override
	public Set<String> findImmeublesActifs() {
		return db.stream()
				.filter(i -> i.getDateRadiation() == null)
				.map(ImmeubleRF::getIdRF)
				.collect(Collectors.toSet());
	}

	@Nullable
	@Override
	public ImmeubleRF findImmeubleActif(int noOfsCommune, int noParcelle, @Nullable Integer index1, @Nullable Integer index2, @Nullable Integer index3, @Nullable FlushMode flushMode) throws NonUniqueResultException {
		throw new NotImplementedException();
	}

	@Override
	public List<ImmeubleRF> getAll() {
		throw new NotImplementedException();
	}

	@Override
	public ImmeubleRF get(Long id) {
		return db.stream()
				.filter(i -> Objects.equals(i.getId(), id))
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
	public ImmeubleRF save(ImmeubleRF object) {
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
	public Iterator<ImmeubleRF> iterate(String query) {
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
