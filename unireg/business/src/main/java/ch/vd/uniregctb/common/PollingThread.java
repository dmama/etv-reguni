package ch.vd.uniregctb.common;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Classe de base d'un thread de polling d'une queue, par exemple pour produire un traitement
 */
public abstract class PollingThread<T> extends Thread {

	private static final Logger LOGGER = Logger.getLogger(PollingThread.class);

	private final long pollingTimeout;
	private final TimeUnit pollingTimeoutUnit;

	private volatile boolean shouldStop = false;

	/**
	 * Constructor
	 * @param name nom du thread
	 * @param pollingTimeout le timeout de chaque itération de polling
	 * @param pollingTimeoutUnit l'unité du timeout de chaque itération de polling
	 */
	protected PollingThread(String name, long pollingTimeout, @NotNull TimeUnit pollingTimeoutUnit) {
		super(name);
		this.pollingTimeout = pollingTimeout;
		this.pollingTimeoutUnit = pollingTimeoutUnit;
	}

	/**
	 * Constructor
	 * @param name nom du thread
	 */
	protected PollingThread(String name) {
		this(name, 1, TimeUnit.SECONDS);
	}

	/**
	 * Appelé pour faire arrêter le thread
	 */
	public final void stopIt() {
		this.shouldStop = true;
		onStopItCall();
	}

	/**
	 * Eventuellement surchargeable pour faire quelque chose de spécifique au moment de la
	 * demande d'arrêt du thread
	 */
	protected void onStopItCall() {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info(String.format("Demande d'arrêt du thread %s", getName()));
		}
	}

	/**
	 * Disponible pour les classes dérivées pour savoir si une demande d'arrêt du thread a été faite
	 * @return <code>true</code> si une demande d'arrêt du thread a été faite
	 */
	protected final boolean shouldStop() {
		return this.shouldStop;
	}

	@Override
	public final void run() {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Démarrage du thread de polling " + getName());
		}
		try {
			while (!shouldStop) {
				final T elt = poll(pollingTimeout, pollingTimeoutUnit);
				if (elt != null && !shouldStop) {
					try {
						Throwable thrown = null;
						try {
							onElementTaken(elt);
							processElement(elt);
						}
						catch (InterruptedException e) {
							thrown = e;
							throw e;
						}
						catch (Exception e) {
							thrown = e;
						}
						catch (Throwable t) {
							thrown = t;
							throw t;
						}
						finally {
							onElementProcessed(elt, thrown);
						}
					}
					catch (Exception e) {
						LOGGER.error("Exception raised during processing of element " + elt);
					}
				}
			}
		}
		catch (InterruptedException e) {
			LOGGER.warn("Thread " + getName() + " interrupted", e);
		}
		catch (Throwable e) {
			LOGGER.warn("Thread " + getName() + " will stop due to exception", e);
			throw e;
		}
		finally {
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("Arrêt du thread de polling " + getName());
			}
			onStop();
		}
	}

	/**
	 * A implémenter par les sous-classes, pour fournir un élément issu du <i>poll</i>
	 * @param timeout durée maximale d'attente
	 * @param timeUnit unité de la durée maximale d'attente
	 * @return un élément <i>pollé</i>, ou <code>null</code> si aucun ne s'est présenté avant le <i>timeout</i>
	 * @throws InterruptedException si le thread a été interrompu pendant l'attente
	 */
	protected abstract T poll(long timeout, @NotNull TimeUnit timeUnit) throws InterruptedException;

	/**
	 * Surchargeable par les classes dérivées pour être notifiées quand un élément a été récupéré
	 * @param element élément récupéré
	 */
	@SuppressWarnings("UnusedParameters")
	protected void onElementTaken(@NotNull T element) {
	}

	/**
	 * A implémenter par les sous-classes pour founir le traitement à appliquer à l'élément récupéré
	 * @param element élément à traiter
	 * @throws InterruptedException si le thread a été interrompu pendant le traitement
	 */
	protected abstract void processElement(@NotNull T element) throws InterruptedException;

	/**
	 * Surchargeable par les classes dérivées pour être notifiées quand le traitement d'un élément
	 * est terminé
	 * @param element élément traité
	 * @param t exception lancée pendant le traitement, si applicable
	 */
	@SuppressWarnings("UnusedParameters")
	protected void onElementProcessed(@NotNull T element, @Nullable Throwable t) {
	}

	/**
	 * Surchargeable par les classes dérivées pour être notifiées de l'arrêt du thread
	 */
	protected void onStop() {
	}
}
