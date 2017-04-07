package ch.vd.uniregctb.evenement.registrefoncier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.hibernate.FlushMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.common.ParamPagination;

public class MockEvenementRFMutationDAO implements EvenementRFMutationDAO {
	private final List<EvenementRFMutation> db = new ArrayList<>();

	public MockEvenementRFMutationDAO() {
	}

	public MockEvenementRFMutationDAO(EvenementRFMutation... muts) {
		Collections.addAll(db, muts);
	}

	@Override
	public List<EvenementRFMutation> getAll() {
		return db;
	}

	@Override
	public EvenementRFMutation get(Long id) {
		return db.stream()
				.filter(m -> Objects.equals(m.getId(), id))
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
		while (iterator.hasNext()) {
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
	public List<Long> findIds(long importId, @NotNull TypeEntiteRF typeEntite, @NotNull Collection<EtatEvenementRF> etats, @NotNull Collection<TypeMutationRF> typesMutation) {
		return db.stream()
				.filter(mut -> mut.getParentImport().getId().equals(importId))
				.filter(mut -> mut.getTypeEntite() == typeEntite)
				.filter(mut -> etats.contains(mut.getEtat()))
				.filter(mut -> typesMutation.contains(mut.getTypeMutation()))
				.map(EvenementRFMutation::getId)
				.collect(Collectors.toList());
	}

	@Override
	public long count(long importId, @NotNull TypeEntiteRF typeEntite, @NotNull TypeMutationRF typeMutation) {
		return db.stream()
				.filter(mut -> mut.getParentImport().getId().equals(importId))
				.filter(mut -> mut.getTypeEntite() == typeEntite)
				.filter(mut -> mut.getTypeMutation() == typeMutation)
				.count();
	}

	@NotNull
	@Override
	public Iterator<String> findRfIds(long importId, @NotNull TypeEntiteRF typeEntite, @NotNull TypeMutationRF typeMutation) {
		return db.stream()
				.filter(mut -> mut.getParentImport().getId().equals(importId))
				.filter(mut -> mut.getTypeEntite() == typeEntite)
				.filter(mut -> mut.getTypeMutation() == typeMutation)
				.map(EvenementRFMutation::getIdRF)
				.collect(Collectors.toList())
				.iterator();
	}

	@Nullable
	@Override
	public EvenementRFMutation find(long importId, @NotNull TypeEntiteRF typeEntite, @NotNull TypeMutationRF typeMutation, @NotNull String idRF) {
		return db.stream()
				.filter(mut -> mut.getParentImport().getId().equals(importId))
				.filter(mut -> mut.getTypeEntite() == typeEntite)
				.filter(mut -> mut.getTypeMutation() == typeMutation)
				.filter(mut -> Objects.equals(mut.getIdRF(), idRF))
				.findFirst()
				.orElse(null);
	}

	@Override
	public int forceMutation(long mutId) {
		throw new NotImplementedException();
	}

	@Override
	public int forceMutations(long importId) {
		throw new NotImplementedException();
	}

	@Override
	public Map<EtatEvenementRF, Integer> countByState(long importId) {
		final Map<EtatEvenementRF, Integer> map = new HashMap<>();
		db.stream()
				.filter(mut -> mut.getParentImport().getId().equals(importId))
				.forEach(mut -> map.compute(mut.getEtat(), (k, v) -> v == null ? 1 : v + 1));
		return map;
	}

	@Override
	public int deleteMutationsFor(long importId, int maxResults) {
		throw new NotImplementedException();
	}

	@Nullable
	@Override
	public Long findNextMutationsToProcess() {
		throw new NotImplementedException();
	}

	@NotNull
	@Override
	public List<EvenementRFMutation> find(long importId, @Nullable List<EtatEvenementRF> etats, @NotNull ParamPagination pagination) {
		throw new NotImplementedException();
	}

	@Override
	public int count(long importId, @Nullable List<EtatEvenementRF> etats) {
		throw new NotImplementedException();
	}
}
