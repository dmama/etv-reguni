package ch.vd.uniregctb.parametrage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;
import ch.vd.uniregctb.declaration.ParametrePeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;

public class PeriodeFiscaleServiceImpl implements PeriodeFiscaleService {
	
	/**
	 * Un logger pour {@link PeriodeFiscaleServiceImpl}
	 */
	private static final Logger LOGGER = Logger.getLogger(PeriodeFiscaleServiceImpl.class);
	
	PeriodeFiscaleDAO dao;
	ParametreAppService parametreAppService;

	public PeriodeFiscale initNouvellePeriodeFiscale() {
		List<PeriodeFiscale> list = dao.getAllDesc();
		if (list == null || list.size() == 0) {
			// Aucune période fiscale, création de la premiere.
			Integer anneePremierePeriode = parametreAppService.getPremierePeriodeFiscale();
			PeriodeFiscale premierePeriodeFiscale =  new PeriodeFiscale();
			premierePeriodeFiscale.setAnnee(anneePremierePeriode);
			premierePeriodeFiscale.setDefaultPeriodeFiscaleParametres();
			dao.save(premierePeriodeFiscale);
			return premierePeriodeFiscale;
		} else {
			// Il existe déjà une période fiscale
			PeriodeFiscale periodeFiscale = list.get(0);
			PeriodeFiscale nllePeriodeFiscale =  new PeriodeFiscale();
			initPeriodeFiscale(nllePeriodeFiscale, periodeFiscale);
			dao.save(nllePeriodeFiscale);
			return nllePeriodeFiscale;
		}
	}

	/**
	 * Copie la {@link PeriodeFiscale} precedente en :
	 * <ul>
	 * 	<li>incremantant l'année
	 * 	<li>duplicant les modeles de document
	 * 	<li>duplicant les feuilles de modèle de document
	 * 	<li>duplicant les paramètres (en incrementant les dates de 1 année)
	 * </ul>
	 *  
	 * @param nllePeriodeFiscale
	 * @param periodeFiscalePrecedente
	 */
	private void initPeriodeFiscale(PeriodeFiscale nllePeriodeFiscale, PeriodeFiscale periodeFiscalePrecedente) {
		nllePeriodeFiscale.setAnnee(periodeFiscalePrecedente.getAnnee() + 1);
		
		// Copie des parametres
		if (periodeFiscalePrecedente.getParametrePeriodeFiscale() != null) {
			Set<ParametrePeriodeFiscale> setParametrePeriodeFiscale = new HashSet<ParametrePeriodeFiscale> (periodeFiscalePrecedente.getParametrePeriodeFiscale().size());
			for (ParametrePeriodeFiscale ppf : periodeFiscalePrecedente.getParametrePeriodeFiscale()) {
				ParametrePeriodeFiscale newPpf = new ParametrePeriodeFiscale();
				newPpf.setPeriodefiscale(nllePeriodeFiscale);
				newPpf.setTypeContribuable(ppf.getTypeContribuable());
				newPpf.setTermeGeneralSommationEffectif(ppf.getTermeGeneralSommationEffectif().addYears(1));
				newPpf.setTermeGeneralSommationReglementaire(ppf.getTermeGeneralSommationReglementaire().addYears(1));
				newPpf.setDateFinEnvoiMasseDI(ppf.getDateFinEnvoiMasseDI().addYears(1));
				setParametrePeriodeFiscale.add(newPpf);
			}
			nllePeriodeFiscale.setParametrePeriodeFiscale(setParametrePeriodeFiscale);
		} else {
			LOGGER.warn("la période fiscale " + periodeFiscalePrecedente.getAnnee() + " n'a pas de paramètres.");			
		}
		
		// Copie des modèles de document
		if (periodeFiscalePrecedente.getModelesDocument() != null) {
			Set<ModeleDocument> setModeleDocument = new HashSet<ModeleDocument> (periodeFiscalePrecedente.getModelesDocument().size());
			for (ModeleDocument md : periodeFiscalePrecedente.getModelesDocument()) {
				ModeleDocument newMd = new ModeleDocument();
				newMd.setPeriodeFiscale(nllePeriodeFiscale);
				newMd.setTypeDocument(md.getTypeDocument());
				
				// Copie des modeles de feuille de document
				Set<ModeleFeuilleDocument> setModeleFeuilleDocument = new HashSet<ModeleFeuilleDocument> (md.getModelesFeuilleDocument().size());
				for(ModeleFeuilleDocument mfd : md.getModelesFeuilleDocument()) {
					ModeleFeuilleDocument newMfd = new ModeleFeuilleDocument();
					newMfd.setModeleDocument(newMd);
					newMfd.setIntituleFeuille(mfd.getIntituleFeuille());
					newMfd.setNumeroFormulaire(mfd.getNumeroFormulaire());
					setModeleFeuilleDocument.add(newMfd);
				}
				newMd.setModelesFeuilleDocument(setModeleFeuilleDocument);
				setModeleDocument.add(newMd);
			}
			nllePeriodeFiscale.setModelesDocument(setModeleDocument);
		}else {
			LOGGER.warn("la période fiscale " + periodeFiscalePrecedente.getAnnee() + " n'a pas de modèles de document.");
		}
	}

	public void setDao(PeriodeFiscaleDAO dao) {
		this.dao = dao;
	}

	public void setParametreAppService(ParametreAppService parametreAppService) {
		this.parametreAppService = parametreAppService;
	}

}
