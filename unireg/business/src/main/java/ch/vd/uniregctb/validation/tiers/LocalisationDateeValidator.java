package ch.vd.uniregctb.validation.tiers;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

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
import ch.vd.uniregctb.validation.EntityValidatorImpl;

/**
 * Classe de validation des "localisations datées"
 */
public abstract class LocalisationDateeValidator<T extends LocalisationDatee> extends EntityValidatorImpl<T> {

	private ServiceInfrastructureService serviceInfra;

	public void setServiceInfra(ServiceInfrastructureService serviceInfra) {
		this.serviceInfra = serviceInfra;
	}

	@Override
	public ValidationResults validate(T entity) {
		final ValidationResults results = new ValidationResults();

		if (entity.isAnnule()) {
			return results;
		}

		final RegDate dateDebut = entity.getDateDebut();
		final RegDate dateFin = entity.getDateFin();
		final TypeAutoriteFiscale typeAutoriteFiscale = entity.getTypeAutoriteFiscale();
		final Integer numeroOfsAutoriteFiscale = entity.getNumeroOfsAutoriteFiscale();

		// La date de début doit être renseignée
		if (dateDebut == null) {
			results.addError(String.format("%s %s possède une date de début nulle", getEntityDisplayName(), entity));
		}
		else if (dateDebut.isAfter(getFutureBeginDate())) {
			// la date de début d'une entité ne doit en aucun cas être dans le futur !
			results.addError(String.format("%s %s possède une date de début dans le futur", getEntityDisplayName(), entity));
		}
		if (dateFin != null && !isDateFermetureFutureAllowed() && dateFin.isAfter(getFutureBeginDate())) {
			// la date de fin non plus, ne doit jamais être dans le futur !
			results.addError(String.format("%s %s possède une date de fin dans le futur", getEntityDisplayName(), entity));
		}
		if (typeAutoriteFiscale == null) {
			results.addError(String.format("%s %s n'a pas de type d'autorité fiscale", getEntityDisplayName(), entity));
		}
		if (numeroOfsAutoriteFiscale == null) {
			results.addError(String.format("%s %s n'a pas d'autorité fiscale renseignée", getEntityDisplayName(), entity));
		}

		// Date de début doit être avant la date de fin
		// Si "date de début" = "date de fin", c'est un cas OK (durée d'un jour)
		if (dateDebut != null && dateFin != null && dateDebut.isAfter(dateFin)) {
			results.addError(String.format("%s %s possède une date de début qui est après la date de fin: début = %s fin = %s", getEntityDisplayName(), entity, RegDateHelper.dateToDisplayString(dateDebut), RegDateHelper.dateToDisplayString(dateFin)));
		}

		if (numeroOfsAutoriteFiscale != null) {

			if (typeAutoriteFiscale == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD || typeAutoriteFiscale == TypeAutoriteFiscale.COMMUNE_HC) {
				try {
					final Commune commune = serviceInfra.getCommuneByNumeroOfs(numeroOfsAutoriteFiscale, dateDebut);
					if (commune == null) {
						results.addError(String.format("%s %s est sur une commune (%d) inconnue dans l'infrastructure à sa date d'entrée en vigueur", getEntityDisplayName(), entity, entity.getNumeroOfsAutoriteFiscale()));
					}
					else if (!commune.isPrincipale()) {
						// vérification que la commune est bien valide sur toute la période
						final DateRange validiteCommune = new DateRangeHelper.Range(commune.getDateDebutValidite(), commune.getDateFinValidite());
						final DateRange validiteDecision = new DateRangeHelper.Range(entity.getDateDebut(), entity.getDateFin() == null ? getFutureBeginDate() : entity.getDateFin());      // on ne considère que la période passée des fors encore actifs
						if (!DateRangeHelper.within(validiteDecision, validiteCommune)) {
							final String debutValiditeCommune = StringUtils.defaultIfBlank(RegDateHelper.dateToDisplayString(validiteCommune.getDateDebut()), "?");
							final String finValiditeCommune = StringUtils.defaultIfBlank(RegDateHelper.dateToDisplayString(validiteCommune.getDateFin()), "?");
							results.addError(String.format("%s %s a une période de validité qui dépasse la période de validité de sa commune %s (%d) (%s - %s)",
							                               getEntityDisplayName(), entity, commune.getNomOfficiel(), commune.getNoOFS(), debutValiditeCommune, finValiditeCommune));
						}
					}
					else {
						// commune faîtière de fractions...
						final String message = String.format("%s %s ne peut pas être sur une commune faîtière de fractions de commune (ici %s / OFS %d), une fraction est attendue dans ce cas",
						                                     getEntityDisplayName(), entity, commune.getNomOfficiel(), numeroOfsAutoriteFiscale);
						results.addError(message);
					}

					// ajouté le test de la cohérence de la commune avec le type d'autorité fiscale
					if (commune != null && commune.isVaudoise() && typeAutoriteFiscale != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
						results.addError(String.format("%s %s montre une incohérence entre le type d'autorité fiscale %s et la commune vaudoise %s (%d)",
						                               getEntityDisplayName(), entity, typeAutoriteFiscale, commune.getNomOfficiel(), commune.getNoOFS()));
					}
					else if (commune != null && !commune.isVaudoise() && typeAutoriteFiscale == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
						results.addError(String.format("%s %s montre une incohérence entre le type d'autorité fiscale %s et la commune non-vaudoise %s (%d)",
						                               getEntityDisplayName(), entity, typeAutoriteFiscale, commune.getNomOfficiel(), commune.getNoOFS()));
					}
				}
				catch (ServiceInfrastructureException e) {
					results.addError(String.format("Impossible de vérifier la validité de la commune pour %s", entity), e);
				}
			}
			else if (typeAutoriteFiscale == TypeAutoriteFiscale.PAYS_HS) {
				try {
					final Pays pays = serviceInfra.getPays(numeroOfsAutoriteFiscale, dateDebut);
					if (pays == null) {
						results.addError(String.format("%s %s est sur un pays (%d) inconnu dans l'infrastructure à sa date d'entrée en vigueur", getEntityDisplayName(), entity, entity.getNumeroOfsAutoriteFiscale()));
					}
					else if (pays.isSuisse()) {
						results.addError(String.format("%s %s devrait être sur un canton (VD ou autre) suisse", getEntityDisplayName(), entity));
					}
					else if (!pays.isEtatSouverain()) {
						results.addError(String.format("%s %s est sur un pays (%s, %d) qui n'est pas un état souverain, mais un territoire", getEntityDisplayName(), entity, pays.getNomCourt(), pays.getNoOFS()));
					}
				}
				catch (ServiceInfrastructureException e) {
					results.addError(String.format("Impossible de vérifier la validité du pays pour %s", entity), e);
				}
			}
		}

		return results;
	}

