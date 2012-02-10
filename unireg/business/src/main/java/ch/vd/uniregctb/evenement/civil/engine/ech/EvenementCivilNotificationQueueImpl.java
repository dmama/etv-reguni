package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchDAO;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

/**
 * Classe utilitaire qui joue le rôle d'une queue bloquante pour le traitement des événements civils reçus de RCPers
 * <ul>
 *     <li>En entrée, les éléments postés dans cette queue sont des numéros d'individu</li>
 *     <li>En sortie, les éléments sont des listes d'événements civils (en fait, structures {@link EvtCivilInfo})</li>
 * </ul>
 * <p/>
 * <b>Quels sont les besoins de {@link java.util.concurrent.locks.Lock Lock} supplémentaire ?</b>
 * <p/>
 * Et bien voilà... je m'étais dit au départ que la méthode {@link #post} devrait faire en sorte d'éliminer les doublons de numéros d'individu...
 * Dans ce cadre, afin de bien les filtrer, il était important de faire en sorte que deux appels à {@link BlockingQueue#add} ne devaient
 * pas être faits en même temps, d'où l'utilisation d'un {@link java.util.concurrent.locks.ReentrantLock ReentrantLock}.
 * <p/>
 * Mais du coup, il était assez raisonnable de penser que la méthode {@link BlockingQueue#poll} devait subir le même genre de contrainte. Et si
 * c'est bien le cas, alors cela signifie qu'aucun appel à {@link #post} ne pourrait être fait pendant que la méthode {@link #poll(long, java.util.concurrent.TimeUnit) poll}
 * est en attente sur l'appel à {@link BlockingQueue#poll}, ce qui n'est pas très bon en terme de performances...
 * <p/>
 * Il restait donc deux axes :
 * <ul>
 *     <li>ne poser un verrou que sur la partie {@link BlockingQueue#add}, ou</li>
 *     <li>ne pas poser de verrou du tout.</li>
 * </ul>
 * <p/>
 * Dans le premier cas, nous conservons une tentative d'élimination de doublons efficace (ce que ne nous offre pas le second cas), et
 * le pire qui peut se produire est que l'élément soit enlevé de la queue entre le moment où on a déterminé qu'il y était déjà (et donc
 * le moment où on a décidé de ne pas l'y rajouter à nouveau) et le moment où on relâche le verrou. En effet, il ne peut pas y avoir,
 * par construction, d'autres ajouts dans cette queue au même moment.
 * <p/>
 * Si on suppose donc (ce qui devrait être le cas, de la manière dont
 * je vois l'implémentation de l'appelant de la méthode {@link #post}) que <i>le nouvel événement est déjà committé en base</i> au moment
 * où l'appel à la méthode {@link #post} est lancé, alors le thread qui est justement en train de récupérer les événements civils de cet
 * individu va de toute façon également récupérer cet événement-là.
 * <p/>
 * En revanche, si on choisit le second cas, alors on perd l'élimination des doublons, et on risque relativement souvent de voir la méthode
 * {@link #poll(long, java.util.concurrent.TimeUnit) poll} faire une requête en base dans le vide (car les événements auraient déjà été traités par
 * le passage précédent de la valeur en doublon). Notons bien que ce cas n'est pas totalement exclu dans la première solution, dans le
 * cas où l'identifiant de l'individu est enlevé de la queue entre le moment où l'événenement correspondant est effectivement committé en base
 * et le moment où la méthode {@link #post(Long) post} vérifie sa présence... mais cela devrait se produire moins souvent.
 */
public class EvenementCivilNotificationQueueImpl implements EvenementCivilNotificationQueue, InitializingBean {

	private static final Logger LOGGER = Logger.getLogger(EvenementCivilNotificationQueueImpl.class);

	private final BlockingQueue<Long> queue = new LinkedBlockingQueue<Long>();
	private final ReentrantLock lock = new ReentrantLock();

	private PlatformTransactionManager transactionManager;
	private HibernateTemplate hibernateTemplate;
	private EvenementCivilEchDAO evtCivilDAO;

	/**
	 * Comparateur qui trie les types d'événements civil par priorité (les types sans priorité sont placés à la fin)
	 */
	private static final Comparator<TypeEvenementCivilEch> PRIORITY_COMPARATOR = new Comparator<TypeEvenementCivilEch>() {
		@Override
		public int compare(TypeEvenementCivilEch o1, TypeEvenementCivilEch o2) {
			final Integer myPrio = o1.getPriorite();
			final Integer otherPrio = o2.getPriorite();
			final int comp;
			if (myPrio == null) {
				comp = (otherPrio == null ? 0 : 1);
			}
			else if (otherPrio == null) {
				comp = -1;
			}
			else {
				comp = myPrio - otherPrio;
			}
			return comp;
		}
	};

