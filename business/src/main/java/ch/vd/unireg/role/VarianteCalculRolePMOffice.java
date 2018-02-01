package ch.vd.uniregctb.role;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.tiers.Entreprise;

public class VarianteCalculRolePMOffice extends VarianteCalculRolePM<RolePMOfficeResults> {

	public VarianteCalculRolePMOffice(RoleHelper roleHelper, Supplier<RolePMOfficeResults> reportBuilder) {
		super(roleHelper, reportBuilder);
	}

	@Override
	public void compile(RolePMOfficeResults rapport, Entreprise contribuable, @Nullable Integer ofsCommuneRole) throws CalculRoleException {
		if (ofsCommuneRole == null) {
			rapport.addIgnore(contribuable, RoleResults.RaisonIgnore.PAS_CONCERNE_PAR_ROLE);
		}
		else {
			rapport.addToRole(contribuable, ofsCommuneRole);
		}
	}
}
