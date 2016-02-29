package ch.vd.uniregctb.validation.tiers;

import org.apache.commons.lang.StringUtils;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.LocalisationDatee;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Classe de validation des "localisations datées"
 */
public abstract class LocalisationDateeValidator<T extends LocalisationDatee> extends DateRangeEntityValidator<T> {

	private ServiceInfrastructureService serviceInfra;

	public void setServiceInfra(ServiceInfrastructureService serviceInfra) {
		this.serviceInfra = serviceInfra;
	}

	@Override
	public ValidationResults validate(T entity) {
		final ValidationResults results = super.validate(entity);

		if (entity.isAnnule()) {
			return results;
		}

		final RegDate dateDebut = entity.getDateDebut();
		final RegDate dateFin = entity.getDateFin();
		final TypeAutoriteFiscale typeAutoriteFiscale = entity.getTypeAutoriteFiscale();
		final Integer numeroOfsAutoriteFiscale = entity.getNumeroOfsAutoriteFiscale();

		if (typeAutoriteFiscale == null) {
			results.addError(String.format("%s %s n'a pas de type d'autorité fiscale", getEntityCategoryName(), getEntityDisplayString(entity)));
		}
		if (numeroOfsAutoriteFiscale == null) {
			results.addError(String.format("%s %s n'a pas d'autorité fiscale renseignée", getEntityCategoryName(), getEntityDisplayString(entity)));
		}

		if (numeroOfsAutoriteFiscale != null) {

			if (typeAutoriteFiscale == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD || typeAutoriteFiscale == TypeAutoriteFiscale.COMMUNE_HC) {
				try {
					final Commune commune = serviceInfra.getCommuneByNumeroOfs(numeroOfsAutoriteFiscale, dateDebut);
					if (commune == null) {
						results.addError(String.format("%s %s est sur une commune (%d) inconnue dans l'infrastructure à sa date d'entrée en vigueur", getEntityCategoryName(), getEntityDisplayString(entity), entity.getNumeroOfsAutoriteFiscale()));
					}
					else if (!commune.isPrincipale()) {
						// vérification que la commune est bien valide sur toute la période
						final DateRange validiteCommune = new DateRangeHelper.Range(commune.getDateDebutValidite(), commune.getDateFinValidite());
						final DateRange validiteDecision = new DateRangeHelper.Range(dateDebut, dateFin == null ? getFutureBeginDate() : dateFin);      // on ne considère que la période passée des fors encore actifs
						if (!DateRangeHelper.within(validiteDecision, validiteCommune)) {
							final String debutValiditeCommune = StringUtils.defaultIfBlank(RegDateHelper.dateToDisplayString(validiteCommune.getDateDebut()), "?");
							final String finValiditeCommune = StringUtils.defaultIfBlank(RegDateHelper.dateToDisplayString(validiteCommune.getDateFin()), "?");
							results.addError(String.format("%s %s a une période de validité qui dépasse la période de validité de sa commune %s (%d) (%s - %s)",
							                               getEntityCategoryName(), getEntityDisplayString(entity), commune.getNomOfficiel(), commune.getNoOFS(), debutValiditeCommune, finValiditeCommune));
						}
					}
					else {
						// commune faîtière de fractions...
						final String message = String.format("%s %s ne peut pas être sur une commune faîtière de fractions de commune (ici %s / OFS %d), une fraction est attendue dans ce cas",
						                                     getEntityCategoryName(), getEntityDisplayString(entity), commune.getNomOfficiel(), numeroOfsAutoriteFiscale);
						results.addError(message);
					}

					// ajouté le test de la cohérence de la commune avec le type d'autorité fiscale
					if (commune != null && commune.isVaudoise() && typeAutoriteFiscale != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
						results.addError(String.format("%s %s montre une incohérence entre le type d'autorité fiscale %s et la commune vaudoise %s (%d)",
						                               getEntityCategoryName(), getEntityDisplayString(entity), typeAutoriteFiscale, commune.getNomOfficiel(), commune.getNoOFS()));
					}
					else if (commune != null && !commune.isVaudoise() && typeAutoriteFiscale == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
						results.addError(String.format("%s %s montre une incohérence entre le type d'autorité fiscale %s et la commune non-vaudoise %s (%d)",
						                               getEntityCategoryName(), getEntityDisplayString(entity), typeAutoriteFiscale, commune.getNomOfficiel(), commune.getNoOFS()));
					}
				}
				catch (ServiceInfrastructureException e) {
					results.addError(String.format("Impossible de vérifier la validité de la commune pour %s", getEntityDisplayString(entity)), e);
				}
			}
			else if (typeAutoriteFiscale == TypeAutoriteFiscale.PAYS_HS) {
				try {
					final Pays pays = serviceInfra.getPays(numeroOfsAutoriteFiscale, dateDebut);
					if (pays == null) {
						results.addError(String.format("%s %s est sur un pays (%d) inconnu dans l'infrastructure à sa date d'entrée en vigueur", getEntityCategoryName(), getEntityDisplayString(entity), entity.getNumeroOfsAutoriteFiscale()));
					}
					else if (pays.isSuisse()) {
						results.addError(String.format("%s %s devrait être sur un canton (VD ou autre) suisse", getEntityCategoryName(), getEntityDisplayString(entity)));
					}
					else if (!pays.isEtatSouverain()) {
						results.addError(String.format("%s %s est sur un pays (%s, %d) qui n'est pas un état souverain, mais un territoire", getEntityCategoryName(), getEntityDisplayString(entity), pays.getNomCourt(), pays.getNoOFS()));
					}
				}
				catch (ServiceInfrastructureException e) {
					results.addError(String.format("Impossible de vérifier la validité du pays pour %s", getEntityDisplayString(entity)), e);
				}
			}
		}

		return results;
	}
}
