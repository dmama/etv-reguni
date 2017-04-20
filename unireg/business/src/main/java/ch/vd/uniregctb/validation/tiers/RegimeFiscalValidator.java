package ch.vd.uniregctb.validation.tiers;

import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.uniregctb.regimefiscal.ServiceRegimeFiscal;
import ch.vd.uniregctb.tiers.RegimeFiscal;

public class RegimeFiscalValidator extends DateRangeEntityValidator<RegimeFiscal> {

	private ServiceRegimeFiscal serviceRegimeFiscal;

	public void setServiceRegimeFiscal(ServiceRegimeFiscal serviceRegimeFiscal) {
		this.serviceRegimeFiscal = serviceRegimeFiscal;
	}

	@Override
	protected String getEntityCategoryName() {
		return "Le régime fiscal";
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
				vr.addError("La portée est un attribut obligatoire pour un régime fiscal.");
			}
			if (StringUtils.isBlank(rf.getCode())) {
				vr.addError("Le code d'un régime fiscal est un attribut obligatoire.");
			}

			// Contrôle de cohérence: le type de régime fiscal doit être valide durant toute la période de validité du régime fiscal.
			final TypeRegimeFiscal typeRF = rf.getCode() != null ? serviceRegimeFiscal.getTypeRegimeFiscal(rf.getCode()) : null;
			if (typeRF != null) {
				final RegDate dateDebutValiditeDuType = RegDate.get(typeRF.getPremierePeriodeFiscaleValidite(), 1, 1);
				final RegDate dateFinValiditeDuType = typeRF.getDernierePeriodeFiscaleValidite() == null ? null : RegDate.get(typeRF.getDernierePeriodeFiscaleValidite(), 12, 31);

				if (RegDateHelper.isBefore(rf.getDateDebut(), dateDebutValiditeDuType, NullDateBehavior.EARLIEST) ||
						RegDateHelper.isAfter(rf.getDateFin(), dateFinValiditeDuType, NullDateBehavior.LATEST)) {
					vr.addWarning(String.format("Le régime fiscal (%s - %s) déborde de la plage de validié du type %s (%d - %d).",
					                            RegDateHelper.dateToDisplayString(rf.getDateDebut()), RegDateHelper.dateToDisplayString(rf.getDateFin()),
					                            typeRF.getLibelleAvecCode(),
					                            typeRF.getPremierePeriodeFiscaleValidite(), typeRF.getDernierePeriodeFiscaleValidite()));
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
