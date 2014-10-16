package ch.vd.uniregctb.validation.decision;

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
import ch.vd.uniregctb.tiers.DecisionAci;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.validation.EntityValidatorImpl;

/**
 * Classe de  validation des décisions ACI
 */
public class DecisionValidator extends EntityValidatorImpl<DecisionAci> {

	private ServiceInfrastructureService serviceInfra;

	public void setServiceInfra(ServiceInfrastructureService serviceInfra) {
		this.serviceInfra = serviceInfra;
	}

	@Override
	protected Class<DecisionAci> getValidatedClass() {
		return DecisionAci.class;
	}

	/**
	 * Interface utilisée pour faire en sorte que la validation d'une décision soit faite
	 * en supposant une date du jour spécifique (pour les tests)
	 */
	private static interface ReferenceDateAccessor {
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
	 * [SIFISC-10141] 
	 * @return <code>false</code> par défaut, surchargeable
	 */
	protected boolean isDateFermetureFutureAllowed() {
		return false;
	}

	@Override
	public ValidationResults validate(DecisionAci decision) {

		final ValidationResults results = new ValidationResults();

		if (decision.isAnnule()) {
			return results;
		}

		final RegDate dateDebut = decision.getDateDebut();
		final RegDate dateFin = decision.getDateFin();
		final TypeAutoriteFiscale typeAutoriteFiscale = decision.getTypeAutoriteFiscale();
		final Integer numeroOfsAutoriteFiscale = decision.getNumeroOfsAutoriteFiscale();

		// La date de début doit être renseignée
		if (dateDebut == null) {
			results.addError(String.format("La décision ACI %s possède une date de début nulle", decision));
		}
		else if (dateDebut.isAfter(getFutureBeginDate())) {
			// la date de début d'une décision ne doit en aucun cas être dans le futur !
			results.addError(String.format("La date de début de la décision ACI %s est dans le futur", decision));
		}
		if (dateFin != null && !isDateFermetureFutureAllowed() && dateFin.isAfter(getFutureBeginDate())) {
			// la date de fin non plus, ne doit jamais être dans le futur !
			results.addError(String.format("La date de fin de la décision ACI %s est dans le futur", decision));
		}
		if (typeAutoriteFiscale == null) {
			results.addError(String.format("La décision ACI %s n'a pas de type d'autorité fiscale", decision));
		}
		if (numeroOfsAutoriteFiscale == null) {
			results.addError(String.format("La décision ACI %s n'a pas d'autorité fiscale renseignée", decision));
		}

		// Date de début doit être avant la date de fin
		// Si "date de début" = "date de fin", c'est un cas OK (for qui dure 1 jour)
		if (dateDebut != null && dateFin != null && dateDebut.isAfter(dateFin)) {
			results.addError(String.format("La décision ACI %s possède une date de début qui est après la date de fin: début = %s fin = %s", decision, RegDateHelper.dateToDisplayString(dateDebut), RegDateHelper.dateToDisplayString(dateFin)));
		}

		if (numeroOfsAutoriteFiscale != null) {

			if (typeAutoriteFiscale == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD || typeAutoriteFiscale == TypeAutoriteFiscale.COMMUNE_HC) {
				try {
					final Commune commune = serviceInfra.getCommuneByNumeroOfs(numeroOfsAutoriteFiscale, dateDebut);
					if (commune == null) {
						results.addError(String.format("La commune de la décision ACI %s (%d) est inconnue dans l'infrastructure à la date de début de la décision", decision, decision.getNumeroOfsAutoriteFiscale()));
					}
					else if (!commune.isPrincipale()) {
						// vérification que la commune est bien valide sur toute la période
						final DateRange validiteCommune = new DateRangeHelper.Range(commune.getDateDebutValidite(), commune.getDateFinValidite());
						final DateRange validiteDecision = new DateRangeHelper.Range(decision.getDateDebut(), decision.getDateFin() == null ? getFutureBeginDate() : decision.getDateFin());      // on ne considère que la période passée des fors encore actifs
						if (!DateRangeHelper.within(validiteDecision, validiteCommune)) {
							final String debutValiditeCommune = validiteCommune.getDateDebut() == null ? "?" : RegDateHelper.dateToDisplayString(validiteCommune.getDateDebut());
							final String finValiditeCommune = validiteCommune.getDateFin() == null ? "?" : RegDateHelper.dateToDisplayString(validiteCommune.getDateFin());
							results.addError(String.format("La période de validité de la décision ACI %s dépasse la période de validité de la commune %s (%d) à laquelle il est assigné (%s - %s)",
									decision, commune.getNomOfficiel(), commune.getNoOFS(), debutValiditeCommune, finValiditeCommune));
						}
					}
					else {
						// commune faîtière de fractions...
						final String message = String.format("La décision ACI %s ne peut pas être ouvert sur une commune faîtière de fractions de commune (ici %s / OFS %d), une fraction est attendue dans ce cas",
								decision, commune.getNomOfficiel(), numeroOfsAutoriteFiscale);
						results.addError(message);
					}

					// ajouté le test de la cohérence de la commune avec le type d'autorité fiscale
					if (commune != null && commune.isVaudoise() && typeAutoriteFiscale != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
						results.addError(String.format("Incohérence entre le type d'autorité fiscale %s et la commune vaudoise %s (%d) sur la décision ACI %s", typeAutoriteFiscale, commune.getNomOfficiel(), commune.getNoOFS(), decision));
					}
					else if (commune != null && !commune.isVaudoise() && typeAutoriteFiscale == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
						results.addError(String.format("Incohérence entre le type d'autorité fiscale %s et la commune non-vaudoise %s (%d) sur la décision ACI %s", typeAutoriteFiscale, commune.getNomOfficiel(), commune.getNoOFS(), decision));
					}
				}
				catch (ServiceInfrastructureException e) {
					results.addError(String.format("Impossible de vérifier la validité de la commune de la décision %s", decision), e);
				}
			}
			else if (typeAutoriteFiscale == TypeAutoriteFiscale.PAYS_HS) {
				try {
					final Pays pays = serviceInfra.getPays(numeroOfsAutoriteFiscale, dateDebut);
					if (pays == null) {
						results.addError(String.format("Le pays de la décision ACI %s (%d) est inconnu dans l'infrastructure", decision, decision.getNumeroOfsAutoriteFiscale()));
					}
					else if (pays.isSuisse()) {
						results.addError(String.format("La décision ACI %s devrait être vaudois ou hors-canton", decision));
					}
					else if (!pays.isEtatSouverain()) {
						results.addError(String.format("Le pays de la décision ACI %s (%s, %d) n'est pas un état souverain, mais un territoire", decision, pays.getNomCourt(), pays.getNoOFS()));
					}
				}
				catch (ServiceInfrastructureException e) {
					results.addError(String.format("Impossible de vérifier la validité du pays de la décision ACI %s", decision), e);
				}
			}
		}

		return results;
	}
}
