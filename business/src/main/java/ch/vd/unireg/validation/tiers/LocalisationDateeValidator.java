package ch.vd.unireg.validation.tiers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.EntiteOFS;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tiers.LocalisationDatee;
import ch.vd.unireg.type.TypeAutoriteFiscale;

/**
 * Classe de validation des "localisations datées"
 */
public abstract class LocalisationDateeValidator<T extends LocalisationDatee> extends DateRangeEntityValidator<T> {

	private ServiceInfrastructureService serviceInfra;

	public void setServiceInfra(ServiceInfrastructureService serviceInfra) {
		this.serviceInfra = serviceInfra;
	}

	@Override
	@NotNull
	public ValidationResults validate(@NotNull T entity) {
		final ValidationResults results = super.validate(entity);

		if (entity.isAnnule()) {
			return results;
		}

		// on ne tient pas compte des périodes futures des entités ouvertes dans le passée et encore actives
		final DateRange relevantEntityRange = new DateRangeHelper.Range(entity.getDateDebut(),
		                                                                RegDateHelper.maximum(Optional.ofNullable(entity.getDateFin()).orElseGet(LocalisationDateeValidator::getFutureBeginDate),
		                                                                                      entity.getDateDebut(),
		                                                                                      NullDateBehavior.LATEST));
		final TypeAutoriteFiscale typeAutoriteFiscale = entity.getTypeAutoriteFiscale();
		final Integer numeroOfsAutoriteFiscale = entity.getNumeroOfsAutoriteFiscale();

		if (typeAutoriteFiscale == null) {
			results.addError(String.format("%s %s n'a pas de type d'autorité fiscale", getEntityCategoryName(), getEntityDisplayString(entity)));
		}
		if (numeroOfsAutoriteFiscale == null) {
			results.addError(String.format("%s %s n'a pas d'autorité fiscale renseignée", getEntityCategoryName(), getEntityDisplayString(entity)));
		}

		if (numeroOfsAutoriteFiscale != null && typeAutoriteFiscale != null) {
			final List<Error> errors = validateAutoriteFiscale(relevantEntityRange, numeroOfsAutoriteFiscale, typeAutoriteFiscale,
			                                                   getEntityCategoryName(), getEntityDisplayString(entity), serviceInfra);
			errors.forEach(t -> {
				if (t.exception == null) {
					results.addError(t.message);
				}
				else {
					results.addError(t.message, t.exception);
				}
			});
		}

		return results;
	}

	public static class Error {
		private final String message;
		private final Exception exception;

		public Error(String message) {
			this.message = message;
			this.exception = null;
		}

		public Error(String message, Exception exception) {
			this.message = message;
			this.exception = exception;
		}

		public String getMessage() {
			return message;
		}

		public Exception getException() {
			return exception;
		}
	}

