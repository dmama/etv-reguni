package ch.vd.uniregctb.identification.contribuable.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.evenement.identification.contribuable.IdentCtbDAO;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuableCriteria;
import ch.vd.uniregctb.identification.contribuable.IdentificationContribuableService;
import ch.vd.uniregctb.identification.contribuable.view.IdentificationMessagesStatsResultView;
import ch.vd.uniregctb.identification.contribuable.view.IdentificationMessagesStatsView;


public class IdentificationMessagesStatsManagerImpl implements IdentificationMessagesStatsManager, ApplicationContextAware {

	private static final String TOUS = "TOUS";

	protected static final Logger LOGGER = Logger.getLogger(IdentificationMessagesStatsManagerImpl.class);

	private IdentCtbDAO identCtbDAO;

	private IdentificationContribuableService identCtbService;

	private MessageSourceAccessor messageSourceAccessor;

	public void setIdentCtbDAO(IdentCtbDAO identCtbDAO) {
		this.identCtbDAO = identCtbDAO;
	}

	public void setIdentCtbService(IdentificationContribuableService identCtbService) {
		this.identCtbService = identCtbService;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.messageSourceAccessor = new MessageSourceAccessor(applicationContext);
	}

	/**
	 * Alimente la vue
	 *
	 * @return
	 */
	@Override
	public IdentificationMessagesStatsView getView() {
		IdentificationMessagesStatsView identificationMessagesStatsView = new IdentificationMessagesStatsView();
		identificationMessagesStatsView.setTypeMessage(TOUS);
		identificationMessagesStatsView.setPeriodeFiscale(-1);
		return identificationMessagesStatsView;
	}

	@Override
	@Transactional(readOnly = true)
	public List<IdentificationMessagesStatsResultView> calculerStats(IdentificationContribuableCriteria bean) {
		List<IdentificationMessagesStatsResultView> statsView = new ArrayList<IdentificationMessagesStatsResultView>();
		Map<IdentificationContribuable.Etat, Integer> resultatsStats = identCtbService.calculerStats(bean);

		for (IdentificationContribuable.Etat etat : IdentificationContribuable.Etat.values()) {
			if (IdentificationContribuable.Etat.RECU != etat && IdentificationContribuable.Etat.SUSPENDU != etat) {

				IdentificationMessagesStatsResultView statViewCourante = new IdentificationMessagesStatsResultView();

				switch (etat) {

				case EXCEPTION:
					statViewCourante.setEtat(messageSourceAccessor.getMessage("label.identification.exception"));

					break;
				case A_TRAITER_MANUELLEMENT:
					statViewCourante.setEtat(messageSourceAccessor.getMessage("label.identification.encours"));
					break;

				case A_TRAITER_MAN_SUSPENDU:
					statViewCourante.setEtat(messageSourceAccessor.getMessage("label.identification.encours"));
					statViewCourante.setResultatIdentification(messageSourceAccessor.getMessage("label.identification.suspendu"));
					break;
				case A_EXPERTISER:
					statViewCourante.setEtat(messageSourceAccessor.getMessage("label.identification.attente.expertise"));
					break;

				case A_EXPERTISER_SUSPENDU:
					statViewCourante.setEtat(messageSourceAccessor.getMessage("label.identification.attente.expertise"));
					statViewCourante.setResultatIdentification(messageSourceAccessor.getMessage("label.identification.suspendu"));
					break;

				case TRAITE_MANUELLEMENT:
					statViewCourante.setEtat(messageSourceAccessor.getMessage("label.identification.archive"));
					statViewCourante.setResultatIdentification(messageSourceAccessor.getMessage("label.identification.cellule.backoffice"));
					break;

				case NON_IDENTIFIE:
					statViewCourante.setEtat(messageSourceAccessor.getMessage("label.identification.archive"));
					statViewCourante.setResultatIdentification(messageSourceAccessor.getMessage("label.identification.non.identification"));
					break;

				case TRAITE_MAN_EXPERT:
					statViewCourante.setEtat(messageSourceAccessor.getMessage("label.identification.archive"));
					statViewCourante.setResultatIdentification(messageSourceAccessor
							.getMessage("label.identification.gestionnaire.backoffice"));
					break;

				case TRAITE_AUTOMATIQUEMENT:
					statViewCourante.setEtat(messageSourceAccessor.getMessage("label.identification.archive"));
					statViewCourante.setResultatIdentification(messageSourceAccessor.getMessage("label.identification.automatique"));
					break;

				default:
					break;
				}
				statViewCourante.setTypeMessage(bean.getTypeMessage());
				statViewCourante.setPeriode(bean.getPeriodeFiscale());
				statViewCourante.setEtatTechnique(etat.name());
				statViewCourante.setNombre(resultatsStats.get(etat));
				statsView.add(statViewCourante);
			}
		}
		Collections.sort(statsView);
		return statsView;
	}

}
