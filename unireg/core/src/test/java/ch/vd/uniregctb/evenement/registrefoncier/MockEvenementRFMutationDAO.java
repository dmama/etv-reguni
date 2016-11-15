package ch.vd.uniregctb.evenement.registrefoncier;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang.ArrayUtils;
import org.hibernate.FlushMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.utils.NotImplementedException;

public class MockEvenementRFMutationDAO implements EvenementRFMutationDAO {
	private final List<EvenementRFMutation> db = new ArrayList<>();

	@Override
	public List<EvenementRFMutation> getAll() {
		return db;
	}

	@Override
	public EvenementRFMutation get(Long id) {
		return db.stream()
				.filter(m -> Objects.equals(m.getId(),id))
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
	public EvenementRFMutation save(EvenementRFMutation object) {
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
		final Iterator<EvenementRFMutation> iterator = db.iterator();
		while (iterator.hasNext()){
			if (iterator.next().getId().equals(id)) {
				iterator.remove();
				break;
			}
		}
	}

	@Override
	public void removeAll() {
		throw new NotImplementedException();
	}

	@Override
	public Iterator<EvenementRFMutation> iterate(String query) {
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

	@NotNull
	@Override
	public List<Long> findIds(long importId, @NotNull EvenementRFMutation.TypeEntite typeEntite, @NotNull EtatEvenementRF... etats) {
		return db.stream()
				.filter(mut -> mut.getParentImport().getId().equals(importId))
				.filter(mut -> mut.getTypeEntite() == typeEntite)
				.filter(mut -> ArrayUtils.contains(etats, mut.getEtat()))
				.map(EvenementRFMutation::getId)
				.collect(Collectors.toList());
	}

	@Nullable
	@Override
	public EvenementRFMutation find(long importId, @NotNull EvenementRFMutation.TypeEntite typeEntite, @NotNull String idImmeubleRF) {
		return db.stream()
				.filter(mut -> mut.getParentImport().getId().equals(importId))
				.filter(mut -> mut.getTypeEntite() == typeEntite)
				.filter(mut -> Objects.equals(mut.getIdRF(), idImmeubleRF))
				.findFirst()
				.orElse(null);
	}
}
