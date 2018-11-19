package ch.vd.unireg.parametrage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.orm.hibernate4.HibernateSystemException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.ModeleFeuilleDocument;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.type.TypeContribuable;

public class PeriodeFiscaleServiceImpl implements PeriodeFiscaleService, InitializingBean {

	/**
	 * Un logger pour {@link PeriodeFiscaleServiceImpl}
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(PeriodeFiscaleServiceImpl.class);

	private PeriodeFiscaleDAO dao;
	private ParametreAppService parametreAppService;
	private PlatformTransactionManager transactionManager;

	@Override
	public PeriodeFiscale initNouvellePeriodeFiscale() {
		List<PeriodeFiscale> list = dao.getAllDesc();
		if (list == null || list.isEmpty()) {
			// Aucune période fiscale, création de la premiere.
			Integer anneePremierePeriode = parametreAppService.getPremierePeriodeFiscalePersonnesPhysiques();
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

	@Override
	public void copyParametres(PeriodeFiscale source, PeriodeFiscale destination) {
		// rien à faire
		if (source == destination) {
			return;
		}

		// nettoyage de la destination
		if (destination.getParametrePeriodeFiscale() != null) {
			destination.getParametrePeriodeFiscale().clear();
		}

		// si rien dans la source, alors c'est tout bon
		if (source.getParametrePeriodeFiscale() == null || source.getParametrePeriodeFiscale().isEmpty()) {
			return;
		}

		// recopie adaptative
		final Map<Class<? extends ParametrePeriodeFiscale>, ParametrePeriodeFiscalInitializer<?>> initializers = new HashMap<>();
		addInitializerMapping(initializers, ParametrePeriodeFiscalePP.class, new ParametrePeriodeFiscalePPInitializer());
		addInitializerMapping(initializers, ParametrePeriodeFiscalePM.class, new ParametrePeriodeFiscalePMInitializer());
		addInitializerMapping(initializers, ParametrePeriodeFiscaleSNC.class, new ParametrePeriodeFiscaleSNCInitializer());
		addInitializerMapping(initializers, ParametrePeriodeFiscaleEmolument.class, new ParametrePeriodeFiscaleEmolumentInitializer());

		for (ParametrePeriodeFiscale ppf : source.getParametrePeriodeFiscale()) {
			final ParametrePeriodeFiscale newParam = initializeNewParameter(initializers, ppf, destination);
			if (newParam != null) {
				destination.addParametrePeriodeFiscale(newParam);
			}
		}
	}

	@Override
	public PeriodeFiscale get(int annee) {
		return dao.getPeriodeFiscaleByYear(annee);
	}

	@Override
	public void copyModelesDocuments(PeriodeFiscale source, PeriodeFiscale destination) {
		// rien à faire
		if (source == destination) {
			return;
		}

		// nettoyage de la destination
		if (destination.getModelesDocument() != null) {
			destination.getModelesDocument().clear();
		}

		// si rien dans la source, alors c'est tout bon
		if (source.getModelesDocument() == null || source.getModelesDocument().isEmpty()) {
			return;
		}

		// copie des modèles et des feuilles associées
		for (ModeleDocument md : source.getModelesDocument()) {
			final ModeleDocument newMd = new ModeleDocument();
			newMd.setPeriodeFiscale(destination);
			newMd.setTypeDocument(md.getTypeDocument());

			// Copie des modeles de feuille de document
			final Set<ModeleFeuilleDocument> setModeleFeuilleDocument = new HashSet<>(md.getModelesFeuilleDocument().size());
			for(ModeleFeuilleDocument mfd : md.getModelesFeuilleDocument()) {
				final ModeleFeuilleDocument newMfd = new ModeleFeuilleDocument(mfd, newMd);
				setModeleFeuilleDocument.add(newMfd);
			}
			newMd.setModelesFeuilleDocument(setModeleFeuilleDocument);
			destination.addModeleDocument(newMd);
		}
	}

	private interface ParametrePeriodeFiscalInitializer<T extends ParametrePeriodeFiscale> {
		T createFrom(T previous, PeriodeFiscale nvellePeriodeFiscale);
	}

	private static class ParametrePeriodeFiscalePPInitializer implements ParametrePeriodeFiscalInitializer<ParametrePeriodeFiscalePP> {
		@Override
		public ParametrePeriodeFiscalePP createFrom(ParametrePeriodeFiscalePP previous, PeriodeFiscale nvellePeriodeFiscale) {
			final int nbYearsDifference = nvellePeriodeFiscale.getAnnee() - previous.getPeriodefiscale().getAnnee();
			return new ParametrePeriodeFiscalePP(previous.getTypeContribuable(),
			                                     previous.getDateFinEnvoiMasseDI().addYears(nbYearsDifference),
			                                     previous.getTermeGeneralSommationReglementaire().addYears(nbYearsDifference),
			                                     previous.getTermeGeneralSommationEffectif().addYears(nbYearsDifference),
			                                     nvellePeriodeFiscale);
		}
	}

	private static class ParametrePeriodeFiscalePMInitializer implements ParametrePeriodeFiscalInitializer<ParametrePeriodeFiscalePM> {
		@Override
		public ParametrePeriodeFiscalePM createFrom(ParametrePeriodeFiscalePM previous, PeriodeFiscale nvellePeriodeFiscale) {
			return new ParametrePeriodeFiscalePM(previous.getTypeContribuable(),
			                                     previous.getDelaiImprimeMois(),
			                                     previous.isDelaiImprimeRepousseFinDeMois(),
			                                     previous.getDelaiToleranceJoursEffective(),
			                                     previous.isDelaiTolereRepousseFinDeMois(),
			                                     previous.getReferenceDelaiInitial(),
			                                     nvellePeriodeFiscale);
		}
	}

	private static class ParametrePeriodeFiscaleSNCInitializer implements ParametrePeriodeFiscalInitializer<ParametrePeriodeFiscaleSNC> {
		@Override
		public ParametrePeriodeFiscaleSNC createFrom(ParametrePeriodeFiscaleSNC previous, PeriodeFiscale nvellePeriodeFiscale) {
			final int nbYearsDifference = nvellePeriodeFiscale.getAnnee() - previous.getPeriodefiscale().getAnnee();
			return new ParametrePeriodeFiscaleSNC(nvellePeriodeFiscale,
			                                      previous.getTermeGeneralRappelImprime().addYears(nbYearsDifference),
			                                      previous.getTermeGeneralRappelEffectif().addYears(nbYearsDifference));
		}
	}

	private static class ParametrePeriodeFiscaleEmolumentInitializer implements ParametrePeriodeFiscalInitializer<ParametrePeriodeFiscaleEmolument> {
		@Override
		public ParametrePeriodeFiscaleEmolument createFrom(ParametrePeriodeFiscaleEmolument previous, PeriodeFiscale nvellePeriodeFiscale) {
			return new ParametrePeriodeFiscaleEmolument(previous.getTypeDocument(),
			                                            previous.getMontant(),
			                                            nvellePeriodeFiscale);
		}
	}

	/**
	 * Méthode externalisée pour assurer une cohérence entre les types des paramètres
	 * @param mappings la map des implémentations des initialiseurs
	 * @param parameterClass classe de paramètre à associer à un initialiseur
	 * @param initializer initialiseur associé à la classe de paramètre
	 * @param <T> type du paramètre
	 */
	private static <T extends ParametrePeriodeFiscale> void addInitializerMapping(Map<Class<? extends ParametrePeriodeFiscale>, ParametrePeriodeFiscalInitializer<?>> mappings,
	                                                                              Class<T> parameterClass,
	                                                                              ParametrePeriodeFiscalInitializer<T> initializer) {
		mappings.put(parameterClass, initializer);
	}

