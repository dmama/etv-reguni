package ch.vd.uniregctb.validation.tiers;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.regimefiscal.ServiceRegimeFiscal;
import ch.vd.uniregctb.regimefiscal.ServiceRegimeFiscalException;
import ch.vd.uniregctb.tiers.RegimeFiscal;

public class RegimeFiscalValidator extends DateRangeEntityValidator<RegimeFiscal> {

	private ServiceRegimeFiscal serviceRegimeFiscal;
	private ParametreAppService parametreAppService;

	public void setServiceRegimeFiscal(ServiceRegimeFiscal serviceRegimeFiscal) {
		this.serviceRegimeFiscal = serviceRegimeFiscal;
	}

	public void setParametreAppService(ParametreAppService parametreAppService) {
		this.parametreAppService = parametreAppService;
	}

	@Override
	protected String getEntityCategoryName() {
		return "Le régime fiscal";
	}

	@Override
	protected String getEntityDisplayString(@NotNull RegimeFiscal entity) {
		return String.format("de portée %s (%s)",
		                     Optional.ofNullable(entity.getPortee()).map(Enum::name).orElse("?"),
		                     rangeToString(entity));
	}

	@Override
	protected Class<RegimeFiscal> getValidatedClass() {
		return RegimeFiscal.class;
	}

	@Override
	public ValidationResults validate(RegimeFiscal rf) {
		final ValidationResults vr = super.validate(rf);
		if (!rf.isAnnule()) {
			if (rf.getPortee() == null) {
				vr.addError(String.format("Le régime fiscal (%s) n'a pas de portée (VD/CH).", rangeToString(rf)));
			}
			if (StringUtils.isBlank(rf.getCode())) {
				vr.addError(String.format("Le régime fiscal %s doit être associé à un code.", getEntityDisplayString(rf)));
			}
			else {
				try {
					// Contrôle de cohérence: le type de régime fiscal doit être valide durant toute la période de validité du régime fiscal.
					final TypeRegimeFiscal typeRF = serviceRegimeFiscal.getTypeRegimeFiscal(rf.getCode());
					final RegDate dateDebutValiditeDuType = RegDate.get(typeRF.getPremierePeriodeFiscaleValidite(), 1, 1);
					final RegDate dateFinValiditeDuType = typeRF.getDernierePeriodeFiscaleValidite() == null ? null : RegDate.get(typeRF.getDernierePeriodeFiscaleValidite(), 12, 31);
					final DateRange rangeValiditeDuType = new DateRangeHelper.Range(dateDebutValiditeDuType, dateFinValiditeDuType);

					// regardons la période de validité du régime fiscal qui dépasse de la période de validité du type
					final List<DateRange> depassements = DateRangeHelper.subtract(rf, Collections.singletonList(rangeValiditeDuType));
					if (depassements != null && !depassements.isEmpty()) {

						// [SIFISC-24542] on ne contrôle ceci qu'à partir de la première période de calcul d'assujettissement PM (-> tous les dépassements antérieurs sont ignorés)
						final int premierePeriodeFiscalePersonnesMorales = parametreAppService.getPremierePeriodeFiscalePersonnesMorales();
						final DateRange rangePertinence = new DateRangeHelper.Range(RegDate.get(premierePeriodeFiscalePersonnesMorales, 1, 1), null);

						final List<DateRange> depassementsPertinents = DateRangeHelper.intersections(rangePertinence, depassements);
						if (depassementsPertinents != null && !depassementsPertinents.isEmpty()) {
							vr.addWarning(String.format("Le régime fiscal %s déborde de la plage de validité du type associé '%s' (%s).",
							                            getEntityDisplayString(rf),
							                            typeRF.getLibelleAvecCode(),
							                            rangeToString(rangeValiditeDuType)));
						}
					}
				}
				catch (ServiceRegimeFiscalException e) {
					vr.addError(String.format("Régime fiscal %s : ", getEntityDisplayString(rf)), e);
				}
			}
		}
		return vr;
	}

	@Override
	protected boolean isDateDebutFutureAllowed() {
		return true;
	}

	@Override
	protected boolean isDateFinFutureAllowed() {
		return true;
	}
}
