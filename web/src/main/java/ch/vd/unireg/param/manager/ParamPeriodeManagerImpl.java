package ch.vd.unireg.param.manager;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.ModeleDocumentDAO;
import ch.vd.unireg.declaration.ModeleFeuilleDocument;
import ch.vd.unireg.declaration.ModeleFeuilleDocumentDAO;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.param.view.ModeleDocumentView;
import ch.vd.unireg.param.view.ModeleFeuilleDocumentView;
import ch.vd.unireg.param.view.ParametrePeriodeFiscalePMEditView;
import ch.vd.unireg.param.view.ParametrePeriodeFiscalePPEditView;
import ch.vd.unireg.param.view.ParametrePeriodeFiscaleSNCEditView;
import ch.vd.unireg.parametrage.ParametrePeriodeFiscale;
import ch.vd.unireg.parametrage.ParametrePeriodeFiscaleDAO;
import ch.vd.unireg.parametrage.ParametrePeriodeFiscaleEmolument;
import ch.vd.unireg.parametrage.ParametrePeriodeFiscalePM;
import ch.vd.unireg.parametrage.ParametrePeriodeFiscalePP;
import ch.vd.unireg.parametrage.ParametrePeriodeFiscaleSNC;
import ch.vd.unireg.parametrage.PeriodeFiscaleService;
import ch.vd.unireg.type.ModeleFeuille;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocumentEmolument;

public class ParamPeriodeManagerImpl implements ParamPeriodeManager {

