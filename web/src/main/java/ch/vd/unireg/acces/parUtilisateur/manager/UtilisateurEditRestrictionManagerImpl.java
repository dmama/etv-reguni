package ch.vd.unireg.acces.parUtilisateur.manager;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.batchtemplate.BatchResults;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.unireg.acces.parUtilisateur.view.DroitAccesUtilisateurView;
import ch.vd.unireg.acces.parUtilisateur.view.RecapPersonneUtilisateurView;
import ch.vd.unireg.acces.parUtilisateur.view.UtilisateurEditRestrictionView;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.CsvHelper;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.common.MimeTypeHelper;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.common.pagination.WebParamPagination;
import ch.vd.unireg.extraction.BaseExtractorImpl;
import ch.vd.unireg.extraction.BatchableExtractor;
import ch.vd.unireg.extraction.ExtractionJob;
import ch.vd.unireg.extraction.ExtractionService;
import ch.vd.unireg.general.manager.TiersGeneralManager;
import ch.vd.unireg.general.manager.UtilisateurManager;
import ch.vd.unireg.general.view.TiersGeneralView;
import ch.vd.unireg.general.view.UtilisateurView;
import ch.vd.unireg.interfaces.civil.IndividuConnectorException;
import ch.vd.unireg.interfaces.entreprise.ServiceEntrepriseException;
import ch.vd.unireg.interfaces.infra.InfrastructureException;
import ch.vd.unireg.security.DroitAccesDAO;
import ch.vd.unireg.security.DroitAccesException;
import ch.vd.unireg.security.DroitAccesService;
import ch.vd.unireg.tiers.DroitAcces;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.Niveau;
import ch.vd.unireg.type.TypeDroitAcces;

