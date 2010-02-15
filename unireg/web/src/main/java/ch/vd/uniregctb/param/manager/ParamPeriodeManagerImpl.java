package ch.vd.uniregctb.param.manager;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleDocumentDAO;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocumentDAO;
import ch.vd.uniregctb.declaration.ParametrePeriodeFiscale;
import ch.vd.uniregctb.declaration.ParametrePeriodeFiscaleDAO;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.param.view.ModeleDocumentView;
import ch.vd.uniregctb.param.view.ModeleFeuilleDocumentView;
import ch.vd.uniregctb.param.view.ParametrePeriodeFiscaleView;
import ch.vd.uniregctb.parametrage.PeriodeFiscaleService;

public class ParamPeriodeManagerImpl implements ParamPeriodeManager {

	/**
	 * Un logger pour {@link ParamPeriodeManagerImpl}
	 */
	private static final Logger LOGGER = Logger.getLogger(ParamPeriodeManagerImpl.class);

	private PeriodeFiscaleDAO periodeFiscaleDAO;
	private ModeleDocumentDAO modeleDocumentDAO;
	private ModeleFeuilleDocumentDAO modeleFeuilleDocumentDAO;
	private ParametrePeriodeFiscaleDAO parametrePeriodeFiscaleDAO;
	private PeriodeFiscaleService periodeFiscaleService;

	public List<PeriodeFiscale> getAllPeriodeFiscale() {
		List<PeriodeFiscale> list = periodeFiscaleDAO.getAllDesc();
		// force le chargment des ParametrePeriodeFiscale
		for (PeriodeFiscale periodeFiscale : list) {
			periodeFiscale.getParametrePeriodeFiscale();
		}
		return list;
	}

	public List<ModeleDocument> getModeleDocuments (PeriodeFiscale periodeFiscale) {
		 return modeleDocumentDAO.getByPeriodeFiscale(periodeFiscale);
	}

	public List<ModeleFeuilleDocument> getModeleFeuilleDocuments(ModeleDocument modeleDocument) {
		return modeleFeuilleDocumentDAO.getByModeleDocument(modeleDocument);
	}

	public List<ParametrePeriodeFiscale> getParametrePeriodeFiscales(PeriodeFiscale periodeFiscale) {
		List<ParametrePeriodeFiscale> list = parametrePeriodeFiscaleDAO.getByPeriodeFiscale(periodeFiscale);
		Collections.sort(
			list,
			new Comparator<ParametrePeriodeFiscale>() {
				public int compare(ParametrePeriodeFiscale o1, ParametrePeriodeFiscale o2) {
					return o1.getTypeContribuable().compareTo(o2.getTypeContribuable());
				}
			}
		);
		return list;
	}

	@Transactional(rollbackFor = Throwable.class)
	public PeriodeFiscale initNouvellePeriodeFiscale() {
		return periodeFiscaleService.initNouvellePeriodeFiscale();

	}

