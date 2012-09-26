package ch.vd.uniregctb.tiers.validator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Pays;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.NatureTiers;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersException;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.manager.TypeForFiscal;
import ch.vd.uniregctb.tiers.view.ForFiscalView;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Niveau;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class ForFiscalViewValidator implements Validator {

	/**
	 * Un logger pour {@link ForFiscalViewValidator}
	 */
	//private static final Logger LOGGER = Logger.getLogger(ForFiscalValidator.class);

	private TiersService tiersService;
	private ServiceInfrastructureService infraService;
	private SecurityProviderInterface securityProvider;

	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return ForFiscalView.class.equals(clazz);
	}

	@Override
	@Transactional(readOnly = true)
	public void validate(Object obj, Errors errors) {
		final ForFiscalView forFiscalView = (ForFiscalView) obj;
		final TypeForFiscal typeFor = TypeForFiscal.getType(forFiscalView.getGenreImpot(), forFiscalView.getMotifRattachement());
		final Tiers tiers = this.tiersService.getTiers(forFiscalView.getNumeroCtb());
		final TypeAutoriteFiscale typeAutoriteFiscale = forFiscalView.getTypeAutoriteFiscale();
		if (forFiscalView.isChangementModeImposition()) {
			if (forFiscalView.getRegDateChangement() == null) {
				errors.rejectValue("dateChangement", "error.date.changement.vide");
			}
			else if (RegDate.get().isBefore(forFiscalView.getRegDateChangement())) {
				errors.rejectValue("dateChangement", "error.date.changement.posterieure.date.jour");
			}
		}
		else {
			boolean isOrdinaire = false;
			final ModeImposition modeImp = forFiscalView.getModeImposition();
			if (typeFor == TypeForFiscal.PRINCIPAL) {
				switch (modeImp) {
					case ORDINAIRE:
					case DEPENSE:
					case INDIGENT:
						isOrdinaire = true;
						break;
				}
			}
			if ((forFiscalView.getGenreImpot() == GenreImpot.REVENU_FORTUNE
					|| (forFiscalView.getGenreImpot() == GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE)
					&& (forFiscalView.getDateOuverture() == null))) {
				ValidationUtils.rejectIfEmpty(errors, "dateOuverture", "error.date.ouverture.vide");
			}

			if ((forFiscalView.getGenreImpot() != GenreImpot.REVENU_FORTUNE)
					&& (forFiscalView.getGenreImpot() != GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE)
					&& (forFiscalView.getDateEvenement() == null)) {
				ValidationUtils.rejectIfEmpty(errors, "dateEvenement", "error.date.evenement.vide");
			}

			if (forFiscalView.getRegDateOuverture() != null) {
				if (RegDate.get().isBefore(forFiscalView.getRegDateOuverture())) {
					errors.rejectValue("dateOuverture", "error.date.ouverture.posterieure.date.jour");
				}
			}

			if (forFiscalView.getRegDateFermeture() != null) {
				if (RegDate.get().isBefore(forFiscalView.getRegDateFermeture())) {
					errors.rejectValue("dateFermeture", "error.date.fermeture.posterieure.date.jour");
				}
				else if (forFiscalView.getRegDateOuverture() != null &&
						forFiscalView.getDateOuverture().after(forFiscalView.getDateFermeture())) {
					errors.rejectValue("dateFermeture", "error.date.fermeture.anterieure");
				}
			}

			if (forFiscalView.getRegDateChangement() != null) {
				if (RegDate.get().isBefore(forFiscalView.getRegDateChangement())) {
					errors.rejectValue("dateChangement", "error.date.changement.posterieure.date.jour");
				}
			}

			if (forFiscalView.getGenreImpot() == GenreImpot.REVENU_FORTUNE) {
				if (forFiscalView.getDateOuverture() != null && forFiscalView.getMotifOuverture() == null) {
					if (typeFor == TypeForFiscal.PRINCIPAL || typeFor == TypeForFiscal.SECONDAIRE) {
						boolean allowEmptyMotif = typeAutoriteFiscale == TypeAutoriteFiscale.COMMUNE_HC || typeAutoriteFiscale == TypeAutoriteFiscale.PAYS_HS;
						if (allowEmptyMotif) {
							// [SIFISC-4065] On n'autorise les motifs d'ouverture vide que s'il n'y a pas de for principal non annulé existant avant le for que l'on veut créer maintenant
							allowEmptyMotif = tiers.getDernierForFiscalPrincipalAvant(forFiscalView.getRegDateOuverture()) == null;
						}
						if (!allowEmptyMotif) {
							errors.rejectValue("motifOuverture", "error.motif.ouverture.vide");
						}
					}
				}
				else if (forFiscalView.getMotifOuverture() != null) {
					if (forFiscalView.getNatureTiers() == NatureTiers.AutreCommunaute) {
						if (forFiscalView.getMotifOuverture() == MotifFor.MAJORITE ||
								forFiscalView.getMotifOuverture() == MotifFor.PERMIS_C_SUISSE) {
							errors.rejectValue("motifOuverture", "error.motif.ouverture.invalide");
						}
					}
					if ((forFiscalView.getMotifRattachement() == MotifRattachement.ACTIVITE_INDEPENDANTE ||
							forFiscalView.getMotifRattachement() == MotifRattachement.DIRIGEANT_SOCIETE)
							&& forFiscalView.getMotifOuverture() != MotifFor.DEBUT_EXPLOITATION
							&& forFiscalView.getMotifOuverture() != MotifFor.FUSION_COMMUNES) {
						errors.rejectValue("motifOuverture", "error.motif.ouverture.invalide");

					}
					else if (forFiscalView.getMotifRattachement() == MotifRattachement.IMMEUBLE_PRIVE
							&& forFiscalView.getMotifOuverture() != MotifFor.ACHAT_IMMOBILIER
							&& forFiscalView.getMotifOuverture() != MotifFor.FUSION_COMMUNES) {
						errors.rejectValue("motifOuverture", "error.motif.ouverture.invalide");
					}
					else if (forFiscalView.getMotifRattachement() == MotifRattachement.SEJOUR_SAISONNIER
							&& forFiscalView.getMotifOuverture() != MotifFor.SEJOUR_SAISONNIER
							&& forFiscalView.getMotifOuverture() != MotifFor.FUSION_COMMUNES) {
						errors.rejectValue("motifOuverture", "error.motif.ouverture.invalide");
					}
				}

				if (forFiscalView.getDateFermeture() != null) {
					if (forFiscalView.getMotifFermeture() == null) {
						if (typeFor == TypeForFiscal.PRINCIPAL || typeFor == TypeForFiscal.SECONDAIRE) {
							errors.rejectValue("motifFermeture", "error.motif.fermeture.vide");
						}
					}
					else {
						if (forFiscalView.getNatureTiers() == NatureTiers.AutreCommunaute) {
							if (forFiscalView.getMotifFermeture() == MotifFor.VEUVAGE_DECES ||
									forFiscalView.getMotifFermeture() == MotifFor.PERMIS_C_SUISSE) {
								errors.rejectValue("motifFermeture", "error.motif.fermeture.invalide");
							}
						}
						if ((forFiscalView.getMotifRattachement() == MotifRattachement.ACTIVITE_INDEPENDANTE ||
								forFiscalView.getMotifRattachement() == MotifRattachement.DIRIGEANT_SOCIETE)
								&& forFiscalView.getMotifFermeture() != MotifFor.FIN_EXPLOITATION
								&& forFiscalView.getMotifFermeture() != MotifFor.FUSION_COMMUNES) {
							errors.rejectValue("motifFermeture", "error.motif.fermeture.invalide");
						}
						else if (forFiscalView.getMotifRattachement() == MotifRattachement.IMMEUBLE_PRIVE
								&& forFiscalView.getMotifFermeture() != MotifFor.VENTE_IMMOBILIER
								&& forFiscalView.getMotifFermeture() != MotifFor.FUSION_COMMUNES) {
							errors.rejectValue("motifFermeture", "error.motif.fermeture.invalide");
						}
						else if (forFiscalView.getMotifRattachement() == MotifRattachement.SEJOUR_SAISONNIER
								&& forFiscalView.getMotifFermeture() != MotifFor.SEJOUR_SAISONNIER
								&& forFiscalView.getMotifFermeture() != MotifFor.FUSION_COMMUNES) {
							errors.rejectValue("motifFermeture", "error.motif.fermeture.invalide");
						}
					}
				}
			}

			if (typeAutoriteFiscale == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				if (forFiscalView.getNumeroForFiscalCommune() == null) {
					if (forFiscalView.getLibFractionCommune() == null) {
						errors.rejectValue("libFractionCommune", "error.commune.vide");
					}
					else {
						errors.rejectValue("libFractionCommune", "error.commune.non.vd");
					}
				}
			}
			else if (typeAutoriteFiscale == TypeAutoriteFiscale.COMMUNE_HC) {
				if (forFiscalView.getNumeroForFiscalCommuneHorsCanton() == null) {
					if (forFiscalView.getLibCommuneHorsCanton() == null) {
						errors.rejectValue("libCommuneHorsCanton", "error.commune.vide");
					}
					else {
						errors.rejectValue("libCommuneHorsCanton", "error.commune.non.hc");
					}
				}
			}
			else if (typeAutoriteFiscale == TypeAutoriteFiscale.PAYS_HS) {
				if (forFiscalView.getNumeroForFiscalPays() == null) {
					if (forFiscalView.getLibPays() == null) {
						errors.rejectValue("libPays", "error.pays.vide");
					}
					else {
						errors.rejectValue("libPays", "error.pays.non.valide");
					}
				}
				else if (forFiscalView.getId() == null) { // [UNIREG-3338] en cas de création d'un nouveau for fiscal, le pays doit être valide
					final Integer noOfsPays = forFiscalView.getNumeroAutoriteFiscale();
					final Pays pays = infraService.getPays(noOfsPays);
					if (pays == null) {
						errors.rejectValue("libPays", "error.pays.inconnu");
					}
					else if (!pays.isValide()) {
						errors.rejectValue("libPays", "error.pays.non.valide");
					}
				}
			}

			ForFiscalPrincipal dernierForPrincipal = tiers.getDernierForFiscalPrincipal();
			if ((dernierForPrincipal != null) && (dernierForPrincipal.getDateFin() != null)) {
				if (typeFor == TypeForFiscal.PRINCIPAL) {
					if (forFiscalView.getId() == null) {
						if (tiers.getForFiscalPrincipalAt(forFiscalView.getRegDateOuverture()) != null) {
							errors.rejectValue("dateOuverture", "error.date.chevauchement");
						}
					}
					for (ForFiscalPrincipal forPrincipal : tiers.getForsFiscauxPrincipauxActifsSorted()) {
						if (forPrincipal.isValidAt(forFiscalView.getRegDateFermeture()) &&
								((forFiscalView.getId() == null) || (forPrincipal.getId().longValue() != forFiscalView.getId().longValue()))) {
							errors.rejectValue("dateFermeture", "error.date.chevauchement");
							break;
						}
					}
				}
			}
			final ForDebiteurPrestationImposable dernierForDPI = tiers.getDernierForDebiteur();
			if ((dernierForDPI != null) && (dernierForDPI.getDateFin() != null)) {
				if (typeFor == TypeForFiscal.DEBITEUR_PRESTATION_IMPOSABLE) {
					if (forFiscalView.getId() == null) {
						if (tiers.getForDebiteurPrestationImposableAt(forFiscalView.getRegDateOuverture()) != null) {
							errors.rejectValue("dateOuverture", "error.date.chevauchement");
						}
					}
					for (ForFiscal forFiscal : tiers.getForsFiscauxValidAt(forFiscalView.getRegDateFermeture())) {
						if (forFiscal instanceof ForDebiteurPrestationImposable &&
								((forFiscalView.getId() == null) || (forFiscal.getId().longValue() != forFiscalView.getId().longValue()))) {
							errors.rejectValue("dateFermeture", "error.date.chevauchement");
							break;
						}
					}
				}
			}

			//gestion des droits
			//seul la date de fermeture et le motif de fermeture (si existant) sont éditables
			final String msgErrorForSec = (forFiscalView.getId() == null) ? "error.motif.rattachement.interdit" : "error.tiers.interdit";

			final Niveau acces = SecurityHelper.getDroitAcces(securityProvider, forFiscalView.getNumeroCtb());
			if (acces == null || acces == Niveau.LECTURE) {
				errors.reject("global.error.msg", "Droits insuffisants pour modifier ce tiers");
			}

			if (typeFor == TypeForFiscal.DEBITEUR_PRESTATION_IMPOSABLE) {
				if (!SecurityHelper.isGranted(securityProvider, Role.CREATE_DPI)) {
					errors.rejectValue("genreImpot", "error.tiers.interdit");
				}
			}
			else if (typeFor == TypeForFiscal.PRINCIPAL) {
				//forFiscalView.getNatureTiers est tjs != MENEAGE_COMMUN (si couple => HABITANT ou NON_HABITANT
				if (forFiscalView.getNatureTiers() == NatureTiers.Habitant) {
					if ((isOrdinaire && !SecurityHelper.isGranted(securityProvider, Role.FOR_PRINC_ORDDEP_HAB)) ||
							(!isOrdinaire && !SecurityHelper.isGranted(securityProvider, Role.FOR_PRINC_SOURC_HAB))) {
						errors.rejectValue("motifRattachement", msgErrorForSec);
					}
				}
				else if (forFiscalView.getNatureTiers() == NatureTiers.NonHabitant) {
					boolean isGris = false;
					if (typeAutoriteFiscale == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
						isGris = true;
					}
					if ((isOrdinaire && !isGris && !SecurityHelper.isGranted(securityProvider, Role.FOR_PRINC_ORDDEP_HCHS)) ||
							(!isOrdinaire && !isGris && !SecurityHelper.isGranted(securityProvider, Role.FOR_PRINC_SOURC_HCHS)) ||
							(isOrdinaire && isGris && !SecurityHelper.isGranted(securityProvider, Role.FOR_PRINC_ORDDEP_GRIS)) ||
							(!isOrdinaire && isGris && !SecurityHelper.isGranted(securityProvider, Role.FOR_PRINC_SOURC_GRIS))) {
						errors.rejectValue("motifRattachement", msgErrorForSec);
					}
				}
				else {
					//pour + tard : traiter le cas des entreprises
					errors.reject("global.error.msg", "Droits insuffisants pour modifier ce tiers");
				}
			}
			else if (typeFor == TypeForFiscal.SECONDAIRE) {
				//pour + tard : traiter le cas des entreprises
				if (!SecurityHelper.isGranted(securityProvider, Role.FOR_SECOND_PP)) {
					errors.rejectValue("motifRattachement", msgErrorForSec);
				}
			}
			else if (typeFor == TypeForFiscal.AUTRE_ELEMENT) {
				if (!SecurityHelper.isGranted(securityProvider, Role.FOR_AUTRE)) {
					errors.rejectValue("motifRattachement", msgErrorForSec);
				}
			}
			else if (typeFor == TypeForFiscal.AUTRE_IMPOT) {
				if (!SecurityHelper.isGranted(securityProvider, Role.FOR_AUTRE)) {
					errors.rejectValue("genreImpot", (forFiscalView.getId() == null) ? "error.genre.impot.interdit" : "error.tiers.interdit");
				}
			}
			if (TypeForFiscal.DEBITEUR_PRESTATION_IMPOSABLE == typeFor) {
				if (TypeAutoriteFiscale.PAYS_HS == typeAutoriteFiscale) {
					errors.rejectValue("typeAutoriteFiscale", "error.type.autorite.incorrect");
				}
			}
			else if (TypeForFiscal.PRINCIPAL != typeFor
					&& TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD != typeAutoriteFiscale) {
				errors.rejectValue("typeAutoriteFiscale", "error.type.autorite.incorrect");
			}

		}
		if ((forFiscalView.getDateFermeture() == null) && (forFiscalView.getMotifFermeture() != null)) {
			errors.rejectValue("dateFermeture", "error.date.fermeture.vide");
		}

		if (tiers instanceof PersonnePhysique) {
			if (MotifRattachement.DOMICILE == forFiscalView.getMotifRattachement()
					&& TypeForFiscal.PRINCIPAL == typeFor
					&& TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD == typeAutoriteFiscale) {

				final PersonnePhysique pp = (PersonnePhysique) tiers;
				final RegDate date;
				if (forFiscalView.isChangementModeImposition()) {
					date = RegDate.get(forFiscalView.getDateChangement());
				}
				else {
					date = RegDate.get(forFiscalView.getDateOuverture());
				}

				// [UNIREG-1235]
				// La règle est la suivante:
				// - un contribuable de nationalité suisse ne peut être qu'à l'ordinaire ou indigent
				// - un contribuable étranger avec un permis C peut être à l'ordinaire, indigent, ou à la dépense
				// - pour tous les autres, tous les modes sont admis (donc y compris pour ceux dont on ne connait ni la nationalité ni le permis de séjour)
                // - [SIFISC-4528] exception pour les non-habitants étrangers, on ne contrôle pas leur permis pour pouvoir eventuellement leur ajouter un for source
                //   antérieur à leur obtention du permis C,
				final Set<ModeImposition> autorises = new HashSet<ModeImposition>();

				// nationalité suisse ou étrangère ?
				Boolean isSuisse;
				try {
					isSuisse = tiersService.isSuisse(pp, date);
				}
				catch (TiersException e) {
					// je ne sais pas s'il est suisse ou pas...
					isSuisse = null;
				}

				// Suisse et habitant?
				if (isSuisse != null && isSuisse && pp.isHabitantVD()) {
					autorises.add(ModeImposition.INDIGENT);
					autorises.add(ModeImposition.ORDINAIRE);
				}
				else {

					// permis de séjour C ou autre ?
					Boolean isSansPermisC;
					try {
						isSansPermisC = tiersService.isEtrangerSansPermisC(pp, date);
					}
					catch (TiersException e) {
						// on ne sait pas...
						isSansPermisC = null;
					}

					// permis C et habitant ?
					if (isSansPermisC != null && !isSansPermisC && pp.isHabitantVD()) {
						autorises.add(ModeImposition.INDIGENT);
						autorises.add(ModeImposition.ORDINAIRE);
						autorises.add(ModeImposition.DEPENSE);
					}
					else {
						// tous sont autorisés
						autorises.addAll(Arrays.asList(ModeImposition.values()));
					}
				}

				final ModeImposition modeImposition = forFiscalView.getModeImposition();
				if (modeImposition == null || !autorises.contains(modeImposition)) {
					errors.rejectValue("modeImposition", "error.mode.imposition.incorrect");
				}
			}
		}
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}
}
