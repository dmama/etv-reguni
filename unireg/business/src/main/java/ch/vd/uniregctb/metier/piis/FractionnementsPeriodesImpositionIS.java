package ch.vd.uniregctb.metier.piis;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections4.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.common.DecalageDateHelper;
import ch.vd.uniregctb.metier.common.ForFiscalPrincipalContext;
import ch.vd.uniregctb.metier.common.Fraction;
import ch.vd.uniregctb.metier.common.FractionContrariante;
import ch.vd.uniregctb.metier.common.FractionDecalee;
import ch.vd.uniregctb.metier.common.Fractionnements;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPP;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class FractionnementsPeriodesImpositionIS implements Iterable<Fraction> {

	private final int pf;
	private final ForFiscalPrincipalPP justeAvantPF;
	private final ForFiscalPrincipalPP justeApresPF;
	private final ServiceInfrastructureService infraService;
	private final Fractionnements<ForFiscalPrincipalPP> fractionnements;

	public FractionnementsPeriodesImpositionIS(List<ForFiscalPrincipalPP> principauxDansPF, int pf, @Nullable ForFiscalPrincipalPP justeAvantPF, @Nullable ForFiscalPrincipalPP justeApresPF, ServiceInfrastructureService infraService) {
		this.pf = pf;
		this.justeAvantPF = justeAvantPF;
		this.justeApresPF = justeApresPF;
		this.infraService = infraService;
		this.fractionnements = new Fractionnements<ForFiscalPrincipalPP>(principauxDansPF) {
			@Override
			protected Fraction isFractionOuverture(ForFiscalPrincipalContext<ForFiscalPrincipalPP> forPrincipal) {
				return FractionnementsPeriodesImpositionIS.this.isFractionOuverture(forPrincipal);
			}

			@Override
			protected Fraction isFractionFermeture(ForFiscalPrincipalContext<ForFiscalPrincipalPP> forPrincipal) {
				return FractionnementsPeriodesImpositionIS.this.isFractionFermeture(forPrincipal);
			}
		};
	}

	@NotNull
	private Localisation getLocalisation(ForFiscalPrincipal forPrincipal) {
		if (forPrincipal == null) {
			return Localisation.get(null, infraService);
		}
		else {
			return Localisation.get(forPrincipal.getNumeroOfsAutoriteFiscale(), forPrincipal.getDateDebut(), forPrincipal.getTypeAutoriteFiscale(), infraService);
		}
	}

	@Nullable
	protected static MotifFor getMotifEffectif(Localisation localisationAvant, MotifFor motifFermeture, Localisation localisationApres, MotifFor motifOuverture) {
		// changement de localisation -> on se moque des motifs
		if (!localisationAvant.isInconnue() && !localisationApres.isInconnue() && !localisationAvant.equals(localisationApres)) {
			if (localisationApres.isHS()) {
				return MotifFor.DEPART_HS;
			}
			else if (localisationAvant.isHS()) {
				return MotifFor.ARRIVEE_HS;
			}
			else if (localisationApres.isHC()) {
				return MotifFor.DEPART_HC;
			}
			else if (localisationAvant.isHC()) {
				return MotifFor.ARRIVEE_HC;
			}
			else {
				Assert.fail("On ne devrait jamais arriver ici avec des localisations différentes avant et après...");
			}
		}

		// à un moment, il faut savoir faire confiance aux données...
		final MotifFor motif = Fraction.getMotifEffectif(motifFermeture, motifOuverture);
		if (localisationApres.isInconnue() || localisationAvant.isInconnue()) {
			// on ne sait pas mieux faire que ça, de toute façon...
			return motif;
		}
		else if (motif == MotifFor.ARRIVEE_HC || motif == MotifFor.DEPART_HC || motif == MotifFor.ARRIVEE_HS || motif == MotifFor.DEPART_HS) {
			// ici, on a des mouvements à l'intérieur d'un même canton ou pays qui sont qualifiés de départ/arrivée -> mettons déménagement, plutôt...
			return MotifFor.DEMENAGEMENT_VD;
		}
		else {
			return motif;
		}
	}

	private Fraction isFractionOuverture(ForFiscalPrincipalContext<ForFiscalPrincipalPP> forPrincipal) {
		final ForFiscalPrincipalPP current = forPrincipal.getCurrent();
		final ForFiscalPrincipalPP previous = forPrincipal.getPrevious();

		Fraction fraction = null;

		// pas de fractionnement au début d'un for qui a déjà commencé sur une autre PF
		if (current.getDateDebut().year() == pf) {

			// décès -> fractionnement à la date pile
			if (current.getMotifOuverture() == MotifFor.VEUVAGE_DECES) {
				fraction = new FractionContrariante(current.getDateDebut(), MotifFor.VEUVAGE_DECES, null);
			}
			else {
				final ForFiscalPrincipalPP previousPourMotif = (previous == null && current.getDateDebut() == RegDate.get(pf, 1, 1) ? justeAvantPF : previous);
				final Localisation currentLocalisation = getLocalisation(current);
				final Localisation previousLocalisation = getLocalisation(previousPourMotif);
				final MotifFor motifEffectif = getMotifEffectif(previousLocalisation, previousPourMotif != null ? previousPourMotif.getMotifFermeture() : null, currentLocalisation, current.getMotifOuverture());

				// transition Suisse <-> étranger : fractionnement à la date pile
				if (motifEffectif == MotifFor.DEPART_HS || motifEffectif == MotifFor.ARRIVEE_HS) {
					fraction = new FractionContrariante(current.getDateDebut(), motifEffectif, null);
				}
				else if ((current.getModeImposition() == ModeImposition.SOURCE || current.getModeImposition() == ModeImposition.MIXTE_137_1) && (motifEffectif == MotifFor.ARRIVEE_HC || motifEffectif == MotifFor.DEPART_HC)) {
					// la date d'ouverture du for est la date d'arrivée dans le nouveau canton
					// -> la date de fraction doit être le premier jour du mois qui suit celui de la date de départ (= la veille de l'arrivée)
					// [SIFISC-18817] ce calcul n'est valable que pour les 'source' ou 'mixte1'
					final RegDate dateFraction = current.getDateDebut().getOneDayBefore().getLastDayOfTheMonth().getOneDayAfter();
					fraction = new FractionDecalee(dateFraction, new DateRangeHelper.Range(current.getDateDebut(), dateFraction), motifEffectif, null);
				}
				else if (motifEffectif == MotifFor.ARRIVEE_HC) {
					// [SIFISC-18817] on revient ;
					// 1. au premier jour du mois de la même année suivant le passage au rôle s'il y en a eu un dans l'année avant l'arrivée HC
					// 2. au premier janvier (en fait, non, c'est plutôt "à la dernière arrivée HS dans la PF avant le départ HC", et à défaut au premier janvier) sinon
					if (current.getModeImposition() == ModeImposition.MIXTE_137_2 && previous != null && !previous.getModeImposition().isRole()) {
						fraction = getFractionArriveeDepartHC(forPrincipal, current.getDateDebut(), motifEffectif, new Predicate<ForFiscalPrincipalPP>() {
							@Override
							public boolean evaluate(ForFiscalPrincipalPP ff) {
								return ff.getTypeAutoriteFiscale() != TypeAutoriteFiscale.PAYS_HS;
							}
						});
					}
					else {
						fraction = getFractionArriveeDepartHC(forPrincipal, current.getDateDebut(), motifEffectif, new Predicate<ForFiscalPrincipalPP>() {
							@Override
							public boolean evaluate(ForFiscalPrincipalPP ff) {
								return ff.getModeImposition().isRole() && ff.getTypeAutoriteFiscale() != TypeAutoriteFiscale.PAYS_HS;
							}
						});
					}
				}
				else if (motifEffectif == MotifFor.DEPART_HC && previous != null && previous.getModeImposition().isRole() && !previous.getModeImposition().isSource()) {
					// [SIFISC-18817] départ HC d'un "ordinaire", on se ramène
					// - au premier jour du mois suivant le passage à l'ordinaire, si celui-ci a eu lieu dans la même PF (avant le départ, évidemment...)
					// - au premier janvier (en fait, non, c'est plutôt "à la dernière arrivée HS dans la PF avant le départ HC", et à défaut au premier janvier) sinon
					fraction = getFractionArriveeDepartHC(forPrincipal, current.getDateDebut(), motifEffectif, new Predicate<ForFiscalPrincipalPP>() {
						@Override
						public boolean evaluate(ForFiscalPrincipalPP ff) {
							return ff.getTypeAutoriteFiscale() != TypeAutoriteFiscale.PAYS_HS
									&& ff.getModeImposition().isRole()
									&& !ff.getModeImposition().isSource();
						}
					});
				}
				else if (motifEffectif == MotifFor.DEPART_HC) {
					// la date d'ouverture du for est la date d'arrivée dans le nouveau canton
					// -> la date de fraction doit être le premier jour du mois qui suit celui de la date de départ (= la veille de l'arrivée)
					final RegDate dateFraction = current.getDateDebut().getOneDayBefore().getLastDayOfTheMonth().getOneDayAfter();
					fraction = new FractionDecalee(dateFraction, new DateRangeHelper.Range(current.getDateDebut(), dateFraction), motifEffectif, null);
				}
				else if (previous != null && previous.getModeImposition() == ModeImposition.SOURCE && !current.getModeImposition().isSource() && motifEffectif == MotifFor.PERMIS_C_SUISSE) {
					// attention, pas tout à fait pareil que le mariage, puisque pour le moment, l'obtention du permis C le 1er jour du mois
					// provoque un assujettissement ordinaire immédiat (avant 2014, après on décale quand-même au mois suivant)
	                final RegDate dateFraction = DecalageDateHelper.getDateDebutAssujettissementOrdinaireApresPermisCNationaliteSuisse(current.getDateDebut());
					fraction = new FractionDecalee(dateFraction, new DateRangeHelper.Range(current.getDateDebut(), dateFraction), motifEffectif, null);
				}
				// [SIFISC-18817] le mariage avec non-sourcier (ordinaire ou mixte) cause maintenant un fractionnement
				else if (previous != null && previous.getModeImposition() == ModeImposition.SOURCE && current.getModeImposition() != ModeImposition.SOURCE && motifEffectif == MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION) {
	                final RegDate dateFraction = current.getDateDebut().getLastDayOfTheMonth().getOneDayAfter();
					fraction = new FractionDecalee(dateFraction, new DateRangeHelper.Range(current.getDateDebut(), dateFraction), motifEffectif, null);
				}
			}
		}

		return fraction;
	}

	private Fraction isFractionFermeture(ForFiscalPrincipalContext<ForFiscalPrincipalPP> forPrincipal) {

		final ForFiscalPrincipalPP current = forPrincipal.getCurrent();
		final ForFiscalPrincipalPP next = forPrincipal.getNext();

		Fraction fraction = null;

		// pas de fractionnement sur un for ouvert ou fermé après la fin de la PF
		if (current.getDateFin() != null && current.getDateFin().year() == pf) {

			// décès -> fractionnement à la date pile
			if (current.getMotifFermeture() == MotifFor.VEUVAGE_DECES) {
				fraction = new FractionContrariante(current.getDateFin().getOneDayAfter(), null, MotifFor.VEUVAGE_DECES);
			}
			else {
				final ForFiscalPrincipalPP nextPourMotif = (next == null && current.getDateFin() == RegDate.get(pf, 12, 31) ? justeApresPF : next);
				final Localisation currentLocalisation = getLocalisation(current);
				final Localisation nextLocalisation = getLocalisation(nextPourMotif);
				final MotifFor motifEffectif = getMotifEffectif(currentLocalisation, current.getMotifFermeture(), nextLocalisation, nextPourMotif != null ? nextPourMotif.getMotifOuverture() : null);

				if (motifEffectif == MotifFor.DEPART_HS || motifEffectif == MotifFor.ARRIVEE_HS) {
					fraction = new FractionContrariante(current.getDateFin().getOneDayAfter(), null, motifEffectif);
				}
				else if ((motifEffectif == MotifFor.DEPART_HC || motifEffectif == MotifFor.ARRIVEE_HC) && next != null && (next.getModeImposition() == ModeImposition.SOURCE || next.getModeImposition() == ModeImposition.MIXTE_137_1)) {
					// la date de fermeture du for est la date de départ dans l'ancien canton
					// -> la date de fraction doit être le premier jour du mois qui suit celui de la date de départ
					// [SIFISC-18817] ce calcul n'est valable que pour les 'source' ou 'mixte1'
					final RegDate dateFraction = current.getDateFin().getLastDayOfTheMonth().getOneDayAfter();
					fraction = new FractionDecalee(dateFraction, new DateRangeHelper.Range(current.getDateFin(), dateFraction), null, motifEffectif);
				}
				else if (motifEffectif == MotifFor.ARRIVEE_HC && next != null) {
					// [SIFISC-18817] on revient ;
					// - au premier jour du mois de la même année suivant le passage au rôle s'il y en a eu un dans l'année avant l'arrivée HC
					// - au premier janvier (en fait, non, c'est plutôt "à la dernière arrivée HS dans la PF avant le départ HC", et à défaut au premier janvier) sinon
					if (current.getModeImposition().isRole() && next.getModeImposition() != ModeImposition.MIXTE_137_2) {
						fraction = getFractionArriveeDepartHC(forPrincipal, current.getDateFin(), motifEffectif, new Predicate<ForFiscalPrincipalPP>() {
							@Override
							public boolean evaluate(ForFiscalPrincipalPP ff) {
								return ff.getModeImposition().isRole() && ff.getTypeAutoriteFiscale() != TypeAutoriteFiscale.PAYS_HS;
							}
						});
					}
					else {
						fraction = getFractionArriveeDepartHC(forPrincipal, current.getDateFin(), motifEffectif, new Predicate<ForFiscalPrincipalPP>() {
							@Override
							public boolean evaluate(ForFiscalPrincipalPP ff) {
								return ff.getTypeAutoriteFiscale() != TypeAutoriteFiscale.PAYS_HS;
							}
						});
					}
				}
				else if (motifEffectif == MotifFor.DEPART_HC && current.getModeImposition().isRole() && !current.getModeImposition().isSource()) {
					// [SIFISC-18817] départ HC d'un "ordinaire", on se ramène
					// - au premier jour du mois suivant le passage à l'ordinaire, si celui-ci a eu lieu dans la même PF (avant le départ, évidemment...)
					// - au premier janvier (en fait, non, c'est plutôt "à la dernière arrivée HS dans la PF avant le départ HC", et à défaut au premier janvier) sinon
					fraction = getFractionArriveeDepartHC(forPrincipal, current.getDateFin(), motifEffectif, new Predicate<ForFiscalPrincipalPP>() {
						@Override
						public boolean evaluate(ForFiscalPrincipalPP ff) {
							return ff.getTypeAutoriteFiscale() != TypeAutoriteFiscale.PAYS_HS
									&& ff.getModeImposition().isRole()
									&& !ff.getModeImposition().isSource();
						}
					});
				}
				else if (motifEffectif == MotifFor.DEPART_HC) {
					// la date de fermeture du for est la date de départ dans l'ancien canton
					// -> la date de fraction doit être le premier jour du mois qui suit celui de la date de départ
					final RegDate dateFraction = current.getDateFin().getLastDayOfTheMonth().getOneDayAfter();
					fraction = new FractionDecalee(dateFraction, new DateRangeHelper.Range(current.getDateFin(), dateFraction), null, motifEffectif);
				}
				else if (current.getModeImposition() == ModeImposition.SOURCE && next != null && !next.getModeImposition().isSource() && motifEffectif == MotifFor.PERMIS_C_SUISSE) {
					// attention, pas tout à fait pareil que le mariage, puisque pour le moment, l'obtention du permis C le 1er jour du mois
					// provoque un assujettissement ordinaire immédiat (avant 2014, après on décale quand-même au mois suivant)
					final RegDate dateFraction = DecalageDateHelper.getDateDebutAssujettissementOrdinaireApresPermisCNationaliteSuisse(current.getDateFin().getOneDayAfter());
					fraction = new FractionDecalee(dateFraction, new DateRangeHelper.Range(current.getDateFin(), dateFraction), null, motifEffectif);
				}
				// [SIFISC-18817] le mariage avec non-sourcier (ordinaire ou mixte) cause maintenant un fractionnement
				else if (current.getModeImposition() == ModeImposition.SOURCE && next != null && next.getModeImposition() != ModeImposition.SOURCE && motifEffectif == MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION) {
					final RegDate dateFraction = current.getDateFin().getOneDayAfter().getLastDayOfTheMonth().getOneDayAfter();
					fraction = new FractionDecalee(dateFraction, new DateRangeHelper.Range(current.getDateFin(), dateFraction), null, motifEffectif);
				}
			}
		}

		return fraction;
	}

	private <T extends ForFiscalPrincipal> Fraction getFractionArriveeDepartHC(ForFiscalPrincipalContext<T> source, RegDate dateReference, MotifFor motifEffectif, Predicate<? super T> predicate) {
		final ForFiscalPrincipalContext<T> lastPreviousMatching = slideToLastPreviousMatching(source, predicate);
		final T arrivee = lastPreviousMatching.getCurrent();
		final RegDate dateFraction;
		if (arrivee.getDateDebut().year() < pf) {
			dateFraction = RegDate.get(pf, 1, 1);
		}
		else {
			final T beforeMatching = lastPreviousMatching.getPrevious();
			final MotifFor motifEffectifArrivee = getMotifEffectif(getLocalisation(beforeMatching),
			                                                       beforeMatching != null ? beforeMatching.getMotifFermeture() : null,
			                                                       getLocalisation(arrivee),
			                                                       arrivee.getMotifOuverture());
			if (motifEffectifArrivee == MotifFor.ARRIVEE_HS || motifEffectifArrivee == MotifFor.DEPART_HS) {
				dateFraction = arrivee.getDateDebut();
			}
			else if (beforeMatching == null) {
				dateFraction = RegDate.get(pf, 1, 1);
			}
			else {
				dateFraction = arrivee.getDateDebut().getLastDayOfTheMonth().getOneDayAfter();
			}
		}

		final Fraction fraction;
		if (dateFraction.compareTo(dateReference) > 0) {
			fraction = new FractionDecalee(dateFraction, new DateRangeHelper.Range(dateReference, dateFraction), motifEffectif, null);
		}
		else {
			fraction = new FractionDecalee(dateFraction, new DateRangeHelper.Range(dateFraction, dateReference), motifEffectif, null);
		}
		return fraction;
	}

	/**
	 * @param origin point de départ
	 * @param predicate prédicat de matching
	 * @param <T> type des fors fiscaux principaux
	 * @return le dernier contexte qui matche le prédicat sur son champ {@link ForFiscalPrincipalContext#current current} en glissant vers les précédents à partir de l'origine donnée
	 * @throws IllegalArgumentException si l'origine ne satisfait pas au prédicat passé
	 */
	private static <T extends ForFiscalPrincipal> ForFiscalPrincipalContext<T> slideToLastPreviousMatching(ForFiscalPrincipalContext<T> origin, Predicate<? super T> predicate) {
		if (!predicate.evaluate(origin.getCurrent())) {
			throw new IllegalArgumentException("Le point origine ne satisfait pas au prédicat.");
		}

		ForFiscalPrincipalContext<T> last = origin;
		ForFiscalPrincipalContext<T> cursor = origin.slideToPrevious();
		while (cursor.getCurrent() != null && predicate.evaluate(cursor.getCurrent())) {
			last = cursor;
			cursor = cursor.slideToPrevious();
		}
		return last;
	}

	@Override
	public Iterator<Fraction> iterator() {
		return fractionnements.iterator();
	}

	public boolean isEmpty() {
		return fractionnements.isEmpty();
	}

	@Nullable
	public Fraction getAt(RegDate date) {
		return fractionnements.getAt(date);
	}
}
