package ch.vd.uniregctb.validation.fors;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.validation.EntityValidatorImpl;

/**
 * Classe de base pour les validateurs de fors fiscaux
 */
public abstract class ForFiscalValidator<T extends ForFiscal> extends EntityValidatorImpl<T> {

	private ServiceInfrastructureService serviceInfra;

	public void setServiceInfra(ServiceInfrastructureService serviceInfra) {
		this.serviceInfra = serviceInfra;
	}

	public ValidationResults validate(T ff) {

		final ValidationResults results = new ValidationResults();

		if (ff.isAnnule()) {
			return results;
		}

		final RegDate dateDebut = ff.getDateDebut();
		final RegDate dateFin = ff.getDateFin();
		final TypeAutoriteFiscale typeAutoriteFiscale = ff.getTypeAutoriteFiscale();
		final Integer numeroOfsAutoriteFiscale = ff.getNumeroOfsAutoriteFiscale();

		// La date de début doit être renseignée
		if (dateDebut == null) {
			results.addError(String.format("Le for %s possède une date de début nulle", ff));
		}
		if (typeAutoriteFiscale == null) {
			results.addError(String.format("Le for %s n'a pas de type d'autorité fiscale", ff));
		}
		if (numeroOfsAutoriteFiscale == null) {
			results.addError(String.format("Le for %s n'a pas d'autorité fiscale renseignée", ff));
		}

		// Date de début doit être avant la date de fin
		// Si "date de début" = "date de fin", c'est un cas OK (for qui dure 1 jour)
		if (dateDebut != null && dateFin != null && dateDebut.isAfter(dateFin)) {
			results.addError(String.format("Le for %s possède une date de début qui est après la date de fin: début = %s fin = %s", ff, RegDateHelper.dateToDisplayString(dateDebut), RegDateHelper.dateToDisplayString(dateFin)));
		}

		if (numeroOfsAutoriteFiscale != null) {

			if (typeAutoriteFiscale == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD || typeAutoriteFiscale == TypeAutoriteFiscale.COMMUNE_HC) {
				try {
					final Commune commune = serviceInfra.getCommuneByNumeroOfsEtendu(numeroOfsAutoriteFiscale, dateDebut);
					if (commune == null) {
						results.addError(String.format("La commune du for fiscal %s (%d) est inconnue dans l'infrastructure à la date de début du for", ff, ff.getNumeroOfsAutoriteFiscale()));
					}
					else if (!commune.isPrincipale()) {
						// vérification que la commune est bien valide sur toute la période
						final DateRange validiteCommune = new DateRangeHelper.Range(commune.getDateDebutValidite(), commune.getDateFinValidite());
						if (!DateRangeHelper.within(ff, validiteCommune)) {
							final String debutValiditeCommune = validiteCommune.getDateDebut() == null ? "?" : RegDateHelper.dateToDisplayString(validiteCommune.getDateDebut());
							final String finValiditeCommune = validiteCommune.getDateFin() == null ? "?" : RegDateHelper.dateToDisplayString(validiteCommune.getDateFin());
							results.addError(String.format("La période de validité du for fiscal %s dépasse la période de validité de la commune à laquelle il est assigné (%s - %s)",
									ff, debutValiditeCommune, finValiditeCommune));
						}
					}
					else {
						// commune faîtière de fractions...
						final String message = String.format("Le for fiscal %s ne peut pas être ouvert sur une commune faîtière de fractions de commune (ici %s / OFS %d), une fraction est attendue dans ce cas",
								ff, commune.getNomMinuscule(), numeroOfsAutoriteFiscale);
						results.addError(message);
					}
				}
				catch (InfrastructureException e) {
					results.addError("Impossible de vérifier la validité de la commune du for", e);
				}
			}
		}

		return results;
	}
}