	/**
	 * Interface utilisée pour faire en sorte que la validation d'une décision soit faite
	 * en supposant une date du jour spécifique (pour les tests)
	 */
	private interface ReferenceDateAccessor {
		RegDate getReferenceDate();
	}

	private static final ReferenceDateAccessor CURRENT_DATE_ACCESSOR = new ReferenceDateAccessor() {
		@Override
		public RegDate getReferenceDate() {
			return RegDate.get();
		}
	};

	private static final ThreadLocal<ReferenceDateAccessor> futureBeginDateAccessors = new ThreadLocal<ReferenceDateAccessor>() {
		@Override
		protected ReferenceDateAccessor initialValue() {
			return CURRENT_DATE_ACCESSOR;
		}
	};

	/**
	 * Méthode utilisable dans les tests et qui fait en sorte que la date de "début du futur" soit la date donnée
	 * @param date date qui devra dorénavant être considérée comme la date du jour pour ce qui concerne la validation des décisions
	 */
	public static void setFutureBeginDate(@Nullable final RegDate date) {
		if (date == null) {
			futureBeginDateAccessors.remove();
		}
		else {
			futureBeginDateAccessors.set(new ReferenceDateAccessor() {
				@Override
				public RegDate getReferenceDate() {
					return date;
				}
			});
		}
	}

	private static ReferenceDateAccessor getFutureBeginDateAccessor() {
		return futureBeginDateAccessors.get();
	}

	protected static RegDate getFutureBeginDate() {
		final ReferenceDateAccessor accessor = getFutureBeginDateAccessor();
		return accessor.getReferenceDate();
	}

	/**
	 * [SIFISC-10141] Certains fors fiscaux (sur les DPI) peuvent avoir des dates de fermeture dans le futur
	 * @return <code>false</code> par défaut, surchargeable
	 */
	protected boolean isDateFermetureFutureAllowed() {
		return false;
	}

	/**
	 * @return une chaîne comme "Le for fiscal", ou "La décision ACI", ou...
	 */
	protected abstract String getEntityDisplayName();

}
