package ch.vd.uniregctb.identification.contribuable.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuableCriteria;
import ch.vd.uniregctb.identification.contribuable.IdentificationContribuableService;
import ch.vd.uniregctb.identification.contribuable.view.IdentificationMessagesStatsCriteriaView;
import ch.vd.uniregctb.identification.contribuable.view.IdentificationMessagesStatsResultView;


public class IdentificationMessagesStatsManagerImpl implements IdentificationMessagesStatsManager, ApplicationContextAware {

	private static final String TOUS = "TOUS";

	protected static final Logger LOGGER = LoggerFactory.getLogger(IdentificationMessagesStatsManagerImpl.class);

	private IdentificationContribuableService identCtbService;

	private MessageSourceAccessor messageSourceAccessor;

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
	public IdentificationMessagesStatsCriteriaView getView() {
		IdentificationMessagesStatsCriteriaView identificationMessagesStatsCriteriaView = new IdentificationMessagesStatsCriteriaView();
		identificationMessagesStatsCriteriaView.setTypeMessage(TOUS);
		identificationMessagesStatsCriteriaView.setPeriodeFiscale(-1);
		return identificationMessagesStatsCriteriaView;
	}

	@Override
	@Transactional(readOnly = true)
	public List<IdentificationMessagesStatsResultView> calculerStats(IdentificationContribuableCriteria bean) {
		List<IdentificationMessagesStatsResultView> statsView = new ArrayList<>();
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
