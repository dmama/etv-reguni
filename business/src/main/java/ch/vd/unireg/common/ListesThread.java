package ch.vd.unireg.common;

import java.sql.SQLException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.mutable.MutableInt;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.Interruptible;
import ch.vd.unireg.cache.ServiceCivilCacheWarmer;
import ch.vd.unireg.hibernate.HibernateCallback;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersDAO.Parts;

public abstract class ListesThread<T extends ListesResults<T>> extends Thread {

	public static final Logger LOGGER = LoggerFactory.getLogger(ListesThread.class);

    private final BlockingQueue<List<Long>> queue;

    private final T results;

    private final Interruptible interruptible;

    private boolean queueFilled = false;

    private final TransactionTemplate transactionTemplate;

    private final TiersDAO tiersDAO;

	private final ServiceCivilCacheWarmer serviceCivilCacheWarmer;

    private final AtomicInteger compteur;

    private final HibernateTemplate hibernateTemplate;

	/**
	 * Ensemble des parties à récupérer des tiers : les tiers eux-mêmes et les rapports entre tiers
	 */
    protected static final Set<Parts> PARTS_FISCALES = Collections.unmodifiableSet(EnumSet.of(Parts.RAPPORTS_ENTRE_TIERS));

	public ListesThread(BlockingQueue<List<Long>> queue, Interruptible interruptible, AtomicInteger compteur,
	                    ServiceCivilCacheWarmer serviceCivilCacheWarmer,
	                    PlatformTransactionManager transactionManager,
	                    TiersDAO tiersDAO, HibernateTemplate hibernateTemplate, T results) {
        this.queue = queue;
        this.interruptible = interruptible;
        this.compteur = compteur;
	    this.serviceCivilCacheWarmer = serviceCivilCacheWarmer;
	    this.hibernateTemplate = hibernateTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.tiersDAO = tiersDAO;
        this.results = results;
    }

    @Override
    public void run() {
        try {
            List<Long> ids = getIds();
            while (ids != null) {

                // traitement d'un lot d'IDs
                traiteLot(ids);

                // prochain lot
                ids = getIds();
            }
        }
	    catch (Exception e) {
		    LOGGER.error(String.format("Thread %s terminé sur une exception", getClass().getSimpleName()), e);
	    }
    }

    /**
     * Si on renvoie null, c'est la fin de l'itération
     */
    private List<Long> getIds() throws InterruptedException {
        while (true) {
            final List<Long> ids = queue.poll(1, TimeUnit.SECONDS);
            if (interruptible.isInterrupted()) {
                return null;
            } else if (ids != null) {
                return ids;
            } else if (queueFilled) {
                // la queue a été complètement remplie, et il
                // n'y a plus rien dedans... -> c'est fini
                return null;
            }
        }
    }

    public void onQueueFilled() {
        queueFilled = true;
    }

    /**
     * Le vrai boulot est ici
     */
    private void traiteLot(final List<Long> ids) {

	    final Set<Parts> partsToFetch = getFiscalPartsToFetch();
	    final MutableInt niveauExtraction = new MutableInt(0);
	    try {

			// comme ça on est certain de recréer une session à chaque fois
			hibernateTemplate.executeWithNewSession(new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(Session session) throws HibernateException, SQLException {

					// read-only transaction
					session.setFlushMode(FlushMode.MANUAL);
					transactionTemplate.setReadOnly(true);
					transactionTemplate.execute(status -> {
						final List<Tiers> tiers = tiersDAO.getBatch(ids, partsToFetch);

						// prefetch des données civiles
						prefetchDonneesCiviles(ids, null);

						for (Tiers t : tiers) {
							traiteTiers(t);
							niveauExtraction.increment();
							if (interruptible.isInterrupted()) {
								break;
							}
						}
						return null;
					});

					return null;
				}
			});

	    }
	    catch (Exception e) {
		    LOGGER.error("Exception levée lors du traitement d'un lot", e);
		    rattrappageLot(ids.subList(niveauExtraction.intValue(), ids.size()));
	    }
    }

	protected void prefetchDonneesCiviles(List<Long> idsTiers, RegDate date) {
		final Set<AttributeIndividu> attributes = EnumSet.noneOf(AttributeIndividu.class);
		fillAttributesIndividu(attributes);
		final AttributeIndividu[] attributesArray;
		if (!attributes.isEmpty()) {
			attributesArray = attributes.toArray(new AttributeIndividu[0]);
		}
		else {
			attributesArray = null;
		}
		serviceCivilCacheWarmer.warmIndividusPourTiers(idsTiers, date, true, attributesArray);
	}

	protected void fillAttributesIndividu(Set<AttributeIndividu> attributes) {
		// par défaut, rien de spécial à ajouter
	}

	protected Set<Parts> getFiscalPartsToFetch() {
		return PARTS_FISCALES;
	}

	/**
	 * Appelé lors du traitement par lot, si tout explose (la liste des ids non encore traités est fournie),
	 * on fera donc un traitement individualisé pour chacun des ids
	 * @param ids
	 */
	private void rattrappageLot(List<Long> ids) {
		for (final Long id : ids) {

			// comme ça on est certain de recréer une session à chaque fois
			hibernateTemplate.executeWithNewSession(new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(Session session) throws HibernateException, SQLException {

					// read-only transaction
					transactionTemplate.setReadOnly(true);
					transactionTemplate.execute(status -> {
						try {
							final Tiers tiers = tiersDAO.get(id);
							traiteTiers(tiers);
						}
						catch (Exception e) {
							LOGGER.error("Exception levée sur le tiers " + id, e);
							results.addErrorException(id, e);
						}
						return null;
					});

					return null;
				}
			});

			if (interruptible.isInterrupted()) {
				break;
			}
		}
	}

	private void traiteTiers(Tiers tiers) {
		if (tiers instanceof Contribuable) {
			final Contribuable ctb = (Contribuable) tiers;
			try {
				traiteContribuable(ctb);
			}
			catch (Exception e) {
				results.addErrorException(ctb, e);
			}
		} else {
			traiteNonContribuable(tiers);
		}

		compteur.incrementAndGet();
	}

	protected void traiteContribuable(Contribuable ctb) throws Exception {
		results.addContribuable(ctb);
	}

	/**
     * A sous-classer si des non-contribuables doivent être traités. Aucune exception ne doit sortir !
     *
     * @param t
     */
    protected void traiteNonContribuable(Tiers t) {
    }

    public final T getResults() {
        return results;
    }
}