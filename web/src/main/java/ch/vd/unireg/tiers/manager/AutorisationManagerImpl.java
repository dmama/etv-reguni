package ch.vd.unireg.tiers.manager;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.service.ServiceCivilService;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.mandataire.AccesMandatairesView;
import ch.vd.unireg.mandataire.ConfigurationMandataire;
import ch.vd.unireg.metier.assujettissement.Assujettissement;
import ch.vd.unireg.metier.assujettissement.TypeAssujettissement;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.AutreCommunaute;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.ForFiscalSecondaire;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.NatureTiers;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersException;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Niveau;
import ch.vd.unireg.type.TypeAutoriteFiscale;

import static ch.vd.unireg.security.Role.*;
import static ch.vd.unireg.security.SecurityHelper.isGranted;
import static ch.vd.unireg.type.TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class AutorisationManagerImpl implements AutorisationManager {

	static final String MODIF_FISCAL = "FISCAL";
	static final String MODIF_CIVIL = "CIVIL";
	static final String MODIF_ADRESSE = "ADR";
	static final String MODIF_COMPLEMENT = "CPLT";
	static final String MODIF_RAPPORT = "RPT";
	static final String MODIF_ETABLISSEMENT = "ETAB";
	static final String MODIF_DOSSIER = "DOS";
	static final String MODIF_DEBITEUR = "DBT";
	static final String MODIF_DI = "DI";
	static final String MODIF_QSNC = "QSNC";
	static final String MODIF_BOUCLEMENTS = "BOUCLEMENTS";
	static final String MODIF_ETATS_PM = "ETATS_PM";
	static final String MODIF_REGIMES_FISCAUX = "REGIMES_FISCAUX";
	static final String MODIF_ALLEGEMENTS_FISCAUX = "ALLEGEMENTS_FISCAUX";
	static final String MODIF_FLAGS_PM = "FLAGS_PM";
	static final String MODIF_AUTRES_DOCS_FISCAUX = "AUTRES_DOCS";
	static final String MODIF_IDE = "IDE";
	static final String MODIF_MOUVEMENT = "MVT";
	static final String FISCAL_FOR_PRINC = "FOR_PRINC";
	static final String FISCAL_FOR_SEC = "FOR_SEC";
	static final String FISCAL_FOR_AUTRE = "FOR_AUTRE";
	static final String FISCAL_SIT_FAMILLLE = "SIT_FAM";
	static final String ADR_D = "ADR_D";
	static final String ADR_C = "ADR_C";
	static final String ADR_B = "ADR_B";
	static final String ADR_P = "ADR_P";
	static final String COMPLEMENT_COMMUNICATION = "CPLT_COM";
	static final String COMPLEMENT_COOR_FIN = "CPLT_COOR_FIN";
	static final String DOSSIER_TRAVAIL = "DOS_TRA";
	static final String DOSSIER_NO_TRAVAIL = "DOS_NO_TRA";
	static final String FISCAL_DECISION_ACI = "DEC_ACI";
	static final String MODIF_CTB_AVEC_DECISION_ACI = "CTB_DCI_ACI";
	static final String MODIF_MANDATS = "MANDATS";
	static final String MODIF_MANDATS_SPECIAUX = "MANDATS_SPECIAUX";
	static final String MODIF_MANDATS_GENERAUX = "MANDATS_GENERAUX";
	static final String MODIF_MANDATS_TIERS = "MANDATS_TIERS";
	static final String MODIF_REMARQUES = "REMARQUES";
	static final String MODIF_DEMANDES_DEGREVEMENT_ICI = "DEMANDES_DEGREVEMENT_ICI";
	static final String MODIF_DEGREVEMENTS_ICI = "DEGREVEMENTS_ICI";
	static final String MODIF_EXONERATIONS_IFONC = "EXONERATIONS_IFONC";
	static final String MODIF_ETIQUETTES = "ETIQUETTES";

	private TiersService tiersService;
	private ServiceCivilService serviceCivil;
	private SecurityProviderInterface securityProvider;
	private ServiceInfrastructureService infrastructureService;
	private ConfigurationMandataire configurationMandataire;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceCivil(ServiceCivilService serviceCivil) {
		this.serviceCivil = serviceCivil;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setInfrastructureService(ServiceInfrastructureService infrastructureService) {
		this.infrastructureService = infrastructureService;
	}

	public void setConfigurationMandataire(ConfigurationMandataire configurationMandataire) {
		this.configurationMandataire = configurationMandataire;
	}

	@Override
	public boolean isVisuAllowed(@NotNull Tiers tiers) {

		if (!SecurityHelper.isGranted(securityProvider, Role.VISU_ALL)) { // visualisation limitée

			// visualisation limitée : pas de droits pour les inactifs, les DPI et les gris (selon SIFISC-25963, gris = non-habitant avec for vaudois source-pure)
			if (tiers.isDebiteurInactif() || tiers.getNatureTiers() == NatureTiers.DebiteurPrestationImposable) {
				return false;
			}

			// visualisation limitée : pas de droits pour les gris (selon SIFISC-25963, gris = non-habitant avec for vaudois source-pure)
			final ForFiscalPrincipalPP ffpCourant = getForFiscalCourant(tiers, ForFiscalPrincipalPP.class);
			if (tiers.getNatureTiers() == NatureTiers.NonHabitant && ffpCourant != null &&
					ffpCourant.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD &&
					ffpCourant.getModeImposition() == ModeImposition.SOURCE) {
				return false;
			}
		}

		// dans tous les autres cas : ok
		return true;
	}

	private static <T extends ForFiscalPrincipal> T getForFiscalCourant(@NotNull Tiers tiers, Class<T> clazz) {
		return Stream.of(tiers)
				.map(t -> t.getForsFiscauxValidAt(null))
				.flatMap(List::stream)
				.filter(clazz::isInstance)
				.map(clazz::cast)
				.findFirst()
				.orElse(null);
	}


	@Override
	public boolean isEditAllowed(Tiers tiers) {

		final Niveau acces = SecurityHelper.getDroitAcces(securityProvider, tiers);
		if (acces == null || acces == Niveau.LECTURE) {
			return false;
		}

		if (tiers instanceof ContribuableImpositionPersonnesPhysiques) {
			return isEditAllowedPP((ContribuableImpositionPersonnesPhysiques) tiers);
		}
		else if (tiers instanceof CollectiviteAdministrative) {
			return isEditAllowedCA((CollectiviteAdministrative) tiers);
		}
		else if (tiers instanceof Entreprise) {
			return isEditAllowedEntreprise((Entreprise) tiers);
		}
		else if (tiers instanceof Etablissement) {
			return isEditAllowedEtablissement((Etablissement) tiers);
		}
		else if (tiers instanceof DebiteurPrestationImposable) {
			return isEditAllowedDPI();
		}
		else if (tiers instanceof AutreCommunaute) {
			//SIFISC-29391, Edition des autres Communautés sans plus de vérification pour être conforme à la 18R2.
			return true;
		}
		else {
			return false;
		}
	}

	private boolean isEditAllowedDPI() {
		return SecurityHelper.isAnyGranted(securityProvider, Role.CREATE_MODIF_DPI);
	}

	@Override
	public boolean isEditAllowedPP(ContribuableImpositionPersonnesPhysiques tiers) {
		return isEditAllowedPP(tiers, AuthenticationHelper.getCurrentPrincipal(), AuthenticationHelper.getCurrentOID());
	}

	private boolean isEditAllowedPP(ContribuableImpositionPersonnesPhysiques tiers, String visa, int oid) {
		boolean isHabitant = false;
		final ContribuableImpositionPersonnesPhysiques tiersAssujetti;

		if (tiers instanceof PersonnePhysique) {
			final PersonnePhysique pp = (PersonnePhysique) tiers;
			final MenageCommun menage = tiersService.findMenageCommun(pp, null);
			if (menage != null) {
				tiersAssujetti = menage;
			}
			else {
				tiersAssujetti = pp;
			}
			if (pp.isHabitantVD()) {
				isHabitant = true;
			}
		}
		else if (tiers instanceof MenageCommun) {
			//les ménages n'ont jamais les onglets civil et rapport prestation
			final MenageCommun menageCommun = (MenageCommun) tiers;
			tiersAssujetti = menageCommun;
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
			allowed = (SecurityHelper.isGranted(securityProvider, Role.MODIF_NONHAB_DEBPUR, visa, oid) && !isHabitant) ||
					(SecurityHelper.isGranted(securityProvider, Role.MODIF_HAB_DEBPUR, visa, oid) && isHabitant);
			break;
		case HC_HS:
			allowed = SecurityHelper.isGranted(securityProvider, Role.MODIF_HC_HS, visa, oid);
			break;
		case ORDINAIRE:
			allowed = SecurityHelper.isGranted(securityProvider, Role.MODIF_VD_ORD, visa, oid);
			break;
		case SOURCIER:
			allowed = SecurityHelper.isGranted(securityProvider, Role.MODIF_VD_SOURC, visa, oid);
			break;
		case MIXTE:
			allowed = SecurityHelper.isAnyGranted(securityProvider, visa, oid, Role.MODIF_VD_ORD, Role.MODIF_VD_SOURC);
			break;
		default:
			throw new IllegalArgumentException("Type de contribuable inconnu = [" + typeCtb + ']');
		}

		return allowed;
	}

	@NotNull
	private static TypeCtb getTypeCtb(@NotNull ContribuableImpositionPersonnesPhysiques tiers) {

		TypeCtb typeCtb = TypeCtb.NON_ASSUJETTI; //0 non assujetti, 1 HC/HS, 2 VD ordinaire, 3 VD sourcier pur, 4 VD sourcier mixte

		final ForFiscalPrincipalPP ffp = tiers.getForFiscalPrincipalAt(null);
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
		return SecurityHelper.isAnyGranted(securityProvider, Role.CREATE_CA, Role.MODIF_CA);
	}

	@Override
	public boolean isEditAllowedEntreprise(Entreprise tiers) {
		return SecurityHelper.isAnyGranted(securityProvider, Role.MODIF_PM, Role.CREATE_ENTREPRISE);
	}

	@Override
	public boolean isEditAllowedEtablissement(Etablissement tiers) {
		return SecurityHelper.isAnyGranted(securityProvider, Role.MODIF_PM, Role.ETABLISSEMENTS);
	}

	@NotNull
	@Override
	public Autorisations getAutorisations(@Nullable Tiers tiers, String visa, int oid) {
		final Map<String, Boolean> map = getAutorisationsMap(tiers, visa, oid);
		return new Autorisations(map);
	}

	@Override
	public RetourModeImpositionAllowed isModeImpositionAllowed(@NotNull Tiers tiers, @NotNull ModeImposition modeImposition, @NotNull TypeAutoriteFiscale typeAutoriteFiscale, MotifRattachement motifRattachement,
	                                                           RegDate date, String visa, int oid) {

		final NatureTiers natureTiers = getNatureTiersRestreinte(tiers);
		final boolean isOrdinaire = modeImposition == ModeImposition.ORDINAIRE || modeImposition == ModeImposition.DEPENSE || modeImposition == ModeImposition.INDIGENT;

		// Vérification des droits de l'utilisateur
		if (natureTiers == NatureTiers.Habitant) {
			if ((isOrdinaire && !SecurityHelper.isGranted(securityProvider, Role.FOR_PRINC_ORDDEP_HAB, visa, oid)) ||
					(!isOrdinaire && !SecurityHelper.isGranted(securityProvider, Role.FOR_PRINC_SOURC_HAB, visa, oid))) {

				return RetourModeImpositionAllowed.INTERDIT;
			}
		}
		else if (natureTiers == NatureTiers.NonHabitant) {
			boolean isGris = false;
			if (typeAutoriteFiscale == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				isGris = true;
			}
			if ((isOrdinaire && !isGris && !SecurityHelper.isGranted(securityProvider, Role.FOR_PRINC_ORDDEP_HCHS, visa, oid)) ||
					(!isOrdinaire && !isGris && !SecurityHelper.isGranted(securityProvider, Role.FOR_PRINC_SOURC_HCHS, visa, oid)) ||
					(isOrdinaire && isGris && !SecurityHelper.isGranted(securityProvider, Role.FOR_PRINC_ORDDEP_GRIS, visa, oid)) ||
					(!isOrdinaire && isGris && !SecurityHelper.isGranted(securityProvider, Role.FOR_PRINC_SOURC_GRIS, visa, oid))) {

				return RetourModeImpositionAllowed.DROITS_INCOHERENTS;
			}
		}

		// Vérification de la cohérence métier du mode d'imposition
		if (tiers instanceof PersonnePhysique) {
			if (MotifRattachement.DOMICILE == motifRattachement && TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD == typeAutoriteFiscale) {

				final PersonnePhysique pp = (PersonnePhysique) tiers;

				// [UNIREG-1235]
				// La règle est la suivante:
				// - un contribuable de nationalité suisse ne peut être qu'à l'ordinaire ou indigent
				// - un contribuable étranger avec un permis C peut être à l'ordinaire, indigent, ou à la dépense
				// - pour tous les autres, tous les modes sont admis (donc y compris pour ceux dont on ne connait ni la nationalité ni le permis de séjour)
				// - [SIFISC-4528] exception pour les non-habitants étrangers, on ne contrôle pas leur permis pour pouvoir eventuellement leur ajouter un for source
				//   antérieur à leur obtention du permis C,
				final Set<ModeImposition> autorises = EnumSet.noneOf(ModeImposition.class);

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

				if (!autorises.contains(modeImposition)) {
					return RetourModeImpositionAllowed.REGLES_INCOHERENTES;
				}
			}
		}

		return RetourModeImpositionAllowed.OK;
	}

	private NatureTiers getNatureTiersRestreinte(Tiers tiers) {
		if (tiers instanceof MenageCommun) {
			final MenageCommun menageCommun = (MenageCommun) tiers;
			for (PersonnePhysique pp : tiersService.getPersonnesPhysiques(menageCommun)) {
				if (pp.isHabitantVD()) {
					return NatureTiers.Habitant;
				}
			}
			return NatureTiers.NonHabitant;
		}
		else {
			return tiers.getNatureTiers();
		}
	}

	@NotNull
	private Map<String, Boolean> getAutorisationsMap(@Nullable Tiers tiers, String visa, int oid) {

		final Map<String, Boolean> map = new HashMap<>();

		if (tiers == null) {
			// cas spécial du tiers nul : le tiers est entrain d'être crée. Les droits ci-dessous sont appliqués dans ce cas-là.
			map.put(MODIF_CIVIL, Boolean.TRUE);
			if (SecurityHelper.isGranted(securityProvider, Role.COMPLT_COMM, visa, oid)) {
				map.put(MODIF_COMPLEMENT, Boolean.TRUE);
				map.put(COMPLEMENT_COMMUNICATION, Boolean.TRUE);
			}
			if (SecurityHelper.isGranted(securityProvider, Role.COOR_FIN, visa, oid)) {
				map.put(MODIF_COMPLEMENT, Boolean.TRUE);
				map.put(COMPLEMENT_COOR_FIN, Boolean.TRUE);
			}
			map.put(MODIF_FISCAL, Boolean.TRUE); // pour la création de débiteur
			return map;
		}

		final Niveau acces = SecurityHelper.getDroitAcces(securityProvider, visa, tiers.getNumero());
		if (acces == null || acces == Niveau.LECTURE || !SecurityHelper.isGranted(securityProvider, Role.VISU_ALL)) {
			map.put(MODIF_FISCAL, Boolean.FALSE);
			map.put(MODIF_CIVIL, Boolean.FALSE);
			map.put(MODIF_ADRESSE, Boolean.FALSE);
			map.put(MODIF_COMPLEMENT, Boolean.FALSE);
			map.put(MODIF_RAPPORT, Boolean.FALSE);
			map.put(MODIF_ETABLISSEMENT, Boolean.FALSE);
			map.put(MODIF_DOSSIER, Boolean.FALSE);
			map.put(MODIF_DEBITEUR, Boolean.FALSE);
			map.put(MODIF_MOUVEMENT, Boolean.FALSE);
			map.put(FISCAL_FOR_PRINC, Boolean.FALSE);
			map.put(FISCAL_FOR_SEC, Boolean.FALSE);
			map.put(FISCAL_FOR_AUTRE, Boolean.FALSE);
			map.put(FISCAL_SIT_FAMILLLE, Boolean.FALSE);
			map.put(ADR_D, Boolean.FALSE);
			map.put(ADR_C, Boolean.FALSE);
			map.put(ADR_B, Boolean.FALSE);
			map.put(ADR_P, Boolean.FALSE);
			map.put(COMPLEMENT_COMMUNICATION, Boolean.FALSE);
			map.put(COMPLEMENT_COOR_FIN, Boolean.FALSE);
			map.put(DOSSIER_TRAVAIL, Boolean.FALSE);
			map.put(DOSSIER_NO_TRAVAIL, Boolean.FALSE);
			map.put(MODIF_DI, Boolean.FALSE);
			map.put(MODIF_QSNC, Boolean.FALSE);
			map.put(FISCAL_DECISION_ACI, Boolean.FALSE);
			map.put(MODIF_BOUCLEMENTS, Boolean.FALSE);
			map.put(MODIF_ETATS_PM, Boolean.FALSE);
			map.put(MODIF_REGIMES_FISCAUX, Boolean.FALSE);
			map.put(MODIF_ALLEGEMENTS_FISCAUX, Boolean.FALSE);
			map.put(MODIF_FLAGS_PM, Boolean.FALSE);
			map.put(MODIF_AUTRES_DOCS_FISCAUX, Boolean.FALSE);
			map.put(MODIF_REMARQUES, Boolean.FALSE);
			map.put(MODIF_MANDATS, Boolean.FALSE);
			map.put(MODIF_DEMANDES_DEGREVEMENT_ICI, Boolean.FALSE);
			map.put(MODIF_DEGREVEMENTS_ICI, Boolean.FALSE);
			map.put(MODIF_EXONERATIONS_IFONC, Boolean.FALSE);
			map.put(MODIF_ETIQUETTES, Boolean.FALSE);
			return map;
		}

		if (SecurityHelper.isGranted(securityProvider, Role.REMARQUE_TIERS, visa, oid)) {
			map.put(MODIF_REMARQUES, Boolean.TRUE);
		}

		if (SecurityHelper.isGranted(securityProvider, Role.COOR_FIN, visa, oid)) {
			map.put(MODIF_COMPLEMENT, Boolean.TRUE);
			map.put(COMPLEMENT_COOR_FIN, Boolean.TRUE);
		}

		if (SecurityHelper.isGranted(securityProvider, Role.COMPLT_COMM, visa, oid)) {
			map.put(MODIF_COMPLEMENT, Boolean.TRUE);
			map.put(COMPLEMENT_COMMUNICATION, Boolean.TRUE);
		}

		if (SecurityHelper.isGranted(securityProvider, Role.SUIVI_DOSS, visa, oid)) {
			map.put(MODIF_MOUVEMENT, Boolean.TRUE);
		}

		if (tiers.isDesactive(null)) {
			// droits pour un contribuable annulé
			if (SecurityHelper.isGranted(securityProvider, Role.MODIF_NONHAB_INACTIF, visa, oid)) {
				map.put(MODIF_DOSSIER, Boolean.FALSE);
				map.put(MODIF_ETABLISSEMENT, Boolean.FALSE);
				map.put(MODIF_FISCAL, Boolean.FALSE);
				map.put(MODIF_DI, Boolean.FALSE);
				map.put(MODIF_QSNC, Boolean.FALSE);
				map.put(MODIF_IDE, Boolean.FALSE);
				if (SecurityHelper.isGranted(securityProvider, Role.ADR_PP_D, visa, oid)) {
					map.put(MODIF_ADRESSE, Boolean.TRUE);
					map.put(ADR_D, Boolean.TRUE);
				}
				if (SecurityHelper.isGranted(securityProvider, Role.ADR_PP_B, visa, oid)) {
					map.put(MODIF_ADRESSE, Boolean.TRUE);
					map.put(ADR_B, Boolean.TRUE);
				}
				if (SecurityHelper.isGranted(securityProvider, Role.ADR_PP_C, visa, oid)) {
					map.put(MODIF_ADRESSE, Boolean.TRUE);
					map.put(ADR_C, Boolean.TRUE);
				}
				if (SecurityHelper.isGranted(securityProvider, Role.ADR_P, visa, oid)) {
					map.put(MODIF_ADRESSE, Boolean.TRUE);
					map.put(ADR_P, Boolean.TRUE);
				}
				if (SecurityHelper.isGranted(securityProvider, Role.GEST_DECISION_ACI, visa, oid)) {
					map.put(FISCAL_DECISION_ACI, Boolean.TRUE);
				}
			}
			return map;
		}

		// Donner accès dès la présence de la procédure
		if (SecurityHelper.isGranted(securityProvider, Role.GEST_SNC, visa, oid)) {
			map.put(MODIF_RAPPORT, Boolean.TRUE);
			map.put(MODIF_DOSSIER, Boolean.TRUE);
			map.put(DOSSIER_NO_TRAVAIL, Boolean.TRUE);
		}

		if (tiers instanceof PersonnePhysique) {
			// pour le moment, on ne peut les modifier que sur les personnes physiques...
			if (SecurityHelper.isGranted(securityProvider, Role.GEST_ETIQUETTES, visa, oid)) {
				map.put(MODIF_ETIQUETTES, Boolean.TRUE);
			}
		}

		if (tiers instanceof Contribuable) {
			if (SecurityHelper.isGranted(securityProvider, Role.CREATE_MODIF_DPI, visa, oid)) {
				map.put(MODIF_DEBITEUR, Boolean.TRUE);
			}

			final AccesMandatairesView accesMandataire = new AccesMandatairesView(tiers, configurationMandataire, infrastructureService);
			if (accesMandataire.hasGeneralInEdition() && SecurityHelper.isAnyGranted(securityProvider, visa, oid, Role.MODIF_MANDAT_GENERAL)) {
				map.put(MODIF_MANDATS_GENERAUX, Boolean.TRUE);
				map.put(MODIF_MANDATS, Boolean.TRUE);
			}
			if (accesMandataire.hasSpecialInEdition() && SecurityHelper.isAnyGranted(securityProvider, visa, oid, Role.MODIF_MANDAT_SPECIAL)) {
				map.put(MODIF_MANDATS_SPECIAUX, Boolean.TRUE);
				map.put(MODIF_MANDATS, Boolean.TRUE);
			}
			if (accesMandataire.hasTiersPerceptionInEdition() && SecurityHelper.isAnyGranted(securityProvider, visa, oid, Role.MODIF_MANDAT_TIERS)) {
				map.put(MODIF_MANDATS_TIERS, Boolean.TRUE);
				map.put(MODIF_MANDATS, Boolean.TRUE);
			}
		}

		if (tiers instanceof ContribuableImpositionPersonnesPhysiques) {
			final ContribuableImpositionPersonnesPhysiques contribuable = (ContribuableImpositionPersonnesPhysiques) tiers;
			final boolean modifiableSelonRoleEtDecisions = isCtbModifiableSelonRoleEtDecisions(contribuable, visa, oid);
			if (SecurityHelper.isGranted(securityProvider, Role.SIT_FAM, visa, oid)) {
				final boolean isSitFamActive = isSituationFamilleActive(contribuable);
				boolean civilOK = true;
				if (tiers instanceof PersonnePhysique) {
					final PersonnePhysique pp = (PersonnePhysique) tiers;
					if (pp.isHabitantVD()) {
						final Individu ind = serviceCivil.getIndividu(pp.getNumeroIndividu(), null);
						if (ind != null && ind.getEtatsCivils() != null) {
							for (EtatCivil etatCivil : ind.getEtatsCivils().asList()) {
								if (etatCivil.getDateDebut() == null) {
									civilOK = false;
								}
							}
						}
					}
				}
				if (civilOK && modifiableSelonRoleEtDecisions && (isSitFamActive || !contribuable.getSituationsFamille().isEmpty())) {
					map.put(MODIF_FISCAL, Boolean.TRUE);
					map.put(FISCAL_SIT_FAMILLLE, Boolean.TRUE);
				}
			}
			if (SecurityHelper.isAnyGranted(securityProvider, visa, oid, Role.DI_EMIS_PP, Role.DI_DELAI_PP, Role.DI_DUPLIC_PP, Role.DI_QUIT_PP, Role.DI_SOM_PP, Role.DI_DESANNUL_PP, Role.DI_LIBERER_PP)) {
				map.put(MODIF_DI, Boolean.TRUE);
			}

			if (SecurityHelper.isGranted(securityProvider, Role.GEST_DECISION_ACI, visa, oid)) {
				map.put(FISCAL_DECISION_ACI, Boolean.TRUE);
			}
			//SIFISC-13335
			final Assujettissement assujettissement = tiersService.getAssujettissement((Contribuable) tiers, null);
			final boolean isNonAssujettit = assujettissement == null || TypeAssujettissement.NON_ASSUJETTI.equals(assujettissement.getType());
			final boolean isHabitant = tiersService.isHabitant((Contribuable) tiers);
			boolean allowed = (SecurityHelper.isGranted(securityProvider, Role.MODIF_NONHAB_DEBPUR, visa, oid) && !isHabitant) ||
					(SecurityHelper.isGranted(securityProvider, Role.MODIF_HAB_DEBPUR, visa, oid) && isHabitant);
			if (isNonAssujettit && allowed) {
				map.put(MODIF_CIVIL, Boolean.TRUE);
				map.put(MODIF_FISCAL, Boolean.TRUE);
				map.put(FISCAL_SIT_FAMILLLE, Boolean.TRUE);
			}

			map.put(MODIF_QSNC, Boolean.FALSE);
		}

		if (tiers instanceof PersonnePhysique) {
			PersonnePhysique pp = (PersonnePhysique) tiers;
			if (pp.isHabitantVD()) {
				setDroitHabitant(pp, visa, oid, map);
			}
			else {
				setDroitNonHabitant(pp, visa, oid, map);
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
				setDroitHabitant(menageCommun, visa, oid, map);
			}
			else {
				setDroitNonHabitant(menageCommun, visa, oid, map);
			}
		}
		else if (tiers instanceof AutreCommunaute) {
			//les autres communautés n'ont jamais les onglets fiscal, rapport prestation et dossier apparenté
			if (SecurityHelper.isGranted(securityProvider, Role.MODIF_AC, visa, oid)) {
				map.put(MODIF_CIVIL, Boolean.TRUE);
				map.put(MODIF_IDE, Boolean.TRUE);
				if (SecurityHelper.isGranted(securityProvider, Role.ADR_PM_D, visa, oid)) {
					map.put(MODIF_ADRESSE, Boolean.TRUE);
					map.put(ADR_D, Boolean.TRUE);
				}
				if (SecurityHelper.isGranted(securityProvider, Role.ADR_PM_B, visa, oid)) {
					map.put(MODIF_ADRESSE, Boolean.TRUE);
					map.put(ADR_B, Boolean.TRUE);
				}
				if (SecurityHelper.isGranted(securityProvider, Role.ADR_PM_C, visa, oid)) {
					map.put(MODIF_ADRESSE, Boolean.TRUE);
					map.put(ADR_C, Boolean.TRUE);
				}
				if (SecurityHelper.isGranted(securityProvider, Role.ADR_P, visa, oid)) {
					map.put(MODIF_ADRESSE, Boolean.TRUE);
					map.put(ADR_P, Boolean.TRUE);
				}
			}
		}
		else if (tiers instanceof DebiteurPrestationImposable) {
			//les DPI n'ont jamais les onglets civil, dossier apparenté et débiteur IS
			if (SecurityHelper.isGranted(securityProvider, Role.CREATE_MODIF_DPI, visa, oid)) {
				map.put(MODIF_FISCAL, Boolean.TRUE);
				map.put(MODIF_ADRESSE, Boolean.TRUE);
				map.put(ADR_B, Boolean.TRUE);
				map.put(ADR_C, Boolean.TRUE);
				map.put(ADR_D, Boolean.TRUE);
				map.put(MODIF_RAPPORT, Boolean.TRUE);
				map.put(MODIF_DOSSIER, Boolean.TRUE);
			}
			if (SecurityHelper.isGranted(securityProvider, Role.RT, visa, oid)) {
				map.put(MODIF_RAPPORT, Boolean.TRUE);
				map.put(MODIF_DOSSIER, Boolean.TRUE);
			}
			if (SecurityHelper.isGranted(securityProvider, Role.ADR_P, visa, oid)) {
				map.put(MODIF_ADRESSE, Boolean.TRUE);
				map.put(ADR_P, Boolean.TRUE);
			}
			if (SecurityHelper.isGranted(securityProvider, Role.MODIF_FISCAL_DPI, visa, oid)) {
				map.put(MODIF_FISCAL, Boolean.TRUE);
			}
		}
		else if (tiers instanceof Entreprise) {
			if (SecurityHelper.isGranted(securityProvider, Role.MODIF_PM, visa, oid)) {
				map.put(MODIF_CIVIL, Boolean.TRUE);
				map.put(MODIF_IDE, Boolean.TRUE);
				if (SecurityHelper.isGranted(securityProvider, Role.ADR_PM_D, visa, oid)) {
					map.put(MODIF_ADRESSE, Boolean.TRUE);
					map.put(ADR_D, Boolean.TRUE);
				}
				if (SecurityHelper.isGranted(securityProvider, Role.ADR_PM_B, visa, oid)) {
					map.put(MODIF_ADRESSE, Boolean.TRUE);
					map.put(ADR_B, Boolean.TRUE);
				}
				if (SecurityHelper.isGranted(securityProvider, Role.ADR_PM_C, visa, oid)) {
					map.put(MODIF_ADRESSE, Boolean.TRUE);
					map.put(ADR_C, Boolean.TRUE);
				}
				if (SecurityHelper.isGranted(securityProvider, Role.ADR_P, visa, oid)) {
					map.put(MODIF_ADRESSE, Boolean.TRUE);
					map.put(ADR_P, Boolean.TRUE);
				}
			}

			if (SecurityHelper.isGranted(securityProvider, Role.ETAT_PM, visa, oid)) {
				map.put(MODIF_ETATS_PM, Boolean.TRUE);
			}
			if (SecurityHelper.isGranted(securityProvider, Role.ETABLISSEMENTS, visa, oid)) {
				map.put(MODIF_ETABLISSEMENT, Boolean.TRUE);
			}

			if (SecurityHelper.isAnyGranted(securityProvider, visa, oid, Role.DI_EMIS_PM, Role.DI_DELAI_PM, Role.DI_DUPLIC_PM, Role.DI_QUIT_PM, Role.DI_SOM_PM, Role.DI_SUSPENDRE_PM, Role.DI_DESUSPENDRE_PM, Role.DI_DESANNUL_PM, Role.DI_LIBERER_PM)) {
				map.put(MODIF_DI, Boolean.TRUE);
			}
			if (SecurityHelper.isAnyGranted(securityProvider, visa, oid, Role.QSNC_EMISSION, Role.QSNC_DUPLICATA, Role.QSNC_QUITTANCEMENT, Role.QSNC_RAPPEL)) {
				map.put(MODIF_QSNC, Boolean.TRUE);
			}

			if (SecurityHelper.isGranted(securityProvider, Role.GEST_DECISION_ACI, visa, oid)) {
				map.put(FISCAL_DECISION_ACI, Boolean.TRUE);
				map.put(MODIF_FISCAL, Boolean.TRUE);
			}

			final boolean ctbModifiableSelonRoleEtDecision = isCtbModifiableSelonRoleEtDecisions((Entreprise) tiers, visa, oid);
			if (ctbModifiableSelonRoleEtDecision && SecurityHelper.isGranted(securityProvider, Role.FOR_PRINC_PM, visa, oid)) {
				map.put(FISCAL_FOR_PRINC, Boolean.TRUE);
				map.put(MODIF_FISCAL, Boolean.TRUE);
			}
			if (ctbModifiableSelonRoleEtDecision && SecurityHelper.isGranted(securityProvider, Role.FOR_SECOND_PM, visa, oid)) {
				map.put(FISCAL_FOR_SEC, Boolean.TRUE);
				map.put(MODIF_FISCAL, Boolean.TRUE);
			}
			if (SecurityHelper.isGranted(securityProvider, Role.BOUCLEMENTS_PM, visa, oid)) {
				map.put(MODIF_BOUCLEMENTS, Boolean.TRUE);
			}

			if (SecurityHelper.isGranted(securityProvider, Role.REGIMES_FISCAUX, visa, oid)) {
				map.put(MODIF_REGIMES_FISCAUX, Boolean.TRUE);
			}
			if (SecurityHelper.isGranted(securityProvider, Role.ALLEGEMENTS_FISCAUX, visa, oid)) {
				map.put(MODIF_ALLEGEMENTS_FISCAUX, Boolean.TRUE);
			}
			if (SecurityHelper.isGranted(securityProvider, Role.FLAGS_PM, visa, oid)) {
				map.put(MODIF_FLAGS_PM, Boolean.TRUE);
			}
			if (SecurityHelper.isAnyGranted(securityProvider, visa, oid, Role.ENVOI_DEMANDE_BILAN_FINAL, Role.ENVOI_AUTORISATION_RADIATION, Role.ENVOI_LETTRE_TYPE_INFO_LIQUIDATION)) {
				map.put(MODIF_AUTRES_DOCS_FISCAUX, Boolean.TRUE);
			}
			if (SecurityHelper.isGranted(securityProvider, Role.DEMANDES_DEGREVEMENT_ICI)) {
				map.put(MODIF_DEMANDES_DEGREVEMENT_ICI, Boolean.TRUE);
			}
			if (SecurityHelper.isGranted(securityProvider, Role.DEGREVEMENTS_ICI)) {
				map.put(MODIF_DEGREVEMENTS_ICI, Boolean.TRUE);
			}
			if (SecurityHelper.isGranted(securityProvider, Role.EXONERATIONS_IFONC)) {
				map.put(MODIF_EXONERATIONS_IFONC, Boolean.TRUE);
			}
		}
		else if (tiers instanceof Etablissement) {
			if (SecurityHelper.isGranted(securityProvider, Role.MODIF_PM, visa, oid)) {
				map.put(MODIF_CIVIL, Boolean.TRUE);
				map.put(MODIF_IDE, Boolean.TRUE);

				if (SecurityHelper.isGranted(securityProvider, Role.ADR_PM_D, visa, oid)) {
					map.put(MODIF_ADRESSE, Boolean.TRUE);
					map.put(ADR_D, Boolean.TRUE);
				}
				if (SecurityHelper.isGranted(securityProvider, Role.ADR_PM_B, visa, oid)) {
					map.put(MODIF_ADRESSE, Boolean.TRUE);
					map.put(ADR_B, Boolean.TRUE);
				}
				if (SecurityHelper.isGranted(securityProvider, Role.ADR_PM_C, visa, oid)) {
					map.put(MODIF_ADRESSE, Boolean.TRUE);
					map.put(ADR_C, Boolean.TRUE);
				}
				if (SecurityHelper.isGranted(securityProvider, Role.ADR_P, visa, oid)) {
					map.put(MODIF_ADRESSE, Boolean.TRUE);
					map.put(ADR_P, Boolean.TRUE);
				}
			}
		}

		// UNIREG-2120 Possibilite de créer un debiteur à partir d'une collectivité administrative
		// UNIREG-3362 Création de débiteur à partir d'une PM
		else if (tiers instanceof CollectiviteAdministrative) {
			map.put(MODIF_COMPLEMENT, Boolean.FALSE);
			map.put(MODIF_MOUVEMENT, Boolean.FALSE);
		}

		return map;
	}

	/**
	 * Vérifie que le contribuable a une décision en cours ou pas et le cas echéant vérifier les droits en modification
	 *
	 * @param contribuable
	 * @param visa
	 * @param oid
	 * @return true si l'utilisateur a le droit de modifier un ctb sous décision ou si il n'y a pas de décision ouverte, false sinon
	 */
	private boolean isCtbModifiableSelonRoleEtDecisions(Contribuable contribuable, String visa, int oid) {
		if (tiersService.isSousInfluenceDecisions(contribuable)) {
			return SecurityHelper.isGranted(securityProvider, Role.GEST_DECISION_ACI, visa, oid);
		}
		else {
			return true;
		}
	}

	/**
	 * Indique si l'on a le droit ou non de saisir une nouvelle situation de famille
	 *
	 * @param contribuable un contribuable
	 * @return <b>vrai</b> si l'utilisateur courant a le droit de saisir une nouvelle situation de famille sur le contribuable spécifié; <b>faux</b> autrement.
	 */
	protected boolean isSituationFamilleActive(Contribuable contribuable) {
		// [UNIREG-1278] il doit être possible de créer une situation de famille même si le contribuable est hors canton
		// [SIFISC-23943] il ne faut pas tenir compte des fors annulés !!!
		return contribuable.getForsFiscauxNonAnnules(false).stream()
				.filter(ff -> ff instanceof ForFiscalPrincipal || ff instanceof ForFiscalSecondaire)
				.anyMatch(ff -> ff.getDateFin() == null);
	}

	/**
	 * enrichi la map de droit d'édition des onglets pour un habitant ou un ménage commun considéré habitant
	 */
	private boolean setDroitHabitant(ContribuableImpositionPersonnesPhysiques tiers, String visa, int oid, Map<String, Boolean> allowedOnglet) {

		if (!(tiers instanceof PersonnePhysique || tiers instanceof MenageCommun)) {
			throw new IllegalArgumentException("Le tiers " + tiers.getNumero() + " n'est ni une personne physique ni un ménage commun");
		}
		final boolean modifiableSelonRoleEtDecisions = isCtbModifiableSelonRoleEtDecisions(tiers, visa, oid);
		//les habitants n'ont jamais les onglets civil et rapport prestation
		boolean isEditable = codeFactorise1(tiers, visa, oid, allowedOnglet);
		if (isEditAllowedPP(tiers, visa, oid)) {
			codeFactorise2(visa, oid, allowedOnglet);
			isEditable = true;
			allowedOnglet.put(MODIF_IDE, TRUE);
		}
		isEditable = codeFactorise3(tiers, visa, oid, allowedOnglet, isEditable);

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

		if ((modifiableSelonRoleEtDecisions && typeImposition.isOrdinaireDepenseOuNonActif() && isGranted(securityProvider, FOR_PRINC_ORDDEP_HAB, visa, oid)) ||
				(modifiableSelonRoleEtDecisions && typeImposition.isSourcierOuNonActif() && isGranted(securityProvider, FOR_PRINC_SOURC_HAB, visa, oid))) {
			allowedOnglet.put(MODIF_FISCAL, TRUE);
			allowedOnglet.put(FISCAL_FOR_PRINC, TRUE);
			isEditable = true;
		}
		if (isPersonnePhysique && typeImposition == TypeImposition.SOURCIER && isGranted(securityProvider, RT, visa, oid)) {
			allowedOnglet.put(MODIF_DOSSIER, TRUE);
			allowedOnglet.put(DOSSIER_TRAVAIL, TRUE);
			isEditable = true;
		}
		return isEditable;
	}

	/**
	 * enrichi la map de droit d'édition des onglets pour un non habitant ou un ménage commun considéré non habitant
	 */
	private boolean setDroitNonHabitant(ContribuableImpositionPersonnesPhysiques tiers, String visa, int oid, Map<String, Boolean> allowedOnglet) {

		final boolean modifiableSelonRoleEtDecisions = isCtbModifiableSelonRoleEtDecisions(tiers, visa, oid);

		//les non habitants n'ont jamais l'onglet rapport prestation
		//les ménage commun n'ont jamais les onglets civil et rapport prestation

		if (!(tiers instanceof PersonnePhysique || tiers instanceof MenageCommun)) {
			throw new IllegalArgumentException("Le tiers " + tiers.getNumero() + " n'est ni une personne physique ni un ménage commun");
		}

		final boolean isPersonnePhysique = tiers instanceof PersonnePhysique;

		boolean isEditable = codeFactorise1(tiers, visa, oid, allowedOnglet);
		if (tiers.isDebiteurInactif()) {//I107
			if (isGranted(securityProvider, MODIF_NONHAB_INACTIF, visa, oid)) {
				if (isPersonnePhysique) {
					allowedOnglet.put(MODIF_CIVIL, TRUE);
					allowedOnglet.put(MODIF_IDE, TRUE);
				}
				allowedOnglet.put(MODIF_DOSSIER, FALSE);
				allowedOnglet.put(MODIF_FISCAL, FALSE);
				if (isGranted(securityProvider, ADR_PP_D, visa, oid)) {
					allowedOnglet.put(MODIF_ADRESSE, TRUE);
					allowedOnglet.put(ADR_D, TRUE);
				}
				if (isGranted(securityProvider, ADR_PP_B, visa, oid)) {
					allowedOnglet.put(MODIF_ADRESSE, TRUE);
					allowedOnglet.put(ADR_B, TRUE);
				}
				if (isGranted(securityProvider, ADR_PP_C, visa, oid)) {
					allowedOnglet.put(MODIF_ADRESSE, TRUE);
					allowedOnglet.put(ADR_C, TRUE);
				}
				isEditable = true;
			}
		}
		else {
			if (isEditAllowedPP(tiers, visa, oid)) {
				if (isPersonnePhysique) {
					allowedOnglet.put(MODIF_CIVIL, TRUE);
					allowedOnglet.put(MODIF_IDE, TRUE);
				}
				codeFactorise2(visa, oid, allowedOnglet);
				isEditable = true;
			}

			isEditable = codeFactorise3(tiers, visa, oid, allowedOnglet, isEditable);

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
			if (isPersonnePhysique && typeImposition == TypeImposition.SOURCIER && isGranted(securityProvider, RT, visa, oid)) {
				allowedOnglet.put(MODIF_DOSSIER, TRUE);
				allowedOnglet.put(DOSSIER_TRAVAIL, TRUE);
				isEditable = true;
			}
			final boolean autoriteFiscaleVaudoiseOuIndeterminee = typeAutoriteFiscale == null || typeAutoriteFiscale == COMMUNE_OU_FRACTION_VD;
			final boolean autoriteFiscaleNonVaudoiseOuIndeterminee = typeAutoriteFiscale != COMMUNE_OU_FRACTION_VD;
			if (modifiableSelonRoleEtDecisions) {
				if (typeImposition.isOrdinaireDepenseOuNonActif() && autoriteFiscaleNonVaudoiseOuIndeterminee && isGranted(securityProvider, FOR_PRINC_ORDDEP_HCHS, visa, oid) ||
						(typeImposition.isOrdinaireDepenseOuNonActif() && autoriteFiscaleVaudoiseOuIndeterminee && isGranted(securityProvider, FOR_PRINC_ORDDEP_GRIS, visa, oid)) ||
						(typeImposition.isSourcierOuNonActif() && autoriteFiscaleNonVaudoiseOuIndeterminee && isGranted(securityProvider, FOR_PRINC_SOURC_HCHS, visa, oid)) ||
						(typeImposition.isSourcierOuNonActif() && autoriteFiscaleVaudoiseOuIndeterminee && isGranted(securityProvider, FOR_PRINC_SOURC_GRIS, visa, oid))) {
					allowedOnglet.put(MODIF_FISCAL, TRUE);
					allowedOnglet.put(FISCAL_FOR_PRINC, TRUE);
					isEditable = true;
				}
			}
		}
		return isEditable;
	}

	/**
	 * Code commun pour les méthodes setDroitNonHabitant et setDroitHabitant
	 */
	private boolean codeFactorise1(Tiers tiers, String visa, int oid, Map<String, Boolean> allowedOnglet) {
		boolean isEditable = false;
		if (SecurityHelper.isGranted(securityProvider, Role.ADR_P, visa, oid)) {
			allowedOnglet.put(MODIF_ADRESSE, Boolean.TRUE);
			allowedOnglet.put(ADR_P, Boolean.TRUE);
			isEditable = true;
		}

		if (SecurityHelper.isGranted(securityProvider, Role.ADR_PP_C_DCD, visa, oid) && tiers instanceof PersonnePhysique) {
			PersonnePhysique pp = (PersonnePhysique) tiers;
			if (tiersService.isDecede(pp)) {
				allowedOnglet.put(MODIF_ADRESSE, Boolean.TRUE);
				allowedOnglet.put(ADR_C, Boolean.TRUE);
				isEditable = true;
			}
		}
		else if (SecurityHelper.isGranted(securityProvider, Role.ADR_PP_C_DCD, visa, oid) && tiers instanceof MenageCommun) {
			MenageCommun mc = (MenageCommun) tiers;
			for (PersonnePhysique pp : tiersService.getPersonnesPhysiques(mc)) {
				if (tiersService.isDecede(pp)) {
					allowedOnglet.put(MODIF_ADRESSE, Boolean.TRUE);
					allowedOnglet.put(ADR_C, Boolean.TRUE);
					isEditable = true;
					break;
				}
			}
		}
		return isEditable;
	}

	/**
	 * Code commun pour les méthodes setDroitNonHabitant et setDroitHabitant
	 */
	private void codeFactorise2(String visa, int oid, Map<String, Boolean> allowedOnglet) {
		allowedOnglet.put(MODIF_DOSSIER, Boolean.TRUE);
		allowedOnglet.put(DOSSIER_NO_TRAVAIL, Boolean.TRUE);
		if (SecurityHelper.isGranted(securityProvider, Role.ADR_PP_D, visa, oid)) {
			allowedOnglet.put(MODIF_ADRESSE, Boolean.TRUE);
			allowedOnglet.put(ADR_D, Boolean.TRUE);
		}
		if (SecurityHelper.isGranted(securityProvider, Role.ADR_PP_B, visa, oid)) {
			allowedOnglet.put(MODIF_ADRESSE, Boolean.TRUE);
			allowedOnglet.put(ADR_B, Boolean.TRUE);
		}
		if (SecurityHelper.isGranted(securityProvider, Role.ADR_PP_C, visa, oid)) {
			allowedOnglet.put(MODIF_ADRESSE, Boolean.TRUE);
			allowedOnglet.put(ADR_C, Boolean.TRUE);
		}
	}

	/**
	 * Code commun pour les méthodes setDroitNonHabitant et setDroitHabitant
	 */
	private boolean codeFactorise3(ContribuableImpositionPersonnesPhysiques tiers, String visa, int oid, Map<String, Boolean> allowedOnglet, boolean isEditable) {
		final boolean modifiableSelonRoleEtDecisions = isCtbModifiableSelonRoleEtDecisions(tiers, visa, oid);
		if (modifiableSelonRoleEtDecisions && !tiers.getForsFiscauxPrincipauxActifsSorted().isEmpty() && SecurityHelper.isGranted(securityProvider, Role.FOR_SECOND_PP, visa, oid)) {
			allowedOnglet.put(MODIF_FISCAL, Boolean.TRUE);
			allowedOnglet.put(FISCAL_FOR_SEC, Boolean.TRUE);
			isEditable = true;
		}
		if (modifiableSelonRoleEtDecisions && SecurityHelper.isGranted(securityProvider, Role.FOR_AUTRE, visa, oid)) {
			allowedOnglet.put(MODIF_FISCAL, Boolean.TRUE);
			allowedOnglet.put(FISCAL_FOR_AUTRE, Boolean.TRUE);
			isEditable = true;
		}
		return isEditable;
	}

	private enum TypeImposition {
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

	private static TypeImposition calculeTypeImposition(ContribuableImpositionPersonnesPhysiques ctb) {
		final TypeImposition type;
		final ForFiscalPrincipal forFiscalPrincipal = ctb.getForFiscalPrincipalAt(null);
		if (forFiscalPrincipal != null) {
			final ModeImposition modeImposition = ctb.getForFiscalPrincipalAt(null).getModeImposition();
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
	 */
	private static Pair<TypeImposition, TypeAutoriteFiscale> calculeTypeImpositionEtAutoriteFiscale(ContribuableImpositionPersonnesPhysiques tiers) {
		final TypeImposition typeImposition = calculeTypeImposition(tiers);
		final ForFiscalPrincipal forFiscalPrincipal = tiers.getForFiscalPrincipalAt(null);
		final TypeAutoriteFiscale typeAutoriteFiscale;
		if (forFiscalPrincipal != null) {
			typeAutoriteFiscale = forFiscalPrincipal.getTypeAutoriteFiscale();
		}
		else {
			typeAutoriteFiscale = null;
		}
		return new Pair<>(typeImposition, typeAutoriteFiscale);
	}
}
