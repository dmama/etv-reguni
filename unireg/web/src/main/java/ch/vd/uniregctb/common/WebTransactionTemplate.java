package ch.vd.uniregctb.common;

import org.apache.log4j.Logger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.BindingResult;

import ch.vd.registre.base.tx.TxCallbackException;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.registre.base.validation.ValidationMessage;
import ch.vd.uniregctb.metier.MetierServiceException;

/**
 * Template de transaction spécialisé pour les contrôleurs Spring MVC v3, qui intercepte les exceptions communes (validation, action, ...) et renseigne les messages d'erreurs dans les résultats de
 * binding. Ces messages seront ensuite automatiquement affichées dans l'entête de la page (voir template.jsp).
 *
 * @param <T> le type de retour de la méthode <i>execute</i>.
 */
public class WebTransactionTemplate<T> {

	private final Logger logger = Logger.getLogger(WebTransactionTemplate.class);

	private BindingResult results;
	private TransactionTemplate template;

	public WebTransactionTemplate(PlatformTransactionManager transactionManager, BindingResult result) {
		this.results = result;
		this.template = new TransactionTemplate(transactionManager);
	}

	public <T> T execute(TransactionCallback<T> action) throws TransactionException, WebTransactionException {
		try {
			return template.execute(action);
		}
		catch (ActionException e) {
			logger.debug("Action exception catched -> redisplaying form : " + e.getMessage());
			for (String s : e.getErrors()) {
				results.reject("global.error.msg", s);
			}
			throw new WebTransactionException(e);
		}
		catch (ValidationException e) {
			logger.debug("Validation exception catched -> redisplaying form : " + e.getMessage());
			for (ValidationMessage s : e.getErrors()) {
				results.reject("global.error.msg", s.getMessage());
			}
			throw new WebTransactionException(e);
		}
		catch (ObjectNotFoundException e) {
			logger.debug("ObjectNotFound exception catched -> redisplaying form : " + e.getMessage());
			results.reject("global.error.msg", e.getMessage());
			throw new WebTransactionException(e);
		}
		catch (TxCallbackException e) {
			final Throwable cause = e.getCause();
			if (cause instanceof MetierServiceException) {
				results.reject("global.error.msg", cause.getMessage());
				throw new WebTransactionException(cause);
			}
			else {
				throw e;
			}
		}
	}

}
