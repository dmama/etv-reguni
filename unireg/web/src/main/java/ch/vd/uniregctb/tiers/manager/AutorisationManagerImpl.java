package ch.vd.uniregctb.tiers.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.Pair;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.view.TiersEditView;
import ch.vd.uniregctb.tiers.view.TiersVisuView;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.Niveau;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class AutorisationManagerImpl implements AutorisationManager {

	private TiersService tiersService;
	private ServiceCivilService serviceCivil;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceCivil(ServiceCivilService serviceCivil) {
		this.serviceCivil = serviceCivil;
	}

	@Override
	public boolean isEditAllowed(Tiers tiers) {

		final Niveau acces = SecurityProvider.getDroitAcces(tiers);
		if (acces == null || acces == Niveau.LECTURE) {
			return false;
		}

		if (tiers instanceof PersonnePhysique || tiers instanceof MenageCommun) {
			return isEditAllowedPP(tiers);
		}
		else if (tiers instanceof CollectiviteAdministrative) {
			return isEditAllowedCA((CollectiviteAdministrative) tiers);
		}
		else {
			return false;
		}
	}

	@Override
	public boolean isEditAllowedPP(Tiers tiers) {

		boolean isHabitant = false;
		Tiers tiersAssujetti;

		if (tiers instanceof PersonnePhysique) {
			PersonnePhysique pp = (PersonnePhysique) tiers;
			MenageCommun menage = tiersService.findMenageCommun(pp, null);
			if (menage != null) {
				tiersAssujetti = menage;
			}
			else tiersAssujetti = tiers;
			if (pp.isHabitantVD()) {
				isHabitant = true;
			}
		}
		else if (tiers instanceof MenageCommun) {
			tiersAssujetti = tiers;
			//les ménages n'ont jamais les onglets civil et rapport prestation
			final MenageCommun menageCommun = (MenageCommun) tiers;
			for (PersonnePhysique pp : tiersService.getPersonnesPhysiques(menageCommun)) {
				if (pp.isHabitantVD()) {
					isHabitant = true;
					break;
				}
			}
		}
		else {
			throw new IllegalArgumentException("Le type de tiers = [" + tiers.getClass().getSimpleName() + "] n'est pas autorisé.");
		}

		final boolean allowed;

		final TypeCtb typeCtb = getTypeCtb(tiersAssujetti);
		switch (typeCtb) {
		case NON_ASSUJETTI:
			allowed = (SecurityProvider.isGranted(Role.MODIF_NONHAB_DEBPUR) && !isHabitant) ||
					(SecurityProvider.isGranted(Role.MODIF_HAB_DEBPUR) && isHabitant);
			break;
		case HC_HS:
			allowed = SecurityProvider.isGranted(Role.MODIF_HC_HS);
			break;
		case ORDINAIRE:
			allowed = SecurityProvider.isGranted(Role.MODIF_VD_ORD);
			break;
		case SOURCIER:
			allowed = SecurityProvider.isGranted(Role.MODIF_VD_SOURC);
			break;
		case MIXTE:
			allowed = SecurityProvider.isAnyGranted(Role.MODIF_VD_ORD, Role.MODIF_VD_SOURC);
			break;
		default:
			throw new IllegalArgumentException("Type de contribuable inconnu = [" + typeCtb + ']');
		}

		return allowed;
	}

	@NotNull
	private static TypeCtb getTypeCtb(@NotNull Tiers tiers) {

		TypeCtb typeCtb = TypeCtb.NON_ASSUJETTI; //0 non assujetti, 1 HC/HS, 2 VD ordinaire, 3 VD sourcier pur, 4 VD sourcier mixte

		final ForFiscalPrincipal ffp = tiers.getForFiscalPrincipalAt(null);
		if (ffp != null) {
			final TypeAutoriteFiscale typeFor = ffp.getTypeAutoriteFiscale();
			if (typeFor == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				final ModeImposition modeImp = ffp.getModeImposition();
				switch (modeImp) {
				case SOURCE:
					typeCtb = TypeCtb.SOURCIER;
					break;
				case MIXTE_137_1:
				case MIXTE_137_2:
					typeCtb = TypeCtb.MIXTE;
					break;
				default:
					typeCtb = TypeCtb.ORDINAIRE;
				}
			}
			else {
				typeCtb = TypeCtb.HC_HS;
			}
		}

		return typeCtb;
	}

	private enum TypeCtb {
		NON_ASSUJETTI,
		HC_HS,
		ORDINAIRE,
		SOURCIER,
		MIXTE
	}

	@Override
	public boolean isEditAllowedCA(CollectiviteAdministrative tiers) {
		return SecurityProvider.isAnyGranted(Role.CREATE_CA, Role.MODIF_CA);
	}

	@Override
	public Map<String, Boolean> getAutorisations(Tiers tiers) {

		final Map<String, Boolean> map = new HashMap<String, Boolean>();

		final Niveau acces = SecurityProvider.getDroitAcces(tiers);
		if (acces == null || acces == Niveau.LECTURE) {
			map.put(TiersVisuView.MODIF_FISCAL, Boolean.FALSE);
			map.put(TiersVisuView.MODIF_CIVIL, Boolean.FALSE);
			map.put(TiersVisuView.MODIF_ADRESSE, Boolean.FALSE);
			map.put(TiersVisuView.MODIF_COMPLEMENT, Boolean.FALSE);
			map.put(TiersVisuView.MODIF_RAPPORT, Boolean.FALSE);
			map.put(TiersVisuView.MODIF_DOSSIER, Boolean.FALSE);
			map.put(TiersVisuView.MODIF_DEBITEUR, Boolean.FALSE);
			map.put(TiersVisuView.MODIF_MOUVEMENT, Boolean.FALSE);
			map.put(TiersEditView.FISCAL_FOR_PRINC, Boolean.FALSE);
			map.put(TiersEditView.FISCAL_FOR_SEC, Boolean.FALSE);
			map.put(TiersEditView.FISCAL_FOR_AUTRE, Boolean.FALSE);
			map.put(TiersEditView.FISCAL_SIT_FAMILLLE, Boolean.FALSE);
			map.put(TiersEditView.ADR_D, Boolean.FALSE);
			map.put(TiersEditView.ADR_C, Boolean.FALSE);
			map.put(TiersEditView.ADR_B, Boolean.FALSE);
			map.put(TiersEditView.ADR_P, Boolean.FALSE);
			map.put(TiersEditView.COMPLEMENT_COMMUNICATION, Boolean.FALSE);
			map.put(TiersEditView.COMPLEMENT_COOR_FIN, Boolean.FALSE);
			map.put(TiersEditView.DOSSIER_TRAVAIL, Boolean.FALSE);
			map.put(TiersEditView.DOSSIER_NO_TRAVAIL, Boolean.FALSE);
			map.put(TiersVisuView.MODIF_DI, Boolean.FALSE);
			return map;
		}

		if (SecurityProvider.isGranted(Role.COOR_FIN)) {
			map.put(TiersVisuView.MODIF_COMPLEMENT, Boolean.TRUE);
			map.put(TiersEditView.COMPLEMENT_COOR_FIN, Boolean.TRUE);
		}

		if (SecurityProvider.isGranted(Role.SUIVI_DOSS)) {
			map.put(TiersVisuView.MODIF_MOUVEMENT, Boolean.TRUE);
		}

		if (tiers.isDesactive(null)) {
			// droits pour un contribuable annulé
			if (SecurityProvider.isGranted(Role.MODIF_NONHAB_INACTIF)) {
				map.put(TiersVisuView.MODIF_COMPLEMENT, Boolean.TRUE);
				map.put(TiersEditView.COMPLEMENT_COMMUNICATION, Boolean.TRUE);
				map.put(TiersVisuView.MODIF_DOSSIER, Boolean.FALSE);
				map.put(TiersVisuView.MODIF_FISCAL, Boolean.FALSE);
				map.put(TiersVisuView.MODIF_DI, Boolean.FALSE);
				if (SecurityProvider.isGranted(Role.ADR_PP_D)) {
					map.put(TiersVisuView.MODIF_ADRESSE, Boolean.TRUE);
					map.put(TiersEditView.ADR_D, Boolean.TRUE);
				}
				if (SecurityProvider.isGranted(Role.ADR_PP_B)) {
					map.put(TiersVisuView.MODIF_ADRESSE, Boolean.TRUE);
					map.put(TiersEditView.ADR_B, Boolean.TRUE);
				}
				if (SecurityProvider.isGranted(Role.ADR_PP_C)) {
					map.put(TiersVisuView.MODIF_ADRESSE, Boolean.TRUE);
					map.put(TiersEditView.ADR_C, Boolean.TRUE);
				}
				if (SecurityProvider.isGranted(Role.ADR_P)) {
					map.put(TiersVisuView.MODIF_ADRESSE, Boolean.TRUE);
					map.put(TiersEditView.ADR_P, Boolean.TRUE);
				}
			}
			return map;
		}

		if (tiers instanceof Contribuable) {
			if (SecurityProvider.isGranted(Role.CREATE_DPI)) {
				map.put(TiersVisuView.MODIF_DEBITEUR, Boolean.TRUE);
			}
			if ((tiers instanceof PersonnePhysique || tiers instanceof MenageCommun)) {
				if (SecurityProvider.isGranted(Role.SIT_FAM)) {
					Contribuable contribuable = (Contribuable) tiers;
					boolean isSitFamActive = isSituationFamilleActive(contribuable);
					boolean civilOK = true;
					if (tiers instanceof PersonnePhysique) {
						PersonnePhysique pp = (PersonnePhysique) tiers;
						if (pp.isHabitantVD()) {
							Individu ind = serviceCivil.getIndividu(pp.getNumeroIndividu(), null);
							for (EtatCivil etatCivil : ind.getEtatsCivils()) {
								if (etatCivil.getDateDebut() == null) {
									civilOK = false;
								}
							}
						}
					}
					if (civilOK && (isSitFamActive || !contribuable.getSituationsFamille().isEmpty())) {
						map.put(TiersVisuView.MODIF_FISCAL, Boolean.TRUE);
						map.put(TiersEditView.FISCAL_SIT_FAMILLLE, Boolean.TRUE);
					}
				}
				if (SecurityProvider.isAnyGranted(Role.DI_EMIS_PP, Role.DI_DELAI_PM, Role.DI_DUPLIC_PP, Role.DI_QUIT_PP, Role.DI_SOM_PP)) {
					map.put(TiersVisuView.MODIF_DI, Boolean.TRUE);
				}
			}
		}

		if (tiers instanceof PersonnePhysique) {
			PersonnePhysique pp = (PersonnePhysique) tiers;
			if (pp.isHabitantVD()) {
				setDroitHabitant(tiers, map);
			}
			else {
				setDroitNonHabitant(tiers, map);
			}
		}
		else if (tiers instanceof MenageCommun) {
			//les ménages n'ont jamais les onglets civil et rapport prestation
			MenageCommun menageCommun = (MenageCommun) tiers;
			boolean isHabitant = false;
			for (PersonnePhysique pp : tiersService.getPersonnesPhysiques(menageCommun)) {
				if (pp.isHabitantVD()) {
					isHabitant = true;
					break;
				}
			}
			if (isHabitant) {
				setDroitHabitant(tiers, map);
			}
			else {
				setDroitNonHabitant(tiers, map);
			}
		}
		else if (tiers instanceof AutreCommunaute) {
			//les autres communautés n'ont jamais les onglets fiscal, rapport prestation et dossier apparenté
			if (SecurityProvider.isGranted(Role.MODIF_AC)) {
				map.put(TiersVisuView.MODIF_CIVIL, Boolean.TRUE);
				map.put(TiersVisuView.MODIF_COMPLEMENT, Boolean.TRUE);
				map.put(TiersEditView.COMPLEMENT_COMMUNICATION, Boolean.TRUE);
				if (SecurityProvider.isGranted(Role.ADR_PM_D)) {
					map.put(TiersVisuView.MODIF_ADRESSE, Boolean.TRUE);
					map.put(TiersEditView.ADR_D, Boolean.TRUE);
				}
				if (SecurityProvider.isGranted(Role.ADR_PM_B)) {
					map.put(TiersVisuView.MODIF_ADRESSE, Boolean.TRUE);
					map.put(TiersEditView.ADR_B, Boolean.TRUE);
				}
				if (SecurityProvider.isGranted(Role.ADR_PM_C)) {
					map.put(TiersVisuView.MODIF_ADRESSE, Boolean.TRUE);
					map.put(TiersEditView.ADR_C, Boolean.TRUE);
				}
				if (SecurityProvider.isGranted(Role.ADR_P)) {
					map.put(TiersVisuView.MODIF_ADRESSE, Boolean.TRUE);
					map.put(TiersEditView.ADR_P, Boolean.TRUE);
				}
			}
		}
		else if (tiers instanceof DebiteurPrestationImposable) {
			//les DPI n'ont jamais les onglets civil, dossier apparenté et débiteur IS
			if (SecurityProvider.isGranted(Role.CREATE_DPI)) {
				map.put(TiersVisuView.MODIF_FISCAL, Boolean.TRUE);
				map.put(TiersVisuView.MODIF_ADRESSE, Boolean.TRUE);
				map.put(TiersEditView.ADR_B, Boolean.TRUE);
				map.put(TiersEditView.ADR_C, Boolean.TRUE);
				map.put(TiersEditView.ADR_D, Boolean.TRUE);
				map.put(TiersVisuView.MODIF_COMPLEMENT, Boolean.TRUE);
				map.put(TiersEditView.COMPLEMENT_COMMUNICATION, Boolean.TRUE);
			}
			if (SecurityProvider.isGranted(Role.RT)) {
				map.put(TiersVisuView.MODIF_RAPPORT, Boolean.TRUE);
			}
			if (SecurityProvider.isGranted(Role.ADR_P)) {
				map.put(TiersVisuView.MODIF_ADRESSE, Boolean.TRUE);
				map.put(TiersEditView.ADR_P, Boolean.TRUE);
			}
		}

		// UNIREG-2120 Possibilite de créer un debiteur à partir d'une collectivité administrative
	    // UNIREG-3362 Création de débiteur à partir d'une PM
		else if (tiers instanceof CollectiviteAdministrative || tiers instanceof Entreprise) {
			map.put(TiersVisuView.MODIF_COMPLEMENT, Boolean.FALSE);
			map.put(TiersVisuView.MODIF_MOUVEMENT, Boolean.FALSE);
		}
		
		return map;
	}

	/**
	 * Indique si l'on a le droit ou non de saisir une nouvelle situation de famille
	 *
	 * @param contribuable un contribuable
	 * @return <b>vrai</b> si l'utilisateur courant a le droit de saisir une nouvelle situation de famille sur le contribuable spécifié; <b>faux</b> autrement.
	 */
	protected boolean isSituationFamilleActive(Contribuable contribuable) {
		Set<ForFiscal> forsFiscaux = contribuable.getForsFiscaux();
		for (ForFiscal forFiscal : forsFiscaux) {
			if (forFiscal instanceof ForFiscalPrincipal) {
				ForFiscalPrincipal forFiscalPrincipal = (ForFiscalPrincipal) forFiscal;
				//[UNIREG-1278] il doit être possible de créer une situation de famille même si le contribuable est hors canton
				if (forFiscalPrincipal.getDateFin() == null) {
					return true;
				}
			}
			if (forFiscal instanceof ForFiscalSecondaire) {
				ForFiscalSecondaire forFiscalSecondaire = (ForFiscalSecondaire) forFiscal;
				if (forFiscalSecondaire.getDateFin() == null) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * enrichi la map de droit d'édition des onglets pour un habitant ou un ménage commun considéré habitant
	 *
	 * @param tiers
	 * @param allowedOnglet
	 */
	private boolean setDroitHabitant(Tiers tiers, Map<String, Boolean> allowedOnglet) {

		Assert.isTrue(tiers instanceof PersonnePhysique || tiers instanceof MenageCommun, "Le tiers " + tiers.getNumero() + " n'est ni une personne physique ni un ménage commun");

		//les habitants n'ont jamais les onglets civil et rapport prestation
		boolean isEditable = codeFactorise1(tiers, allowedOnglet);
		if (isEditAllowedPP(tiers)) {
			codeFactorise2(allowedOnglet);
			isEditable = true;
		}
		isEditable = codeFactorise3(tiers, allowedOnglet, isEditable);

		final boolean isPersonnePhysique = tiers instanceof PersonnePhysique;
		TypeImposition typeImposition = calculeTypeImposition(tiers);
		if (typeImposition == TypeImposition.AUCUN_FOR_ACTIF && isPersonnePhysique) {
			// [UNIREG-1736] un sourcier est un individu qui a un for source ou dont le
			// ménage commun actif a un for source...
			final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple((PersonnePhysique) tiers, null);
			if (ensemble != null) {
				final MenageCommun menage = ensemble.getMenage();
				typeImposition = calculeTypeImposition(menage);
			}
		}

		if ((typeImposition.isOrdinaireDepenseOuNonActif() && SecurityProvider.isGranted(Role.FOR_PRINC_ORDDEP_HAB)) ||
				(typeImposition.isSourcierOuNonActif() && SecurityProvider.isGranted(Role.FOR_PRINC_SOURC_HAB))) {
			allowedOnglet.put(TiersVisuView.MODIF_FISCAL, Boolean.TRUE);
			allowedOnglet.put(TiersEditView.FISCAL_FOR_PRINC, Boolean.TRUE);
			isEditable = true;
		}
		if (isPersonnePhysique && typeImposition == TypeImposition.SOURCIER && SecurityProvider.isGranted(Role.RT)) {
			allowedOnglet.put(TiersVisuView.MODIF_DOSSIER, Boolean.TRUE);
			allowedOnglet.put(TiersEditView.DOSSIER_TRAVAIL, Boolean.TRUE);
			isEditable = true;
		}
		return isEditable;
	}

	/**
	 * enrichi la map de droit d'édition des onglets pour un non habitant ou un ménage commun considéré non habitant
	 *
	 * @param tiers
	 * @param allowedOnglet
	 */
	private boolean setDroitNonHabitant(Tiers tiers, Map<String, Boolean> allowedOnglet) {

		//les non habitants n'ont jamais l'onglet rapport prestation
		//les ménage commun n'ont jamais les onglets civil et rapport prestation

		Assert.isTrue(tiers instanceof PersonnePhysique || tiers instanceof MenageCommun, "Le tiers " + tiers.getNumero() + " n'est ni une personne physique ni un ménage commun");

		final boolean isPersonnePhysique = tiers instanceof PersonnePhysique;

		boolean isEditable = codeFactorise1(tiers, allowedOnglet);
		if (tiers.isDebiteurInactif()) {//I107
			if (SecurityProvider.isGranted(Role.MODIF_NONHAB_INACTIF)) {
				if (isPersonnePhysique) {
					allowedOnglet.put(TiersVisuView.MODIF_CIVIL, Boolean.TRUE);
				}
				allowedOnglet.put(TiersVisuView.MODIF_COMPLEMENT, Boolean.TRUE);
				allowedOnglet.put(TiersEditView.COMPLEMENT_COMMUNICATION, Boolean.TRUE);
				allowedOnglet.put(TiersVisuView.MODIF_DOSSIER, Boolean.FALSE);
				allowedOnglet.put(TiersVisuView.MODIF_FISCAL, Boolean.FALSE);
				if (SecurityProvider.isGranted(Role.ADR_PP_D)) {
					allowedOnglet.put(TiersVisuView.MODIF_ADRESSE, Boolean.TRUE);
					allowedOnglet.put(TiersEditView.ADR_D, Boolean.TRUE);
				}
				if (SecurityProvider.isGranted(Role.ADR_PP_B)) {
					allowedOnglet.put(TiersVisuView.MODIF_ADRESSE, Boolean.TRUE);
					allowedOnglet.put(TiersEditView.ADR_B, Boolean.TRUE);
				}
				if (SecurityProvider.isGranted(Role.ADR_PP_C)) {
					allowedOnglet.put(TiersVisuView.MODIF_ADRESSE, Boolean.TRUE);
					allowedOnglet.put(TiersEditView.ADR_C, Boolean.TRUE);
				}
				isEditable = true;
			}
		}
		else {
			if (isEditAllowedPP(tiers)) {
				if (isPersonnePhysique) {
					allowedOnglet.put(TiersVisuView.MODIF_CIVIL, Boolean.TRUE);
				}
				codeFactorise2(allowedOnglet);
				isEditable = true;
			}

			isEditable = codeFactorise3(tiers, allowedOnglet, isEditable);

			Pair<TypeImposition, TypeAutoriteFiscale> types = calculeTypeImpositionEtAutoriteFiscale(tiers);
			if (types.getFirst() == TypeImposition.AUCUN_FOR_ACTIF && isPersonnePhysique) {
				// [UNIREG-1736] un sourcier est un individu qui a un for source ou dont le
				// ménage commun actif a un for source...
				final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple((PersonnePhysique) tiers, null);
				if (ensemble != null) {
					final MenageCommun menage = ensemble.getMenage();
					types = calculeTypeImpositionEtAutoriteFiscale(menage);
				}
			}

			final TypeImposition typeImposition = types.getFirst();
			final TypeAutoriteFiscale typeAutoriteFiscale = types.getSecond();
			if (isPersonnePhysique && typeImposition == TypeImposition.SOURCIER && SecurityProvider.isGranted(Role.RT)) {
				allowedOnglet.put(TiersVisuView.MODIF_DOSSIER, Boolean.TRUE);
				allowedOnglet.put(TiersEditView.DOSSIER_TRAVAIL, Boolean.TRUE);
				isEditable = true;
			}
			final boolean autoriteFiscaleVaudoiseOuIndeterminee = typeAutoriteFiscale == null || typeAutoriteFiscale == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
			final boolean autoriteFiscaleNonVaudoiseOuIndeterminee = typeAutoriteFiscale != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
			if ((typeImposition.isOrdinaireDepenseOuNonActif() && autoriteFiscaleNonVaudoiseOuIndeterminee && SecurityProvider.isGranted(Role.FOR_PRINC_ORDDEP_HCHS)) ||
					(typeImposition.isOrdinaireDepenseOuNonActif() && autoriteFiscaleVaudoiseOuIndeterminee && SecurityProvider.isGranted(Role.FOR_PRINC_ORDDEP_GRIS)) ||
					(typeImposition.isSourcierOuNonActif() && autoriteFiscaleNonVaudoiseOuIndeterminee && SecurityProvider.isGranted(Role.FOR_PRINC_SOURC_HCHS)) ||
					(typeImposition.isSourcierOuNonActif() && autoriteFiscaleVaudoiseOuIndeterminee && SecurityProvider.isGranted(Role.FOR_PRINC_SOURC_GRIS))) {
				allowedOnglet.put(TiersVisuView.MODIF_FISCAL, Boolean.TRUE);
				allowedOnglet.put(TiersEditView.FISCAL_FOR_PRINC, Boolean.TRUE);
				isEditable = true;
			}
		}
		return isEditable;
	}

	/**
	 * Code commun pour les méthodes setDroitNonHabitant et setDroitHabitant
	 *
	 * @param tiers
	 * @param allowedOnglet
	 * @return
	 */
	private boolean codeFactorise1(Tiers tiers, Map<String, Boolean> allowedOnglet) {
		boolean isEditable = false;
		if (SecurityProvider.isGranted(Role.ADR_P)) {
			allowedOnglet.put(TiersVisuView.MODIF_ADRESSE, Boolean.TRUE);
			allowedOnglet.put(TiersEditView.ADR_P, Boolean.TRUE);
			isEditable = true;
		}

		if (SecurityProvider.isGranted(Role.ADR_PP_C_DCD) && tiers instanceof PersonnePhysique) {
			PersonnePhysique pp = (PersonnePhysique) tiers;
			if (tiersService.isDecede(pp)) {
				allowedOnglet.put(TiersVisuView.MODIF_ADRESSE, Boolean.TRUE);
				allowedOnglet.put(TiersEditView.ADR_C, Boolean.TRUE);
				isEditable = true;
			}
		}
		else if (SecurityProvider.isGranted(Role.ADR_PP_C_DCD) && tiers instanceof MenageCommun) {
			MenageCommun mc = (MenageCommun) tiers;
			for (PersonnePhysique pp : tiersService.getPersonnesPhysiques(mc)) {
				if (tiersService.isDecede(pp)) {
					allowedOnglet.put(TiersVisuView.MODIF_ADRESSE, Boolean.TRUE);
					allowedOnglet.put(TiersEditView.ADR_C, Boolean.TRUE);
					isEditable = true;
					break;
				}
			}
		}
		return isEditable;
	}

	/**
	 * Code commun pour les méthodes setDroitNonHabitant et setDroitHabitant
	 *
	 * @param allowedOnglet
	 * @return
	 */
	private void codeFactorise2(Map<String, Boolean> allowedOnglet) {
		allowedOnglet.put(TiersVisuView.MODIF_COMPLEMENT, Boolean.TRUE);
		allowedOnglet.put(TiersEditView.COMPLEMENT_COMMUNICATION, Boolean.TRUE);
		allowedOnglet.put(TiersVisuView.MODIF_DOSSIER, Boolean.TRUE);
		allowedOnglet.put(TiersEditView.DOSSIER_NO_TRAVAIL, Boolean.TRUE);
		if (SecurityProvider.isGranted(Role.ADR_PP_D)) {
			allowedOnglet.put(TiersVisuView.MODIF_ADRESSE, Boolean.TRUE);
			allowedOnglet.put(TiersEditView.ADR_D, Boolean.TRUE);
		}
		if (SecurityProvider.isGranted(Role.ADR_PP_B)) {
			allowedOnglet.put(TiersVisuView.MODIF_ADRESSE, Boolean.TRUE);
			allowedOnglet.put(TiersEditView.ADR_B, Boolean.TRUE);
		}
		if (SecurityProvider.isGranted(Role.ADR_PP_C)) {
			allowedOnglet.put(TiersVisuView.MODIF_ADRESSE, Boolean.TRUE);
			allowedOnglet.put(TiersEditView.ADR_C, Boolean.TRUE);
		}
	}

	/**
	 * Code commun pour les méthodes setDroitNonHabitant et setDroitHabitant
	 *
	 * @param tiers
	 * @param allowedOnglet
	 * @param isEditable
	 * @return
	 */
	private boolean codeFactorise3(Tiers tiers, Map<String, Boolean> allowedOnglet,
	                               boolean isEditable) {
		if (!tiers.getForsFiscauxPrincipauxActifsSorted().isEmpty() && SecurityProvider.isGranted(Role.FOR_SECOND_PP)) {
			allowedOnglet.put(TiersVisuView.MODIF_FISCAL, Boolean.TRUE);
			allowedOnglet.put(TiersEditView.FISCAL_FOR_SEC, Boolean.TRUE);
			isEditable = true;
		}
		if (SecurityProvider.isGranted(Role.FOR_AUTRE)) {
			allowedOnglet.put(TiersVisuView.MODIF_FISCAL, Boolean.TRUE);
			allowedOnglet.put(TiersEditView.FISCAL_FOR_AUTRE, Boolean.TRUE);
			isEditable = true;
		}
		return isEditable;
	}

	private static enum TypeImposition {
		AUCUN_FOR_ACTIF,
		ORDINAIRE_DEPENSE,
		SOURCIER;

		public boolean isOrdinaireDepenseOuNonActif() {
			return this == AUCUN_FOR_ACTIF || this == ORDINAIRE_DEPENSE;
		}

		public boolean isSourcierOuNonActif() {
			return this == AUCUN_FOR_ACTIF || this == SOURCIER;
		}
	}

	private static TypeImposition calculeTypeImposition(Tiers tiers) {
		final TypeImposition type;
		final ForFiscalPrincipal forFiscalPrincipal = tiers.getForFiscalPrincipalAt(null);
		if (forFiscalPrincipal != null) {
			final ModeImposition modeImposition = tiers.getForFiscalPrincipalAt(null).getModeImposition();
			switch (modeImposition) {
			case SOURCE:
			case MIXTE_137_1:
			case MIXTE_137_2:
				type = TypeImposition.SOURCIER;
				break;
			default:
				type = TypeImposition.ORDINAIRE_DEPENSE;
				break;
			}
		}
		else {
			type = TypeImposition.AUCUN_FOR_ACTIF;
		}
		return type;
	}

	/**
	 * Le type d'autorité fiscale est null en cas d'absence de for fiscal principal actif
	 *
	 * @param tiers
	 * @return
	 */
	private static Pair<TypeImposition, TypeAutoriteFiscale> calculeTypeImpositionEtAutoriteFiscale(Tiers tiers) {
		final TypeImposition typeImposition = calculeTypeImposition(tiers);
		final TypeAutoriteFiscale typeAutoriteFiscale;
		final ForFiscalPrincipal forFiscalPrincipal = tiers.getForFiscalPrincipalAt(null);
		if (forFiscalPrincipal != null) {
			typeAutoriteFiscale = forFiscalPrincipal.getTypeAutoriteFiscale();
		}
		else {
			typeAutoriteFiscale = null;
		}
		return new Pair<TypeImposition, TypeAutoriteFiscale>(typeImposition, typeAutoriteFiscale);
	}
}
