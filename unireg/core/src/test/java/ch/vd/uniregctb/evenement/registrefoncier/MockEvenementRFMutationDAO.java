package ch.vd.uniregctb.evenement.registrefoncier;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.FlushMode;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.utils.NotImplementedException;

public class MockEvenementRFMutationDAO implements EvenementRFMutationDAO {
	private final List<EvenementRFMutation> db = new ArrayList<>();

	@Override
	public List<EvenementRFMutation> getAll() {
		return db;
	}

	@Override
	public EvenementRFMutation get(Long id) {
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
	public EvenementRFMutation save(EvenementRFMutation object) {
		db.add(object);
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
		throw new NotImplementedException();
	}
}
