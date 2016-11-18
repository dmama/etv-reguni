package ch.vd.uniregctb.evenement.registrefoncier;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.FlushMode;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.utils.NotImplementedException;

public class MockEvenementRFImportDAO implements EvenementRFImportDAO {

	private List<EvenementRFImport> db = new ArrayList<>();

	@Override
	public List<EvenementRFImport> getAll() {
		return db;
	}

	@Override
	public EvenementRFImport get(Long id) {
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
	public EvenementRFImport save(EvenementRFImport object) {
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
	public Iterator<EvenementRFImport> iterate(String query) {
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

	@Nullable
	@Override
	public EvenementRFImport findNextImportToProcess() {
		throw new NotImplementedException();
	}

	@Override
	public int deleteMutationsFor(long importId, int maxResults) {
		throw new NotImplementedException();
	}
}
