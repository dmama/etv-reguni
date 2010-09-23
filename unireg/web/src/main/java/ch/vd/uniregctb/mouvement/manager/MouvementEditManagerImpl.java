package ch.vd.uniregctb.mouvement.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.securite.model.Operateur;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.mouvement.EnvoiDossier;
import ch.vd.uniregctb.mouvement.EnvoiDossierVersCollaborateur;
import ch.vd.uniregctb.mouvement.EnvoiDossierVersCollectiviteAdministrative;
import ch.vd.uniregctb.mouvement.EtatMouvementDossier;
import ch.vd.uniregctb.mouvement.MouvementDossier;
import ch.vd.uniregctb.mouvement.ReceptionDossier;
import ch.vd.uniregctb.mouvement.ReceptionDossierArchives;
import ch.vd.uniregctb.mouvement.ReceptionDossierClassementGeneral;
import ch.vd.uniregctb.mouvement.ReceptionDossierClassementIndependants;
import ch.vd.uniregctb.mouvement.ReceptionDossierPersonnel;
import ch.vd.uniregctb.mouvement.view.MouvementDetailView;
import ch.vd.uniregctb.mouvement.view.MouvementListView;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.Localisation;
import ch.vd.uniregctb.type.TypeMouvement;
import ch.vd.uniregctb.utils.WebContextUtils;

public class MouvementEditManagerImpl extends AbstractMouvementManagerImpl implements MouvementEditManager {

	private static final int COLL_SUCC_ACI = 1344;

	// private static final String UTILISATEUR_RECEPTION = "utilisateurReception";
	private static final String UTILISATEUR_ENVOI = "utilisateurEnvoi";
	private static final String COLLECTIVITE = "collectivite";

	/**
	 * Alimente la vue MouvementListView en fonction d'un contribuable
	 * @return une vue MouvementListView
	 * @throws InfrastructureException
	 */
	@Transactional(readOnly = true)
	public MouvementListView findByNumeroDossier(Long numero, boolean seulementTraites) throws InfrastructureException {
		final MouvementListView mvtListView = new MouvementListView();
		mvtListView.setContribuable(creerCtb(numero));
		final List<MouvementDetailView> mvtsView = new ArrayList<MouvementDetailView>();
		final List<MouvementDossier> mvts = getMouvementDossierDAO().findByNumeroDossier(numero, seulementTraites, true);
		for (MouvementDossier mvt : mvts) {
			final MouvementDetailView mvtView = getView(mvt);
			mvtsView.add(mvtView);
		}
		Collections.sort(mvtsView);
		mvtListView.setMouvements(mvtsView);
		return mvtListView;
	}


	/**
	 * Creer une vue pour le mvt de dossier
	 *
	 * @param numero
	 * @return
	 */
	@Transactional(readOnly = true)
	public MouvementDetailView creerMvt(Long numero) {
		final MouvementDetailView mvtDetailView = new MouvementDetailView();
		mvtDetailView.setContribuable(creerCtb(numero));
		mvtDetailView.setTypeMouvement(TypeMouvement.EnvoiDossier);
		mvtDetailView.setDestinationEnvoi(UTILISATEUR_ENVOI);
		mvtDetailView.setLocalisation(Localisation.PERSONNE);

		final String visaOperateur = AuthenticationHelper.getCurrentPrincipal();
		final Operateur operateur = getServiceSecuriteService().getOperateur(visaOperateur);
		if (operateur != null) {
			mvtDetailView.setUtilisateurReception(visaOperateur);
			mvtDetailView.setNumeroUtilisateurReception(operateur.getIndividuNoTechnique());
		}
		return mvtDetailView;
	}


	/**
	 * Creer une vue pour le mvt de dossier depuis la tache transmission de dossier
	 *
	 * @param numero
	 * @param idTache
	 * @return
	 * @throws InfrastructureException
	 */
	@Transactional(readOnly = true)
	public MouvementDetailView creerMvtForTacheTransmissionDossier(Long numero, Long idTache) throws InfrastructureException {
		final MouvementDetailView mvtDetailView = new MouvementDetailView();
		mvtDetailView.setContribuable(creerCtb(numero));
		mvtDetailView.setIdTache(idTache);
		mvtDetailView.setTypeMouvement(TypeMouvement.EnvoiDossier);
		mvtDetailView.setDestinationEnvoi(COLLECTIVITE);
		mvtDetailView.setNoCollAdmDestinataireEnvoi(COLL_SUCC_ACI);
		final CollectiviteAdministrative collectiviteAdministrative = getServiceInfra().getCollectivite(COLL_SUCC_ACI);
		mvtDetailView.setCollAdmDestinataireEnvoi(collectiviteAdministrative.getNomCourt());
		return mvtDetailView;
	}


