package ch.vd.unireg.role;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.type.MotifRattachement;

public abstract class VarianteCalculRolePP<R extends RoleResults<R>> extends VarianteCalculRoleBase<ContribuableImpositionPersonnesPhysiques, R> {

	public VarianteCalculRolePP(RoleHelper roleHelper, Supplier<R> reportBuilder) {
		super(roleHelper, reportBuilder);
	}

	@NotNull
	@Override
	public final List<Long> getIdsContribuables(int annee, @Nullable Set<Integer> ofsCommune) {
		return roleHelper.getIdsContribuablesPP(annee, ofsCommune);
	}

	@NotNull
	@Override
	public final Map<Integer, List<ContribuableImpositionPersonnesPhysiques>> dispatch(int annee, Collection<ContribuableImpositionPersonnesPhysiques> contribuables) {
		return roleHelper.dispatchPP(annee, contribuables);
	}

	@Override
	protected RoleResults.RaisonIgnore getRaisonIgnore(ContribuableImpositionPersonnesPhysiques contribuable, boolean avecSubSetCommune, RegDate dateReference) {
		final RegDate debutAnneeReference = RegDate.get(dateReference.year(), 1, 1);

		// sourcier gris...
		if (roleHelper.isSourcierGris(contribuable, dateReference)) {
			return RoleResults.RaisonIgnore.SOURCIER_GRIS;
		}

		// diplomate suisse ?
		final ForFiscalPrincipal ffp = contribuable.getDernierForFiscalPrincipalAvant(dateReference);
		if (ffp != null && RegDateHelper.isAfterOrEqual(ffp.getDateFin(), debutAnneeReference, NullDateBehavior.LATEST) && ffp.getMotifRattachement() == MotifRattachement.DIPLOMATE_SUISSE) {
			return RoleResults.RaisonIgnore.DIPLOMATE_SUISSE;
		}

		// en dernier ressort, on se réfère au calcul général
		return super.getRaisonIgnore(contribuable, avecSubSetCommune, dateReference);
	}
}
