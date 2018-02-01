package ch.vd.unireg.validation.tiers;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang.StringUtils;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
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
	public ValidationResults validate(T entity) {
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

		if (numeroOfsAutoriteFiscale != null) {

			if (typeAutoriteFiscale == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD || typeAutoriteFiscale == TypeAutoriteFiscale.COMMUNE_HC) {
				try {
					// avec ces communes qui changent de nom (Bussigny ?) ou même de canton (Moutier (BE -> JU) !!), il peut-être nécessaire
					// de gérer les cas où une localisation datée est à cheval sur plusieurs instances de communes (au sens FiDoR) sans pour
					// autant se subdiviser elle-même (devra-t-on forcément créer une rupture de fors à la date du rattachement de la commune
					// de Moutier (BE) au canton du Jura ?...)

					final List<Commune> communes = serviceInfra.getCommuneHistoByNumeroOfs(numeroOfsAutoriteFiscale);
					if (communes == null || communes.isEmpty()) {
						results.addError(String.format("%s %s est sur une commune (%d) inconnue de l'infrastructure", getEntityCategoryName(), getEntityDisplayString(entity), entity.getNumeroOfsAutoriteFiscale()));
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
							final List<DateRange> presenceSurCommunePrincipale = DateRangeHelper.intersections(relevantEntityRange, principalRanges);
							if (presenceSurCommunePrincipale != null && !presenceSurCommunePrincipale.isEmpty()) {
								// commune faîtière de fractions...
								final String nomCommune = DateRangeHelper.rangeAt(communes, presenceSurCommunePrincipale.get(0).getDateDebut()).getNomOfficiel();
								final String message = String.format("%s %s ne peut pas être sur une commune faîtière de fractions de commune (ici %s / OFS %d), une fraction est attendue dans ce cas",
								                                     getEntityCategoryName(), getEntityDisplayString(entity), nomCommune, numeroOfsAutoriteFiscale);
								results.addError(message);
							}
						}

						// ... on continue avec les communes "normales" (= non-faîtières, ou alors carrément leurs fractions)
						final List<DateRange> nonPrincipalRanges = rangesByPrincipalFlag.get(Boolean.FALSE);
						if (nonPrincipalRanges != null && !nonPrincipalRanges.isEmpty()) {

							// on recherche les périodes de l'entité non-couvertes par la communes
							final List<DateRange> nonCouverts = DateRangeHelper.subtract(relevantEntityRange, nonPrincipalRanges);
							if (nonCouverts != null && !nonCouverts.isEmpty()) {
								for (DateRange nonCouvert : nonCouverts) {
									final String nomCommune;
									if (nonCouvert.getDateDebut() != null && DateRangeHelper.rangeAt(nonPrincipalRanges, nonCouvert.getDateDebut().getOneDayBefore()) != null) {
										nomCommune = DateRangeHelper.rangeAt(communes, nonCouvert.getDateDebut().getOneDayBefore()).getNomOfficiel();
									}
									else if (nonCouvert.getDateFin() != null && DateRangeHelper.rangeAt(nonPrincipalRanges, nonCouvert.getDateFin().getOneDayAfter()) != null) {
										nomCommune = DateRangeHelper.rangeAt(communes, nonCouvert.getDateFin().getOneDayAfter()).getNomOfficiel();
									}
									else {
										// vraiment très bizarre... Nous sommes au bord d'un trou (= la partie non-couverte) mais il n'y a rien ni d'un côté ni de l'autre...
										// (cela pourrait également se produire si le trou était infini à gauche et à droite, mais dans ce cas, il faudrait que les _nonPrincipalRanges_
										// soient vides, et nous ne devrions pas être ici...)
										throw new IllegalStateException("Erreur algorithmique dans la détermination du nom de la commune...");
									}
									results.addError(String.format("%s %s a une période de validité qui dépasse la période de validité de la commune %s (%d)%s",
									                               getEntityCategoryName(), getEntityDisplayString(entity),
									                               nomCommune, numeroOfsAutoriteFiscale,
									                               getDescriptionPeriode(nonCouvert)));
								}
							}

							// il faut aussi vérifier la cohérence entre la commune et le type d'autorité fiscale (attention, une commune peut changer de canton !!!)
							final Map<Boolean, List<Commune>> communesParFlagVaudoise = communes.stream()
									.collect(Collectors.toMap(Commune::isVaudoise, Collections::singletonList, ListUtils::union));
							if (typeAutoriteFiscale == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
								// l'entité semble dire que la commune est vaudoise, on va aller vérifier ça...
								final List<Commune> periodesNonVaudoises = communesParFlagVaudoise.get(Boolean.FALSE);
								if (periodesNonVaudoises != null && !periodesNonVaudoises.isEmpty()) {
									final List<DateRange> intersections = DateRangeHelper.intersections(relevantEntityRange, periodesNonVaudoises);
									if (intersections != null && !intersections.isEmpty()) {
										for (DateRange periodeNonVaudoise : intersections) {
											final String nomCommune = DateRangeHelper.rangeAt(communes, periodeNonVaudoise.getDateFin()).getNomOfficiel();
											results.addError(String.format("%s %s montre une incohérence entre son type d'autorité fiscale %s et la commune non-vaudoise %s (%d)%s",
											                               getEntityCategoryName(), getEntityDisplayString(entity),
											                               typeAutoriteFiscale,
											                               nomCommune, numeroOfsAutoriteFiscale,
											                               getDescriptionPeriode(periodeNonVaudoise)));
										}
									}
								}
							}
							else {
								// l'entité semble penser que la commune est hors-canton, il faut tout de même vérifier aussi
								final List<Commune> periodesVaudoises = communesParFlagVaudoise.get(Boolean.TRUE);
								if (periodesVaudoises != null && !periodesVaudoises.isEmpty()) {
									final List<DateRange> intersections = DateRangeHelper.intersections(relevantEntityRange, periodesVaudoises);
									if (intersections != null && !intersections.isEmpty()) {
										for (DateRange periodeNonVaudoise : intersections) {
											final String nomCommune = DateRangeHelper.rangeAt(communes, periodeNonVaudoise.getDateFin()).getNomOfficiel();
											results.addError(String.format("%s %s montre une incohérence entre son type d'autorité fiscale %s et la commune vaudoise %s (%d)%s",
											                               getEntityCategoryName(), getEntityDisplayString(entity),
											                               typeAutoriteFiscale,
											                               nomCommune, numeroOfsAutoriteFiscale,
											                               getDescriptionPeriode(periodeNonVaudoise)));
										}
									}
								}
							}
						}
					}
				}
				catch (ServiceInfrastructureException e) {
					results.addError(String.format("Impossible de vérifier la validité de la commune pour %s", getEntityDisplayString(entity)), e);
				}
			}
			else if (typeAutoriteFiscale == TypeAutoriteFiscale.PAYS_HS) {
				try {
					final Pays pays = serviceInfra.getPays(numeroOfsAutoriteFiscale, relevantEntityRange.getDateDebut());
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
