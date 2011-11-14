package ch.vd.uniregctb.acces.parUtilisateur.manager;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.acces.parUtilisateur.view.DroitAccesUtilisateurView;
import ch.vd.uniregctb.acces.parUtilisateur.view.RecapPersonneUtilisateurView;
import ch.vd.uniregctb.acces.parUtilisateur.view.UtilisateurEditRestrictionView;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.BatchResults;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.MimeTypeHelper;
import ch.vd.uniregctb.extraction.BaseExtractorImpl;
import ch.vd.uniregctb.extraction.BatchableExtractor;
import ch.vd.uniregctb.extraction.ExtractionJob;
import ch.vd.uniregctb.extraction.ExtractionService;
import ch.vd.uniregctb.general.manager.TiersGeneralManager;
import ch.vd.uniregctb.general.manager.UtilisateurManager;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.general.view.UtilisateurView;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.security.DroitAccesDAO;
import ch.vd.uniregctb.security.DroitAccesException;
import ch.vd.uniregctb.security.DroitAccesService;
import ch.vd.uniregctb.tiers.DroitAcces;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.Niveau;
import ch.vd.uniregctb.type.TypeDroitAcces;

public class UtilisateurEditRestrictionManagerImpl implements UtilisateurEditRestrictionManager {

	private UtilisateurManager utilisateurManager;
	private TiersGeneralManager tiersGeneralManager;
	private DroitAccesDAO droitAccesDAO;
	private DroitAccesService droitAccesService;
	private AdresseService adresseService;
	private TiersService tiersService;
	private ExtractionService extractionService;

	public void setUtilisateurManager(UtilisateurManager utilisateurManager) {
		this.utilisateurManager = utilisateurManager;
	}

	public void setTiersGeneralManager(TiersGeneralManager tiersGeneralManager) {
		this.tiersGeneralManager = tiersGeneralManager;
	}

	public void setDroitAccesDAO(DroitAccesDAO droitAccesDAO) {
		this.droitAccesDAO = droitAccesDAO;
	}

