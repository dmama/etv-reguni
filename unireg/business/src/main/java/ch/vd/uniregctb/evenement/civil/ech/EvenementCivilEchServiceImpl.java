package ch.vd.uniregctb.evenement.civil.ech;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.civil.ServiceCivilException;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.evenement.civil.EvenementCivilCriteria;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class EvenementCivilEchServiceImpl implements EvenementCivilEchService, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementCivilEchServiceImpl.class);

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
	public List<EvenementCivilEchBasicInfo> buildLotEvenementsCivilsNonTraites(long noIndividu) {
		final List<EvenementCivilEchBasicInfo> infos = buildListeEvenementsCivilsNonTraitesPourIndividuAvecReferences(noIndividu);
		if (infos != null && infos.size() > 1) {
			Collections.sort(infos, EVT_CIVIL_COMPARATOR);
		}
		return infos;
	}

	/**
	 * @param source événement civil dont on veut contruire la grappe complète de dépendances
	 * @return la liste (ordonnée du moins dépendant au plus dépendant) des constituants de la grappe
	 */
	@Override
	public List<EvenementCivilEchBasicInfo> buildGrappe(EvenementCivilEch source) throws EvenementCivilException {
		// récupération du numéro d'individu
		final long noIndividu;
		if (source.getNumeroIndividu() != null) {
			noIndividu = source.getNumeroIndividu();
		}
		else {
			noIndividu = getNumeroIndividuPourEvent(source);
		}

		// récupération de tous les événements liés à cet individu
		final List<EvenementCivilEch> allEvents = evenementCivilEchDAO.getEvenementsCivilsPourIndividu(noIndividu, true);

		// indexation de tout ce petit monde
		final Map<Long, EvenementCivilEch> idMap = new HashMap<>(allEvents.size());
		for (EvenementCivilEch evt : allEvents) {
			idMap.put(evt.getId(), evt);
		}

		// récupération des éléments dont notre source dépend
		final Set<Long> taken = new HashSet<>(idMap.size());
		final List<EvenementCivilEch> list = new LinkedList<>();
		EvenementCivilEch referenced = source;
		while (referenced != null) {
			if (!taken.add(referenced.getId())) {
				// on l'a déjà vu, celui-là, pas la peine de continuer (boucle ?)
				break;
			}
			list.add(0, referenced);       // insertion en début de liste
			referenced = idMap.get(referenced.getRefMessageId());
		}

		// dans l'autre sens, maintenant, les éléments qui dépendent de notre source
		final List<EvenementCivilEch> notTakenYet = new LinkedList<>(allEvents);
		notTakenYet.removeAll(list);
		while (true) {
			final List<EvenementCivilEch> referencer = new ArrayList<>();
			final Iterator<EvenementCivilEch> notTakenYetIterator = notTakenYet.iterator();
			while (notTakenYetIterator.hasNext()) {
				final EvenementCivilEch elt = notTakenYetIterator.next();
				if (taken.contains(elt.getRefMessageId())) {
					referencer.add(elt);
					notTakenYetIterator.remove();
				}
			}

			// on ne trouve plus personne
			if (referencer.size() == 0) {
				break;
			}

			list.addAll(referencer);
			for (EvenementCivilEch ref : referencer) {
				taken.add(ref.getId());
			}
		}

        return buildInfos(list, noIndividu);
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

	private List<EvenementCivilEchBasicInfo> buildListeEvenementsCivilsNonTraitesPourIndividuAvecReferences(long noIndividu) {
		// on récupère tous les événements civils attribuables à l'individu donné (= ceux qui ont le numéro d'individu assigné ceux qui n'ont pas de numéros d'individus assignés mais qui font référence, directement ou
		// pas, à un événement civil qui est assigné à cet individu)
		final List<EvenementCivilEch> evts = evenementCivilEchDAO.getEvenementsCivilsPourIndividu(noIndividu, true);
		if (evts != null && evts.size() > 0) {
			final List<EvenementCivilEchBasicInfo> liste = buildInfos(evts, noIndividu);
			buildReferenceAwareInformation(liste);
			return liste.size() > 0 ? liste : Collections.emptyList();
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
								? Collections.emptyList()
								: referrers.subList(indexFirstNonEch99 + 1, referrers.size());
						firstNonEch99.setReferrers(newReferrers);
						toAdd.add(firstNonEch99);

						// les eCH-99 non traités doivent également apparaître individuellement afin d'être pris en compte, le cas échéant, par la mécanique de post
						// processing des "indexations pures"
						for (EvenementCivilEchBasicInfo newRef : newReferrers) {
							if (EvenementCivilEchSourceHelper.isFromEch99(newRef) && !newRef.getEtat().isTraite()) {
								toAdd.add(newRef);
							}
						}
					}
				}
				else {
					// les eCH-99 non traités doivent également apparaître individuellement afin d'être pris en compte, le cas échéant, par la mécanique de post
					// processing des "indexations pures"
					for (EvenementCivilEchBasicInfo ref : info.getReferrers()) {
						if (EvenementCivilEchSourceHelper.isFromEch99(ref) && !ref.getEtat().isTraite()) {
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
	        return getNumeroIndividuPourEvent(event.getId(), event, new HashSet<>());
        }
    }

	private long getNumeroIndividuPourEvent(long eventId, @Nullable EvenementCivilEch event, Set<Long> loopPreventer) throws EvenementCivilException {

		if (event != null && event.getNumeroIndividu() != null) {
			return event.getNumeroIndividu();
		}

		Long noIndividu = null;
		try {
		    final IndividuApresEvenement apresEvenement = serviceCivil.getIndividuAfterEvent(eventId);
		    if (apresEvenement != null) {
			    final Individu individu = apresEvenement.getIndividu();
			    if (individu != null) {
				    noIndividu = individu.getNoTechnique();
			    }
		    }

			if (noIndividu == null && LOGGER.isDebugEnabled()) {
			    LOGGER.debug(String.format("Aucune information exploitable fournie par le GetIndividuAfterEvent(%d), essayons le GetIndividuByEvent", eventId));
			}
		}
		catch (ServiceCivilException e) {
			if (LOGGER.isDebugEnabled()) {
			    LOGGER.debug(String.format("Exception lancée par le GetIndividuAfterEvent(%d), essayons le GetIndividuByEvent...", eventId), e);
			}
		}

		if (noIndividu == null) {
			Individu individu;
			try {
				individu = serviceCivil.getIndividuByEvent(eventId, null);
				if (individu == null && LOGGER.isDebugEnabled()) {
					LOGGER.debug(String.format("Aucune information exploitable fournie par le GetIndividuByEvent(%d), essayons les dépendances (grappe)", eventId));
				}
			}
			catch (ServiceCivilException e) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug(String.format("Exception lancée par le GetIndividuByEvent(%d), essayons les dépendances (grappe)...", eventId), e);
				}
				individu = null;
			}

		    if (individu == null) {
			    if (event == null || event.getRefMessageId() == null || loopPreventer.contains(event.getRefMessageId())) {
	                throw new EvenementCivilException(String.format("Impossible de trouver l'individu lié à l'événement civil %d", eventId));
			    }
			    else {
				    final Long refMessageId = event.getRefMessageId();
				    loopPreventer.add(refMessageId);
				    try {
					    final EvenementCivilEch refEvent = get(refMessageId);
	                    noIndividu = getNumeroIndividuPourEvent(refMessageId, refEvent, loopPreventer);
				    }
				    catch (EvenementCivilException e) {
					    throw new EvenementCivilException(String.format("Impossible de trouver l'individu lié à l'événement civil %d", eventId), e);
				    }
			    }
		    }
			else {
		        noIndividu = individu.getNoTechnique();
		    }
	    }
		return noIndividu;
	}

	@Override
    public EvenementCivilEch assigneNumeroIndividu(final EvenementCivilEch event, final long numeroIndividu) {
        final EvenementCivilEch evt = evenementCivilEchDAO.get(event.getId());
        evt.setNumeroIndividu(numeroIndividu);
        return evenementCivilEchDAO.save(evt);
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
		    catch (Exception e) {
			    LOGGER.error("Impossible de recalculer le flag 'habitant' sur l'individu n°" + numeroIndividu, e);
		    }
	    }
    }
}
