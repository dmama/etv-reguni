package ch.vd.uniregctb.tiers.validator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
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

public class ForFiscalValidator implements Validator {

	/**
	 * Un logger pour {@link ForFiscalValidator}
	 */
	//private static final Logger LOGGER = Logger.getLogger(ForFiscalValidator.class);

	private TiersService tiersService;

	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return ForFiscalView.class.equals(clazz) ;
	}

	@Transactional(readOnly = true)
	public void validate(Object obj, Errors errors) {
		ForFiscalView forFiscalView = (ForFiscalView) obj;
		TypeForFiscal typeFor = TypeForFiscal.getType(forFiscalView.getGenreImpot(), forFiscalView.getMotifRattachement());
		Tiers tiers = this.tiersService.getTiers(forFiscalView.getNumeroCtb());
		if (forFiscalView.isChangementModeImposition()) {
			if (forFiscalView.getDateChangement() == null) {
				errors.rejectValue("dateChangement", "error.date.changement.vide");
				return;
			}
		}
		else {
			boolean isOrdinaire = false;
			ModeImposition modeImp = forFiscalView.getModeImposition();
			if (typeFor == TypeForFiscal.PRINCIPAL ) {
				switch (modeImp) {
					case ORDINAIRE :
					case DEPENSE :
					case INDIGENT :
						isOrdinaire = true;
						break;
				}
			}
			if (	(forFiscalView.getGenreImpot().equals(GenreImpot.REVENU_FORTUNE)
					||(forFiscalView.getGenreImpot().equals(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE) )
					&& (forFiscalView.getDateOuverture() == null))) {
				ValidationUtils.rejectIfEmpty(errors, "dateOuverture", "error.date.ouverture.vide");
			}

			if ((!forFiscalView.getGenreImpot().equals(GenreImpot.REVENU_FORTUNE))
					&& (!forFiscalView.getGenreImpot().equals(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE))
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

			if (forFiscalView.getGenreImpot().equals(GenreImpot.REVENU_FORTUNE)) {
				if(forFiscalView.getDateOuverture() != null && forFiscalView.getMotifOuverture() == null){
					if (forFiscalView.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD &&
							(typeFor == TypeForFiscal.PRINCIPAL || typeFor == TypeForFiscal.SECONDAIRE)) {
						errors.rejectValue("motifOuverture", "error.motif.ouverture.vide");
					}
				}
				else if(forFiscalView.getMotifOuverture() != null){
					if(forFiscalView.getNatureTiers().equals(Tiers.NATURE_AUTRECOMMUNAUTE)){
						if(forFiscalView.getMotifOuverture().equals(MotifFor.MAJORITE) ||
								forFiscalView.getMotifOuverture().equals(MotifFor.PERMIS_C_SUISSE) ){
							errors.rejectValue("motifOuverture", "error.motif.ouverture.invalide");
						}
					}
					if ((forFiscalView.getMotifRattachement().equals(MotifRattachement.ACTIVITE_INDEPENDANTE) ||
							forFiscalView.getMotifRattachement().equals(MotifRattachement.DIRIGEANT_SOCIETE))
							&& !forFiscalView.getMotifOuverture().equals(MotifFor.DEBUT_EXPLOITATION)
							&& !forFiscalView.getMotifOuverture().equals(MotifFor.FUSION_COMMUNES)) {
						errors.rejectValue("motifOuverture", "error.motif.ouverture.invalide");

					}
					else if (forFiscalView.getMotifRattachement().equals(MotifRattachement.IMMEUBLE_PRIVE)
							&& !forFiscalView.getMotifOuverture().equals(MotifFor.ACHAT_IMMOBILIER)
							&& !forFiscalView.getMotifOuverture().equals(MotifFor.FUSION_COMMUNES)) {
						errors.rejectValue("motifOuverture", "error.motif.ouverture.invalide");
					}
					else if (forFiscalView.getMotifRattachement().equals(MotifRattachement.SEJOUR_SAISONNIER)
							&& !forFiscalView.getMotifOuverture().equals(MotifFor.SEJOUR_SAISONNIER)
							&& !forFiscalView.getMotifOuverture().equals(MotifFor.FUSION_COMMUNES)) {
						errors.rejectValue("motifOuverture", "error.motif.ouverture.invalide");
					}
				}

				if (forFiscalView.getDateFermeture() != null) {
					if(forFiscalView.getMotifFermeture() == null){
						if (forFiscalView.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD &&
								(typeFor == TypeForFiscal.PRINCIPAL || typeFor == TypeForFiscal.SECONDAIRE)) {
							errors.rejectValue("motifFermeture", "error.motif.fermeture.vide");
						}
					}
					else {
						if(forFiscalView.getNatureTiers().equals(Tiers.NATURE_AUTRECOMMUNAUTE)){
							if(forFiscalView.getMotifFermeture().equals(MotifFor.VEUVAGE_DECES) ||
									forFiscalView.getMotifFermeture().equals(MotifFor.PERMIS_C_SUISSE)){
								errors.rejectValue("motifFermeture", "error.motif.fermeture.invalide");
							}
						}
						if ((forFiscalView.getMotifRattachement().equals(MotifRattachement.ACTIVITE_INDEPENDANTE)||
								forFiscalView.getMotifRattachement().equals(MotifRattachement.DIRIGEANT_SOCIETE))
								&& !forFiscalView.getMotifFermeture().equals(MotifFor.FIN_EXPLOITATION)
								&& !forFiscalView.getMotifFermeture().equals(MotifFor.FUSION_COMMUNES)) {
							errors.rejectValue("motifFermeture", "error.motif.fermeture.invalide");
						}
						else if (forFiscalView.getMotifRattachement().equals(MotifRattachement.IMMEUBLE_PRIVE)
								&& !forFiscalView.getMotifFermeture().equals(MotifFor.VENTE_IMMOBILIER)
								&& !forFiscalView.getMotifFermeture().equals(MotifFor.FUSION_COMMUNES)) {
							errors.rejectValue("motifFermeture", "error.motif.fermeture.invalide");
						}
						else if (forFiscalView.getMotifRattachement().equals(MotifRattachement.SEJOUR_SAISONNIER)
								&& !forFiscalView.getMotifFermeture().equals(MotifFor.SEJOUR_SAISONNIER)
								&& !forFiscalView.getMotifFermeture().equals(MotifFor.FUSION_COMMUNES)) {
							errors.rejectValue("motifFermeture", "error.motif.fermeture.invalide");
						}
					}
				}
			}

			if (forFiscalView.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				if (forFiscalView.getNumeroForFiscalCommune() == null) {
					if (forFiscalView.getLibFractionCommune() == null) {
						errors.rejectValue("libFractionCommune", "error.commune.vide");
					} else {
						errors.rejectValue("libFractionCommune", "error.commune.non.vd");
					}
				}
			}
			else if (forFiscalView.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC) {
				if (forFiscalView.getNumeroForFiscalCommuneHorsCanton() == null) {
					if (forFiscalView.getLibCommuneHorsCanton() == null) {
						errors.rejectValue("libCommuneHorsCanton", "error.commune.vide");
					} else {
						errors.rejectValue("libCommuneHorsCanton", "error.commune.non.hc");
					}
				}
			}
			else if (forFiscalView.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS) {
				if (forFiscalView.getNumeroForFiscalPays() == null) {
					if (forFiscalView.getLibPays() == null) {
						errors.rejectValue("libPays", "error.pays.vide");
					} else {
						errors.rejectValue("libPays", "error.pays.non.valide");
					}
				}
			}

			ForFiscalPrincipal dernierForPrincipal = tiers.getDernierForFiscalPrincipal();
			if ((dernierForPrincipal != null) && (dernierForPrincipal.getDateFin() != null)) {
				if(typeFor == TypeForFiscal.PRINCIPAL){
					if( forFiscalView.getId() == null){
						if( tiers.getForFiscalPrincipalAt(forFiscalView.getRegDateOuverture()) != null) {
							errors.rejectValue("dateOuverture", "error.date.chevauchement");
						}
					}
					for(ForFiscalPrincipal forPrincipal : tiers.getForsFiscauxPrincipauxActifsSorted()){
						if (	forPrincipal.isValidAt(forFiscalView.getRegDateFermeture()) &&
							((forFiscalView.getId() == null) || (forPrincipal.getId().longValue() != forFiscalView.getId().longValue()))){
							errors.rejectValue("dateFermeture", "error.date.chevauchement");
							break;
						}
					}
				}
			}
			ForDebiteurPrestationImposable dernierForDPI = tiers.getDernierForDebiteur();
			if ((dernierForDPI != null) && (dernierForDPI.getDateFin() != null)) {
				if( typeFor == TypeForFiscal.DEBITEUR_PRESTATION_IMPOSABLE ){
					if(forFiscalView.getId() == null){
						if( tiers.getForDebiteurPrestationImposableAt(forFiscalView.getRegDateOuverture()) != null) {
							errors.rejectValue("dateOuverture", "error.date.chevauchement");
						}
					}
					for(ForFiscal forFiscal : tiers.getForsFiscauxValidAt(forFiscalView.getRegDateFermeture())){
						if(	forFiscal instanceof ForDebiteurPrestationImposable &&
							((forFiscalView.getId() == null) || (forFiscal.getId().longValue() != forFiscalView.getId().longValue()))){
							errors.rejectValue("dateFermeture", "error.date.chevauchement");
							break;
						}
					}
				}
			}

			//gestion des droits
			//seul la date de fermeture et le motif de fermeture (si existant) sont éditables
			String msgErrorModeImpo = (forFiscalView.getId() == null) ? "error.mode.imposition.interdit" : "error.tiers.interdit";
			String msgErrorForSec = (forFiscalView.getId() == null) ? "error.motif.rattachement.interdit" : "error.tiers.interdit";

			final Niveau acces = SecurityProvider.getDroitAcces(forFiscalView.getNumeroCtb());
			if (acces == null || acces.equals(Niveau.LECTURE)) {
				errors.reject("error.tiers.interdit");
			}

			if(typeFor == TypeForFiscal.DEBITEUR_PRESTATION_IMPOSABLE){
				if(!SecurityProvider.isGranted(Role.CREATE_DPI)){
					errors.reject("error.tiers.interdit");
				}
			}
			else if(typeFor == TypeForFiscal.PRINCIPAL){
				//forFiscalView.getNatureTiers est tjs != MENEAGE_COMMUN (si couple => HABITANT ou NON_HABITANT
				if(forFiscalView.getNatureTiers().equals(Tiers.NATURE_HABITANT)){
					if((isOrdinaire && !SecurityProvider.isGranted(Role.FOR_PRINC_ORDDEP_HAB)) ||
							(!isOrdinaire && !SecurityProvider.isGranted(Role.FOR_PRINC_SOURC_HAB))){
						errors.reject(msgErrorModeImpo);
					}
				}
				else if(forFiscalView.getNatureTiers().equals(Tiers.NATURE_NONHABITANT)){
					boolean isGris = false;
					if(forFiscalView.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD){
						isGris = true;
					}
					if((isOrdinaire && !isGris && !SecurityProvider.isGranted(Role.FOR_PRINC_ORDDEP_HCHS)) ||
							(!isOrdinaire && !isGris && !SecurityProvider.isGranted(Role.FOR_PRINC_SOURC_HCHS)) ||
							(isOrdinaire && isGris && !SecurityProvider.isGranted(Role.FOR_PRINC_ORDDEP_GRIS)) ||
							(!isOrdinaire && isGris && !SecurityProvider.isGranted(Role.FOR_PRINC_SOURC_GRIS))){
						errors.reject(msgErrorModeImpo);
					}
				}
				else {
					//pour + tard : traiter le cas des entreprises
					errors.reject("error.tiers.interdit");
				}
			}
			else if(typeFor == TypeForFiscal.SECONDAIRE){
				//pour + tard : traiter le cas des entreprises
				if(!SecurityProvider.isGranted(Role.FOR_SECOND_PP)){
					errors.reject(msgErrorForSec);
				}
			}
			else if(typeFor == TypeForFiscal.AUTRE_ELEMENT){
				if(!SecurityProvider.isGranted(Role.FOR_AUTRE)){
					errors.reject(msgErrorForSec);
				}
			}
			else if(typeFor == TypeForFiscal.AUTRE_IMPOT){
				if(!SecurityProvider.isGranted(Role.FOR_AUTRE)){
					errors.reject((forFiscalView.getId() == null) ? "error.genre.impot.interdit" : "error.tiers.interdit");
				}
			}
			if (TypeForFiscal.DEBITEUR_PRESTATION_IMPOSABLE.equals(typeFor)) {
				if (TypeAutoriteFiscale.PAYS_HS.equals(forFiscalView.getTypeAutoriteFiscale())) {
					errors.reject("error.type.autorite.incorrect");
				}
			} else if (!TypeForFiscal.PRINCIPAL.equals(typeFor)
					&& !TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD.equals(forFiscalView.getTypeAutoriteFiscale())) {
				errors.reject("error.type.autorite.incorrect");
			}

		}
		if ((forFiscalView.getDateFermeture() == null) && (forFiscalView.getMotifFermeture() != null)) {
			errors.rejectValue("dateFermeture", "error.date.fermeture.vide");
		}

		if (tiers instanceof PersonnePhysique) {
			if (MotifRattachement.DOMICILE.equals(forFiscalView.getMotifRattachement())
					&& TypeForFiscal.PRINCIPAL.equals(typeFor)
					&& TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD.equals(forFiscalView.getTypeAutoriteFiscale())) {

				final PersonnePhysique pp = (PersonnePhysique) tiers;
				final RegDate date;
				if (forFiscalView.isChangementModeImposition()) {
					date = RegDate.get(forFiscalView.getDateChangement());
				} else {
					date = RegDate.get(forFiscalView.getDateOuverture());
				}

				// [UNIREG-1235] La règle est en fait la suivante:
				// - un contribuable de nationalité suisse ne peut être qu'à l'ordinaire ou indigent
				// - un contribuable étranger avec un permis C peut être à l'ordinaire, indigent, ou à la dépense
				// - pour tous les autres, tous les modes sont admins (donc y compris pour ceux dont on ne connait ni la nationalité ni le permis de séjour)
				final Set<ModeImposition> autorises = new HashSet<ModeImposition>();

				// nationalité suisse ou étrangère ?
				Boolean isSuisse;
				try {
					isSuisse = Boolean.valueOf(tiersService.isSuisse(pp, date));
				}
				catch (TiersException e) {
					// je ne sais pas s'il est suisse ou pas...
					isSuisse = null;
				}

				// Suisse ?
				if (isSuisse != null && isSuisse.booleanValue()) {
					autorises.add(ModeImposition.INDIGENT);
					autorises.add(ModeImposition.ORDINAIRE);
				}
				else {

					// permis de séjour C ou autre ?
					Boolean isSansPermisC;
					try {
						isSansPermisC = Boolean.valueOf(tiersService.isEtrangerSansPermisC(pp, date));
					}
					catch (TiersException e) {
						// on ne sait pas...
						isSansPermisC = null;
					}

					// permis C ?
					if (isSansPermisC != null && !isSansPermisC.booleanValue()) {
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

	/**
	 * @param tiersService the tiersService to set
	 */
	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}
}
