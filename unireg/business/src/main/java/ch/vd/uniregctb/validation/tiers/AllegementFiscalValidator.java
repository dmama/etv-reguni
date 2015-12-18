package ch.vd.uniregctb.validation.tiers;

import java.math.BigDecimal;

import org.apache.commons.lang.StringUtils;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.AllegementFiscal;

public class AllegementFiscalValidator extends DateRangeEntityValidator<AllegementFiscal> {

	/**
	 * Le pourcentage d'allègement maximal autorisé
	 */
	private static final BigDecimal MAX_PERCENTAGE = BigDecimal.valueOf(100L);

	/**
	 * Le pourcentage d'allègement minimal autorisé
	 */
	private static final BigDecimal MIN_PERCENTAGE = BigDecimal.ZERO;

	private ServiceInfrastructureService infraService;

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	@Override
	protected String getEntityCategoryName() {
		return "L'allègement fiscal";
	}

	@Override
	protected Class<AllegementFiscal> getValidatedClass() {
		return AllegementFiscal.class;
	}

	@Override
	public ValidationResults validate(AllegementFiscal af) {
		final ValidationResults vr = super.validate(af);

		if (!af.isAnnule()) {
			// quelques données obligatoires
			if (af.getTypeCollectivite() == null) {
				vr.addError(String.format("%s %s n'a pas de type de collectivité assigné.", getEntityCategoryName(), getEntityDisplayString(af)));
			}
			if (af.getTypeImpot() == null) {
				vr.addError(String.format("%s %s n'a pas de type d'impôt assigné.", getEntityCategoryName(), getEntityDisplayString(af)));
			}

			// seul le type de collectivité communal donne droit à la donnée d'une commune
			if (af.getNoOfsCommune() != null && af.getTypeCollectivite() != AllegementFiscal.TypeCollectivite.COMMUNE) {
				vr.addError(String.format("%s %s indique une commune alors que la collectivité associée n'est pas de type communal.", getEntityCategoryName(), getEntityDisplayString(af)));
			}

			// si une commune est donnée, elle doit être vaudoise et valide aux dates de validité de l'allègement
			if (af.getNoOfsCommune() != null) {
				final Commune commune = infraService.getCommuneByNumeroOfs(af.getNoOfsCommune(), af.getDateDebut());
				if (commune == null) {
					vr.addError(String.format("%s %s est sur une commune (%d) inconnue dans l'infrastructure à sa date d'entrée en vigueur.", getEntityCategoryName(), getEntityDisplayString(af), af.getNoOfsCommune()));
				}
				else {
					// vaudoise ou pas vaudoise ?
					if (!commune.isVaudoise()) {
						vr.addError(String.format("%s %s est sur une commune sise hors-canton (%s - %d).", getEntityCategoryName(), getEntityDisplayString(af), commune.getNomOfficielAvecCanton(), commune.getNoOFS()));
					}

					// périodes de validité
					final DateRange validiteCommune = new DateRangeHelper.Range(commune.getDateDebutValidite(), commune.getDateFinValidite());
					final DateRange validiteAllegement = new DateRangeHelper.Range(af.getDateDebut(), af.getDateFin() == null ? getFutureBeginDate() : af.getDateFin());      // on ne considère que la période passée des allègements encore actifs
					if (!DateRangeHelper.within(validiteAllegement, validiteCommune)) {
						final String debutValiditeCommune = StringUtils.defaultIfBlank(RegDateHelper.dateToDisplayString(validiteCommune.getDateDebut()), "?");
						final String finValiditeCommune = StringUtils.defaultIfBlank(RegDateHelper.dateToDisplayString(validiteCommune.getDateFin()), "?");
						vr.addError(String.format("%s %s a une période de validité qui dépasse la période de validité de sa commune %s (%d) (%s - %s)",
						                          getEntityCategoryName(), getEntityDisplayString(af), commune.getNomOfficiel(), commune.getNoOFS(), debutValiditeCommune, finValiditeCommune));
					}
				}
			}

			// le pourcentage d'allègement doit être fixé (à une valeur raisonnable, qui plus est...)
			final BigDecimal pourcentageAllegement = af.getPourcentageAllegement();
			if (pourcentageAllegement == null) {
				vr.addError(String.format("%s %s n'a pas de pourcentage d'allègement fixé.", getEntityCategoryName(), getEntityDisplayString(af)));
			}
			else if (pourcentageAllegement.compareTo(MIN_PERCENTAGE) < 0 || pourcentageAllegement.compareTo(MAX_PERCENTAGE) > 0) {
				vr.addError(String.format("%s %s a un pourcentage d'allègement hors des limites admises (%s-%s) : %s%%.",
				                          getEntityCategoryName(), getEntityDisplayString(af), MIN_PERCENTAGE, MAX_PERCENTAGE, pourcentageAllegement));
			}
		}

		return vr;
	}
}
