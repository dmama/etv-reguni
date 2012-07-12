package ch.vd.uniregctb.parametrage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.orm.hibernate3.HibernateSystemException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;
import ch.vd.uniregctb.declaration.ParametrePeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.type.TypeContribuable;

public class PeriodeFiscaleServiceImpl implements PeriodeFiscaleService, InitializingBean {

	/**
	 * Un logger pour {@link PeriodeFiscaleServiceImpl}
	 */
	private static final Logger LOGGER = Logger.getLogger(PeriodeFiscaleServiceImpl.class);

	private PeriodeFiscaleDAO dao;
	private ParametreAppService parametreAppService;
	private PlatformTransactionManager transactionManager;

	@Override
	public PeriodeFiscale initNouvellePeriodeFiscale() {
		List<PeriodeFiscale> list = dao.getAllDesc();
		if (list == null || list.isEmpty()) {
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

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		AuthenticationHelper.pushPrincipal(AuthenticationHelper.SYSTEM_USER);
		try {
			final TransactionTemplate template = new TransactionTemplate(transactionManager);
			template.execute(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					// [UNIREG-1976] on ajoute à la volée les paramètres pour les diplomates suisses
					final List<PeriodeFiscale> periodes = dao.getAll();
					for (PeriodeFiscale p : periodes) {
						final Set<ParametrePeriodeFiscale> params = p.getParametrePeriodeFiscale();
						ParametrePeriodeFiscale horssuisse = null;
						ParametrePeriodeFiscale diplomatesuisse = null;
						for (ParametrePeriodeFiscale pp : params) {
							if (pp.getTypeContribuable() == TypeContribuable.HORS_SUISSE) {
								horssuisse = pp;
							}
							else if (pp.getTypeContribuable() == TypeContribuable.DIPLOMATE_SUISSE) {
								diplomatesuisse = pp;
							}
						}
						if (diplomatesuisse == null && horssuisse != null) {
							LOGGER.info("Ajout des paramètres spécifiques aux diplomates suisses sur la période fiscale " + p.getAnnee());
							diplomatesuisse = horssuisse.duplicate();
							diplomatesuisse.setTypeContribuable(TypeContribuable.DIPLOMATE_SUISSE);
							params.add(diplomatesuisse);
						}
					}
					return null;
				}
			});
		}
		catch (HibernateSystemException e) {
			if (e.getMessage().startsWith("a different object with the same identifier value was already associated with the session")) {
				final String message = "\n\n"+
						"**************************************************\n" +
						"* !!! Problème de séquence hibernate détecté !!! *\n" +
						"**************************************************\n";
				LOGGER.error(message);
			}
			throw e;
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}
}
