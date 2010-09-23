package ch.vd.uniregctb.mouvement.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.civil.model.EnumAttributeIndividu;
import ch.vd.securite.model.Operateur;
import ch.vd.securite.model.ProfilOperateur;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.general.manager.TiersGeneralManager;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.individu.HostCivilService;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;
import ch.vd.uniregctb.mouvement.EnvoiDossier;
import ch.vd.uniregctb.mouvement.EnvoiDossierVersCollaborateur;
import ch.vd.uniregctb.mouvement.EnvoiDossierVersCollectiviteAdministrative;
import ch.vd.uniregctb.mouvement.MouvementDossier;
import ch.vd.uniregctb.mouvement.MouvementDossierDAO;
import ch.vd.uniregctb.mouvement.ReceptionDossier;
import ch.vd.uniregctb.mouvement.ReceptionDossierPersonnel;
import ch.vd.uniregctb.mouvement.view.MouvementDetailView;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeMouvement;
import ch.vd.uniregctb.utils.WebContextUtils;

public class AbstractMouvementManagerImpl implements AbstractMouvementManager, MessageSourceAware {

	public static final Logger LOGGER = Logger.getLogger(AbstractMouvementManagerImpl.class);

	private static final int NB_MAX_MOUVEMENTS_GARDES = 10;

	private TiersGeneralManager tiersGeneralManager;

	private TiersService tiersService;

	private TiersDAO tiersDAO;

	private ServiceInfrastructureService serviceInfra;

	private HostCivilService hostCivilService;

	private ServiceSecuriteService serviceSecuriteService;

	private MessageSource messageSource;

	private MouvementDossierDAO mouvementDossierDAO;

	public void setMouvementDossierDAO(MouvementDossierDAO mouvementDossierDAO) {
		this.mouvementDossierDAO = mouvementDossierDAO;
	}

	protected MouvementDossierDAO getMouvementDossierDAO() {
		return mouvementDossierDAO;
	}

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

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
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

	public void setHostCivilService(HostCivilService hostCivilService) {
		this.hostCivilService = hostCivilService;
	}

	public void setServiceSecuriteService(ServiceSecuriteService serviceSecuriteService) {
		this.serviceSecuriteService = serviceSecuriteService;
	}

	protected ServiceSecuriteService getServiceSecuriteService() {
		return serviceSecuriteService;
	}

	/**
	 * Point d'entrée principal pour bâtir la view détaillée pour un mouvement de dossier donné
	 * @param mvt le mouvement depuis lequel bâtir la vue
	 * @return la vue
	 * @throws InfrastructureException
	 */
	@Transactional(readOnly = true)
	public MouvementDetailView getView(MouvementDossier mvt) throws InfrastructureException {
		final MouvementDetailView view = buildAndFillCommonElements(mvt);
		if (mvt instanceof ReceptionDossier) {
			fillReceptionDossier((ReceptionDossier) mvt, view);
		}
		else if (mvt instanceof EnvoiDossier) {
			fillEnvoiDossier((EnvoiDossier) mvt, view);
		}
		return view;
	}

