package ch.vd.uniregctb.role;

import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;

public class VarianteCalculRolePPOffices extends VarianteCalculRolePP<RolePPOfficesResults> {

	private final Map<Integer, Integer> officeParCommune;

	public VarianteCalculRolePPOffices(RoleHelper roleHelper, Map<Integer, Integer> officeParCommune, Supplier<RolePPOfficesResults> reportBuilder) {
		super(roleHelper, reportBuilder);
		this.officeParCommune = officeParCommune;
	}

	@Override
	public void compile(RolePPOfficesResults rapport, ContribuableImpositionPersonnesPhysiques contribuable, @Nullable Integer ofsCommuneRole) {
		if (ofsCommuneRole == null) {
			rapport.addIgnore(contribuable, getRaisonIgnore(contribuable, rapport.oid != null, RegDate.get(rapport.annee, 12, 31)));
		}
		else {
			final Integer oid = officeParCommune.get(ofsCommuneRole);
			if (oid == null) {
				rapport.addIgnore(contribuable, getRaisonIgnore(contribuable, rapport.oid != null, RegDate.get(rapport.annee, 12, 31)));
			}
			else {
				rapport.addToRole(contribuable, ofsCommuneRole, oid);
			}
		}
	}
}
