package ch.vd.unireg.entreprise.complexe;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.ActionException;
import ch.vd.unireg.common.ControllerUtils;
import ch.vd.unireg.common.TiersNotFoundException;
import ch.vd.unireg.metier.MetierServiceException;
import ch.vd.unireg.metier.MetierServicePM;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersMapHelper;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.tiers.validator.TiersCriteriaValidator;
import ch.vd.unireg.transaction.TransactionHelper;
import ch.vd.unireg.utils.RegDateEditor;

public abstract class AbstractProcessusComplexeController implements MessageSourceAware {

	public static final String ACTION_COMMAND = "actionCommand";

	protected TiersService tiersService;
	protected TiersMapHelper tiersMapHelper;
	protected ControllerUtils controllerUtils;
	protected MessageSource messageSource;
	protected MetierServicePM metierService;

	private SecurityProviderInterface securityProvider;
	private Validator validator;
	private TransactionHelper transactionHelper;

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

	public void setTransactionHelper(TransactionHelper transactionHelper) {
		this.transactionHelper = transactionHelper;
	}

	public void setMetierService(MetierServicePM metierService) {
		this.metierService = metierService;
	}

	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@InitBinder(value = SearchTiersComponent.COMMAND)
	protected void initSearchCommandBinder(WebDataBinder binder) {
		binder.setValidator(new TiersCriteriaValidator());
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, true, false, RegDateHelper.StringFormat.DISPLAY));
	}

	@InitBinder(value = ACTION_COMMAND)
	protected void initActionCommandBinder(WebDataBinder binder) {
		binder.setValidator(validator);
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false, RegDateHelper.StringFormat.DISPLAY));
	}

	/**
	 * Lance le traitement du callback dans une transaction en lecture/écriture et transforme une éventuelle {@link MetierServiceException} en {@link ActionException}
	 * @param callback action à lancer
	 * @param <T> type du résultat renvoyé par l'action
	 * @return le résultat renvoyé par l'action
	 */
	protected final <T> T doInTransaction(TransactionHelper.ExceptionThrowingCallback<T, MetierServiceException> callback) {
		try {
			return transactionHelper.doInTransactionWithException(false, callback);
		}
		catch (MetierServiceException e) {
			throw new ActionException(e.getMessage(), e);
		}
	}

	/**
	 * Lance le traitement du callback dans une transaction en lecture/écriture et transforme une éventuelle {@link MetierServiceException} en {@link ActionException}
	 * @param callback action à lancer
	 */
	protected final void doInTransaction(TransactionHelper.ExceptionThrowingCallbackWithoutResult<MetierServiceException> callback) {
		try {
			transactionHelper.doInTransactionWithException(false, callback);
		}
		catch (MetierServiceException e) {
			throw new ActionException(e.getMessage(), e);
		}
	}

	/**
	 * Lance le traitement du callback dans une transaction en lecture seule
	 * @param callback traitement à exécuter dans le contexte de la transaction
	 */
	protected final void doInReadOnlyTransaction(TransactionCallbackWithoutResult callback) {
		transactionHelper.doInTransaction(true, callback);
	}

	/**
	 * Lance le traitement du callback dans une transaction en lecture seule
	 * @param callback traitement à exécuter dans le contexte de la transaction
	 */
	protected final <T> T doInReadOnlyTransaction(TransactionCallback<T> callback) {
		return transactionHelper.doInTransaction(true, callback);
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
	 * Construit un composant de recherche
	 * @param searchCriteriaBeanName le nom du bean en session pour les critères de recherche
	 * @param searchViewPath le chemin d'accès à la jsp de visualisation du formulaire de recherche (et des résultats)
	 * @param criteriaFiller callback de remplissage des critères additionnels de recherche (type de tiers, constraintes supplémentaires...)
	 * @param modelFiller [optionnel] callback de remplissage du modèle à l'affichage de la page de recherche (avec ou sans résultats)
	 * @param searchAdapter [optionnel] callback de transformation des résultats de recherche avant placement dans le modèle
	 * @return un composant de recherche
	 */
	protected SearchTiersComponent buildSearchComponent(String searchCriteriaBeanName, String searchViewPath,
	                                                    SearchTiersComponent.TiersCriteriaFiller criteriaFiller,
	                                                    SearchTiersComponent.ModelFiller modelFiller,
	                                                    SearchTiersComponent.TiersSearchAdapter<?> searchAdapter) {
		return new SearchTiersComponent(tiersService, messageSource, tiersMapHelper,
		                                searchCriteriaBeanName, searchViewPath,
		                                criteriaFiller, modelFiller, searchAdapter);
	}

	/**
	 * Construit un composant de recherche
	 * @param searchCriteriaBeanName le nom du bean en session pour les critères de recherche
	 * @param searchViewPath le chemin d'accès à la jsp de visualisation du formulaire de recherche (et des résultats)
	 * @param criteriaFiller callback de remplissage des critères additionnels de recherche (type de tiers, constraintes supplémentaires...)
	 * @param modelFiller [optionnel] callback de remplissage du modèle à l'affichage de la page de recherche (avec ou sans résultats)
	 * @return un composant de recherche
	 */
	protected SearchTiersComponent buildSearchComponent(String searchCriteriaBeanName, String searchViewPath,
	                                                    SearchTiersComponent.TiersCriteriaFiller criteriaFiller,
	                                                    SearchTiersComponent.ModelFiller modelFiller) {
		return new SearchTiersComponent(tiersService, messageSource, tiersMapHelper,
		                                searchCriteriaBeanName, searchViewPath,
		                                criteriaFiller, modelFiller);
	}

	/**
	 * Construit un composant de recherche
	 * @param searchCriteriaBeanName le nom du bean en session pour les critères de recherche
	 * @param searchViewPath le chemin d'accès à la jsp de visualisation du formulaire de recherche (et des résultats)
	 * @param criteriaFiller callback de remplissage des critères additionnels de recherche (type de tiers, constraintes supplémentaires...)
	 * @return un composant de recherche
	 */
	protected SearchTiersComponent buildSearchComponent(String searchCriteriaBeanName, String searchViewPath,
	                                                    SearchTiersComponent.TiersCriteriaFiller criteriaFiller) {
		return new SearchTiersComponent(tiersService, messageSource, tiersMapHelper,
		                                searchCriteriaBeanName, searchViewPath, criteriaFiller);
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
}