	/**
	 * Vérifie que l'autorité fiscale (numéro Ofs + type) d'un range (for fiscal ou autre) est bien valide sur toute la plage de validité du range considéré.
	 *
	 * L'algorithme tient compte de :
	 * <ul>
	 *     <li>la plage de validité du range</li>
	 *     <li>la plage de validité de ou des communes correspondantes</li>
	 *     <li>du type de communes (commune faîtière ou non)</li>
	 *     <li>de la localisation des communes (VD ou HC)</li>
	 * </ul>
	 *
	 * @param dateRange                le range à valider
	 * @param numeroOfsAutoriteFiscale le numéro OFS de l'autorité fiscale considérée
	 * @param typeAutoriteFiscale      l'autorité fiscale considérée
	 * @param entityCategoryName       le nom du type de range (e.g. "Le for fiscal")
	 * @param entityDisplayString      l'identification du range (e.g. "ForFiscalPrincipal(02.02.2010...05.10.2015)")
	 * @param serviceInfra             le service d'infrastructure
	 * @return les erreurs constatées ou une liste vide si tout est en ordre
	 */
	public static List<Error> validateAutoriteFiscale(@NotNull DateRange dateRange, @NotNull Integer numeroOfsAutoriteFiscale, @NotNull TypeAutoriteFiscale typeAutoriteFiscale,
	                                                  @NotNull String entityCategoryName, @NotNull String entityDisplayString, @NotNull ServiceInfrastructureService serviceInfra) {

		final List<Error> errors = new ArrayList<>();
		if (typeAutoriteFiscale == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD || typeAutoriteFiscale == TypeAutoriteFiscale.COMMUNE_HC) {
			try {
				// avec ces communes qui changent de nom (Bussigny ?) ou même de canton (Moutier (BE -> JU) !!), il peut-être nécessaire
				// de gérer les cas où une localisation datée est à cheval sur plusieurs instances de communes (au sens FiDoR) sans pour
				// autant se subdiviser elle-même (devra-t-on forcément créer une rupture de fors à la date du rattachement de la commune
				// de Moutier (BE) au canton du Jura ?...)

				final List<Commune> communes = serviceInfra.getCommuneHistoByNumeroOfs(numeroOfsAutoriteFiscale);
				if (communes == null || communes.isEmpty()) {
					errors.add(new Error(String.format("%s %s est sur une commune (%d) inconnue de l'infrastructure", entityCategoryName, entityDisplayString, numeroOfsAutoriteFiscale)));
				}
				else {
					final Map<Boolean, List<DateRange>> rangesByPrincipalFlag = communes.stream()
							.collect(Collectors.toMap(Commune::isPrincipale,
							                          c -> Collections.singletonList(new DateRangeHelper.Range(c)),
							                          ListUtils::union));
					rangesByPrincipalFlag.entrySet().stream()
							.filter(entry -> entry.getValue().size() > 1)
							.forEach(entry -> entry.setValue(DateRangeHelper.collateRange(entry.getValue())));

					// Dans un premier temps, on regarde le cas des communes principales (= faîtières de fractions)
					final List<DateRange> principalRanges = rangesByPrincipalFlag.get(Boolean.TRUE);
					if (principalRanges != null && !principalRanges.isEmpty()) {
						final List<DateRange> presenceSurCommunePrincipale = DateRangeHelper.intersections(dateRange, principalRanges);
						if (presenceSurCommunePrincipale != null && !presenceSurCommunePrincipale.isEmpty()) {
							// commune faîtière de fractions...
							final String nomCommune = DateRangeHelper.rangeAt(communes, presenceSurCommunePrincipale.get(0).getDateDebut()).getNomOfficiel();
							final String message = String.format("%s %s ne peut pas être sur une commune faîtière de fractions de commune (ici %s / OFS %d), une fraction est attendue dans ce cas",
							                                     entityCategoryName, entityDisplayString, nomCommune, numeroOfsAutoriteFiscale);
							errors.add(new Error(message));
						}
					}

					// ... on continue avec les communes "normales" (= non-faîtières, ou alors carrément leurs fractions)
					final List<DateRange> nonPrincipalRanges = rangesByPrincipalFlag.get(Boolean.FALSE);
					if (nonPrincipalRanges != null && !nonPrincipalRanges.isEmpty()) {

						// on recherche les périodes de l'entité non-couvertes par la communes
						final List<DateRange> nonCouverts = DateRangeHelper.subtract(dateRange, nonPrincipalRanges);
						if (nonCouverts != null && !nonCouverts.isEmpty()) {
							for (DateRange nonCouvert : nonCouverts) {
								final Commune commune;
								if (nonCouvert.getDateDebut() != null && DateRangeHelper.rangeAt(nonPrincipalRanges, nonCouvert.getDateDebut().getOneDayBefore()) != null) {
									commune = DateRangeHelper.rangeAt(communes, nonCouvert.getDateDebut().getOneDayBefore());
								}
								else if (nonCouvert.getDateFin() != null && DateRangeHelper.rangeAt(nonPrincipalRanges, nonCouvert.getDateFin().getOneDayAfter()) != null) {
									commune = DateRangeHelper.rangeAt(communes, nonCouvert.getDateFin().getOneDayAfter());
								}
								else {
									commune = null;
								}

								if (commune == null) {
									// [SIFISC-27647] le for fiscal n'intersecte avec aucune des communes
									if (communes.size() == 1) {
										final Commune commune0 = communes.get(0);
										errors.add(new Error(String.format("%s %s a une période de validité dehors de la période de validité %s de la commune %s (%d)%s",
										                                   entityCategoryName, entityDisplayString,
										                                   DateRangeHelper.toDisplayString(commune0),
										                                   commune0.getNomOfficiel(), numeroOfsAutoriteFiscale,
										                                   getDescriptionPeriode(nonCouvert))));
									}
									else {
										final String noms = String.join(" et ", communes.stream()
												.map(EntiteOFS::getNomOfficiel)
												.collect(Collectors.toList()));
										errors.add(new Error(String.format("%s %s a une période de validité dehors de la période de validité des communes %s (%d)%s",
										                                   entityCategoryName, entityDisplayString,
										                                   noms, numeroOfsAutoriteFiscale,
										                                   getDescriptionPeriode(nonCouvert))));
									}
								}
								else {
									errors.add(new Error(String.format("%s %s a une période de validité qui dépasse la période de validité %s de la commune %s (%d)%s",
									                                   entityCategoryName, entityDisplayString,
									                                   DateRangeHelper.toDisplayString(commune),
									                                   commune.getNomOfficiel(), numeroOfsAutoriteFiscale,
									                                   getDescriptionPeriode(nonCouvert))));
								}
							}
						}

						// il faut aussi vérifier la cohérence entre la commune et le type d'autorité fiscale (attention, une commune peut changer de canton !!!)
						final Map<Boolean, List<Commune>> communesParFlagVaudoise = communes.stream()
								.collect(Collectors.toMap(Commune::isVaudoise, Collections::singletonList, ListUtils::union));
						if (typeAutoriteFiscale == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
							// l'entité semble dire que la commune est vaudoise, on va aller vérifier ça...
							final List<Commune> periodesNonVaudoises = communesParFlagVaudoise.get(Boolean.FALSE);
							if (periodesNonVaudoises != null && !periodesNonVaudoises.isEmpty()) {
								final List<DateRange> intersections = DateRangeHelper.intersections(dateRange, periodesNonVaudoises);
								if (intersections != null && !intersections.isEmpty()) {
									for (DateRange periodeNonVaudoise : intersections) {
										final String nomCommune = DateRangeHelper.rangeAt(communes, periodeNonVaudoise.getDateFin()).getNomOfficiel();
										errors.add(new Error(String.format("%s %s montre une incohérence entre son type d'autorité fiscale %s et la commune non-vaudoise %s (%d)%s",
										                                   entityCategoryName, entityDisplayString,
										                                   typeAutoriteFiscale,
										                                   nomCommune, numeroOfsAutoriteFiscale,
										                                   getDescriptionPeriode(periodeNonVaudoise))));
									}
								}
							}
						}
						else {
							// l'entité semble penser que la commune est hors-canton, il faut tout de même vérifier aussi
							final List<Commune> periodesVaudoises = communesParFlagVaudoise.get(Boolean.TRUE);
							if (periodesVaudoises != null && !periodesVaudoises.isEmpty()) {
								final List<DateRange> intersections = DateRangeHelper.intersections(dateRange, periodesVaudoises);
								if (intersections != null && !intersections.isEmpty()) {
									for (DateRange periodeNonVaudoise : intersections) {
										final String nomCommune = DateRangeHelper.rangeAt(communes, periodeNonVaudoise.getDateFin()).getNomOfficiel();
										errors.add(new Error(String.format("%s %s montre une incohérence entre son type d'autorité fiscale %s et la commune vaudoise %s (%d)%s",
										                                   entityCategoryName, entityDisplayString,
										                                   typeAutoriteFiscale,
										                                   nomCommune, numeroOfsAutoriteFiscale,
										                                   getDescriptionPeriode(periodeNonVaudoise))));
									}
								}
							}
						}
					}
				}
			}
			catch (ServiceInfrastructureException e) {
				errors.add(new Error(String.format("Impossible de vérifier la validité de la commune pour %s", entityDisplayString), e));
			}
		}
		else if (typeAutoriteFiscale == TypeAutoriteFiscale.PAYS_HS) {
			try {
				final Pays pays = serviceInfra.getPays(numeroOfsAutoriteFiscale, dateRange.getDateDebut());
				if (pays == null) {
					errors.add(new Error(String.format("%s %s est sur un pays (%d) inconnu dans l'infrastructure à sa date d'entrée en vigueur", entityCategoryName, entityDisplayString, numeroOfsAutoriteFiscale)));
				}
				else if (pays.isSuisse()) {
					errors.add(new Error(String.format("%s %s devrait être sur un canton (VD ou autre) suisse", entityCategoryName, entityDisplayString)));
				}
				else if (!pays.isEtatSouverain()) {
					errors.add(new Error(String.format("%s %s est sur un pays (%s, %d) qui n'est pas un état souverain, mais un territoire", entityCategoryName, entityDisplayString, pays.getNomCourt(), pays.getNoOFS())));
				}
			}
			catch (ServiceInfrastructureException e) {
				errors.add(new Error(String.format("Impossible de vérifier la validité du pays pour %s", entityDisplayString), e));
			}
		}

		return errors;
	}

	private static String getDescriptionPeriode(DateRange range) {
		final RegDate aujourdhui = getFutureBeginDate();
		final Predicate<RegDate> isNull = date -> date == null || date == aujourdhui;
		final boolean debutNull = isNull.test(range.getDateDebut());
		final boolean finNull = isNull.test(range.getDateFin());
		if (debutNull && finNull) {
			return StringUtils.EMPTY;
		}
		if (debutNull) {
			return String.format(" jusqu'au %s", RegDateHelper.dateToDisplayString(range.getDateFin()));
		}
		if (finNull) {
			return String.format(" depuis le %s", RegDateHelper.dateToDisplayString(range.getDateDebut()));
		}
		return String.format(" entre le %s et le %s",
		                     RegDateHelper.dateToDisplayString(range.getDateDebut()),
		                     RegDateHelper.dateToDisplayString(range.getDateFin()));
	}
}
