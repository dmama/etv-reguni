package ch.vd.unireg.mouvement.manager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableInt;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.batchtemplate.BatchResults;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.CsvHelper;
import ch.vd.unireg.common.EditiqueErrorHelper;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.common.MimeTypeHelper;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.common.pagination.ParamSorting;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.EditiqueResultat;
import ch.vd.unireg.editique.EditiqueResultatDocument;
import ch.vd.unireg.editique.EditiqueResultatErreur;
import ch.vd.unireg.extraction.BaseExtractorImpl;
import ch.vd.unireg.extraction.BatchableExtractor;
import ch.vd.unireg.extraction.ExtractionJob;
import ch.vd.unireg.extraction.ExtractionService;
import ch.vd.unireg.interfaces.infra.InfrastructureException;
import ch.vd.unireg.mouvement.BordereauMouvementDossier;
import ch.vd.unireg.mouvement.BordereauMouvementDossierDAO;
import ch.vd.unireg.mouvement.EnvoiDossierVersCollectiviteAdministrative;
import ch.vd.unireg.mouvement.EtatMouvementDossier;
import ch.vd.unireg.mouvement.MouvementDossier;
import ch.vd.unireg.mouvement.MouvementDossierCriteria;
import ch.vd.unireg.mouvement.MouvementService;
import ch.vd.unireg.mouvement.ProtoBordereauMouvementDossier;
import ch.vd.unireg.mouvement.ReceptionDossierClassementGeneral;
import ch.vd.unireg.mouvement.view.BordereauEnvoiReceptionView;
import ch.vd.unireg.mouvement.view.BordereauEnvoiView;
import ch.vd.unireg.mouvement.view.BordereauListElementView;
import ch.vd.unireg.mouvement.view.ContribuableView;
import ch.vd.unireg.mouvement.view.MouvementDetailView;
import ch.vd.unireg.mouvement.view.MouvementMasseCriteriaView;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.type.Localisation;
import ch.vd.unireg.type.TypeMouvement;

public class MouvementMasseManagerImpl extends AbstractMouvementManagerImpl implements MouvementMasseManager {

	private MouvementService mouvementService;

	private BordereauMouvementDossierDAO bordereauDAO;