	/**
	 * Comparateur qui trie les événements civils par date, puis par priorité
	 */
	private static final Comparator<EvtCivilInfo> EVT_CIVIL_COMPARATOR = new Comparator<EvtCivilInfo>() {
		@Override
		public int compare(EvtCivilInfo o1, EvtCivilInfo o2) {
			int comp = o1.date.compareTo(o2.date);
			if (comp == 0) {
				comp = PRIORITY_COMPARATOR.compare(o1.type, o2.type);
			}
			return comp;
		}
	};

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvtCivilDAO(EvenementCivilEchDAO evtCivilDAO) {
		this.evtCivilDAO = evtCivilDAO;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (LOGGER.isInfoEnabled()) {
			final List<TypeEvenementCivilEch> all = new ArrayList<TypeEvenementCivilEch>(Arrays.asList(TypeEvenementCivilEch.values()));
			Collections.sort(all, PRIORITY_COMPARATOR);
			final StringBuilder b = new StringBuilder("A date égale, les événements civils e-CH seront traités dans l'ordre suivant : ");
			boolean first = true;
			for (TypeEvenementCivilEch type : all) {
				if (type.getPriorite() != null) {
					if (!first) {
						b.append(", ");
					}
					b.append(type).append(" (").append(type.getCodeECH()).append(')');
					first = false;
				}
			}
			LOGGER.info(b.toString());
		}
	}

	@Override
	public void post(Long noIndividu) {
		lock.lock();
		try {
			internalPost(noIndividu);
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public void postAll(Collection<Long> nosIndividus) {
		if (nosIndividus != null && nosIndividus.size() > 0) {
			lock.lock();
			try {
				for (Long noIndividu : nosIndividus) {
					internalPost(noIndividu);
				}
			}
			finally {
				lock.unlock();
			}
		}
	}

	/**
	 * Doit impérativement être appelé avec le verrou {@link #lock} possédé
	 * @param noIndividu numéro d'individu à poster dans la queue interne
	 */
	private void internalPost(Long noIndividu) {
		Assert.isTrue(lock.isHeldByCurrentThread());

		if (noIndividu == null) {
			throw new NullPointerException("noIndividu");
		}

		if (!queue.contains(noIndividu)) {
			queue.add(noIndividu);
		}
	}
	
	@Override
	public Batch poll(long timeout, TimeUnit unit) throws InterruptedException {
		final Long noIndividu = queue.poll(timeout, unit);
		if (noIndividu != null) {
			// 1. trouve tous les événements civils de cet individu qui sont dans un état A_TRAITER, EN_ATTENTE, EN_ERREUR
			// 2. tri de ces événements par date, puis type d'événement
			return new Batch(noIndividu, buildLotEvenementsCivils(noIndividu));
		}

		// rien à faire... à la prochaine !
		return null;
	}

	private List<EvtCivilInfo> buildLotEvenementsCivils(final long noIndividu) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		final List<EvtCivilInfo> infos = template.execute(new TransactionCallback<List<EvtCivilInfo>>() {
			@Override
			public List<EvtCivilInfo> doInTransaction(TransactionStatus status) {
				return hibernateTemplate.executeWithNewSession(new HibernateCallback<List<EvtCivilInfo>>() {
					@Override
					public List<EvtCivilInfo> doInHibernate(Session session) throws HibernateException, SQLException {
						return buildListeEvenementsCivilsATraiterPourIndividu(noIndividu);
					}
				});
			}
		});

		if (infos != null && infos.size() > 1) {
			Collections.sort(infos, EVT_CIVIL_COMPARATOR);
		}
		return infos;
	}

	/**
	 * Interroge le DAO des événements civils pour construire une collection des événements à traiter pour l'individu donné
	 * @param noIndividu numéro de l'individu civil dont on cherche les événements à traiter
	 * @return une liste des informations autour des événements à traiter
	 */
	private List<EvtCivilInfo> buildListeEvenementsCivilsATraiterPourIndividu(long noIndividu) {
		final List<EvenementCivilEch> evts = evtCivilDAO.getEvenementsCivilsNonTraites(noIndividu);
		if (evts != null && evts.size() > 0) {
			final List<EvtCivilInfo> liste = new ArrayList<EvtCivilInfo>(evts.size());
			for (EvenementCivilEch evt : evts) {
				final EvtCivilInfo info = new EvtCivilInfo(evt.getId(), noIndividu, evt.getEtat(), evt.getType(), evt.getAction(),
				                                           evt.getRefMessageId(), evt.getDateEvenement());
				liste.add(info);
			}
			return liste;
		}
		else {
			return Collections.emptyList();
		}
	}

	@Override
	public int getInflightCount() {
		return queue.size();
	}
}