	/**
	 * Un logger pour {@link ParamPeriodeManagerImpl}
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(ParamPeriodeManagerImpl.class);

	private PeriodeFiscaleDAO periodeFiscaleDAO;
	private ModeleDocumentDAO modeleDocumentDAO;
	private ModeleFeuilleDocumentDAO modeleFeuilleDocumentDAO;
	private ParametrePeriodeFiscaleDAO parametrePeriodeFiscaleDAO;
	private PeriodeFiscaleService periodeFiscaleService;

	@Override
	@Transactional(readOnly = true)
	public List<PeriodeFiscale> getAllPeriodeFiscale() {
		List<PeriodeFiscale> list = periodeFiscaleDAO.getAllDesc();
		// force le chargment des ParametrePeriodeFiscale
		for (PeriodeFiscale periodeFiscale : list) {
			periodeFiscale.getParametrePeriodeFiscale();
		}
		return list;
	}

	@Override
	@Transactional(readOnly = true)
	public List<ModeleDocument> getModeleDocuments (PeriodeFiscale periodeFiscale) {
		 return modeleDocumentDAO.getByPeriodeFiscale(periodeFiscale);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ModeleFeuilleDocument> getModeleFeuilleDocuments(ModeleDocument modeleDocument) {
		return modeleFeuilleDocumentDAO.getByModeleDocument(modeleDocument);
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public PeriodeFiscale initNouvellePeriodeFiscale() {
		return periodeFiscaleService.initNouvellePeriodeFiscale();

	}

	@Override
	@Transactional(readOnly = true)
	public ParametrePeriodeFiscalePP getPPDepenseByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		return parametrePeriodeFiscaleDAO.getPPDepenseByPeriodeFiscale(periodeFiscale);
	}

	@Override
	@Transactional(readOnly = true)
	public ParametrePeriodeFiscalePP getPPHorsCantonByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		return parametrePeriodeFiscaleDAO.getPPHorsCantonByPeriodeFiscale(periodeFiscale);
	}

	@Override
	@Transactional(readOnly = true)
	public ParametrePeriodeFiscalePP getPPHorsSuisseByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		return parametrePeriodeFiscaleDAO.getPPHorsSuisseByPeriodeFiscale(periodeFiscale);
	}

	@Override
	@Transactional(readOnly = true)
	public ParametrePeriodeFiscalePP getPPDiplomateSuisseByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		return parametrePeriodeFiscaleDAO.getPPDiplomateSuisseByPeriodeFiscale(periodeFiscale);
	}

	@Override
	@Transactional(readOnly = true)
	public ParametrePeriodeFiscalePP getPPVaudByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		return parametrePeriodeFiscaleDAO.getPPVaudByPeriodeFiscale(periodeFiscale);
	}

	@Override
	@Transactional(readOnly = true)
	public ParametrePeriodeFiscalePM getPMVaudByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		return parametrePeriodeFiscaleDAO.getPMVaudByPeriodeFiscale(periodeFiscale);
	}

	@Override
	@Transactional(readOnly = true)
	public ParametrePeriodeFiscalePM getPMHorsCantonByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		return parametrePeriodeFiscaleDAO.getPMHorsCantonByPeriodeFiscale(periodeFiscale);
	}

	@Override
	@Transactional(readOnly = true)
	public ParametrePeriodeFiscalePM getPMHorsSuisseByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		return parametrePeriodeFiscaleDAO.getPMHorsSuisseByPeriodeFiscale(periodeFiscale);
	}

	@Override
	@Transactional(readOnly = true)
	public ParametrePeriodeFiscaleSNC getSNCByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		return parametrePeriodeFiscaleDAO.getSNCByPeriodeFiscale(periodeFiscale);
	}

	@Override
	@Transactional(readOnly = true)
	public ParametrePeriodeFiscaleEmolument getEmolumentSommationDIPPByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		return parametrePeriodeFiscaleDAO.getEmolumentSommationDIPPByPeriodeFiscale(periodeFiscale);
	}

	@Override
	public ParametrePeriodeFiscalePM getPMUtilitePubliqueByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		return parametrePeriodeFiscaleDAO.getPMUtilitePubliqueByPeriodeFiscale(periodeFiscale);
	}

	public void setPeriodeFiscaleDAO(PeriodeFiscaleDAO periodeFiscaleDAO) {
		this.periodeFiscaleDAO = periodeFiscaleDAO;
	}

	public void setPeriodeFiscaleService(PeriodeFiscaleService periodeFiscaleService) {
		this.periodeFiscaleService = periodeFiscaleService;
	}

	public void setModeleDocumentDAO(ModeleDocumentDAO modeleDocumentDAO) {
		this.modeleDocumentDAO = modeleDocumentDAO;
	}

	public void setModeleFeuilleDocumentDAO(ModeleFeuilleDocumentDAO modeleFeuilleDocumentDAO) {
		this.modeleFeuilleDocumentDAO = modeleFeuilleDocumentDAO;
	}

	public void setParametrePeriodeFiscaleDAO(ParametrePeriodeFiscaleDAO parametrePeriodeFiscaleDAO) {
		this.parametrePeriodeFiscaleDAO = parametrePeriodeFiscaleDAO;
	}

	@Override
	@Transactional(readOnly = true)
	public ParametrePeriodeFiscalePPEditView createParametrePeriodeFiscalePPEditView(Long idPeriode) {
		final ParametrePeriodeFiscalePPEditView ppfv = new ParametrePeriodeFiscalePPEditView();
		final PeriodeFiscale pf = retrievePeriodeFromDAO(idPeriode);

		ppfv.setIdPeriodeFiscale(pf.getId());
		ppfv.setAnneePeriodeFiscale(pf.getAnnee());
		ppfv.setCodeControleSurSommationDI(pf.isShowCodeControleSommationDeclarationPP());

		final ParametrePeriodeFiscaleEmolument emolument = pf.getParametrePeriodeFiscaleEmolument(TypeDocumentEmolument.SOMMATION_DI_PP);
		ppfv.setMontantEmolumentSommationDI(emolument != null ? emolument.getMontant() : null);
		ppfv.setEmolumentSommationDI(emolument != null && emolument.getMontant() != null);

		ppfv.setFinEnvoiMasseDIDepense(pf.getParametrePeriodeFiscalePPDepense().getDateFinEnvoiMasseDI());
		ppfv.setFinEnvoiMasseDIDiplomate(pf.getParametrePeriodeFiscalePPDiplomateSuisse().getDateFinEnvoiMasseDI());
		ppfv.setFinEnvoiMasseDIHorsCanton(pf.getParametrePeriodeFiscalePPHorsCanton().getDateFinEnvoiMasseDI());
		ppfv.setFinEnvoiMasseDIHorsSuisse(pf.getParametrePeriodeFiscalePPHorsSuisse().getDateFinEnvoiMasseDI());
		ppfv.setFinEnvoiMasseDIVaud(pf.getParametrePeriodeFiscalePPVaudoisOrdinaire().getDateFinEnvoiMasseDI());

		ppfv.setSommationEffectiveDepense(pf.getParametrePeriodeFiscalePPDepense().getTermeGeneralSommationEffectif());
		ppfv.setSommationEffectiveDiplomate(pf.getParametrePeriodeFiscalePPDiplomateSuisse().getTermeGeneralSommationEffectif());
		ppfv.setSommationEffectiveHorsCanton(pf.getParametrePeriodeFiscalePPHorsCanton().getTermeGeneralSommationEffectif());
		ppfv.setSommationEffectiveHorsSuisse(pf.getParametrePeriodeFiscalePPHorsSuisse().getTermeGeneralSommationEffectif());
		ppfv.setSommationEffectiveVaud(pf.getParametrePeriodeFiscalePPVaudoisOrdinaire().getTermeGeneralSommationEffectif());

		ppfv.setSommationReglementaireDepense(pf.getParametrePeriodeFiscalePPDepense().getTermeGeneralSommationReglementaire());
		ppfv.setSommationReglementaireDiplomate(pf.getParametrePeriodeFiscalePPDiplomateSuisse().getTermeGeneralSommationReglementaire());
		ppfv.setSommationReglementaireHorsCanton(pf.getParametrePeriodeFiscalePPHorsCanton().getTermeGeneralSommationReglementaire());
		ppfv.setSommationReglementaireHorsSuisse(pf.getParametrePeriodeFiscalePPHorsSuisse().getTermeGeneralSommationReglementaire());
		ppfv.setSommationReglementaireVaud(pf.getParametrePeriodeFiscalePPVaudoisOrdinaire().getTermeGeneralSommationReglementaire());

		return ppfv;
	}

	@Override
	@Transactional(readOnly = true)
	public ParametrePeriodeFiscalePMEditView createParametrePeriodeFiscalePMEditView(Long idPeriode) {
		final ParametrePeriodeFiscalePMEditView ppfv = new ParametrePeriodeFiscalePMEditView();
		final PeriodeFiscale pf = retrievePeriodeFromDAO(idPeriode);

		ppfv.setIdPeriodeFiscale(idPeriode);
		ppfv.setAnneePeriodeFiscale(pf.getAnnee());
		ppfv.setCodeControleSurSommationDI(pf.isShowCodeControleSommationDeclarationPM());

		final ParametrePeriodeFiscalePM hc = pf.getParametrePeriodeFiscalePM(TypeContribuable.HORS_CANTON);
		if (hc != null) {
			ppfv.setDelaiImprimeMoisHorsCanton(hc.getDelaiImprimeMois());
			ppfv.setDelaiImprimeRepousseFinDeMoisHorsCanton(hc.isDelaiImprimeRepousseFinDeMois());
			ppfv.setToleranceJoursHorsCanton(hc.getDelaiToleranceJoursEffective());
			ppfv.setToleranceRepousseeFinDeMoisHorsCanton(hc.isDelaiTolereRepousseFinDeMois());
			ppfv.setRefDelaiHorsCanton(hc.getReferenceDelaiInitial());
		}

		final ParametrePeriodeFiscalePM hs = pf.getParametrePeriodeFiscalePM(TypeContribuable.HORS_SUISSE);
		if (hs != null) {
			ppfv.setDelaiImprimeMoisHorsSuisse(hs.getDelaiImprimeMois());
			ppfv.setDelaiImprimeRepousseFinDeMoisHorsSuisse(hs.isDelaiImprimeRepousseFinDeMois());
			ppfv.setToleranceJoursHorsSuisse(hs.getDelaiToleranceJoursEffective());
			ppfv.setToleranceRepousseeFinDeMoisHorsSuisse(hs.isDelaiTolereRepousseFinDeMois());
			ppfv.setRefDelaiHorsSuisse(hs.getReferenceDelaiInitial());
		}

		final ParametrePeriodeFiscalePM vd = pf.getParametrePeriodeFiscalePM(TypeContribuable.VAUDOIS_ORDINAIRE);
		if (vd != null) {
			ppfv.setDelaiImprimeMoisVaud(vd.getDelaiImprimeMois());
			ppfv.setDelaiImprimeRepousseFinDeMoisVaud(vd.isDelaiImprimeRepousseFinDeMois());
			ppfv.setToleranceJoursVaud(vd.getDelaiToleranceJoursEffective());
			ppfv.setToleranceRepousseeFinDeMoisVaud(vd.isDelaiTolereRepousseFinDeMois());
			ppfv.setRefDelaiVaud(vd.getReferenceDelaiInitial());
		}

		final ParametrePeriodeFiscalePM up = pf.getParametrePeriodeFiscalePM(TypeContribuable.UTILITE_PUBLIQUE);
		if (up != null) {
			ppfv.setDelaiImprimeMoisUtilitePublique(up.getDelaiImprimeMois());
			ppfv.setDelaiImprimeRepousseFinDeMoisUtilitePublique(up.isDelaiImprimeRepousseFinDeMois());
			ppfv.setToleranceJoursUtilitePublique(up.getDelaiToleranceJoursEffective());
			ppfv.setToleranceRepousseeFinDeMoisUtilitePublique(up.isDelaiTolereRepousseFinDeMois());
			ppfv.setRefDelaiUtilitePublique(up.getReferenceDelaiInitial());
		}

		return ppfv;
	}

	@Override
	@Transactional(readOnly = true)
	public ParametrePeriodeFiscaleSNCEditView createParametrePeriodeFiscaleSNCEditView(Long idPeriode) {
		final ParametrePeriodeFiscaleSNCEditView view = new ParametrePeriodeFiscaleSNCEditView();
		final PeriodeFiscale pf = retrievePeriodeFromDAO(idPeriode);

		view.setIdPeriodeFiscale(pf.getId());
		view.setAnneePeriodeFiscale(pf.getAnnee());
		view.setCodeControleSurRappelQSNC(pf.isShowCodeControleRappelQuestionnaireSNC());

		final ParametrePeriodeFiscaleSNC data = pf.getParametrePeriodeFiscaleSNC();
		if (data != null) {
			view.setRappelReglementaire(data.getTermeGeneralRappelImprime());
			view.setRappelEffectif(data.getTermeGeneralRappelEffectif());
		}
		return view;
	}

	@Override
	public ModeleDocumentView createModeleDocumentViewAdd(Long idPeriode) {
		ModeleDocumentView mdv = new ModeleDocumentView();
		PeriodeFiscale pf = retrievePeriodeFromDAO(idPeriode);
		mdv.setIdPeriode(idPeriode);
		mdv.setAnneePeriodeFiscale(pf.getAnnee());
		return mdv;
	}

	@Override
	@Transactional(readOnly = true)
	public ModeleFeuilleDocumentView createModeleFeuilleDocumentViewAdd(Long periodeId, Long modeleId) {
		ModeleFeuilleDocumentView mfdv = new ModeleFeuilleDocumentView();
		PeriodeFiscale pf = retrievePeriodeFromDAO(periodeId);
		ModeleDocument md = retrieveModeleFromDAO(modeleId);
		mfdv.setIdPeriode(pf.getId());
		mfdv.setPeriodeAnnee(pf.getAnnee());
		mfdv.setIdModele(modeleId);
		mfdv.setModeleDocumentTypeDocument(md.getTypeDocument());
		return mfdv;
	}

	@Override
	@Transactional(readOnly = true)
	public ModeleFeuilleDocumentView createModeleFeuilleDocumentViewEdit(Long periodeId, Long modeleId, Long feuilleId) {
		final ModeleFeuilleDocumentView mfdv = createModeleFeuilleDocumentViewAdd(periodeId, modeleId);
		final ModeleFeuilleDocument mfd = retrieveFeuilleFromDAO(feuilleId);
		mfdv.setIdFeuille(feuilleId);

		final ModeleFeuille modeleFeuille = ModeleFeuille.fromNoCADEV(mfd.getNoCADEV());
		mfdv.setModeleFeuille(modeleFeuille);
		return mfdv;
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void saveParametrePeriodeFiscaleView(ParametrePeriodeFiscalePPEditView ppfv) {

		// ATTENTION : L'ordre des elements dans les tableaux est primordiale pour le bon fonctionnement de l'algo

		final PeriodeFiscale pf = periodeFiscaleDAO.get(ppfv.getIdPeriodeFiscale());
		pf.setShowCodeControleSommationDeclarationPP(ppfv.isCodeControleSurSommationDI());

		final ParametrePeriodeFiscaleEmolument emolument = pf.getParametrePeriodeFiscaleEmolument(TypeDocumentEmolument.SOMMATION_DI_PP);
		if (ppfv.isEmolumentSommationDI()) {
			if (emolument == null) {
				final ParametrePeriodeFiscaleEmolument param = new ParametrePeriodeFiscaleEmolument();
				param.setTypeDocument(TypeDocumentEmolument.SOMMATION_DI_PP);
				param.setMontant(ppfv.getMontantEmolumentSommationDI());
				pf.addParametrePeriodeFiscale(param);
			}
			else {
				emolument.setMontant(ppfv.getMontantEmolumentSommationDI());
			}
		}
		else if (emolument != null) {
			emolument.setMontant(null);
		}

		final ParametrePeriodeFiscalePP[] ppfs = new ParametrePeriodeFiscalePP[] {
				pf.getParametrePeriodeFiscalePPVaudoisOrdinaire(),
				pf.getParametrePeriodeFiscalePPHorsCanton(),
				pf.getParametrePeriodeFiscalePPHorsSuisse(),
				pf.getParametrePeriodeFiscalePPDepense(),
				pf.getParametrePeriodeFiscalePPDiplomateSuisse()
		};

		final RegDate[][] termes = new RegDate [][] {
			{ppfv.getSommationEffectiveVaud(), ppfv.getSommationReglementaireVaud(), ppfv.getFinEnvoiMasseDIVaud() },
			{ppfv.getSommationEffectiveHorsCanton(), ppfv.getSommationReglementaireHorsCanton(), ppfv.getFinEnvoiMasseDIHorsCanton() },
			{ppfv.getSommationEffectiveHorsSuisse(), ppfv.getSommationReglementaireHorsSuisse(), ppfv.getFinEnvoiMasseDIHorsSuisse() },
			{ppfv.getSommationEffectiveDepense(), ppfv.getSommationReglementaireDepense(), ppfv.getFinEnvoiMasseDIDepense() },
			{ppfv.getSommationEffectiveDiplomate(), ppfv.getSommationReglementaireDiplomate(), ppfv.getFinEnvoiMasseDIDiplomate() }
		};

		assert (ppfs.length == termes.length);

		// On verifie que tous les parametres de periode fiscale ne soient pas null
		for (ParametrePeriodeFiscalePP ppf : ppfs) {
			if (ppf == null) {
				String msgErr = "Impossible de retrouver tous les paramètres PP pour la période fiscale : " + ppfv.getAnneePeriodeFiscale();
				LOGGER.error(msgErr);
				throw new ObjectNotFoundException(msgErr);
			}
		}

		// On met à jour les parametres de periode fiscale
		for (int i = 0; i < ppfs.length; i++) {
			ppfs[i].setTermeGeneralSommationEffectif(termes[i][0]);
			ppfs[i].setTermeGeneralSommationReglementaire(termes[i][1]);
			ppfs[i].setDateFinEnvoiMasseDI(termes[i][2]);
		}

		// Sauvegarde
		for (ParametrePeriodeFiscale ppf : ppfs) {
			parametrePeriodeFiscaleDAO.save(ppf);
		}
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void saveParametrePeriodeFiscaleView(ParametrePeriodeFiscalePMEditView view) {

		// ATTENTION à l'ordre des évéments dans les différents tableaux, qui doivent se correspondre...
		final PeriodeFiscale pf = periodeFiscaleDAO.get(view.getIdPeriodeFiscale());
		pf.setShowCodeControleSommationDeclarationPM(view.isCodeControleSurSommationDI());

		final ParametrePeriodeFiscalePM[] ppfs = new ParametrePeriodeFiscalePM[] {
				pf.getParametrePeriodeFiscalePM(TypeContribuable.VAUDOIS_ORDINAIRE),
				pf.getParametrePeriodeFiscalePM(TypeContribuable.HORS_CANTON),
				pf.getParametrePeriodeFiscalePM(TypeContribuable.HORS_SUISSE),
				pf.getParametrePeriodeFiscalePM(TypeContribuable.UTILITE_PUBLIQUE),
		};

		final int[][] delais = new int[][] {
				{view.getDelaiImprimeMoisVaud(), view.getToleranceJoursVaud()},
				{view.getDelaiImprimeMoisHorsCanton(), view.getToleranceJoursHorsCanton()},
				{view.getDelaiImprimeMoisHorsSuisse(), view.getToleranceJoursHorsSuisse()},
				{view.getDelaiImprimeMoisUtilitePublique(), view.getToleranceJoursUtilitePublique()}
		};

		final ParametrePeriodeFiscalePM.ReferencePourDelai[] refDelais = new ParametrePeriodeFiscalePM.ReferencePourDelai[] {
				view.getRefDelaiVaud(),
				view.getRefDelaiHorsCanton(),
				view.getRefDelaiHorsSuisse(),
				view.getRefDelaiUtilitePublique()
		};

		final boolean[][] reportsFinDeMois = new boolean[][] {
				{view.getDelaiImprimeRepousseFinDeMoisVaud(), view.getToleranceRepousseeFinDeMoisVaud()},
				{view.getDelaiImprimeRepousseFinDeMoisHorsCanton(), view.getToleranceRepousseeFinDeMoisHorsCanton()},
				{view.getDelaiImprimeRepousseFinDeMoisHorsSuisse(), view.getToleranceRepousseeFinDeMoisHorsSuisse()},
				{view.getDelaiImprimeRepousseFinDeMoisUtilitePublique(), view.getToleranceRepousseeFinDeMoisUtilitePublique()}
		};

		// On verifie que tous les parametres de periode fiscale ne soient pas null
		for (ParametrePeriodeFiscalePM ppf : ppfs) {
			if (ppf == null) {
				final String msgErr = "Impossible de retrouver tous les paramètres PM pour la période fiscale : " + pf.getAnnee();
				LOGGER.error(msgErr);
				throw new ObjectNotFoundException(msgErr);
			}
		}

		// mise à jour des paramètres
		for (int i = 0 ; i < ppfs.length ; ++ i) {
			ppfs[i].setDelaiImprimeMois(delais[i][0]);
			ppfs[i].setDelaiImprimeRepousseFinDeMois(reportsFinDeMois[i][0]);
			ppfs[i].setDelaiToleranceJoursEffective(delais[i][1]);
			ppfs[i].setDelaiTolereRepousseFinDeMois(reportsFinDeMois[i][1]);
			ppfs[i].setReferenceDelaiInitial(refDelais[i]);
		}
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void saveParametrePeriodeFiscaleView(ParametrePeriodeFiscaleSNCEditView view) {
		final PeriodeFiscale pf = periodeFiscaleDAO.get(view.getIdPeriodeFiscale());
		final ParametrePeriodeFiscaleSNC data = pf.getParametrePeriodeFiscaleSNC();
		if (data == null) {
			throw new ObjectNotFoundException("Impossible de retrouver les paramètres SNC pour la période fiscale " + pf.getAnnee());
		}
		data.setTermeGeneralRappelEffectif(view.getRappelEffectif());
		data.setTermeGeneralRappelImprime(view.getRappelReglementaire());
		pf.setShowCodeControleRappelQuestionnaireSNC(view.isCodeControleSurRappelQSNC());

	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void saveModeleDocumentView(ModeleDocumentView mdv) {
		ModeleDocument md = new ModeleDocument();
		PeriodeFiscale pf = periodeFiscaleDAO.get(mdv.getIdPeriode());
		md.setTypeDocument(mdv.getTypeDocument());
		pf.addModeleDocument(md);
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void addFeuille(Long idModele, ModeleFeuille modeleFeuille) {
		final ModeleDocument md = modeleDocumentDAO.get(idModele);
		final ModeleFeuilleDocument mfd = new ModeleFeuilleDocument();
		mfd.setNoCADEV(modeleFeuille.getNoCADEV());
		mfd.setNoFormulaireACI(modeleFeuille.getNoFormulaireACI());
		mfd.setIntituleFeuille(modeleFeuille.getDescription());
		mfd.setPrincipal(modeleFeuille.isPrincipal());
		md.addModeleFeuilleDocument(mfd);
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void updateFeuille(Long idFeuille, ModeleFeuille modeleFeuille) {
		final ModeleFeuilleDocument mfd = modeleFeuilleDocumentDAO.get(idFeuille);
		mfd.setNoCADEV(modeleFeuille.getNoCADEV());
		mfd.setNoFormulaireACI(modeleFeuille.getNoFormulaireACI());
		mfd.setIntituleFeuille(modeleFeuille.getDescription());
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void deleteModeleDocument(Long idModeleDocument) {
		modeleDocumentDAO.remove(idModeleDocument);
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void deleteModeleFeuilleDocument(Long idModeleFeuilleDocument) {
		modeleFeuilleDocumentDAO.remove(idModeleFeuilleDocument);
	}

	private PeriodeFiscale retrievePeriodeFromDAO(Long idPeriode) {
		PeriodeFiscale pf = periodeFiscaleDAO.get(idPeriode);
		if (pf == null) {
			throw new ObjectNotFoundException("Impossible de retrouver la période fiscale id : " + idPeriode);
		}
		return pf;
	}

	private ModeleDocument retrieveModeleFromDAO(Long idModeleDocument) {
		ModeleDocument md = modeleDocumentDAO.get(idModeleDocument);
		if (md == null) {
			throw new ObjectNotFoundException("Impossible de retrouver le modèle de document id : " + idModeleDocument);
		}
		return md;
	}

	private ModeleFeuilleDocument retrieveFeuilleFromDAO(Long idFeuille) {
		ModeleFeuilleDocument mfd = modeleFeuilleDocumentDAO.get(idFeuille);
		if (mfd == null) {
			throw new ObjectNotFoundException("Impossible de retrouver la feuille du modèle de document id : " + idFeuille);
		}
		return mfd;
	}

}