	public void setDroitAccesService(DroitAccesService droitAccesService) {
		this.droitAccesService = droitAccesService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setExtractionService(ExtractionService extractionService) {
		this.extractionService = extractionService;
	}

	/**
	 * Annule une liste de Restrictions
	 *
	 * @param listIdRestriction
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void annulerRestrictions(List<Long> listIdRestriction) throws DroitAccesException {
		for (Long id : listIdRestriction) {
			droitAccesService.annuleDroitAcces(id);
		}
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void annulerToutesLesRestrictions(Long noIndividuOperateur) {
		droitAccesService.annuleToutLesDroitAcces(noIndividuOperateur);
	}

	/**
	 * Alimente la vue du controller
	 * @return
	 * @throws ServiceInfrastructureException
	 */
	@Override
	@Transactional(readOnly = true)
	public UtilisateurEditRestrictionView get(long noIndividuOperateur) throws ServiceInfrastructureException, AdresseException {

		final UtilisateurView utilisateurView = utilisateurManager.get(noIndividuOperateur);
		final UtilisateurEditRestrictionView utilisateurEditRestrictionView = new UtilisateurEditRestrictionView();
		utilisateurEditRestrictionView.setUtilisateur(utilisateurView);
		final List<DroitAccesUtilisateurView> views = new ArrayList<DroitAccesUtilisateurView>();
		final List<DroitAcces> restrictions = droitAccesDAO.getDroitsAcces(noIndividuOperateur);
		for (DroitAcces droitAcces : restrictions) {
			final DroitAccesUtilisateurView droitAccesView = new DroitAccesUtilisateurView(droitAcces, tiersService, adresseService);
			views.add(droitAccesView);
		}
		utilisateurEditRestrictionView.setRestrictions(views);
		return utilisateurEditRestrictionView;

	}


	/**
	 * Alimente la vue RecapPersonneUtilisateurView
	 *
	 * @param numeroPP
	 * @param noIndividuOperateur
	 * @return
	 * @throws ServiceInfrastructureException
	 * @throws AdressesResolutionException
	 */
	@Override
	@Transactional(readOnly = true)
	public RecapPersonneUtilisateurView get(Long numeroPP, Long noIndividuOperateur) throws ServiceInfrastructureException, AdressesResolutionException {
		RecapPersonneUtilisateurView recapPersonneUtilisateurView = new RecapPersonneUtilisateurView();

		UtilisateurView utilisateurView = utilisateurManager.get(noIndividuOperateur);
		recapPersonneUtilisateurView.setUtilisateur(utilisateurView);

		PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(numeroPP);
		TiersGeneralView tiersGeneralView = tiersGeneralManager.getPersonnePhysique(pp, true);
		recapPersonneUtilisateurView.setDossier(tiersGeneralView);

		recapPersonneUtilisateurView.setType(TypeDroitAcces.INTERDICTION);

		return recapPersonneUtilisateurView;
	}

	/**
	 * Persiste le DroitAcces
	 * @param recapPersonneUtilisateurView
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void save(RecapPersonneUtilisateurView recapPersonneUtilisateurView) throws DroitAccesException {

		final long operateurId = recapPersonneUtilisateurView.getUtilisateur().getNumeroIndividu();
		final long tiersId = recapPersonneUtilisateurView.getDossier().getNumero();
		final TypeDroitAcces type = recapPersonneUtilisateurView.getType();
		final Niveau niveau = (recapPersonneUtilisateurView.isLectureSeule() ? Niveau.LECTURE : Niveau.ECRITURE);

		droitAccesService.ajouteDroitAcces(operateurId, tiersId, type, niveau);
	}

	@Override
	@Transactional(readOnly = true)
	public ExtractionJob exportListeDroitsAcces(Long operateurId) {
		final UtilisateurView utilisateurView = utilisateurManager.get(operateurId);
		;
		final DroitsAccesExtractor extractor = new DroitsAccesExtractor(utilisateurView);
		final String visa = AuthenticationHelper.getCurrentPrincipal();
		return extractionService.postExtractionQuery(visa, extractor);
	}

	public class DroitsAccesExtractor extends BaseExtractorImpl implements BatchableExtractor<Long, DroitsAccesExtractionResult> {


		private final UtilisateurView utilisateurView;


		public DroitsAccesExtractor(UtilisateurView utilisateurView) {
			this.utilisateurView = utilisateurView;
		}

		@Override
		public DroitsAccesExtractionResult createRapport(boolean rapportFinal) {
			return new DroitsAccesExtractionResult();
		}

		@Override
		public BatchTransactionTemplate.Behavior getBatchBehavior() {
			return BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE;
		}

		@Override
		public List<Long> buildElementList() {
			getStatusManager().setMessage("Recherche des droits d'accès pour l'utilisateur...");
			try {
				return droitAccesDAO.getIdsDroitsAcces(utilisateurView.getNumeroIndividu());
			}
			finally {
				getStatusManager().setMessage("Extraction des droits d'accès...", 0);
			}
		}

		@Override
		public int getBatchSize() {
			return 100;
		}

		@Override
		public boolean doBatchExtraction(List<Long> batch, DroitsAccesExtractionResult rapport) throws Exception {

			final List<DroitAccesUtilisateurView> infos = new ArrayList<DroitAccesUtilisateurView>();
			for (Long idDroitAcces : batch) {
				DroitAcces droitAcces = droitAccesDAO.get(idDroitAcces);
				if (!droitAcces.isAnnule()) {
					DroitAccesUtilisateurView accesView = new DroitAccesUtilisateurView(droitAcces, tiersService, adresseService);
					infos.add(accesView);
				}

			}
			rapport.addDroitsAcces(infos);
			return !wasInterrupted();
		}

		@Override
		public void afterTransactionCommit(DroitsAccesExtractionResult rapportFinal, int percentProgression) {
			getStatusManager().setMessage("Extraction des droits d'accès...", percentProgression);
		}

		@Override
		public InputStream getStreamForExtraction(DroitsAccesExtractionResult rapportFinal) throws IOException {
			final String contenu = buildCsv(rapportFinal.acces);
			return CsvHelper.getInputStream(contenu);
		}

		@Override
		public String getMimeType() {
			return CsvHelper.MIME_TYPE;
		}

		@Override
		public String getFilenameRadical() {
			return "DroitsAcces";
		}

		@Override
		public String getExtractionName() {
			return "Droits d'accès";
		}

		@Override
		public String getExtractionDescription() {
			final StringBuilder b = new StringBuilder();
			b.append("Extraction des droits d'accès pour l'utilisateur ");
			b.append(utilisateurView.getVisaOperateur());
			b.append(" - ");
			b.append(utilisateurView.getPrenomNom());
			return b.toString();
		}

		private String buildCsv(List<DroitAccesUtilisateurView> acces) {
			final String filename = String.format("%s%s", getFilenameRadical(), MimeTypeHelper.getFileExtensionForType(getMimeType()));
			return CsvHelper.asCsvFile(acces, filename, null, new CsvHelper.FileFiller<DroitAccesUtilisateurView>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("TYPE_DROIT").append(CsvHelper.COMMA);
					b.append("NIVEAU").append(CsvHelper.COMMA);
					b.append("DATE_DEBUT").append(CsvHelper.COMMA);
					b.append("DATE_FIN").append(CsvHelper.COMMA);

					b.append("NUMERO_CTB").append(CsvHelper.COMMA);
					b.append("PRENOM_NOM").append(CsvHelper.COMMA);
					b.append("LOCALITE").append(CsvHelper.COMMA);
					b.append("DATE_NAISSANCE");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, DroitAccesUtilisateurView elt) {
					b.append(elt.getType().name()).append(CsvHelper.COMMA);
					b.append(elt.getNiveau().name()).append(CsvHelper.COMMA);
					b.append(RegDateHelper.dateToDisplayString(elt.getDateDebut())).append(CsvHelper.COMMA);
					b.append(RegDateHelper.dateToDisplayString(elt.getDateFin())).append(CsvHelper.COMMA);
					b.append(elt.getNumeroCTB()).append(CsvHelper.COMMA);
					b.append(CsvHelper.asCsvField(elt.getPrenomNom())).append(CsvHelper.COMMA);
					b.append(elt.getLocalite()).append(CsvHelper.COMMA);
					b.append(RegDateHelper.dateToDashString(elt.getDateNaissance()));
					return true;
				}
			});
		}
	}


	public static class DroitsAccesExtractionResult implements BatchResults<Long, DroitsAccesExtractionResult> {

		private final List<DroitAccesUtilisateurView> acces = new LinkedList<DroitAccesUtilisateurView>();

		@Override
		public void addErrorException(Long element, Exception e) {

		}

		@Override
		public void addAll(DroitsAccesExtractionResult right) {
			acces.addAll(right.acces);
		}

		public void addDroitsAcces(List<DroitAccesUtilisateurView> paramAcces) {
			acces.addAll(paramAcces);

		}
	}
}