	/**
	 * Méthode externalisée pour assurer une cohérence entre les types des paramètres
	 * @param mappings la map des implémentations des initialiseurs
	 * @param clazz classe de paramètre pour laquelle on veut trouver l'initialiseur
	 * @param <T> type du paramètre
	 * @return l'initialiseur responsable pour la classe de paramètre passée
	 */
	@Nullable
	private static <T extends ParametrePeriodeFiscale> ParametrePeriodeFiscalInitializer<T> getInitializerForParameterClass(Map<Class<? extends ParametrePeriodeFiscale>, ParametrePeriodeFiscalInitializer<?>> mappings,
	                                                                                                                        Class<T> clazz) {
		//noinspection unchecked
		return  (ParametrePeriodeFiscalInitializer<T>) mappings.get(clazz);
	}

	@Nullable
	private static <T extends ParametrePeriodeFiscale> T initializeNewParameter(Map<Class<? extends ParametrePeriodeFiscale>, ParametrePeriodeFiscalInitializer<?>> initializers,
	                                                                            T parametrePrecedent,
	                                                                            PeriodeFiscale periodeFiscaleCible) {
		//noinspection unchecked
		final Class<T> clazz = (Class<T>) parametrePrecedent.getClass();
		final ParametrePeriodeFiscalInitializer<T> initializer = getInitializerForParameterClass(initializers, clazz);
		if (initializer == null) {
			LOGGER.warn(String.format("Impossible de recopier le paramètre de classe %s de la période fiscale %d sur la période fiscale %d.",
			                          clazz.getName(), parametrePrecedent.getPeriodefiscale().getAnnee(), periodeFiscaleCible.getAnnee()));
			return null;
		}
		return initializer.createFrom(parametrePrecedent, periodeFiscaleCible);
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

		// Copie des paramètres
		if (periodeFiscalePrecedente.getParametrePeriodeFiscale() != null) {
			copyParametres(periodeFiscalePrecedente, nllePeriodeFiscale);
		}
		else {
			LOGGER.warn("la période fiscale " + periodeFiscalePrecedente.getAnnee() + " n'a pas de paramètres.");
		}

		// Copie des modèles de document
		if (periodeFiscalePrecedente.getModelesDocument() != null) {
			copyModelesDocuments(periodeFiscalePrecedente, nllePeriodeFiscale);
		}
		else {
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
						final ParametrePeriodeFiscalePP horsSuissePP = p.getParametrePeriodeFiscalePP(TypeContribuable.HORS_SUISSE);
						final ParametrePeriodeFiscalePP diplomateSuisse = p.getParametrePeriodeFiscalePP(TypeContribuable.DIPLOMATE_SUISSE);
						if (diplomateSuisse == null && horsSuissePP != null) {
							LOGGER.info("Ajout des paramètres spécifiques aux diplomates suisses sur la période fiscale " + p.getAnnee());
							final ParametrePeriodeFiscalePP newDiplomateSuisse = horsSuissePP.duplicate();
							newDiplomateSuisse.setTypeContribuable(TypeContribuable.DIPLOMATE_SUISSE);
							p.addParametrePeriodeFiscale(newDiplomateSuisse);
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