	protected List<MouvementDetailView> getViews(Collection<MouvementDossier> mvts, boolean sortByNoDossier) throws InfrastructureException {
		if (mvts != null && mvts.size() > 0) {
			prefetchIndividus(mvts);
			final List<MouvementDetailView> liste = new ArrayList<MouvementDetailView>(mvts.size());
			for (MouvementDossier mvt : mvts) {
				liste.add(getView(mvt));
			}
			if (sortByNoDossier && liste.size() > 1) {
				Collections.sort(liste, new Comparator<MouvementDetailView>() {
					public int compare(MouvementDetailView o1, MouvementDetailView o2) {
						final long no1 = o1.getContribuable().getNumero();
						final long no2 = o2.getContribuable().getNumero();
						return no1 < no2 ? -1 : (no1 > no2 ? 1 : 0);
					}
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

		final ServiceCivilService serviceCivil = tiersService.getServiceCivilService();
		if (serviceCivil.isWarmable() && mvts != null && mvts.size() > 1) {

			// d'abord on cherche tous les identifiants de tiers
			final Set<Long> idsTiers = new HashSet<Long>(mvts.size());
			for (MouvementDossier mvt : mvts) {
				idsTiers.add(mvt.getContribuable().getNumero());
			}

			// que l'on découpe ensuite en petits lots pour récupérer les numéros d'individus
			final int nbLots = idsTiers.size() / TAILLE_LOT + 1;
			final List<Long> listeIdsTiers = new ArrayList<Long>(idsTiers);
			for (int i = 0 ; i < nbLots ; ++ i) {
				final int idxMin = i * TAILLE_LOT;
				final int idxMax = Math.min((i + 1) * TAILLE_LOT, listeIdsTiers.size());
				if (idxMin < idxMax) {
					final Set<Long> lotTiersIds = new HashSet<Long>(listeIdsTiers.subList(idxMin, idxMax));
					final Set<Long> noIndividus = tiersDAO.getNumerosIndividu(lotTiersIds, true);
					tiersService.getServiceCivilService().getIndividus(noIndividus, null, EnumAttributeIndividu.ADRESSES);
				}
			}

			final long end = System.nanoTime();
			LOGGER.info(String.format("Prefetched les individus de %d mouvements en %d millisecondes", mvts.size(), (end - start) / 1000000L));
		}
	}

	private MouvementDetailView buildAndFillCommonElements(MouvementDossier mvt) {
		final MouvementDetailView view = new MouvementDetailView();
		view.setId(mvt.getId());
		view.setDateMouvement(mvt.getDateMouvement() != null ? mvt.getDateMouvement().asJavaDate() : null);
		view.setEtatMouvement(mvt.getEtat());
		view.setDateExecution(mvt.getLogModifDate());
		view.setExecutant(mvt.getLogModifUser());

		final TiersGeneralView tiersGeneralView = tiersGeneralManager.getTiers(mvt.getContribuable(), false);
		view.setContribuable(tiersGeneralView);

		view.setAnnule(mvt.isAnnule());
		view.setAnnulable(isAnnulable(mvt));
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
				final ch.vd.uniregctb.tiers.CollectiviteAdministrative ca = envoi.getCollectiviteAdministrativeEmettrice();
				if (ca != null && oid == ca.getNumeroCollectiviteAdministrative()) {
					isAnnulable = true;
				}
			}
			else if (mvt instanceof ReceptionDossier) {
				final RegDate aujourdhui = RegDate.get();
				final RegDate dateMouvement = mvt.getDateMouvement();
				if (dateMouvement == null || RegDateHelper.equals(aujourdhui, dateMouvement)) {
					final String visaConnecte = AuthenticationHelper.getCurrentPrincipal();
					final String createurMouvement = mvt.getLogCreationUser();
					if (StringUtils.equals(visaConnecte, createurMouvement)) {
						isAnnulable = true;
					}
				}
			}
			else {
				Assert.fail("Type de mouvement de dossier non supporté : " + mvt.getClass().getName());
			}
		}
		return isAnnulable;
	}

	private static class InfoCollaborateur {
		/**
		 * Numéro d'individu du collaborateur
		 */
		public final long noIndividu;

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

		private InfoCollaborateur(long noIndividu, String nomPrenom, String noTelephoneDansOid, String visaOperateur) {
			this.noIndividu = noIndividu;
			this.nomPrenom = nomPrenom;
			this.noTelephoneDansOid = noTelephoneDansOid;
			this.visaOperateur = visaOperateur;
		}
	}

	private InfoCollaborateur getInfosCollaborateur(long noIndividu) {

		final String nomUtilisateur = hostCivilService.getNomUtilisateur(noIndividu);

		final Operateur operateur = serviceSecuriteService.getOperateur(noIndividu);
		final String visaOperateur;
		final String noTelephone;
		if (operateur != null) {
			visaOperateur = operateur.getCode();
			final List<ch.vd.infrastructure.model.CollectiviteAdministrative> collectivites = serviceSecuriteService.getCollectivitesUtilisateur(visaOperateur);
			if (collectivites != null) {
				final ch.vd.infrastructure.model.CollectiviteAdministrative collectivite = collectivites.get(0);
				final ProfilOperateur profileUtilisateur = serviceSecuriteService.getProfileUtilisateur(visaOperateur, collectivite.getNoColAdm());
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
		}
		return new InfoCollaborateur(noIndividu, nomUtilisateur, noTelephone, visaOperateur);
	}

	private void fillEnvoiDossier(EnvoiDossier envoi, MouvementDetailView view) throws InfrastructureException {
		view.setTypeMouvement(TypeMouvement.EnvoiDossier);
		view.setCollectiviteAdministrative(getNomCollectiviteAdministrative(envoi.getCollectiviteAdministrativeEmettrice()));

		if (envoi instanceof EnvoiDossierVersCollaborateur) {
			final long noIndividu = ((EnvoiDossierVersCollaborateur) envoi).getNoIndividuDestinataire();
			final InfoCollaborateur infoCollaborateur = getInfosCollaborateur(noIndividu);
			view.setDestinationUtilisateur(infoCollaborateur.nomPrenom);
			view.setNomPrenomUtilisateur(infoCollaborateur.nomPrenom);
			view.setNumeroTelephoneUtilisateur(infoCollaborateur.noTelephoneDansOid);
			view.setUtilisateurEnvoi(infoCollaborateur.visaOperateur);
		}
		else if (envoi instanceof EnvoiDossierVersCollectiviteAdministrative) {
			final String nomCollectiviteAdm = getNomCollectiviteAdministrative(((EnvoiDossierVersCollectiviteAdministrative) envoi).getCollectiviteAdministrativeDestinataire());
			view.setDestinationUtilisateur(nomCollectiviteAdm);
		}
	}

	private void fillReceptionDossier(ReceptionDossier reception, MouvementDetailView view) throws InfrastructureException {
		view.setTypeMouvement(TypeMouvement.ReceptionDossier);
		if (reception.getCollectiviteAdministrativeReceptrice() != null) {
			final String nomCollectiviteAdm = getNomCollectiviteAdministrative(reception.getCollectiviteAdministrativeReceptrice());
			view.setCollectiviteAdministrative(nomCollectiviteAdm);
		}
		if (reception instanceof ReceptionDossierPersonnel) {
			final long noIndividu = ((ReceptionDossierPersonnel) reception).getNoIndividuRecepteur();
			final InfoCollaborateur infoCollaborateur = getInfosCollaborateur(noIndividu);
			view.setDestinationUtilisateur(infoCollaborateur.nomPrenom);
			view.setNomPrenomUtilisateur(infoCollaborateur.nomPrenom);
			view.setNumeroTelephoneUtilisateur(infoCollaborateur.noTelephoneDansOid);
			view.setUtilisateurReception(infoCollaborateur.visaOperateur);
		}
		else {
			final String messageKey = String.format("option.localisation.%s", reception.getLocalisation().name());
			final String localisationStr = messageSource.getMessage(messageKey, null, WebContextUtils.getDefaultLocale());
			view.setDestinationUtilisateur(localisationStr);
		}
		view.setLocalisation(reception.getLocalisation());
	}

	private String getNomCollectiviteAdministrative(ch.vd.uniregctb.tiers.CollectiviteAdministrative ca) throws InfrastructureException {
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

	protected void detruireMouvementsTropVieux(Contribuable ctb) {
		detruireMouvementsTropVieux(mouvementDossierDAO, ctb);
	}

	protected static void detruireMouvementsTropVieux(MouvementDossierDAO dao, Contribuable ctb) {
		// on ne doit garder que les x derniers mouvements de dossiers pour ce contribuable
		if (ctb.getMouvementsDossier().size() > NB_MAX_MOUVEMENTS_GARDES) {

			final List<MouvementDossier> mvts = new ArrayList<MouvementDossier>(ctb.getMouvementsDossier());
			Collections.sort(mvts, new AntiChronologiqueMouvementComparator());

			final List<MouvementDossier> aDetruire = mvts.subList(NB_MAX_MOUVEMENTS_GARDES, mvts.size());
			final Set<MouvementDossier> restant = new HashSet<MouvementDossier>(mvts.subList(0, NB_MAX_MOUVEMENTS_GARDES));
			ctb.setMouvementsDossier(restant);

			for (MouvementDossier mvtADetruire : aDetruire) {
				dao.remove(mvtADetruire.getId());
			}
		}
	}
}
