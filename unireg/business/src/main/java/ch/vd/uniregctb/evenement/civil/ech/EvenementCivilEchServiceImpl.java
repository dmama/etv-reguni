package ch.vd.uniregctb.evenement.civil.ech;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.evenement.civil.EvenementCivilCriteria;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class EvenementCivilEchServiceImpl implements EvenementCivilEchService, InitializingBean {

	private static final Logger LOGGER = Logger.getLogger(EvenementCivilEchServiceImpl.class);

	private static final Comparator<ActionEvenementCivilEch> ACTION_PRIORITY_COMPARATOR = new ActionEvenementCivilEchComparator();

	/**
	 * Comparateur qui trie les types d'événements civil par priorité (les types sans priorité sont placés à la fin)
	 */
	private static final Comparator<TypeEvenementCivilEch> TYPE_PRIORITY_COMPARATOR = new TypeEvenementCivilEchComparator();

	/**
	 * Comparateur qui trie les événements civils par date, puis par priorité
	 */
	private static final Comparator<EvenementCivilEchBasicInfo> EVT_CIVIL_COMPARATOR = new EvenementCivilEchBasicInfoComparator();

	private ServiceCivilService serviceCivil;
    private EvenementCivilEchDAO evenementCivilEchDAO;
	private PlatformTransactionManager transactionManager;
	private HibernateTemplate hibernateTemplate;

	@Override
	public void afterPropertiesSet() throws Exception {
		if (LOGGER.isInfoEnabled()) {
			final List<TypeEvenementCivilEch> all = new ArrayList<TypeEvenementCivilEch>(Arrays.asList(TypeEvenementCivilEch.values()));
			Collections.sort(all, TYPE_PRIORITY_COMPARATOR);
			StringBuilder b = new StringBuilder("A date égale, les événements civils e-CH seront traités dans l'ordre suivant : ");
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
			final List<ActionEvenementCivilEch> allActions = new ArrayList<ActionEvenementCivilEch>(Arrays.asList(ActionEvenementCivilEch.values()));
			Collections.sort(allActions, ACTION_PRIORITY_COMPARATOR);
			b = new StringBuilder("A date égale, et priorité de type égale, les événements civils e-CH seront traités dans l'ordre suivant : ");
			first = true;
			for (ActionEvenementCivilEch action : allActions) {
					if (!first) {
						b.append(", ");
					}
					b.append(action);
					first = false;
			}
			LOGGER.info(b.toString());
		}
	}


	@SuppressWarnings({"UnusedDeclaration"})
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvenementCivilEchDAO(EvenementCivilEchDAO evenementCivilEchDAO) {
		this.evenementCivilEchDAO = evenementCivilEchDAO;
	}

	public void setServiceCivil(ServiceCivilService serviceCivil) {
		this.serviceCivil = serviceCivil;
	}

	@Override
	public List<EvenementCivilEchBasicInfo> buildLotEvenementsCivils(final long noIndividu) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		final List<EvenementCivilEchBasicInfo> infos = template.execute(new TransactionCallback<List<EvenementCivilEchBasicInfo>>() {
			@Override
			public List<EvenementCivilEchBasicInfo> doInTransaction(TransactionStatus status) {
				return hibernateTemplate.executeWithNewSession(new HibernateCallback<List<EvenementCivilEchBasicInfo>>() {
					@Override
					public List<EvenementCivilEchBasicInfo> doInHibernate(Session session) throws HibernateException, SQLException {
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

	private List<EvenementCivilEchBasicInfo> buildListeEvenementsCivilsATraiterPourIndividu(long noIndividu) {
		final List<EvenementCivilEch> evts = evenementCivilEchDAO.getEvenementsCivilsNonTraites(Arrays.asList(noIndividu));
		if (evts != null && evts.size() > 0) {
			final List<EvenementCivilEchBasicInfo> liste = new ArrayList<EvenementCivilEchBasicInfo>(evts.size());
			for (EvenementCivilEch evt : evts) {
				final EvenementCivilEchBasicInfo info = new EvenementCivilEchBasicInfo(evt.getId(), noIndividu, evt.getEtat(), evt.getType(), evt.getAction(),
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
    public long getNumeroIndividuPourEvent(EvenementCivilEch event) throws EvenementCivilException {
        if (event.getNumeroIndividu() != null) {
            return event.getNumeroIndividu();
        }
        else {
            final IndividuApresEvenement apresEvenement = serviceCivil.getIndividuFromEvent(event.getId());
            if (apresEvenement == null) {
                throw new EvenementCivilException(String.format("Pas d'événement RcPers lié à l'événement civil %d", event.getId()));
            }
            final Individu individu = apresEvenement.getIndividu();
            if (individu == null) {
                throw new EvenementCivilException(String.format("Aucune donnée d'individu fournie avec l'événement civil %d", event.getId()));
            }
            return individu.getNoTechnique();
        }
    }

    @Override
    public EvenementCivilEch assigneNumeroIndividu(final EvenementCivilEch event, final long numeroIndividu) {
        if (event.getNumeroIndividu() == null || event.getNumeroIndividu() != numeroIndividu) {
            final TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            return template.execute(new TransactionCallback<EvenementCivilEch>() {
                @Override
                public EvenementCivilEch doInTransaction(TransactionStatus status) {
                    final EvenementCivilEch evt = evenementCivilEchDAO.get(event.getId());
                    evt.setNumeroIndividu(numeroIndividu);
                    return evenementCivilEchDAO.save(evt);
                }
            });
        }
        else {
            return event;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public EvenementCivilEch get(Long id) {
        return evenementCivilEchDAO.get(id);
    }


    @Override
    @Transactional(readOnly = true)
    public List<EvenementCivilEch> find(EvenementCivilCriteria<TypeEvenementCivilEch> criterion, ParamPagination pagination) {
        return evenementCivilEchDAO.find(criterion, pagination);
    }

    @Override
    @Transactional(readOnly = true)
    public int count(EvenementCivilCriteria<TypeEvenementCivilEch> criterion) {
        return evenementCivilEchDAO.count(criterion);
    }

    @Override
    @Transactional
    public void forceEvenement(Long id) {
        EvenementCivilEch evt = evenementCivilEchDAO.get(id);
        if (evt==null) {
            throw new ObjectNotFoundException("evenement ech " + id);
        }
        if (evt.getEtat().isTraite() && evt.getEtat() != EtatEvenementCivil.A_VERIFIER) {
            throw new IllegalArgumentException("l'état de l'événement " + id + " ne lui permet pas d'être forcé");
        }
        evt.setEtat(EtatEvenementCivil.FORCE);
    }
}
