package ch.vd.uniregctb.evenement.civil.ech;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.civil.ServiceCivilException;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.evenement.civil.EvenementCivilCriteria;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.hibernate.HibernateCallback;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.transaction.TransactionTemplate;
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
	private TiersDAO tiersDAO;
	private TiersService tiersService;

	@Override
	public void afterPropertiesSet() throws Exception {
		if (LOGGER.isInfoEnabled()) {
			final List<TypeEvenementCivilEch> all = new ArrayList<>(Arrays.asList(TypeEvenementCivilEch.values()));
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
			final List<ActionEvenementCivilEch> allActions = new ArrayList<>(Arrays.asList(ActionEvenementCivilEch.values()));
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

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setEvenementCivilEchDAO(EvenementCivilEchDAO evenementCivilEchDAO) {
		this.evenementCivilEchDAO = evenementCivilEchDAO;
	}

	public void setServiceCivil(ServiceCivilService serviceCivil) {
		this.serviceCivil = serviceCivil;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
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
		return buildListeEvenementsCivilsATraiterPourIndividuAvecReferences(noIndividu);
	}

	private List<EvenementCivilEchBasicInfo> buildInfos(List<EvenementCivilEch> evts, long noIndividu) {
		if (evts != null && evts.size() > 0) {
			final List<EvenementCivilEchBasicInfo> liste = new ArrayList<>(evts.size());
			for (EvenementCivilEch evt : evts) {
				final EvenementCivilEchBasicInfo info = new EvenementCivilEchBasicInfo(evt, noIndividu);
				liste.add(info);
			}
			return liste;
		}
		else {
			return Collections.emptyList();
		}
	}

	private List<EvenementCivilEchBasicInfo> buildListeEvenementsCivilsATraiterPourIndividuAvecReferences(long noIndividu) {
		// on récupère tous les événements civils attribuables à l'individu donné (= ceux qui ont le numéro d'individu assigné ceux qui n'ont pas de numéros d'individus assignés mais qui font référence, directement ou
		// pas, à un événement civil qui est assigné à cet individu)
		final List<EvenementCivilEch> evts = evenementCivilEchDAO.getEvenementsCivilsPourIndividu(noIndividu, true);
		if (evts != null && evts.size() > 0) {
			final List<EvenementCivilEchBasicInfo> liste = buildInfos(evts, noIndividu);
			buildReferenceAwareInformation(liste);
			return liste.size() > 0 ? liste : Collections.<EvenementCivilEchBasicInfo>emptyList();
		}
		else {
			return Collections.emptyList();
		}
	}

	private static void buildReferenceAwareInformation(List<EvenementCivilEchBasicInfo> liste) {

		// si la liste est vide, pas la peine de faire quoi que ce soit, il ne peut pas y avoir de référence à utiliser
		if (liste == null || liste.size() == 0) {
			return;
		}

		// on construit d'abord un index des données fournies
		final Map<Long, EvenementCivilEchBasicInfo> map = new HashMap<>(liste.size());
		for (EvenementCivilEchBasicInfo info : liste) {
			map.put(info.getId(), info);
		}

		// on élimine les éléments de la liste qui font référence à d'autres éléments de la liste
		// (ceux-là seront présents dans la collection des <i>referrers</i> de leur référence ultime
		// présente dans la liste)
		final Iterator<EvenementCivilEchBasicInfo> iterator = liste.iterator();
		while (iterator.hasNext()) {
			final EvenementCivilEchBasicInfo info = iterator.next();
			boolean toRemove = false;
			if (info.getEtat().isTraite()) {
				toRemove = true;
			}
			if (info.getIdReference() != null) {
				// il faut trouver l'élément non-traité le plus profond dans la chaîne de dépendances pour attacher (ce sera le point d'ancrage de tout le monde)
				EvenementCivilEchBasicInfo ref = map.get(info.getIdReference());
				EvenementCivilEchBasicInfo refNonTraite = ref != null && !ref.getEtat().isTraite() ? ref : null;
				while (ref != null && ref.getIdReference() != null) {
					final EvenementCivilEchBasicInfo newRef = map.get(ref.getIdReference());
					if (newRef != null) {
						ref = newRef;
						if (!ref.getEtat().isTraite()) {
							refNonTraite = ref;
						}
					}
					else {
						break;
					}
				}
				if (refNonTraite != null) {
					refNonTraite.addReferrer(info);
					toRemove = true;
				}
			}

			if (toRemove) {
				iterator.remove();
			}
		}

		// maintenant, on a une liste d'éléments racines de corrections/annulations ;
		// dans le cas où il y a des annulations il faut faire un peu plus attention, selon que c'est l'élément racine qui est annulé ou un autre...
		dealWithAnnulationsInReferences(liste);

		// dans le cas d'un événement principal issu d'un eCH-0099, il faut aussi faire attention afin que ses éventuels dépendants ne soient
		// pas traités implicitement (à cause du raccourci actuellement implémenté pour cette catégorie d'événements)
		// TODO [ech99] jde : à enlever dès que possible...
		dealWithEch99(liste);
	}

	private static void dealWithAnnulationsInReferences(List<EvenementCivilEchBasicInfo> liste) {
		final List<EvenementCivilEchBasicInfo> toAdd = new LinkedList<>();
		for (EvenementCivilEchBasicInfo info : liste) {
			final List<EvenementCivilEchBasicInfo> referrers = new ArrayList<>(info.getReferrers());
			if (referrers.size() == 1) {
				final EvenementCivilEchBasicInfo ref = referrers.get(0);
				if (ref.getAction() == ActionEvenementCivilEch.ANNULATION && !ref.getEtat().isTraite()) {
					//noinspection ConstantConditions,StatementWithEmptyBody
					if (info.getId() == ref.getIdReference() && info.getAction() == ActionEvenementCivilEch.PREMIERE_LIVRAISON) {
						// on peut tout laisser sur place, c'est une annulation compète
					}
					else {
						// on sépare les deux qui ne peuvent pas être traités ensemble (cas d'une annulation qui annule une correction, par
						// exemple -> cela devrait annuler la totalité de la chaîne depuis l'événement initial, ou pas (ce n'est pas clair))
						referrers.clear();
						toAdd.add(ref);
					}
				}
			}
			else if (referrers.size() > 0) {

				// on cherche les éventuelles annulations à traiter
				final Iterator<EvenementCivilEchBasicInfo> refIterator = referrers.iterator();
				while (refIterator.hasNext()) {
					final EvenementCivilEchBasicInfo ref = refIterator.next();
					if (ref.getAction() == ActionEvenementCivilEch.ANNULATION && !ref.getEtat().isTraite()) {
						refIterator.remove();
						toAdd.add(ref);
					}
				}
			}

			// reset referrers with actual re-structured value
			info.setReferrers(referrers);
		}

		liste.addAll(toAdd);
	}

	/**
	 * Dans le cas d'un événement principal issu d'un eCH-0099, il faut aussi faire attention afin que ses éventuels dépendants ne soient pas traités implicitement ;
	 * nous allons donc nous assurer que de tels événements n'ont pas de dépendances
	 * @param liste liste des événements civils à traiter pour un individus
	 */
	private static void dealWithEch99(List<EvenementCivilEchBasicInfo> liste) {
		final List<EvenementCivilEchBasicInfo> toAdd = new LinkedList<>();
		for (EvenementCivilEchBasicInfo info : liste) {
			if (info.getReferrers().size() > 0) {
				if (EvenementCivilEchSourceHelper.isFromEch99(info)) {

					// cherchons le premier élément de la liste des referrers qui ne soit pas issu de eCH-99
					final List<EvenementCivilEchBasicInfo> referrers = info.getSortedReferrers();
					EvenementCivilEchBasicInfo firstNonEch99 = null;
					int indexFirstNonEch99 = 0;
					for (EvenementCivilEchBasicInfo candidate : referrers) {
						if (!EvenementCivilEchSourceHelper.isFromEch99(candidate)) {
							firstNonEch99 = candidate;
							break;
						}
						++ indexFirstNonEch99;
					}

					// tous sont issus de eCH-99 ? -> rien à faire, un traitement implicite va très bien
					if (firstNonEch99 != null) {

						// il y en a un qui n'est pas issu de 99 -> les éléments précédents peuvent rester là
						final List<EvenementCivilEchBasicInfo> remainingReferrers = referrers.subList(0, indexFirstNonEch99);
						info.setReferrers(remainingReferrers);

						// et le non-issu de eCH-99 devient un élément principal dans la liste des événements
						final List<EvenementCivilEchBasicInfo> newReferrers = remainingReferrers.size() == referrers.size() - 1
								? Collections.<EvenementCivilEchBasicInfo>emptyList()
								: referrers.subList(indexFirstNonEch99 + 1, referrers.size());
						firstNonEch99.setReferrers(newReferrers);
						toAdd.add(firstNonEch99);

						// les eCH-99 doivent également apparaître individuellement afin d'être pris en compte, le cas échéant, par la mécanique de post
						// processing des "indexations pures"
						for (EvenementCivilEchBasicInfo newRef : newReferrers) {
							if (EvenementCivilEchSourceHelper.isFromEch99(newRef)) {
								toAdd.add(newRef);
							}
						}
					}
				}
				else {
					// les eCH-99 doivent également apparaître individuellement afin d'être pris en compte, le cas échéant, par la mécanique de post
					// processing des "indexations pures"
					for (EvenementCivilEchBasicInfo ref : info.getReferrers()) {
						if (EvenementCivilEchSourceHelper.isFromEch99(ref)) {
							toAdd.add(ref);
						}
					}
				}
			}
		}
		liste.addAll(toAdd);
	}

	@Override
    public long getNumeroIndividuPourEvent(EvenementCivilEch event) throws EvenementCivilException {
        if (event.getNumeroIndividu() != null) {
            return event.getNumeroIndividu();
        }
        else {
	        Long noIndividu = null;
	        try {
	            final IndividuApresEvenement apresEvenement = serviceCivil.getIndividuAfterEvent(event.getId());
	            if (apresEvenement != null) {
		            final Individu individu = apresEvenement.getIndividu();
		            if (individu != null) {
			            noIndividu = individu.getNoTechnique();
		            }
	            }

		        if (noIndividu == null && LOGGER.isDebugEnabled()) {
		            LOGGER.debug("Aucune information exploitable fournie par le GetIndividuAfterEvent, essayons le GetIndividuByEvent");
		        }
	        }
	        catch (ServiceCivilException e) {
		        if (LOGGER.isDebugEnabled()) {
		            LOGGER.debug("Exception lancée par le GetIndividuAfterEvent, essayons le GetIndividuByEvent...", e);
		        }
	        }

	        if (noIndividu == null) {
	            final Individu individu = serviceCivil.getIndividuByEvent(event.getId(), null);
	            if (individu == null) {
                    throw new EvenementCivilException(String.format("Impossible de trouver l'individu lié à l'événement civil %d", event.getId()));
	            }
	            noIndividu = individu.getNoTechnique();
            }
	        return noIndividu;
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
    public EvenementCivilEch get(Long id) {
        return evenementCivilEchDAO.get(id);
    }


    @Override
    public List<EvenementCivilEch> find(EvenementCivilCriteria<TypeEvenementCivilEch> criterion, ParamPagination pagination) {
        return evenementCivilEchDAO.find(criterion, pagination);
    }

    @Override
    public int count(EvenementCivilCriteria<TypeEvenementCivilEch> criterion) {
        return evenementCivilEchDAO.count(criterion);
    }

    @Override
    public void forceEvenement(Long id) {
	    final EvenementCivilEch evt = evenementCivilEchDAO.get(id);
        if (evt == null) {
            throw new ObjectNotFoundException("Evénement ech " + id);
        }
	    if (evt.getEtat().isTraite() && evt.getEtat() != EtatEvenementCivil.A_VERIFIER) {
            throw new IllegalArgumentException("L'état de l'événement " + id + " ne lui permet pas d'être forcé");
        }
        evt.setEtat(EtatEvenementCivil.FORCE);

	    Audit.info(id, String.format("Forçage manuel de l'événement civil %d de type %s/%s au %s sur l'individu %d", id, evt.getType(), evt.getAction(), RegDateHelper.dateToDisplayString(evt.getDateEvenement()), evt.getNumeroIndividu()));

	    // [SIFISC-6908] En cas de forçage de l'événement, on essaie au moins de mettre-à-jour le flag habitant, pour que les droits d'édition corrects s'appliquent sur la personne physiques.
	    final Long numeroIndividu = evt.getNumeroIndividu();
	    if (numeroIndividu != null) {
		    try {
			    final PersonnePhysique pp = tiersDAO.getPPByNumeroIndividu(numeroIndividu);
			    if (pp != null) {
				    tiersService.updateHabitantFlag(pp, numeroIndividu, id);
			    }
		    }
		    catch (RuntimeException e) {
			    LOGGER.error("Impossible de recalculer le flag 'habitant' sur l'individu n°" + numeroIndividu, e);
		    }
	    }
    }
}