public class UtilisateurEditRestrictionManagerImpl implements UtilisateurEditRestrictionManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(UtilisateurEditRestrictionManagerImpl.class);

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
	public void annulerToutesLesRestrictions(String visaOperateur) {
		droitAccesService.annuleToutLesDroitAcces(visaOperateur);
	}

	/**
	 * Alimente la vue du controller
	 */
	@Override
	@Transactional(readOnly = true)
	public UtilisateurEditRestrictionView get(@NotNull String visaOperateur, WebParamPagination pagination) throws InfrastructureException, AdresseException {
		final UtilisateurView utilisateurView = utilisateurManager.get(visaOperateur);
		final UtilisateurEditRestrictionView utilisateurEditRestrictionView = new UtilisateurEditRestrictionView();
		utilisateurEditRestrictionView.setUtilisateur(utilisateurView);
		final List<DroitAccesUtilisateurView> views = new ArrayList<>();
		final List<DroitAcces> restrictions = droitAccesDAO.getDroitsAcces(visaOperateur, pagination);
		for (DroitAcces droitAcces : restrictions) {
			try {
				final DroitAccesUtilisateurView view = new DroitAccesUtilisateurView(droitAcces, tiersService, adresseService);
				views.add(view);
			}
			catch (ServiceEntrepriseException | IndividuConnectorException e) {
				LOGGER.warn("Exception lors de la récupération des données du contribuable protégé " + FormatNumeroHelper.numeroCTBToDisplay(droitAcces.getTiers().getNumero()) + ".", e);
				final DroitAccesUtilisateurView view = new DroitAccesUtilisateurView(droitAcces, e);
				views.add(view);
			}
		}
		utilisateurEditRestrictionView.setRestrictions(views);
		utilisateurEditRestrictionView.setSize(droitAccesDAO.getDroitAccesCount(visaOperateur));
		return utilisateurEditRestrictionView;

	}



	/**
	 * Alimente la vue RecapPersonneUtilisateurView
	 */
	@Override
	@Transactional(readOnly = true)
	public RecapPersonneUtilisateurView get(Long numeroTiers, String visaOperateur) throws InfrastructureException {
		final RecapPersonneUtilisateurView recapPersonneUtilisateurView = new RecapPersonneUtilisateurView();

		final UtilisateurView utilisateurView = utilisateurManager.get(visaOperateur);
		recapPersonneUtilisateurView.setUtilisateur(utilisateurView);

		final Tiers tiers = tiersService.getTiers(numeroTiers);
		final TiersGeneralView tiersGeneralView = tiersGeneralManager.getTiers(tiers, true);
		recapPersonneUtilisateurView.setDossier(tiersGeneralView);

		recapPersonneUtilisateurView.setType(TypeDroitAcces.INTERDICTION);
		recapPersonneUtilisateurView.setNoDossier(numeroTiers);
		recapPersonneUtilisateurView.setVisaOperateur(visaOperateur);

		return recapPersonneUtilisateurView;
	}

	/**
	 * Persiste le DroitAcces
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void save(RecapPersonneUtilisateurView recapPersonneUtilisateurView) throws DroitAccesException {

		final String visaOperateur = recapPersonneUtilisateurView.getVisaOperateur();
		final long tiersId = recapPersonneUtilisateurView.getNoDossier();
		final TypeDroitAcces type = recapPersonneUtilisateurView.getType();
		final Niveau niveau = (recapPersonneUtilisateurView.isLectureSeule() ? Niveau.LECTURE : Niveau.ECRITURE);

		droitAccesService.ajouteDroitAcces(visaOperateur, tiersId, type, niveau);
	}

	@Override
	@Transactional(readOnly = true)
	public ExtractionJob exportListeDroitsAcces(String visaOperateur) {
		final UtilisateurView utilisateurView = utilisateurManager.get(visaOperateur);
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
		public Behavior getBatchBehavior() {
			return Behavior.REPRISE_AUTOMATIQUE;
		}

		@Override
		public List<Long> buildElementList() {
			getStatusManager().setMessage("Recherche des droits d'accès pour l'utilisateur...");
			try {
				return droitAccesDAO.getIdsDroitsAcces(utilisateurView.getVisaOperateur());
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

			final List<DroitAccesUtilisateurView> infos = new ArrayList<>();
			for (Long idDroitAcces : batch) {
				DroitAcces droitAcces = droitAccesDAO.get(idDroitAcces);
				if (!droitAcces.isAnnule()) {
					try {
						final DroitAccesUtilisateurView view = new DroitAccesUtilisateurView(droitAcces, tiersService, adresseService);
						infos.add(view);
					}
					catch (ServiceEntrepriseException | IndividuConnectorException e) {
						LOGGER.warn("Exception lors de la récupération des données du contribuable protégé " + FormatNumeroHelper.numeroCTBToDisplay(droitAcces.getTiers().getNumero()) + ".", e);
						final DroitAccesUtilisateurView view = new DroitAccesUtilisateurView(droitAcces, e);
						infos.add(view);
					}
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
		public TemporaryFile getExtractionContent(DroitsAccesExtractionResult rapportFinal) {
			return buildCsv(rapportFinal.acces);
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

		private TemporaryFile buildCsv(List<DroitAccesUtilisateurView> acces) {
			final String filename = String.format("%s%s", getFilenameRadical(), MimeTypeHelper.getFileExtensionForType(getMimeType()));
			return CsvHelper.asCsvTemporaryFile(acces, filename, null, new CsvHelper.FileFiller<DroitAccesUtilisateurView>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("TYPE_DROIT").append(CsvHelper.COMMA);
					b.append("NIVEAU").append(CsvHelper.COMMA);
					b.append("DATE_DEBUT").append(CsvHelper.COMMA);
					b.append("DATE_FIN").append(CsvHelper.COMMA);

					b.append("NUMERO_CTB").append(CsvHelper.COMMA);
					b.append("NOM_RAISON_SOCIALE").append(CsvHelper.COMMA);
					b.append("LOCALITE").append(CsvHelper.COMMA);
					b.append("DATE_NAISSANCE_INSC_RC");
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

		private final List<DroitAccesUtilisateurView> acces = new LinkedList<>();

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