	public ParametrePeriodeFiscale getDepenseByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		return parametrePeriodeFiscaleDAO.getDepenseByPeriodeFiscale(periodeFiscale);
	}

	public ParametrePeriodeFiscale getHorsCantonByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		return parametrePeriodeFiscaleDAO.getHorsCantonByPeriodeFiscale(periodeFiscale);
	}

	public ParametrePeriodeFiscale getHorsSuisseByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		return parametrePeriodeFiscaleDAO.getHorsSuisseByPeriodeFiscale(periodeFiscale);
	}

	public ParametrePeriodeFiscale getVaudByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		return parametrePeriodeFiscaleDAO.getVaudByPeriodeFiscale(periodeFiscale);
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

	public ParametrePeriodeFiscaleView createParametrePeriodeFiscaleViewEdit(Long idPeriode) {
		ParametrePeriodeFiscaleView ppfv = new ParametrePeriodeFiscaleView();
		PeriodeFiscale pf = retrievePeriodeFromDAO(idPeriode);

		ppfv.setIdPeriodeFiscale(pf.getId());
		ppfv.setAnneePeriodeFiscale(pf.getAnnee());

		ppfv.setIdDepense(pf.getParametrePeriodeFiscaleDepense().getId());
		ppfv.setIdHorsCanton(pf.getParametrePeriodeFiscaleHorsCanton().getId());
		ppfv.setIdHorsSuisse(pf.getParametrePeriodeFiscaleHorsSuisse().getId());
		ppfv.setIdVaud(pf.getParametrePeriodeFiscaleVaud().getId());

		ppfv.setFinEnvoiMasseDIDepense(pf.getParametrePeriodeFiscaleDepense().getDateFinEnvoiMasseDI());
		ppfv.setFinEnvoiMasseDIHorsCanton(pf.getParametrePeriodeFiscaleHorsCanton().getDateFinEnvoiMasseDI());
		ppfv.setFinEnvoiMasseDIHorsSuisse(pf.getParametrePeriodeFiscaleHorsSuisse().getDateFinEnvoiMasseDI());
		ppfv.setFinEnvoiMasseDIVaud(pf.getParametrePeriodeFiscaleVaud().getDateFinEnvoiMasseDI());

		ppfv.setSommationEffectiveDepense(pf.getParametrePeriodeFiscaleDepense().getTermeGeneralSommationEffectif());
		ppfv.setSommationEffectiveHorsCanton(pf.getParametrePeriodeFiscaleHorsCanton().getTermeGeneralSommationEffectif());
		ppfv.setSommationEffectiveHorsSuisse(pf.getParametrePeriodeFiscaleHorsSuisse().getTermeGeneralSommationEffectif());
		ppfv.setSommationEffectiveVaud(pf.getParametrePeriodeFiscaleVaud().getTermeGeneralSommationEffectif());

		ppfv.setSommationReglementaireDepense(pf.getParametrePeriodeFiscaleDepense().getTermeGeneralSommationReglementaire());
		ppfv.setSommationReglementaireHorsCanton(pf.getParametrePeriodeFiscaleHorsCanton().getTermeGeneralSommationReglementaire());
		ppfv.setSommationReglementaireHorsSuisse(pf.getParametrePeriodeFiscaleHorsSuisse().getTermeGeneralSommationReglementaire());
		ppfv.setSommationReglementaireVaud(pf.getParametrePeriodeFiscaleVaud().getTermeGeneralSommationReglementaire());

		return ppfv;
	}

	public ModeleDocumentView createModeleDocumentView(Long idPeriode, Long idModeleDocument) {
		ModeleDocument md = retrieveModeleFromDAO(idModeleDocument);
		PeriodeFiscale pf = retrievePeriodeFromDAO(idPeriode);
		ModeleDocumentView mdv = new ModeleDocumentView();
		mdv.setIdPeriode(pf.getId());
		mdv.setIdModele(md.getId());
		mdv.setTypeDocument(md.getTypeDocument());
		return mdv;
	}

	public ModeleDocumentView createModeleDocumentViewAdd(Long idPeriode) {
		ModeleDocumentView mdv = new ModeleDocumentView();
		mdv.setIdPeriode(idPeriode);
		return mdv;
	}

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

	public ModeleFeuilleDocumentView createModeleFeuilleDocumentViewEdit(Long periodeId, Long modeleId, Long feuilleId) {
		ModeleFeuilleDocumentView mfdv = createModeleFeuilleDocumentViewAdd(periodeId, modeleId);
		ModeleFeuilleDocument mfd = retrieveFeuilleFromDAO(feuilleId);
		mfdv.setIdFeuille(feuilleId);
		mfdv.setNumeroFormulaire(mfd.getNumeroFormulaire());
		mfdv.setIntituleFeuille(mfd.getIntituleFeuille());
		return mfdv;
	}

	@Transactional(rollbackFor = Throwable.class)
	public void saveParametrePeriodeFiscaleView(ParametrePeriodeFiscaleView ppfv) {

		// ATTENTION : L'ordre des elements dans les tableaux est primordiale pour le bon fonctionnement de l'algo

		ParametrePeriodeFiscale[] ppfs = new ParametrePeriodeFiscale[] {
				parametrePeriodeFiscaleDAO.get(ppfv.getIdVaud()),
				parametrePeriodeFiscaleDAO.get(ppfv.getIdHorsCanton()),
				parametrePeriodeFiscaleDAO.get(ppfv.getIdHorsSuisse()),
				parametrePeriodeFiscaleDAO.get(ppfv.getIdDepense())
		};

		RegDate[][] termes = new RegDate [][] {
			{ppfv.getSommationEffectiveVaud(), ppfv.getSommationReglementaireVaud(), ppfv.getFinEnvoiMasseDIVaud() },
			{ppfv.getSommationEffectiveHorsCanton(), ppfv.getSommationReglementaireHorsCanton(), ppfv.getFinEnvoiMasseDIHorsCanton() },
			{ppfv.getSommationEffectiveHorsSuisse(), ppfv.getSommationReglementaireHorsSuisse(), ppfv.getFinEnvoiMasseDIHorsSuisse() },
			{ppfv.getSommationEffectiveDepense(), ppfv.getSommationReglementaireDepense(), ppfv.getFinEnvoiMasseDIDepense() }
		};

		assert (ppfs.length == termes.length);

		// On verifie que tous les parametres de periode fiscale ne soient pas null
		for (ParametrePeriodeFiscale ppf : ppfs) {
			if (ppf == null) {
				String msgErr = "Impossible de retrouver tous les paramétres pour la période fiscale : " + ppfv.getAnneePeriodeFiscale();
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

	@Transactional(rollbackFor = Throwable.class)
	public void saveModeleDocumentView(ModeleDocumentView mdv) {
		ModeleDocument md = new ModeleDocument();
		PeriodeFiscale pf = periodeFiscaleDAO.get(mdv.getIdPeriode());
		md.setTypeDocument(mdv.getTypeDocument());
		pf.addModeleDocument(md);
	}

	@Transactional(rollbackFor = Throwable.class)
	public void saveModeleFeuilleDocumentViewAdd(ModeleFeuilleDocumentView mfdv) {
		ModeleFeuilleDocument mfd = new ModeleFeuilleDocument();
		mfd.setNumeroFormulaire(mfdv.getNumeroFormulaire());
		mfd.setIntituleFeuille(mfdv.getIntituleFeuille());
		ModeleDocument md = modeleDocumentDAO.get(mfdv.getIdModele());
		md.addModeleFeuilleDocument(mfd);
	}

	@Transactional(rollbackFor = Throwable.class)
	public void saveModeleFeuilleDocumentViewEdit(ModeleFeuilleDocumentView mfdv) {
		ModeleFeuilleDocument mfd = modeleFeuilleDocumentDAO.get(mfdv.getIdFeuille());
		mfd.setNumeroFormulaire(mfdv.getNumeroFormulaire());
		mfd.setIntituleFeuille(mfdv.getIntituleFeuille());
	}

	@Transactional(rollbackFor = Throwable.class)
	public void deleteModeleDocument(Long idModeleDocument) {
		modeleDocumentDAO.remove(idModeleDocument);
	}

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
