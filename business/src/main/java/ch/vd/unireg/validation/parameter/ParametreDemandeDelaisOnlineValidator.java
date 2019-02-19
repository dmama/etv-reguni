package ch.vd.unireg.validation.parameter;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.parametrage.DelaisAccordablesOnline;
import ch.vd.unireg.parametrage.DelaisAccordablesOnlineDIPM;
import ch.vd.unireg.parametrage.DelaisAccordablesOnlineDIPP;
import ch.vd.unireg.parametrage.ParametreDemandeDelaisOnline;
import ch.vd.unireg.validation.EntityValidatorImpl;
import ch.vd.unireg.validation.tiers.TiersValidator;

public class ParametreDemandeDelaisOnlineValidator extends EntityValidatorImpl<ParametreDemandeDelaisOnline> {
	@Override
	protected Class<ParametreDemandeDelaisOnline> getValidatedClass() {
		return ParametreDemandeDelaisOnline.class;
	}

	@Override
	@NotNull
	public ValidationResults validate(@NotNull ParametreDemandeDelaisOnline entity) {

		final ValidationResults results = new ValidationResults();

		final Set<DelaisAccordablesOnline> periodesDelais = entity.getPeriodesDelais();
		if (periodesDelais != null) {
			switch (entity.getTypeTiers()) {
			case PP:
				// on vérifie que les délais sont du bon type
				if (periodesDelais.stream().anyMatch(p -> !(p instanceof DelaisAccordablesOnlineDIPP))) {
					results.addError("Les paramètres des demandes PP contient des délais non-PP");
				}
				else {
					// on vérifie qu'ils ne se chevauchent pas
					final List<DelaisAccordablesOnlineDIPP> periodesDelaisPP = periodesDelais.stream()
							.map(DelaisAccordablesOnlineDIPP.class::cast)
							.collect(Collectors.toList());

					TiersValidator.checkNonOverlap(periodesDelaisPP,
					                               AnnulableHelper::nonAnnule,
					                               results,
					                               "délais PP");

					// les périodes de validité doivent être une année après la période fiscale de référence (parce que les DI 2017 sont envoyées en 2018)
					final Integer periodeFiscale = entity.getPeriodefiscale().getAnnee();
					periodesDelaisPP.stream()
							.filter(p -> p.getDateDebut().year() != periodeFiscale + 1)
							.findFirst()
							.ifPresent(p -> results.addError("La période de délais [" + DateRangeHelper.toDisplayString(p) + "] n'est pas valide par rapport à la période fiscale du [" + periodeFiscale + "]"));
				}

				break;
			case PM:
				if (periodesDelais.stream().anyMatch(p -> !(p instanceof DelaisAccordablesOnlineDIPM))) {
					results.addError("Les paramètres des demandes PM contient des délais non-PM");
				}
				else {
					// on vérifie que l'ordre des délais est déterministe
					final List<Integer> indexes = periodesDelais.stream()
							.filter(AnnulableHelper::nonAnnule)
							.map(DelaisAccordablesOnlineDIPM.class::cast)
							.map(DelaisAccordablesOnlineDIPM::getIndex)
							.sorted()
							.collect(Collectors.toList());
					for (int i = 1; i < indexes.size(); i++) {
						Integer previous = indexes.get(i - 1);
						Integer current = indexes.get(i);
						if (Objects.equals(previous, current)) {
							results.addError("L'index " + current + " apparaît deux fois dans les délais PM");
						}
					}
				}
				break;
			default:
				throw new IllegalArgumentException("Type de tiers inconnu = [" + entity.getTypeTiers() + "]");
			}
		}

		return results;
	}
}
