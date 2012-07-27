package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.civil.EvenementCivilCriteria;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchBasicInfo;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchDAO;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

/**
 * Stratégie qui va regarder si un événement d'annulation en attente concerne un événement d'annonce lui aussi en attente ou en erreur
 * (si c'est le cas, les événements d'annonce, de correction et d'annulation peuvent être traités tout droit car tout est annulé)
 */
public class ErrorPostProcessingAnnulationImpactStrategy implements ErrorPostProcessingStrategy<Object> {

	private static final Logger LOGGER = Logger.getLogger(ErrorPostProcessingAnnulationImpactStrategy.class);

	private static final String COMMENTAIRE = "Groupe d'événements annulés alors qu'ils étaient encore en attente";

	private final EvenementCivilEchDAO evtCivilDAO;

	public ErrorPostProcessingAnnulationImpactStrategy(EvenementCivilEchDAO evtCivilDAO) {
		this.evtCivilDAO = evtCivilDAO;
	}

	@Override
	public boolean needsTransactionOnCollectPhase() {
		return true;
	}

	@NotNull
	@Override
	public List<EvenementCivilEchBasicInfo> doCollectPhase(List<EvenementCivilEchBasicInfo> remainingEvents, CustomDataHolder<Object> customData) {
		final List<EvenementCivilEchBasicInfo> remain = new ArrayList<EvenementCivilEchBasicInfo>(remainingEvents);
		final List<EvenementCivilEchBasicInfo> traites = new LinkedList<EvenementCivilEchBasicInfo>();
		for (EvenementCivilEchBasicInfo info : remainingEvents) {
			if (info.getAction() == ActionEvenementCivilEch.ANNULATION && !info.getEtat().isTraite()) {
				final Long idEvtRef = info.getIdReference();
				if (idEvtRef != null) {
					final EvenementCivilEch evtAnnulation = evtCivilDAO.get(info.getId());
					if (!evtAnnulation.getEtat().isTraite()) {          // au cas où on ne serait plus tout-à-fait à jour
						final EvenementCivilEch evtAnnule = evtCivilDAO.get(idEvtRef);
						if (evtAnnule != null && !evtAnnule.getEtat().isTraite()) {
							// l'événement que l'on veut annuler n'est pas encore traité... on peut tout faire sauter !

							// on recherche encore éventuellement des autres événements qui corrigeraient l'événement annulé...
							final EvenementCivilCriteria<TypeEvenementCivilEch> criterion = new EvenementCivilCriteria<TypeEvenementCivilEch>();
							criterion.setNumeroIndividu(info.getNoIndividu());
							criterion.setType(info.getType());      // deux événements qui se corrigent/s'annulent sont forcément du même type
							final List<EvenementCivilEch> autresEvts = evtCivilDAO.find(criterion, null);

							// on marque redondants tous les événements (l'événement annulé, celui d'annulation et d'éventuels autres qui corrigeaient l'événement annulé)
							final Set<Long> idsATraiter = new HashSet<Long>(autresEvts.size());
							idsATraiter.add(info.getId());
							idsATraiter.add(info.getIdReference());
							int oldSize = 0;
							while (oldSize != idsATraiter.size()) {
								oldSize = idsATraiter.size();
								for (EvenementCivilEch evt : autresEvts) {
									final Long refMessageId = evt.getRefMessageId();
									if (refMessageId != null && idsATraiter.contains(refMessageId) && !evt.getEtat().isTraite()) {
										idsATraiter.add(evt.getId());
									}
								}
							}

							// et maintenant on marque tout le monde comme redondant
							for (EvenementCivilEch evt : autresEvts) {
								if (idsATraiter.contains(evt.getId())) {
									// surtout faire cette construction AVANT de changer les valeurs dans l'événement (sinon le removeAll plus bas ne retrouve pas ses petits) !!!
									final EvenementCivilEchBasicInfo infoEvt = new EvenementCivilEchBasicInfo(evt);

									evt.getErreurs().clear();
									evt.setDateTraitement(DateHelper.getCurrentDate());
									evt.setEtat(EtatEvenementCivil.REDONDANT);
									evt.setCommentaireTraitement(COMMENTAIRE);

									Audit.info(evt.getId(), String.format("Marquage de l'événement %d (%s/%s) comme redondant (groupe d'événements annulés avant d'avoir été traités)", evt.getId(), evt.getType(), evt.getAction()));

									traites.add(infoEvt);
								}
							}
						}
					}
				}
				else {
					LOGGER.warn(String.format("L'événement civil %d est une annulation mais n'a pas de référence vers un autre événement...", info.getId()));
				}
			}
		}
		remain.removeAll(traites);
		return remain;
	}

	@Override
	public boolean needsTransactionOnFinalizePhase() {
		return false;
	}

	@Override
	public void doFinalizePhase(Object customData) {
		// Tout est déjà fait...
	}
}
