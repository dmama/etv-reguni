package ch.vd.uniregctb.mouvement.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.mutable.MutableLong;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.EditiqueErrorHelper;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.mouvement.BordereauMouvementDossier;
import ch.vd.uniregctb.mouvement.BordereauMouvementDossierDAO;
import ch.vd.uniregctb.mouvement.EnvoiDossierVersCollectiviteAdministrative;
import ch.vd.uniregctb.mouvement.EtatMouvementDossier;
import ch.vd.uniregctb.mouvement.MouvementDossier;
import ch.vd.uniregctb.mouvement.MouvementDossierCriteria;
import ch.vd.uniregctb.mouvement.MouvementService;
import ch.vd.uniregctb.mouvement.ProtoBordereauMouvementDossier;
import ch.vd.uniregctb.mouvement.ReceptionDossierClassementGeneral;
import ch.vd.uniregctb.mouvement.view.BordereauEnvoiReceptionView;
import ch.vd.uniregctb.mouvement.view.BordereauEnvoiView;
import ch.vd.uniregctb.mouvement.view.BordereauListElementView;
import ch.vd.uniregctb.mouvement.view.MouvementDetailView;
import ch.vd.uniregctb.mouvement.view.MouvementMasseCriteriaView;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.type.Localisation;
import ch.vd.uniregctb.type.TypeMouvement;

public class MouvementMasseManagerImpl extends AbstractMouvementManagerImpl implements MouvementMasseManager {

	private MouvementService mouvementService;

	private BordereauMouvementDossierDAO bordereauDAO;

	public void setMouvementService(MouvementService mouvementService) {
		this.mouvementService = mouvementService;
	}

	public void setBordereauDAO(BordereauMouvementDossierDAO bordereauDAO) {
		this.bordereauDAO = bordereauDAO;
	}

	private long getIdCollAdmFromNumeroCA(int noCa) {
		final CollectiviteAdministrative ca = getTiersService().getCollectiviteAdministrative(noCa);
		return ca.getNumero();
	}

	@Transactional(readOnly = true)
	public List<MouvementDetailView> find(MouvementMasseCriteriaView view, Integer noCollAdmInitiatrice, ParamPagination paramPagination, MutableLong total) throws InfrastructureException {

		if (view == null) {
			return null;
		}

		final DateRange range = new DateRangeHelper.Range(RegDate.get(view.getDateMouvementMin()), RegDate.get(view.getDateMouvementMax()));
		final MouvementDossierCriteria criteria = new MouvementDossierCriteria();
		criteria.setNoCtb(view.getNoCtb());
		criteria.setRangeDateMouvement(range);
		criteria.setInclureMouvementsAnnules(view.isMouvementsAnnulesInclus());

		final TypeMouvement typeMouvement = view.getTypeMouvement();
		if (typeMouvement == TypeMouvement.EnvoiDossier) {
			if (view.getNoCollAdmDestinataire() != null) {
				criteria.setIdCollAdministrativeDestinataire(getIdCollAdmFromNumeroCA(view.getNoCollAdmDestinataire()));
			}
			criteria.setNoIndividuDestinataire(view.getNoIndividuDestinataire());
		}
		else if (typeMouvement == TypeMouvement.ReceptionDossier) {
			final Localisation localisation = view.getLocalisationReception();
			criteria.setLocalisation(localisation);
			if (localisation == Localisation.PERSONNE) {
				criteria.setNoIndividuRecepteur(view.getNoIndividuReception());
			}
		}
		criteria.setTypeMouvement(typeMouvement);
		criteria.setEtatsMouvement(view.getEtatsRecherches());

		// collectivité administrative initiatrice
		if (noCollAdmInitiatrice != null) {
			criteria.setIdCollAdministrativeInitiatrice(getIdCollAdmFromNumeroCA(noCollAdmInitiatrice));
		}

		final long count = getMouvementDossierDAO().count(criteria);
		if (total != null) {
			total.setValue(count);
		}
		if (count > 0) {
			final List<MouvementDossier> liste = getMouvementDossierDAO().find(criteria, paramPagination);
			return getViews(liste, false);
		}
		else {
			return Collections.emptyList();
		}
	}

