package ch.vd.uniregctb.evenement.registrefoncier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.hibernate.FlushMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.common.ParamPagination;

public class MockEvenementRFImportDAO implements EvenementRFImportDAO {

	private List<EvenementRFImport> db = new ArrayList<>();

	public MockEvenementRFImportDAO() {
	}

	public MockEvenementRFImportDAO(EvenementRFImport... imps) {
		Arrays.stream(imps).forEach(this::save);
	}

	@Override
	public List<EvenementRFImport> getAll() {
		return new ArrayList<>(db);
	}

	@Override
	public EvenementRFImport get(Long id) {
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
		for (Iterator<EvenementRFImport> iterator = db.iterator(); iterator.hasNext(); ) {
			final EvenementRFImport imp = iterator.next();
			if (imp.getId().equals(id)) {
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
	public EvenementRFImport findNextImportToProcess(TypeImportRF type) {
		throw new NotImplementedException();
	}

	@Nullable
	@Override
	public EvenementRFImport findOldestImportWithUnprocessedMutations(long importId, TypeImportRF type) {
		throw new NotImplementedException();
	}

	@Nullable
	@Override
	public RegDate findValueDateOfOldestProcessedImport(long importId, TypeImportRF type) {
		throw new NotImplementedException();
	}

	@Override
	public @NotNull List<EvenementRFImport> find(@NotNull TypeImportRF type) {
		return db.stream()
				.filter(i -> i.getType() == type)
				.collect(Collectors.toList());
	}

	@Override
	public List<EvenementRFImport> find(@Nullable List<EtatEvenementRF> etats, @NotNull ParamPagination pagination) {
		throw new NotImplementedException();
	}

	@Override
	public int count(@Nullable List<EtatEvenementRF> etats) {
		throw new NotImplementedException();
	}

	@Override
	public int fixAbnormalJVMTermination() {
		db.stream()
				.filter(i -> i.getEtat() == EtatEvenementRF.EN_TRAITEMENT)
				.forEach(i -> {
					i.setEtat(EtatEvenementRF.EN_ERREUR);
					i.setErrorMessage("Abnormal JVM termination.");
				});
		return 0;
	}
}
