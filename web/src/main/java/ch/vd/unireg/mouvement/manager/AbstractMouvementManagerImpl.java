package ch.vd.unireg.mouvement.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.cache.ServiceCivilCacheWarmer;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.common.TiersNotFoundException;
import ch.vd.unireg.general.manager.TiersGeneralManager;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.interfaces.service.ServiceSecuriteService;
import ch.vd.unireg.interfaces.service.host.Operateur;
import ch.vd.unireg.mouvement.EnvoiDossier;
import ch.vd.unireg.mouvement.EnvoiDossierVersCollaborateur;
import ch.vd.unireg.mouvement.EnvoiDossierVersCollectiviteAdministrative;
import ch.vd.unireg.mouvement.MouvementDossier;
import ch.vd.unireg.mouvement.MouvementDossierDAO;
import ch.vd.unireg.mouvement.ReceptionDossier;
import ch.vd.unireg.mouvement.ReceptionDossierArchives;
import ch.vd.unireg.mouvement.ReceptionDossierPersonnel;
import ch.vd.unireg.mouvement.view.ContribuableView;
import ch.vd.unireg.mouvement.view.MouvementDetailView;
import ch.vd.unireg.security.ProfileOperateur;
import ch.vd.unireg.tiers.ForGestion;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.TypeMouvement;
import ch.vd.unireg.utils.WebContextUtils;

public class AbstractMouvementManagerImpl implements AbstractMouvementManager, MessageSourceAware {

	public static final Logger LOGGER = LoggerFactory.getLogger(AbstractMouvementManagerImpl.class);

	private TiersGeneralManager tiersGeneralManager;
	private TiersService tiersService;
	private AdresseService adresseService;
	private ServiceInfrastructureService serviceInfra;
	private ServiceSecuriteService serviceSecuriteService;
	private MessageSource messageSource;
	private MouvementDossierDAO mouvementDossierDAO;
	private ServiceCivilCacheWarmer serviceCivilCacheWarmer;

	public void setMouvementDossierDAO(MouvementDossierDAO mouvementDossierDAO) {
		this.mouvementDossierDAO = mouvementDossierDAO;
	}

	protected MouvementDossierDAO getMouvementDossierDAO() {
		return mouvementDossierDAO;
	}

	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	protected MessageSource getMessageSource() {
		return messageSource;
	}

	public void setTiersGeneralManager(TiersGeneralManager tiersGeneralManager) {
		this.tiersGeneralManager = tiersGeneralManager;
	}