	@Transactional(readOnly = true)
	public List<MouvementDetailView> find(MouvementDossierCriteria criteria) throws InfrastructureException {
		final List<MouvementDossier> mvts = getMouvementDossierDAO().find(criteria, null);
		return getViews(mvts, false);
	}

	@Transactional(rollbackFor = Throwable.class)
	public void changeEtat(EtatMouvementDossier nouvelEtat, long mvtId) {
		final MouvementDossier mvt = getMouvementDossierDAO().get(mvtId);
		changeEtat(nouvelEtat, mvt);
	}

	@Transactional(rollbackFor = Throwable.class)
	public void changeEtat(EtatMouvementDossier nouvelEtat, long[] ids) {
		final List<MouvementDossier> mvts = getMouvementDossierDAO().get(ids);
		for (MouvementDossier mvt : mvts) {
			changeEtat(nouvelEtat, mvt);
		}
	}

	@Transactional(readOnly = true)
	public List<BordereauListElementView> getProtoBordereaux(Integer noCollAdmInitiatrice) {
		final List<ProtoBordereauMouvementDossier> protos = getMouvementDossierDAO().getAllProtoBordereaux(noCollAdmInitiatrice);
		if (protos != null && protos.size() > 0) {
			final List<BordereauListElementView> list = new ArrayList<BordereauListElementView>(protos.size());
			for (ProtoBordereauMouvementDossier proto : protos) {
				list.add(getView(proto));
			}
			return list;
		}
		else {
			return null;
		}
	}

	@Transactional(rollbackFor = Throwable.class)
	public EditiqueResultat imprimerBordereau(long[] idsMouvement) throws EditiqueException {
		final List<MouvementDossier> mvts = getMouvementDossierDAO().get(idsMouvement);
		final EditiqueResultat resultat = mouvementService.envoyerImpressionBordereau(mvts);
		if (resultat == null || resultat.getDocument() == null) {
			// je veux faire sauter la transaction pour que le bordereau ne soit pas généré
			throw new EditiqueException(EditiqueErrorHelper.getMessageErreurEditique(resultat));
		}
		return resultat;
	}

	@Transactional(readOnly = true)
	public List<BordereauEnvoiView> findBordereauxAReceptionner(Integer noCollAdmReceptrice) {
		final List<BordereauMouvementDossier> bordereaux = bordereauDAO.getBordereauxAReceptionner(noCollAdmReceptrice);
		if (bordereaux != null && bordereaux.size() > 0) {
			final List<BordereauEnvoiView> liste = new ArrayList<BordereauEnvoiView>(bordereaux.size());
			for (BordereauMouvementDossier b : bordereaux) {
				liste.add(getView(b));
			}
			return liste;
		}
		else {
			return null;
		}
	}

	@Transactional(readOnly = true)
	public BordereauEnvoiReceptionView getBordereauPourReception(long idBordereau) throws InfrastructureException {
		final BordereauEnvoiReceptionView view = new BordereauEnvoiReceptionView();
		final BordereauMouvementDossier bordereau = bordereauDAO.get(idBordereau);
		fillView(bordereau, view);
		view.setMvts(getViews(bordereau.getContenu(), true));
		return view;
	}

	@Transactional(rollbackFor = Throwable.class)
	public void receptionnerMouvementsEnvoi(long[] idsMouvements) {
		final List<MouvementDossier> mvts = getMouvementDossierDAO().get(idsMouvements);
		if (mvts != null && mvts.size() > 0) {
			for (MouvementDossier mvt : mvts) {
				if (mvt instanceof EnvoiDossierVersCollectiviteAdministrative) {
					final EnvoiDossierVersCollectiviteAdministrative envoi = (EnvoiDossierVersCollectiviteAdministrative) mvt;

					envoi.setEtat(EtatMouvementDossier.RECU_BORDEREAU);

					final ReceptionDossierClassementGeneral reception = creerMouvementReceptionClassement(envoi);
					getMouvementDossierDAO().save(reception);
				}
				else {
					throw new RuntimeException("Le mouvement de dossier " + mvt.getId() + " n'est pas un mouvement d'envoi qui peut être réceptionné");
				}
			}
		}
	}

