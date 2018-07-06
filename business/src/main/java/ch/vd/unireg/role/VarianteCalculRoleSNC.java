package ch.vd.unireg.role;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateConstants;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.metier.periodeexploitation.PeriodeExploitation;
import ch.vd.unireg.metier.periodeexploitation.PeriodeExploitationService;
import ch.vd.unireg.tiers.Entreprise;

public class VarianteCalculRoleSNC extends VarianteCalculRolePM<RoleSNCResults> {

	private final PeriodeExploitationService periodeExploitationService;

	public VarianteCalculRoleSNC(RoleHelper roleHelper, Supplier<RoleSNCResults> reportBuilder, PeriodeExploitationService periodeExploitationService) {
		super(roleHelper, reportBuilder);
		this.periodeExploitationService = periodeExploitationService;
	}


	@Override
	public void compile(RoleSNCResults rapport, Entreprise contribuable, @Nullable Integer ofsCommuneRole) throws CalculRoleException {
		rapport.addToRole(contribuable, ofsCommuneRole);
	}

	@Override
	public @NotNull Map<Integer, List<Entreprise>> dispatch(int annee, Collection<Entreprise> contribuables) {
		final Set<Entreprise> entreprisesSNC = contribuables.stream()
				.filter(Entreprise::isSNC)
				.filter(snc -> {
					final List<PeriodeExploitation> periodeExploitations = periodeExploitationService.determinePeriodesExploitation(snc, PeriodeExploitationService.PeriodeContext.THEORIQUE);
					return periodeExploitations.stream()
							.map(PeriodeExploitation::getDateRange)
							.anyMatch(dateRange -> dateRange.isValidAt(RegDateHelper.get(annee, DateConstants.EXTENDED_VALIDITY_RANGE)));

				}).collect(Collectors.toSet());

		return entreprisesSNC.stream()
				.collect(Collectors.toMap(ctb -> getCommunePourRoles(annee, ctb),
				                          Collections::singletonList,
				                          (c1, c2) -> Stream.concat(c1.stream(), c2.stream()).collect(Collectors.toList()),
				                          HashMap::new));

	}

	private int getCommunePourRoles(int annee, Entreprise snc) {
		final List<PeriodeExploitation> periodeExploitations = periodeExploitationService.determinePeriodesExploitation(snc, PeriodeExploitationService.PeriodeContext.THEORIQUE);
		return periodeExploitations.stream()
				.filter(periodeExploitation -> periodeExploitation.getDateRange().isValidAt(RegDateHelper.get(annee, DateConstants.EXTENDED_VALIDITY_RANGE)))
				.findAny().get().getForFiscal().getNumeroOfsAutoriteFiscale();
	}
}
