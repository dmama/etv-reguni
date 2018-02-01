package ch.vd.unireg.validation.tiers;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.foncier.AllegementFoncier;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.RegistreFoncierService;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.ForsParType;
import ch.vd.unireg.validation.ValidationService;

public abstract class ContribuableImpositionPersonnesMoralesValidator<T extends ContribuableImpositionPersonnesMorales> extends ContribuableValidator<T> {

	protected ParametreAppService parametreAppService;
	private RegistreFoncierService registreFoncierService;

	public void setParametreAppService(ParametreAppService parametreAppService) {
		this.parametreAppService = parametreAppService;
	}

	public void setRegistreFoncierService(RegistreFoncierService registreFoncierService) {
		this.registreFoncierService = registreFoncierService;
	}

	@Override
	public ValidationResults validate(T ctb) {
		final ValidationResults vr = super.validate(ctb);
		if (!ctb.isAnnule()) {
			vr.merge(validateAllegementsFonciers(ctb));
		}
		return vr;
	}

	@Override
	protected ValidationResults validateFors(T ctb) {
		final ValidationResults results = super.validateFors(ctb);

		final ForsParType fors = ctb.getForsParType(true);

		// Les fors principaux PP ne sont pas autorisés
		for (ForFiscalPrincipalPP forPP : fors.principauxPP) {
			results.addError("Le for " + forPP + " n'est pas un type de for autorisé sur un contribuable de type PM.");
		}
		return results;
	}

	@Override
	protected boolean isPeriodeImpositionExpected(DeclarationImpotOrdinaire di) {
		return super.isPeriodeImpositionExpected(di)
				&& di.getPeriode() != null
				&& di.getPeriode().getAnnee() >= parametreAppService.getPremierePeriodeFiscalePersonnesMorales();
	}

	private ValidationResults validateAllegementsFonciers(T contribuable) {

		final ValidationResults vr = new ValidationResults();

		// validons d'abord la cohérence d'ensemble avant de valider les allègements un par un
		final Map<AllegementFoncierGroupKey, List<AllegementFoncier>> groups = contribuable.getAllegementsFonciersNonAnnulesTries(AllegementFoncier.class).stream()
				.collect(Collectors.toMap(AllegementFoncierGroupKey::new,
				                          Collections::singletonList,
				                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList())));

		// dans chaque groupe, il ne doit pas y avoir de chevauchement
		for (Map.Entry<AllegementFoncierGroupKey, List<AllegementFoncier>> entry : groups.entrySet()) {
			final List<AllegementFoncier> group = entry.getValue();
			if (group.size() > 1) {
				final List<DateRange> overlaps = DateRangeHelper.overlaps(group);
				if (overlaps != null && !overlaps.isEmpty()) {
					final AllegementFoncierGroupKey key = entry.getKey();
					for (DateRange overlap : overlaps) {
						vr.addError(String.format("La période %s est couverte par plusieurs allègements fonciers de type %s sur l'immeuble %s de la commune %s",
						                          DateRangeHelper.toDisplayString(overlap),
						                          key.typeImpot,
						                          StringUtils.defaultIfBlank(registreFoncierService.getNumeroParcelleComplet(key.immeuble, overlap.getDateFin()), "?"),
						                          Optional.ofNullable(registreFoncierService.getCommune(key.immeuble, overlap.getDateFin())).map(Commune::getNomOfficiel).orElse("?")));
					}
				}
			}
		}

		// et puis il faut aussi valider chacun des allègements pour lui-même
		final ValidationService validationService = getValidationService();
		groups.values().stream()
				.flatMap(List::stream)
				.filter(AnnulableHelper::nonAnnule)
				.map(validationService::validate)
				.forEach(vr::merge);

		return vr;
	}

	private static final class AllegementFoncierGroupKey {

		private final AllegementFoncier.TypeImpot typeImpot;
		private final ImmeubleRF immeuble;
		private final Long idImmeuble;

		public AllegementFoncierGroupKey(AllegementFoncier af) {
			this.typeImpot = af.getTypeImpot();
			this.immeuble = af.getImmeuble();
			this.idImmeuble = this.immeuble != null ? this.immeuble.getId() : null;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final AllegementFoncierGroupKey that = (AllegementFoncierGroupKey) o;
			return typeImpot == that.typeImpot && (idImmeuble != null ? idImmeuble.equals(that.idImmeuble) : that.idImmeuble == null);
		}

		@Override
		public int hashCode() {
			int result = typeImpot != null ? typeImpot.hashCode() : 0;
			result = 31 * result + (idImmeuble != null ? idImmeuble.hashCode() : 0);
			return result;
		}
	}
}