	/**
	 * Persiste en base le nouveau mvt de dossier
	 *
	 * @param mvtDetailView
	 * @throws Exception
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void save(MouvementDetailView mvtDetailView) throws Exception {
		final Contribuable ctb = (Contribuable) getTiersService().getTiers(mvtDetailView.getContribuable().getNumero());

		final MouvementDossier mvt;
		if (mvtDetailView.getTypeMouvement() == TypeMouvement.ReceptionDossier) {

			final ReceptionDossier reception;
			switch (mvtDetailView.getLocalisation()) {
				case PERSONNE:
					final Long noIndividu = mvtDetailView.getNumeroUtilisateurReception();
					if (noIndividu != null) {
						reception = new ReceptionDossierPersonnel(noIndividu);
					}
					else {
						throw new RuntimeException("La donnée de l'utilisateur qui réceptionne le dossier est obligatoire!");
					}
					break;
				case ARCHIVES:
					reception = new ReceptionDossierArchives();
					break;
				case CLASSEMENT_GENERAL:
					reception = new ReceptionDossierClassementGeneral();
					break;
				case CLASSEMENT_INDEPENDANTS:
					reception = new ReceptionDossierClassementIndependants();
					break;
				default:
					throw new RuntimeException("Localisation non-supportée : " + mvtDetailView.getLocalisation());
			}
			final ch.vd.uniregctb.tiers.CollectiviteAdministrative caReceptrice = getTiersService().getOrCreateCollectiviteAdministrative(AuthenticationHelper.getCurrentOID());
			reception.setCollectiviteAdministrativeReceptrice(caReceptrice);
			mvt = reception;
		}
		else if (mvtDetailView.getTypeMouvement() == TypeMouvement.EnvoiDossier) {

			final EnvoiDossier envoiDossier;
			if (mvtDetailView.getNoCollAdmDestinataireEnvoi() != null) {
				final ch.vd.uniregctb.tiers.CollectiviteAdministrative ca = getTiersService().getOrCreateCollectiviteAdministrative(mvtDetailView.getNoCollAdmDestinataireEnvoi());
				envoiDossier = new EnvoiDossierVersCollectiviteAdministrative(ca);
			}
			else if (mvtDetailView.getNumeroUtilisateurEnvoi() != null) {
				envoiDossier = new EnvoiDossierVersCollaborateur(mvtDetailView.getNumeroUtilisateurEnvoi());
			}
			else {
				throw new RuntimeException("Type de mouvement d'envoi de dossier non supporté");
			}
			final ch.vd.uniregctb.tiers.CollectiviteAdministrative caEmettrice = getTiersService().getOrCreateCollectiviteAdministrative(AuthenticationHelper.getCurrentOID());
			envoiDossier.setCollectiviteAdministrativeEmettrice(caEmettrice);
			mvt = envoiDossier;

			// s'il existe déjà un mouvement sur ce contribuable dans l'état "A traité" ou "A envoyer", ce mouvement
			// doit être annulé automatiquement
			annulerMouvementsEnAttente(ctb);
		}
		else {
			mvt = null;
		}

		if (mvt != null) {
			mvt.setDateMouvement(RegDate.get());
			mvt.setEtat(EtatMouvementDossier.TRAITE);
			ctb.addMouvementDossier(mvt);

			detruireMouvementsTropVieux(ctb);
		}
	}

	private void annulerMouvementsEnAttente(Contribuable ctb) {
		AuthenticationHelper.pushPrincipal(String.format("%s-auto-mvt", AuthenticationHelper.getCurrentPrincipal()));
		try {
			for (MouvementDossier mvt : ctb.getMouvementsDossier()) {
				final EtatMouvementDossier etat = mvt.getEtat();
				if (etat.isEnInstance() && !mvt.isAnnule()) {
					mvt.setAnnule(true);
				}
			}
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	/**
	 * Annule un mouvement
	 *
	 * @param idMvt
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void annulerMvt(long idMvt) {
		final MouvementDossier mvt = getMouvementDossierDAO().get(idMvt);
		mvt.setAnnule(true);
	}

	/**
	 * Alimente la vue contribuable pour le mouvement
	 *
	 * @param numero
	 * @return
	 */
	private TiersGeneralView creerCtb(Long numero) {
		final Tiers tiers = getTiersService().getTiers(numero);
		if (tiers == null) {
			throw new ObjectNotFoundException(getMessageSource().getMessage("error.tiers.inexistant" , null, WebContextUtils.getDefaultLocale()));
		}
		return getTiersGeneralManager().get(tiers, true);
	}
}
