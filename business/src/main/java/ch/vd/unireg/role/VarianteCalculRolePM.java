package ch.vd.unireg.role;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.tiers.Entreprise;

public abstract class VarianteCalculRolePM<R extends RoleResults<R>> extends VarianteCalculRoleBase<Entreprise, R> {

	public VarianteCalculRolePM(RoleHelper roleHelper, Supplier<R> reportBuilder) {
		super(roleHelper, reportBuilder);
	}

	@NotNull
	@Override
	public final List<Long> getIdsContribuables(int annee, @Nullable Set<Integer> ofsCommune) {
		return roleHelper.getIdsContribuablesPM(annee, ofsCommune);
	}

	@NotNull
	@Override
	public Map<Integer, List<Entreprise>> dispatch(int annee, Collection<Entreprise> contribuables) {
		return roleHelper.dispatchPM(annee, contribuables);
	}
}
