package ch.vd.unireg.validation.tiers;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tiers.AllegementFiscalCommune;

public class AllegementFiscalCommuneValidator extends AllegementFiscalCantonCommuneValidator<AllegementFiscalCommune> {

	protected ServiceInfrastructureService infraService;

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	@Override
	protected Class<AllegementFiscalCommune> getValidatedClass() {
		return AllegementFiscalCommune.class;
	}

	@Override
	@NotNull
	public ValidationResults validate(AllegementFiscalCommune afc) {
		final ValidationResults vr = super.validate(afc);
		if (!afc.isAnnule()) {
			if (afc.getNoOfsCommune() != null) {
				final Commune commune = infraService.getCommuneByNumeroOfs(afc.getNoOfsCommune(), afc.getDateDebut());
				if (commune == null) {
					vr.addError(String.format("%s %s est sur une commune (%d) inconnue dans l'infrastructure à sa date d'entrée en vigueur.", getEntityCategoryName(), getEntityDisplayString(afc), afc.getNoOfsCommune()));
				}
				else {
					// vaudoise ou pas vaudoise ?
					if (!commune.isVaudoise()) {
						vr.addError(String.format("%s %s est sur une commune sise hors-canton (%s - %d).", getEntityCategoryName(), getEntityDisplayString(afc), commune.getNomOfficielAvecCanton(), commune.getNoOFS()));
					}

					// périodes de validité
					final DateRange validiteCommune = new DateRangeHelper.Range(commune.getDateDebutValidite(), commune.getDateFinValidite());
					final DateRange validiteAllegement = new DateRangeHelper.Range(afc.getDateDebut(), afc.getDateFin() == null ? getFutureBeginDate() : afc.getDateFin());      // on ne considère que la période passée des allègements encore actifs
					if (!DateRangeHelper.within(validiteAllegement, validiteCommune)) {
						final String debutValiditeCommune = StringUtils.defaultIfBlank(RegDateHelper.dateToDisplayString(validiteCommune.getDateDebut()), "?");
						final String finValiditeCommune = StringUtils.defaultIfBlank(RegDateHelper.dateToDisplayString(validiteCommune.getDateFin()), "?");
						vr.addError(String.format("%s %s a une période de validité qui dépasse la période de validité de sa commune %s (%d) (%s - %s)",
						                          getEntityCategoryName(), getEntityDisplayString(afc), commune.getNomOfficiel(), commune.getNoOFS(), debutValiditeCommune, finValiditeCommune));
					}
				}
			}

		}
		return vr;
	}
}
