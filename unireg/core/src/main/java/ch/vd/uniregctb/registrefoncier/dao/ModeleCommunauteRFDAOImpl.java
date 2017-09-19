package ch.vd.uniregctb.registrefoncier.dao;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.common.BaseDAOImpl;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.ModeleCommunauteRF;

public class ModeleCommunauteRFDAOImpl extends BaseDAOImpl<ModeleCommunauteRF, Long> implements ModeleCommunauteRFDAO {
	protected ModeleCommunauteRFDAOImpl() {
		super(ModeleCommunauteRF.class);
	}

	@Override
	public @Nullable ModeleCommunauteRF findByMembers(@NotNull Set<? extends AyantDroitRF> membres) {
		final int hashCode = ModeleCommunauteRF.hashCode(membres);

		// on fait une sélection par hashCode (attention : il peut y avoir des collisions sur le hashCode et on doit vérifier les résultats)
		final Query query = getCurrentSession().createQuery("from ModeleCommunauteRF where membresHashCode = :hashCode and annulationDate is null");
		query.setParameter("hashCode", hashCode);

		// on recherche le modèle qui correspond aux membres spécifiés
		//noinspection unchecked
		final List<ModeleCommunauteRF> list = query.list();
		for (ModeleCommunauteRF modele : list) {
			if (modele.matches(membres)) {
				return modele;
			}
		}

		// pas trouvé
		return null;
	}

	@Override
	public long createWith(Set<Long> membresIds) {

		final Query query = getCurrentSession().createQuery("from AyantDroitRF where id in (:ids)");
		query.setParameterList("ids", membresIds);

		// on cherche les ayants-droits
		//noinspection unchecked
		final List<AyantDroitRF> membres = query.list();
		if (membresIds.size() != membres.size()) {
			final List<Long> askedIds = membresIds.stream()
					.sorted(Comparator.naturalOrder())
					.collect(Collectors.toList());
			final List<Long> foundIds = membres.stream()
					.map(AyantDroitRF::getId)
					.sorted(Comparator.naturalOrder())
					.collect(Collectors.toList());
			throw new IllegalArgumentException("Ids spécifiés = " + Arrays.toString(askedIds.toArray()) + ", membres trouvés = " + Arrays.toString(foundIds.toArray()));
		}

		// on crée la communauté
		ModeleCommunauteRF modele = new ModeleCommunauteRF();
		modele.setMembres(new HashSet<>(membres));
		modele.setMembresHashCode(ModeleCommunauteRF.hashCode(membres));
		modele = save(modele);

		return modele.getId();
	}
}