	@Transactional(readOnly = true)
	public void refreshView(BordereauEnvoiReceptionView view) throws InfrastructureException {
		final BordereauMouvementDossier bordereau = bordereauDAO.get(view.getId());
		fillView(bordereau, view);
		view.setMvts(getViews(bordereau.getContenu(), true));
	}

	private ReceptionDossierClassementGeneral creerMouvementReceptionClassement(EnvoiDossierVersCollectiviteAdministrative envoi) {
		final CollectiviteAdministrative caReceptrice = envoi.getCollectiviteAdministrativeDestinataire();
		final ReceptionDossierClassementGeneral reception = new ReceptionDossierClassementGeneral();
		reception.setCollectiviteAdministrativeReceptrice(caReceptrice);
		reception.setContribuable(envoi.getContribuable());
		reception.setDateMouvement(RegDate.get());
		reception.setEtat(EtatMouvementDossier.TRAITE);
		return reception;
	}


	private BordereauEnvoiView getView(BordereauMouvementDossier bordereau) {
		final BordereauEnvoiView view = new BordereauEnvoiView();
		fillView(bordereau, view);
		return view;
	}

	private void fillView(BordereauMouvementDossier bordereau, BordereauEnvoiView view) {
		view.setId(bordereau.getId());
		view.setNbMouvementsEnvoyes(bordereau.getNombreMouvementsEnvoyes());
		view.setNbMouvementsRecus(bordereau.getNombreMouvementsRecus());

		final CollectiviteAdministrative destinataire = bordereau.getDestinataire();
		if (destinataire != null) {
			final String nomCa = getNomCollectiviteAdministrative(destinataire.getNumeroCollectiviteAdministrative());
			view.setNomCollAdmDestinataire(nomCa);
		}

		final CollectiviteAdministrative expediteur = bordereau.getExpediteur();
		if (expediteur != null) {
			final String nomCa = getNomCollectiviteAdministrative(expediteur.getNumeroCollectiviteAdministrative());
			view.setNomCollAdmEmettrice(nomCa);
		}
	}

	private BordereauListElementView getView(ProtoBordereauMouvementDossier bordereau) {
		final BordereauListElementView view = new BordereauListElementView();
		view.setType(bordereau.type);
		view.setIdCollAdmInitiatrice(bordereau.idCollAdmInitiatrice);
		view.setNoCollAdmInitiatrice(bordereau.noCollAdmInitiatrice);
		view.setNomCollAdmInitiatrice(getNomCollectiviteAdministrative(bordereau.noCollAdmInitiatrice));

		view.setIdCollAdmDestinataire(bordereau.idCollAdmDestinaire);
		view.setNoCollAdmDestinataire(bordereau.noCollAdmDestinataire);
		if (bordereau.noCollAdmDestinataire != null) {
			view.setNomCollAdmDestinataire(getNomCollectiviteAdministrative(bordereau.noCollAdmDestinataire));
		}
		view.setNombreMouvements(bordereau.nbMouvements);
		return view;
	}

	private String getNomCollectiviteAdministrative(int noCa) {
		try {
			final ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative ca = getServiceInfra().getCollectivite(noCa);
			return ca.getNomCourt();
		}
		catch (InfrastructureException e) {
			return String.format("Collectivité - %d", noCa);
		}
	}

	private void changeEtat(EtatMouvementDossier nouvelEtat, MouvementDossier mvt) {
		if (mvt != null && nouvelEtat != null && mvt.getEtat() != nouvelEtat) {
			mvt.setEtat(nouvelEtat);
		}
	}
}
