package ch.vd.uniregctb.common;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.mutable.MutableInt;
import org.apache.log4j.Logger;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersDAO.Parts;

public abstract class ListesThread<T extends ListesResults<T>> extends Thread {

	public static final Logger LOGGER = Logger.getLogger(ListesThread.class);

    private final BlockingQueue<List<Long>> queue;

    private final T results;

    private final StatusManager status;

    private boolean queueFilled = false;

    private final TransactionTemplate transactionTemplate;

    private final TiersDAO tiersDAO;

	private final ServiceCivilCacheWarmer serviceCivilCacheWarmer;

    private final AtomicInteger compteur;

    private final HibernateTemplate hibernateTemplate;

    protected static final Set<Parts> PARTS_FISCALES;

	static {
        // ensemble des parties à récupérer des tiers : les tiers eux-mêmes et les rapports entre tiers
	    final Set<Parts> parts = new HashSet<Parts>(1);
        parts.add(Parts.RAPPORTS_ENTRE_TIERS);

	    PARTS_FISCALES = Collections.unmodifiableSet(parts);
    }

    public ListesThread(BlockingQueue<List<Long>> queue, StatusManager status, AtomicInteger compteur,
                        ServiceCivilCacheWarmer serviceCivilCacheWarmer,
                        PlatformTransactionManager transactionManager,
                        TiersDAO tiersDAO, HibernateTemplate hibernateTemplate, T results) {
        this.queue = queue;
        this.status = status;
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
            if (status.interrupted()) {
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
					transactionTemplate.execute(new TransactionCallback<Object>() {
						@Override
						public Object doInTransaction(TransactionStatus transactionStatus) {
							final List<Tiers> tiers = tiersDAO.getBatch(ids, partsToFetch);

							// prefetch des données civiles
							prefetchDonneesCiviles(ids, null);

							for (Tiers t : tiers) {
								traiteTiers(t);
								niveauExtraction.increment();
								if (status.interrupted()) {
									break;
								}
							}
							return null;
						}
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
		final Set<AttributeIndividu> attributes = new HashSet<AttributeIndividu>();
		fillAttributesIndividu(attributes);
		final AttributeIndividu[] attributesArray;
		if (!attributes.isEmpty()) {
			attributesArray = attributes.toArray(new AttributeIndividu[attributes.size()]);
		}
		else {
			attributesArray = null;
		}
		serviceCivilCacheWarmer.warmIndividusPourTiers(idsTiers, date, attributesArray);
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
					transactionTemplate.execute(new TransactionCallback<Object>() {
						@Override
						public Object doInTransaction(TransactionStatus transactionStatus) {
							try {
								final Tiers tiers = tiersDAO.get(id);
								traiteTiers(tiers);
							}
							catch (Exception e) {
								LOGGER.error("Exception levée sur le tiers " + id, e);
								results.addErrorException(id, e);
							}
							return null;
						}
					});

					return null;
				}
			});

			if (status.interrupted()) {
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