	private ExtractionService extractionService;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setMouvementService(MouvementService mouvementService) {
		this.mouvementService = mouvementService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setBordereauDAO(BordereauMouvementDossierDAO bordereauDAO) {
		this.bordereauDAO = bordereauDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setExtractionService(ExtractionService extractionService) {
		this.extractionService = extractionService;
	}

	/**
	 * Doit être appelé dans une transaction
	 * @param noCa numéro de la collectivité administrative
	 * @return ID technique du tiers correspondant à cette collectivité administrative (ou 0 si cette collectivité est inconnue chez nous)
	 */
	private long getIdCollAdmFromNumeroCA(int noCa) {
		final CollectiviteAdministrative ca = getTiersService().getCollectiviteAdministrative(noCa);
		return ca != null ? ca.getNumero() : 0L;
	}

	@Override
	@Transactional(readOnly = true)
	public List<MouvementDetailView> find(MouvementMasseCriteriaView view, Integer noCollAdmInitiatrice, ParamPagination paramPagination, MutableInt total) throws InfrastructureException {

		if (view == null) {
			return null;
		}

		final MouvementDossierCriteria criteria = createCoreCriteria(view, noCollAdmInitiatrice);
		final int count = getMouvementDossierDAO().count(criteria);
		if (total != null) {
			total.setValue(count);
		}
		if (count > 0) {
			final List<MouvementDossier> liste = getMouvementDossierDAO().find(criteria, paramPagination);
			return getViews(liste, false, false);
		}
		else {
			return Collections.emptyList();
		}
	}

	/**
	 * Doit être appelé dans une transaction
	 * @param view critères tels que montrés dans le formulaire de recherche
	 * @param noCollAdmInitiatrice numéro de la collectivité administrative dans laquelle est loggué l'utilisateur
	 * @return les critères utilisables par le DAO
	 */
	private MouvementDossierCriteria createCoreCriteria(MouvementMasseCriteriaView view, Integer noCollAdmInitiatrice) {
		final DateRange range = new DateRangeHelper.Range(RegDateHelper.get(view.getDateMouvementMin()), RegDateHelper.get(view.getDateMouvementMax()));
		final MouvementDossierCriteria criteria = new MouvementDossierCriteria();
		criteria.setNoCtb(view.getNoCtb());
		criteria.setRangeDateMouvement(range);
		criteria.setInclureMouvementsAnnules(view.isMouvementsAnnulesInclus());
		criteria.setSeulementDerniersMouvements(view.isSeulementDernierMouvementDossiers());

		final TypeMouvement typeMouvement = view.getTypeMouvement();
		if (typeMouvement == TypeMouvement.EnvoiDossier) {
			if (view.getNoCollAdmDestinataire() != null) {
				criteria.setIdCollAdministrativeDestinataire(getIdCollAdmFromNumeroCA(view.getNoCollAdmDestinataire()));
			}
			criteria.setVisaDestinataire(view.getVisaDestinataire());
		}
		else if (typeMouvement == TypeMouvement.ReceptionDossier) {
			final Localisation localisation = view.getLocalisationReception();
			criteria.setLocalisation(localisation);
			if (localisation == Localisation.PERSONNE) {
				criteria.setVisaRecepteur(view.getVisaRecepteur());
			}
		}
		criteria.setTypeMouvement(typeMouvement);
		criteria.setEtatsMouvement(view.getEtatsRecherches());

		// collectivité administrative initiatrice
		if (noCollAdmInitiatrice != null) {
			criteria.setIdCollAdministrativeInitiatrice(getIdCollAdmFromNumeroCA(noCollAdmInitiatrice));
		}
		return criteria;
	}

	@Override
	@Transactional(readOnly = true)
	public List<MouvementDetailView> find(MouvementDossierCriteria criteria) throws InfrastructureException {
		final List<MouvementDossier> mvts = getMouvementDossierDAO().find(criteria, null);
		return getViews(mvts, false, false);
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void changeEtat(EtatMouvementDossier nouvelEtat, long mvtId) {
		final MouvementDossier mvt = getMouvementDossierDAO().get(mvtId);
		changeEtat(nouvelEtat, mvt);
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void changeEtat(EtatMouvementDossier nouvelEtat, long[] ids) {
		final List<MouvementDossier> mvts = getMouvementDossierDAO().get(ids);
		for (MouvementDossier mvt : mvts) {
			changeEtat(nouvelEtat, mvt);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<BordereauListElementView> getProtoBordereaux(Integer noCollAdmInitiatrice) {
		final List<ProtoBordereauMouvementDossier> protos = getMouvementDossierDAO().getAllProtoBordereaux(noCollAdmInitiatrice);
		if (protos != null && !protos.isEmpty()) {
			final List<BordereauListElementView> list = new ArrayList<>(protos.size());
			for (ProtoBordereauMouvementDossier proto : protos) {
				list.add(getView(proto));
			}
			return list;
		}
		else {
			return null;
		}
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public EditiqueResultat imprimerBordereau(long[] idsMouvement) throws EditiqueException {
		final List<MouvementDossier> mvts = getMouvementDossierDAO().get(idsMouvement);
		final EditiqueResultat resultat = mouvementService.envoyerImpressionBordereau(mvts);
		if (!(resultat instanceof EditiqueResultatDocument)) {
			// je veux faire sauter la transaction pour que le bordereau ne soit pas généré
			throw new EditiqueException(getMessageErreurEditique(resultat));
		}
		return resultat;
	}

	private static String getMessageErreurEditique(EditiqueResultat editiqueResultat) {
		if (editiqueResultat instanceof EditiqueResultatErreur) {
			return EditiqueErrorHelper.getMessageErreurEditique((EditiqueResultatErreur) editiqueResultat);
		}
		else {
			return "Erreur inattendue.";
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<BordereauEnvoiView> findBordereauxAReceptionner(Integer noCollAdmReceptrice) {
		final List<BordereauMouvementDossier> bordereaux = bordereauDAO.getBordereauxAReceptionner(noCollAdmReceptrice);
		if (bordereaux != null && !bordereaux.isEmpty()) {
			final List<BordereauEnvoiView> liste = new ArrayList<>(bordereaux.size());
			for (BordereauMouvementDossier b : bordereaux) {
				liste.add(getView(b));
			}
			return liste;
		}
		else {
			return null;
		}
	}

	@Override
	@Transactional(readOnly = true)
	public BordereauEnvoiReceptionView getBordereauPourReception(long idBordereau) throws InfrastructureException {
		final BordereauEnvoiReceptionView view = new BordereauEnvoiReceptionView();
		final BordereauMouvementDossier bordereau = bordereauDAO.get(idBordereau);
		fillView(bordereau, view);
		view.setMvts(getViews(bordereau.getContenu(), true, false));
		return view;
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void receptionnerMouvementsEnvoi(long[] idsMouvements) {
		final List<MouvementDossier> mvts = getMouvementDossierDAO().get(idsMouvements);
		if (mvts != null && !mvts.isEmpty()) {
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

	@Override
	@Transactional(readOnly = true)
	public void refreshView(BordereauEnvoiReceptionView view) throws InfrastructureException {
		final BordereauMouvementDossier bordereau = bordereauDAO.get(view.getId());
		fillView(bordereau, view);
		view.setMvts(getViews(bordereau.getContenu(), true, false));
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
			final ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative ca = getServiceInfra().getCollectivite(noCa);
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

	@Override
	@Transactional(readOnly = true)
	public ExtractionJob exportListeRecherchee(MouvementMasseCriteriaView criteria, Integer noCollAdmInitiatrice, ParamSorting sorting) {
		final MouvementDossierCriteria coreCriteria = createCoreCriteria(criteria, noCollAdmInitiatrice);
		final MouvementDossierExtractor extractor = new MouvementDossierExtractor(coreCriteria, criteria.getNoCollAdmDestinataire(), sorting);
		final String visa = AuthenticationHelper.getCurrentPrincipal();
		return extractionService.postExtractionQuery(visa, extractor);
	}

	/**
	 * Extracteur de mouvements de dossiers répondants à certains critères
	 */
	public class MouvementDossierExtractor extends BaseExtractorImpl implements BatchableExtractor<Long, MouvementDossierExtractionResult> {

		private final MouvementDossierCriteria criteria;
		private final Integer noCaDestinataire;
		private final ParamSorting sorting;

		public MouvementDossierExtractor(MouvementDossierCriteria criteria, Integer noCaDestinataire, ParamSorting sorting) {
			this.criteria = criteria;
			this.noCaDestinataire = noCaDestinataire;
			this.sorting = sorting;
		}

		@Override
		public MouvementDossierExtractionResult createRapport(boolean rapportFinal) {
			return new MouvementDossierExtractionResult();
		}

		@Override
		public Behavior getBatchBehavior() {
			return Behavior.REPRISE_AUTOMATIQUE;
		}

		@Override
		public List<Long> buildElementList() {
			getStatusManager().setMessage("Recherche des mouvements à extraire...");
			try {
				return getMouvementDossierDAO().findIds(criteria, sorting);
			}
			finally {
				getStatusManager().setMessage("Extraction des mouvements...", 0);
			}
		}

		@Override
		public int getBatchSize() {
			return 100;
		}

		@Override
		public boolean doBatchExtraction(List<Long> batch, MouvementDossierExtractionResult rapport) throws Exception {
			// extraction des ID de la liste vers un tableau
			final long[] ids = new long[batch.size()];
			int index = 0;
			for (Long id : batch) {
				if (id != null) {
					ids[index ++] = id;
				}
			}

			// rien à faire si aucun mouvement n'est en fait sélectionné
			if (index > 0) {

				// le dao ne garantit pas de retourner les éléments dans l'ordre des ids indiqués
				// -> afin de garantir l'ordre de tri dans l'extraction, je dois donc repasser par la liste initiale
				final List<MouvementDossier> mvts = getMouvementDossierDAO().get(ids);
				final Map<Long, MouvementDossier> map = new HashMap<>(mvts.size());
				for (MouvementDossier mvt : mvts) {
					map.put(mvt.getId(), mvt);
				}

				// nouvelle liste des mouvements triés dans le même ordre que la liste initiale des ids
				final List<MouvementDossier> mvtsTries = new ArrayList<>(mvts.size());
				for (Long id : batch) {
					if (id != null) {
						final MouvementDossier mvt = map.get(id);
						if (mvt != null) {
							mvtsTries.add(mvt);
						}
					}
				}

				if (!mvtsTries.isEmpty()) {
					final List<MouvementDetailView> infos = getViews(mvtsTries, false, true);
					rapport.addMouvements(infos);
				}
			}

			return !wasInterrupted();
		}

		@Override
		public TemporaryFile getExtractionContent(MouvementDossierExtractionResult rapportFinal) throws IOException {
			return buildCsv(rapportFinal.mvts);
		}

		@Override
		public String getMimeType() {
			return CsvHelper.MIME_TYPE;
		}

		@Override
		public String getFilenameRadical() {
			return "mouvements";
		}

		@Override
		public void afterTransactionCommit(MouvementDossierExtractionResult rapportFinal, int percentProgression) {
			getStatusManager().setMessage("Extraction des mouvements...", percentProgression);
		}

		@Override
		public String getExtractionName() {
			return "Mouvements de dossiers";
		}

		@Override
		public String getExtractionDescription() {
			final StringBuilder b = new StringBuilder();
			b.append("Extraction des mouvements de dossiers");
			if (criteria.isSeulementDerniersMouvements()) {
				b.append(" (seulement le dernier mouvement traité de chaque dossier)");
			}
			else if (criteria.isInclureMouvementsAnnules()) {
				b.append(" (y compris les mouvements annulés)");
			}
			if (criteria.getTypeMouvement() != null) {
				b.append(" de type ");

				switch (criteria.getTypeMouvement()) {
					case EnvoiDossier:
						b.append("'envoi'");
						if (criteria.getVisaDestinataire() != null) {
							b.append(" vers le collaborateur ").append(criteria.getVisaDestinataire().toLowerCase());
						}
						break;
					case ReceptionDossier:
						if (criteria.getLocalisation() != null) {
							switch (criteria.getLocalisation()) {
								case ARCHIVES:
									b.append("'réception aux archives'");
									break;
								case CLASSEMENT_GENERAL:
									b.append("'réception pour classement général'");
									break;
								case CLASSEMENT_INDEPENDANTS:
									b.append("'réception pour classement des indépendants'");
									break;
								case PERSONNE:
									b.append("'réception personnelle'");
									if (criteria.getVisaRecepteur() != null) {
										b.append(" par le collaborateur ").append(criteria.getVisaRecepteur().toLowerCase());
									}
									break;
							}
						}
						else {
							b.append("'réception'");
						}
						break;
				}
			}
			if (criteria.getNoCtb() != null) {
				b.append(" du contribuable ").append(FormatNumeroHelper.numeroCTBToDisplay(criteria.getNoCtb()));
			}
			if (criteria.getIdCollAdministrativeDestinataire() != null) {
				b.append(" vers la collectivité administrative ").append(noCaDestinataire);
			}
			final Collection<EtatMouvementDossier> etatsMouvement = criteria.getEtatsMouvement();
			if (etatsMouvement != null && !etatsMouvement.isEmpty()) {
				b.append(" dans l'état ");
				final int size = etatsMouvement.size();
				final List<EtatMouvementDossier> etats = new ArrayList<>(etatsMouvement);
				for (int i = 0 ; i < size; ++ i) {
					if (i > 0) {
						if (i < size - 1) {
							b.append(", ");
						}
						else {
							b.append(" ou ");
						}
					}
					final EtatMouvementDossier etat = etats.get(i);
					b.append('\'').append(etat.getDescription()).append('\'');
				}
			}
			return b.toString();
		}

		private TemporaryFile buildCsv(List<MouvementDetailView> mvts) {
			final String filename = String.format("%s%s", getFilenameRadical(), MimeTypeHelper.getFileExtensionForType(getMimeType()));
			return CsvHelper.asCsvTemporaryFile(mvts, filename, null, new CsvHelper.FileFiller<MouvementDetailView>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("CTB_ID").append(CsvHelper.COMMA);
					b.append("NOM_RAISON_SOCIALE").append(CsvHelper.COMMA);
					b.append("TYPE_MOUVEMENT").append(CsvHelper.COMMA);
					b.append("ETAT").append(CsvHelper.COMMA);
					b.append("COLL_ADM").append(CsvHelper.COMMA);
					b.append("DESTINATION");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, MouvementDetailView elt) {
					final ContribuableView ctb = elt.getContribuable();
					b.append(ctb.getNumero()).append(CsvHelper.COMMA);
					b.append(CsvHelper.asCsvField(ctb.getNomPrenom())).append(CsvHelper.COMMA);
					b.append(elt.getTypeMouvement()).append(CsvHelper.COMMA);
					b.append(elt.getEtatMouvement()).append(CsvHelper.COMMA);
					b.append(CsvHelper.escapeChars(elt.getCollectiviteAdministrative())).append(CsvHelper.COMMA);
					b.append(CsvHelper.escapeChars(elt.getDestinationUtilisateur()));
					return true;
				}
			});
		}
	}

	public static class MouvementDossierExtractionResult implements BatchResults<Long, MouvementDossierExtractionResult> {

		private final List<MouvementDetailView> mvts = new LinkedList<>();

		@Override
		public void addErrorException(Long element, Exception e) {
		}

		@Override
		public void addAll(MouvementDossierExtractionResult right) {
			this.mvts.addAll(right.mvts);
		}

		public void addMouvements(List<MouvementDetailView> mvts) {
			this.mvts.addAll(mvts);
		}
	}
}
