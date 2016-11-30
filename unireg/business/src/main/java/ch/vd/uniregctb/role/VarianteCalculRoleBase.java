package ch.vd.uniregctb.role;

import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Contribuable;

public abstract class VarianteCalculRoleBase<T extends Contribuable, R extends RoleResults<R>> implements VarianteCalculRole<T, R> {

	protected final RoleHelper roleHelper;
	private final Supplier<R> reportBuilder;

	public VarianteCalculRoleBase(RoleHelper roleHelper, Supplier<R> reportBuilder) {
		this.roleHelper = roleHelper;
		this.reportBuilder = reportBuilder;
	}

	@NotNull
	@Override
	public final R buildRapport() {
		return reportBuilder.get();
	}

	/**
	 * Essai de reconstitution d'une raison pour laquelle le contribuable cité est ignoré dans l'extraction du rôle
	 * @param contribuable contribuable concerné
	 * @return la raison identifiée pour laquelle le contribuable est ignoré
	 */
	protected RoleResults.RaisonIgnore getRaisonIgnore(T contribuable, boolean avecSubSetCommune, RegDate dateReference) {
		// en dernier ressort, on dit qu'il n'est juste pas concerné...
		return avecSubSetCommune ? RoleResults.RaisonIgnore.PAS_CONCERNE_PAR_ROLE_SUR_COMMUNE : RoleResults.RaisonIgnore.PAS_CONCERNE_PAR_ROLE;
	}
}
