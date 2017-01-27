package ch.vd.uniregctb.role;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Entreprise;

public class VarianteCalculRolePMCommunes extends VarianteCalculRolePM<RolePMCommunesResults> {

	public VarianteCalculRolePMCommunes(RoleHelper roleHelper, Supplier<RolePMCommunesResults> reportBuilder) {
		super(roleHelper, reportBuilder);
	}

	@Override
	public void compile(RolePMCommunesResults rapport, Entreprise contribuable, @Nullable Integer ofsCommuneRole) throws CalculRoleException {
		if (ofsCommuneRole == null) {
			rapport.addIgnore(contribuable, getRaisonIgnore(contribuable, rapport.ofsCommune != null, RegDate.get(rapport.annee, 12, 31)));
		}
		else {
			rapport.addToRole(contribuable, ofsCommuneRole);
		}
	}
}
