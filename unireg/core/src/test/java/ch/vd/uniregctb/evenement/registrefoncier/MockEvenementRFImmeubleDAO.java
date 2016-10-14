package ch.vd.uniregctb.evenement.registrefoncier;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.FlushMode;

import ch.vd.registre.base.utils.NotImplementedException;

public class MockEvenementRFImmeubleDAO implements EvenementRFImmeubleDAO {

	private List<EvenementRFImmeuble> db = new ArrayList<>();

	@Override
	public List<EvenementRFImmeuble> getAll() {
		return db;
	}

	@Override
	public EvenementRFImmeuble get(Long id) {
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
	public EvenementRFImmeuble save(EvenementRFImmeuble object) {
		if (object == null) {
			throw new IllegalArgumentException();
		}
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
	public Iterator<EvenementRFImmeuble> iterate(String query) {
		throw new NotImplementedException();
	}

	@Override
	public int getCount(Class<?> clazz) {
		return db.size();
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