	protected TiersGeneralManager getTiersGeneralManager() {
		return tiersGeneralManager;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	protected TiersService getTiersService() {
		return tiersService;
	}

	public void setServiceInfra(ServiceInfrastructureService serviceInfra) {
		this.serviceInfra = serviceInfra;
	}

	protected ServiceInfrastructureService getServiceInfra() {
		return serviceInfra;
	}

	public void setServiceSecuriteService(ServiceSecuriteService serviceSecuriteService) {
		this.serviceSecuriteService = serviceSecuriteService;
	}

	protected ServiceSecuriteService getServiceSecuriteService() {
		return serviceSecuriteService;
	}

	public void setServiceCivilCacheWarmer(ServiceCivilCacheWarmer serviceCivilCacheWarmer) {
		this.serviceCivilCacheWarmer = serviceCivilCacheWarmer;
	}

	/**
	 * Point d'entrée principal pour bâtir la view détaillée pour un mouvement de dossier donné
	 *
	 * @param mvt          le mouvement depuis lequel bâtir la vue
	 * @param isExtraction
	 * @return la vue
	 * @throws ServiceInfrastructureException
	 */
	@Override
	@Transactional(readOnly = true)
	public MouvementDetailView getView(MouvementDossier mvt, boolean isExtraction) throws ServiceInfrastructureException {
		final MouvementDetailView view = buildAndFillCommonElements(mvt, isExtraction);
		if (mvt instanceof ReceptionDossier) {
			fillReceptionDossier((ReceptionDossier) mvt, view);
		}
		else if (mvt instanceof EnvoiDossier) {
			fillEnvoiDossier((EnvoiDossier) mvt, view);
		}
		return view;
	}

	protected List<MouvementDetailView> getViews(Collection<MouvementDossier> mvts, boolean sortByNoDossier, boolean isExtraction) throws ServiceInfrastructureException {
		if (mvts != null && !mvts.isEmpty()) {
			prefetchIndividus(mvts);
			final List<MouvementDetailView> liste = new ArrayList<>(mvts.size());
			for (MouvementDossier mvt : mvts) {
				liste.add(getView(mvt, isExtraction));
			}
			if (sortByNoDossier && liste.size() > 1) {
				liste.sort((o1, o2) -> {
					final long no1 = o1.getContribuable().getNumero();
					final long no2 = o2.getContribuable().getNumero();
					return Long.compare(no1, no2);
				});
			}
			return liste;
		}
		else {
			return null;
		}
	}

	private void prefetchIndividus(Collection<MouvementDossier> mvts) {

		final int TAILLE_LOT = 100;

		final long start = System.nanoTime();

		if (serviceCivilCacheWarmer.isServiceWarmable() && mvts != null && mvts.size() > 1) {

			// d'abord on cherche tous les identifiants de tiers
			final Set<Long> idsTiers = new HashSet<>(mvts.size());
			for (MouvementDossier mvt : mvts) {
				idsTiers.add(mvt.getContribuable().getNumero());
			}

			// que l'on découpe ensuite en petits lots pour récupérer les numéros d'individus
			final int nbLots = idsTiers.size() / TAILLE_LOT + 1;
			final List<Long> listeIdsTiers = new ArrayList<>(idsTiers);
			for (int i = 0; i < nbLots; ++i) {
				final int idxMin = i * TAILLE_LOT;
				final int idxMax = Math.min((i + 1) * TAILLE_LOT, listeIdsTiers.size());
				if (idxMin < idxMax) {
					final Set<Long> lotTiersIds = new HashSet<>(listeIdsTiers.subList(idxMin, idxMax));
					serviceCivilCacheWarmer.warmIndividusPourTiers(lotTiersIds, null, true, AttributeIndividu.ADRESSES);
				}
			}

			final long end = System.nanoTime();
			LOGGER.info(String.format("Prefetched les individus de %d mouvements en %d millisecondes", mvts.size(), (end - start) / 1000000L));
		}
	}

	private MouvementDetailView buildAndFillCommonElements(MouvementDossier mvt, boolean isExtraction) {
		final MouvementDetailView view = new MouvementDetailView();
		view.setId(mvt.getId());
		view.setDateMouvement(mvt.getDateMouvement() != null ? mvt.getDateMouvement().asJavaDate() : null);
		view.setEtatMouvement(mvt.getEtat());
		view.setDateExecution(mvt.getLogModifDate());
		view.setExecutant(mvt.getLogModifUser());
		view.setContribuable(creerCtbView(mvt.getContribuable()));
		view.setAnnule(mvt.isAnnule());
		if (!isExtraction) {
			view.setAnnulable(isAnnulable(mvt));
		}
		return view;
	}

	/**
	 * Alimente la vue contribuable pour le mouvement
	 */
	protected ContribuableView creerCtbView(Long numero) {
		final Tiers tiers = getTiersService().getTiers(numero);
		if (tiers == null) {
			throw new TiersNotFoundException(numero);
		}

		return creerCtbView(tiers);
	}

	private ContribuableView creerCtbView(Tiers tiers) {

		ContribuableView view = new ContribuableView();
		view.setNumero(tiers.getNumero());
		view.setNomCommuneGestion(getCommuneGestion(tiers));
		try {
			view.setNomPrenom(adresseService.getNomCourrier(tiers, null, false));
		}
		catch (AdresseException e) {
			LOGGER.error("Erreur lors du calcul du nom/prénom", e);
		}

		return view;
	}

	private boolean isAnnulable(MouvementDossier mvt) {
		// annulable si :
		// 1. pas déjà annulé
		// 1bis. seuls les mouvements traités sont annulables
		// 2. envoi et demande faite par utilisateur de la collectivité émettrice
		// 3. réception et demande faite par utilisateur qui a réceptionné le même jour
		boolean isAnnulable = false;
		if (!mvt.isAnnule() && mvt.getEtat().isTraite()) {
			if (mvt instanceof EnvoiDossier) {
				final int oid = AuthenticationHelper.getCurrentOID();
				final EnvoiDossier envoi = (EnvoiDossier) mvt;
				final ch.vd.unireg.tiers.CollectiviteAdministrative ca = envoi.getCollectiviteAdministrativeEmettrice();
				if (ca != null && oid == ca.getNumeroCollectiviteAdministrative()) {
					isAnnulable = true;
				}
			}
			else if (mvt instanceof ReceptionDossier) {
				final RegDate aujourdhui = RegDate.get();
				final RegDate dateMouvement = mvt.getDateMouvement();
				if (dateMouvement == null || aujourdhui == dateMouvement) {
					final String visaConnecte = AuthenticationHelper.getCurrentPrincipal();
					final String createurMouvement = mvt.getLogCreationUser();
					if (StringUtils.equals(visaConnecte, createurMouvement)) {
						isAnnulable = true;
					}
				}
			}
			else {
				throw new IllegalArgumentException("Type de mouvement de dossier non supporté : " + mvt.getClass().getName());
			}
		}
		return isAnnulable;
	}

	private static class InfoCollaborateur {
		/**
		 * Prénom et nom du collaborateur
		 */
		public final String nomPrenom;

		/**
		 * Numéro de téléphone et nom court de l'OID du collaborateur : xxxxxxxx (oid)
		 */
		public final String noTelephoneDansOid;

		/**
		 * Visa de l'opérateur
		 */
		public final String visaOperateur;

		private InfoCollaborateur(String nomPrenom, String noTelephoneDansOid, String visaOperateur) {
			this.nomPrenom = nomPrenom;
			this.noTelephoneDansOid = noTelephoneDansOid;
			this.visaOperateur = visaOperateur;
		}
	}

	private InfoCollaborateur getInfosCollaborateur(@NotNull String visa) {

		final Operateur operateur = serviceSecuriteService.getOperateur(visa);
		final String nomUtilisateur;
		final String visaOperateur;
		final String noTelephone;
		if (operateur != null) {
			final NomPrenom nomPrenomOperateur = new NomPrenom(operateur.getNom(), operateur.getPrenom());
			nomUtilisateur = nomPrenomOperateur.getNomPrenom();
			visaOperateur = operateur.getCode();
			final List<CollectiviteAdministrative> collectivites = serviceSecuriteService.getCollectivitesUtilisateur(visaOperateur);
			if (collectivites != null && collectivites.size() > 0) {
				final CollectiviteAdministrative collectivite = collectivites.get(0);
				final ProfileOperateur profileUtilisateur = serviceSecuriteService.getProfileUtilisateur(visaOperateur, collectivite.getNoColAdm());
				if (profileUtilisateur != null && !StringUtils.isBlank(profileUtilisateur.getNoTelephone())) {
					noTelephone = String.format("%s (%s)", profileUtilisateur.getNoTelephone().trim(), collectivite.getNomCourt());
				}
				else {
					noTelephone = null;
				}
			}
			else {
				noTelephone = null;
			}
		}
		else {
			visaOperateur = null;
			noTelephone = null;
			nomUtilisateur = null;
		}
		return new InfoCollaborateur(nomUtilisateur, noTelephone, visaOperateur);
	}

	private void fillEnvoiDossier(EnvoiDossier envoi, MouvementDetailView view) throws ServiceInfrastructureException {
		view.setTypeMouvement(TypeMouvement.EnvoiDossier);
		view.setCollectiviteAdministrative(getNomCollectiviteAdministrative(envoi.getCollectiviteAdministrativeEmettrice()));

		if (envoi instanceof EnvoiDossierVersCollaborateur) {
			final String visaDestinataire = ((EnvoiDossierVersCollaborateur) envoi).getVisaDestinataire();
			if (visaDestinataire != null) {
				final InfoCollaborateur infoCollaborateur = getInfosCollaborateur(visaDestinataire);
				view.setDestinationUtilisateur(infoCollaborateur.nomPrenom);
				view.setNomPrenomUtilisateur(infoCollaborateur.nomPrenom);
				view.setNumeroTelephoneUtilisateur(infoCollaborateur.noTelephoneDansOid);
				view.setVisaUtilisateurEnvoi(infoCollaborateur.visaOperateur);
				view.setNomUtilisateurEnvoi(infoCollaborateur.nomPrenom);
			}
		}
		else if (envoi instanceof EnvoiDossierVersCollectiviteAdministrative) {
			final String nomCollectiviteAdm = getNomCollectiviteAdministrative(((EnvoiDossierVersCollectiviteAdministrative) envoi).getCollectiviteAdministrativeDestinataire());
			view.setDestinationUtilisateur(nomCollectiviteAdm);
		}
	}

	private void fillReceptionDossier(ReceptionDossier reception, MouvementDetailView view) throws ServiceInfrastructureException {
		view.setTypeMouvement(TypeMouvement.ReceptionDossier);
		if (reception.getCollectiviteAdministrativeReceptrice() != null) {
			final String nomCollectiviteAdm = getNomCollectiviteAdministrative(reception.getCollectiviteAdministrativeReceptrice());
			view.setCollectiviteAdministrative(nomCollectiviteAdm);
		}

		// [UNIREG-3402] seuls les mouvements d'archives en masse créés APRES la MeP 11R1 (c'était le 11 mars 2011)
		// doivent pouvoir être libellés "Archives période XXXX", les autres devant rester "Archives" - c'est assez
		// moche, d'accord, mais la partie "uniquement pour les mouvements enregistrées par le batch depuis qu'il est capable de générer des mouvements vers les archives uniquement."
		// n'est apparue que très tardivement dans la spécification de cette modification... après la MeP, en fait.
		final RegDate dateMeP11R1 = RegDate.get(2011, 3, 11);

		if (reception instanceof ReceptionDossierPersonnel) {
			final String visaRecepteur = ((ReceptionDossierPersonnel) reception).getVisaRecepteur();
			if (visaRecepteur != null) {
				final InfoCollaborateur infoCollaborateur = getInfosCollaborateur(visaRecepteur);
				view.setDestinationUtilisateur(infoCollaborateur.nomPrenom);
				view.setNomPrenomUtilisateur(infoCollaborateur.nomPrenom);
				view.setNumeroTelephoneUtilisateur(infoCollaborateur.noTelephoneDansOid);
				view.setVisaUtilisateurReception(infoCollaborateur.visaOperateur);
				view.setNomUtilisateurReception(infoCollaborateur.nomPrenom);
			}
		}
		else if (reception instanceof ReceptionDossierArchives && ((ReceptionDossierArchives) reception).getBordereau() != null && RegDateHelper.get(reception.getLogCreationDate()).isAfter(dateMeP11R1)) {
			final Object[] params = {Integer.toString(reception.getDateMouvement().year())};
			final String localisationStr = messageSource.getMessage("option.localisation.archives.periode", params, WebContextUtils.getDefaultLocale());
			view.setDestinationUtilisateur(localisationStr);
		}
		else {
			final String messageKey = String.format("option.localisation.%s", reception.getLocalisation().name());
			final String localisationStr = messageSource.getMessage(messageKey, null, WebContextUtils.getDefaultLocale());
			view.setDestinationUtilisateur(localisationStr);
		}
		view.setLocalisation(reception.getLocalisation());
	}

	private String getNomCollectiviteAdministrative(ch.vd.unireg.tiers.CollectiviteAdministrative ca) throws ServiceInfrastructureException {
		final String nom;
		if (ca != null) {
			final int iNumCol = ca.getNumeroCollectiviteAdministrative();
			final CollectiviteAdministrative col = serviceInfra.getCollectivite(iNumCol);
			nom = col.getNomCourt();
		}
		else {
			nom = null;
		}
		return nom;
	}

	protected String getCommuneGestion(Tiers tiers) {
		final ForGestion forGestion = tiersService.getDernierForGestionConnu(tiers, null);
		if (forGestion != null) {
			final int ofsCommune = forGestion.getNoOfsCommune();
			try {
				final Commune commune = serviceInfra.getCommuneByNumeroOfs(ofsCommune, forGestion.getDateFin());
				return commune == null ? "" : commune.getNomOfficiel();
			}
			catch (ServiceInfrastructureException e) {
				LOGGER.error("Erreur lors de la récupération de la commune de gestion", e);
				return null;
			}
		}
		return null;
	}

}
