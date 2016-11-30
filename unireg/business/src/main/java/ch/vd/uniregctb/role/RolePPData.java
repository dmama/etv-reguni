package ch.vd.uniregctb.role;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.common.NomPrenom;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;

public class RolePPData extends RoleData {

	public final List<NomPrenom> nomsPrenoms;
	public final List<String> nosAvs;

	public RolePPData(ContribuableImpositionPersonnesPhysiques contribuable, int ofsCommune, int annee, AdresseService adresseService, ServiceInfrastructureService infrastructureService, TiersService tiersService) {
		super(contribuable, ofsCommune, annee, adresseService, infrastructureService);
		if (contribuable instanceof PersonnePhysique) {
			this.nomsPrenoms = buildNomPrenom((PersonnePhysique) contribuable, tiersService);
			this.nosAvs = buildNoAvs((PersonnePhysique) contribuable, tiersService);
		}
		else if (contribuable instanceof MenageCommun) {
			this.nomsPrenoms = buildNomsPrenoms((MenageCommun) contribuable, annee, tiersService);
			this.nosAvs = buildNosAvs((MenageCommun) contribuable, annee, tiersService);
		}
		else {
			throw new IllegalArgumentException("Type bizarre de contribuable PP : " + contribuable.getClass().getSimpleName());
		}
	}

	@NotNull
	private static List<NomPrenom> buildNomPrenom(PersonnePhysique pp, TiersService tiersService) {
		final NomPrenom nomPrenom = tiersService.getDecompositionNomPrenom(pp, false);
		return Optional.ofNullable(nomPrenom).map(Collections::singletonList).orElseGet(Collections::emptyList);
	}

	@NotNull
	private static List<String> buildNoAvs(PersonnePhysique pp, TiersService tiersService) {
		final String avs = tiersService.getNumeroAssureSocial(pp);
		return Optional.ofNullable(avs).map(Collections::singletonList).orElseGet(Collections::emptyList);
	}

	@NotNull
	private static List<NomPrenom> buildNomsPrenoms(MenageCommun mc, int annee, TiersService tiersService) {
		final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(mc, annee);
		return Stream.of(couple.getPrincipal(), couple.getConjoint())
				.filter(Objects::nonNull)
				.map(pp -> buildNomPrenom(pp, tiersService))
				.flatMap(List::stream)
				.collect(Collectors.toList());
	}

	@NotNull
	private static List<String> buildNosAvs(MenageCommun mc, int annee, TiersService tiersService) {
		final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(mc, annee);
		return Stream.of(couple.getPrincipal(), couple.getConjoint())
				.filter(Objects::nonNull)
				.map(pp -> buildNoAvs(pp, tiersService))
				.map(list -> list.isEmpty() ? StringUtils.EMPTY : list.get(0))       // histoire de pouvoir associer le NAVS au conjoint même si le principal n'en a pas
				.collect(Collectors.toList());
	}
}
