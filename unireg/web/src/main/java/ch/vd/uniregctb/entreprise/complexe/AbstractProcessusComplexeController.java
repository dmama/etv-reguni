package ch.vd.uniregctb.entreprise.complexe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.ui.Model;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.tx.TxCallback;
import ch.vd.registre.base.tx.TxCallbackException;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.TooManyResultsIndexerException;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.metier.MetierServicePM;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersIndexedDataView;
import ch.vd.uniregctb.tiers.TiersMapHelper;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;
import ch.vd.uniregctb.transaction.TransactionTemplate;
import ch.vd.uniregctb.utils.RegDateEditor;
import ch.vd.uniregctb.utils.WebContextUtils;

public abstract class AbstractProcessusComplexeController implements MessageSourceAware {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractProcessusComplexeController.class);

	protected TiersService tiersService;
	protected TiersMapHelper tiersMapHelper;
	protected ControllerUtils controllerUtils;
	protected MessageSource messageSource;
	protected MetierServicePM metierService;

	private SecurityProviderInterface securityProvider;
	private Validator validator;
	private PlatformTransactionManager transactionManager;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setValidator(Validator validator) {
		this.validator = validator;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setMetierService(MetierServicePM metierService) {
		this.metierService = metierService;
	}

	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@InitBinder(value = "command")
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(validator);
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, true, false, RegDateHelper.StringFormat.DISPLAY));
	}

	protected abstract class MetierServiceExceptionAwareCallback<T> extends TxCallback<T> {
		@Override
		public abstract T execute(TransactionStatus status) throws MetierServiceException;
	}

	protected abstract class MetierServiceExceptionAwareWithoutResultCallback extends MetierServiceExceptionAwareCallback<Object> {
		@Override
		public final Object execute(TransactionStatus status) throws MetierServiceException {
			doExecute(status);
			return null;
		}

		protected abstract void doExecute(TransactionStatus status) throws MetierServiceException;
	}

	/**
	 * Lance le traitement du callback dans une transaction en lecture/écriture et transforme une éventuelle {@link MetierServiceException} en {@link ActionException}
	 * @param callback action à lancer
	 * @param <T> type du résultat renvoyé par l'action
	 * @return le résultat renvoyé par l'action
	 */
	protected final <T> T doInTransaction(MetierServiceExceptionAwareCallback<T> callback) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		try {
			return template.execute(callback);
		}
		catch (TxCallbackException e) {
			try {
				throw e.getCause();
			}
			catch (RuntimeException | Error re) {
				throw re;
			}
			catch (MetierServiceException me) {
				throw new ActionException(me.getMessage(), me);
			}
			catch (Throwable t) {
				// il ne devrait pas y en avoir...
				throw new RuntimeException(t);
			}
		}
	}

	/**
	 * Lance le traitement du callback dans une transaction en lecture seule
	 * @param callback traitement à exécuter dans le contexte de la transaction
	 */
	protected final void doInReadOnlyTransaction(TransactionCallbackWithoutResult callback) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		template.setReadOnly(true);
		template.execute(callback);
	}

	/**
	 * Lance le traitement du callback dans une transaction en lecture seule
	 * @param callback traitement à exécuter dans le contexte de la transaction
	 */
	protected final <T> T doInReadOnlyTransaction(TransactionCallback<T> callback) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		template.setReadOnly(true);
		return template.execute(callback);
	}

	/**
	 * Récupère le tiers dont l'identifiant est donné (une transaction est censée être déjà en cours)
	 * @param clazz classe attendue pour le tiers
	 * @param id identifiant du tiers
	 * @param <T> type de tiers attendu
	 * @return le tiers s'il existe est supporte la classe donnée
	 * @throws TiersNotFoundException si le tiers n'existe pas ou ne supporte pas la classe demandée
	 */
	protected final <T extends Tiers> T getTiers(Class<T> clazz, long id) throws TiersNotFoundException {
		final Tiers tiers = tiersService.getTiers(id);
		if (tiers == null || !clazz.isAssignableFrom(tiers.getClass())) {
			throw new TiersNotFoundException(id);
		}
		//noinspection unchecked
		return (T) tiers;
	}

	/**
	 * Vérifie que l'un au moins des rôles passés en paramètre est possédé par l'utilisateur courant
	 * @param messageErreur message à lancer en cas d'absence des droits requis
	 * @param roles les rôles en question
	 * @throws AccessDeniedException si aucun rôle n'est possédé par l'utilisateur courant
	 */
	protected final void checkAnyGranted(String messageErreur, Role... roles) throws AccessDeniedException {
		if (!SecurityHelper.isAnyGranted(securityProvider, roles)) {
			throw new AccessDeniedException(messageErreur);
		}
	}

	@NotNull
	protected List<TiersIndexedDataView> _searchTiers(TiersCriteriaView criteriaView) throws IndexerException {
		final List<TiersIndexedData> results = tiersService.search(criteriaView.asCore());
		Assert.notNull(results);

		final List<TiersIndexedDataView> list = new ArrayList<>(results.size());
		for (TiersIndexedData d : results) {
			list.add(new TiersIndexedDataView(d));
		}
		return list;
	}

	/**
	 * Recherche des tiers correspondant aux critères donnés
	 * @param criteriaView les critères, justement
	 * @param model le modéle (pour l'introduction d'un éventuel message d'erreur)
	 * @param champModelMessageErreur le nom du champ à utiliser dans le modèle pour les éventuels messages d'erreur
	 * @return la liste des tiers trouvés (vide si erreur)
	 */
	@NotNull
	protected final List<TiersIndexedDataView> searchTiers(TiersCriteriaView criteriaView, Model model, String champModelMessageErreur) {
		try {
			return _searchTiers(criteriaView);
		}
		catch (TooManyResultsIndexerException e) {
			LOGGER.error("Exception dans l'indexer: " + e.getMessage(), e);
			final String msg;
			if (e.getNbResults() > 0) {
				msg = messageSource.getMessage("error.preciser.recherche.trouves", new Object[] {String.valueOf(e.getNbResults())}, WebContextUtils.getDefaultLocale());
			}
			else {
				msg = messageSource.getMessage("error.preciser.recherche", null, WebContextUtils.getDefaultLocale());
			}
			model.addAttribute(champModelMessageErreur, msg);
		}
		catch (IndexerException e) {
			LOGGER.error("Exception dans l'indexer: " + e.getMessage(), e);
			model.addAttribute(champModelMessageErreur, messageSource.getMessage("error.recherche", null, WebContextUtils.getDefaultLocale()));
		}

		return Collections.emptyList();
	}
}
