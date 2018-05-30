package ch.vd.unireg.identification.contribuable.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.adresse.AdressesResolutionException;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.common.pagination.WebParamPagination;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.evenement.identification.contribuable.IdentCtbDAO;
import ch.vd.unireg.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.unireg.evenement.identification.contribuable.IdentificationContribuable.Etat;
import ch.vd.unireg.evenement.identification.contribuable.IdentificationContribuableCriteria;
import ch.vd.unireg.evenement.identification.contribuable.IdentificationContribuableEtatFilter;
import ch.vd.unireg.evenement.identification.contribuable.TypeDemande;
import ch.vd.unireg.identification.contribuable.IdentifiantUtilisateur;
import ch.vd.unireg.identification.contribuable.IdentificationContribuableService;
import ch.vd.unireg.identification.contribuable.view.IdentificationContribuableListCriteria;
import ch.vd.unireg.identification.contribuable.view.IdentificationMessagesResultView;
import ch.vd.unireg.indexer.messageidentification.GlobalMessageIdentificationSearcher;
import ch.vd.unireg.indexer.messageidentification.MessageIdentificationIndexedData;

public class IdentificationMessagesListManagerImpl implements IdentificationMessagesListManager {

	protected static final Logger LOGGER = LoggerFactory.getLogger(IdentificationMessagesListManagerImpl.class);

	private IdentCtbDAO identCtbDAO;

	private IdentificationContribuableService identCtbService;
	private GlobalMessageIdentificationSearcher searcher;

	public void setIdentCtbDAO(IdentCtbDAO identCtbDAO) {
		this.identCtbDAO = identCtbDAO;
	}

	public void setIdentCtbService(IdentificationContribuableService identCtbService) {
		this.identCtbService = identCtbService;
	}

	public void setSearcher(GlobalMessageIdentificationSearcher searcher) {
		this.searcher = searcher;
	}

	/**
	 * Alimente la vue
	 */
	@Override
	public IdentificationContribuableListCriteria getView(String parametreTypeMessage, Integer parametrePeriode, Etat parametreEtat) {
		final IdentificationContribuableListCriteria identificationContribuableListCriteria = new IdentificationContribuableListCriteria();
		identificationContribuableListCriteria.setUserCourant(AuthenticationHelper.getCurrentPrincipal());
		identificationContribuableListCriteria.setPrioriteEmetteur(null);
		identificationContribuableListCriteria.setTypeMessage(parametreTypeMessage);
		identificationContribuableListCriteria.setPeriodeFiscale(parametrePeriode);
		identificationContribuableListCriteria.setEtatMessage(parametreEtat);

		return identificationContribuableListCriteria;
	}


	/**
	 * Recherche des identifications correspondant aux critères
	 *
	 * @param bean
	 * @param pagination
	 * @param filter
	 * @param typeDemande
	 * @return
	 * @throws AdressesResolutionException
	 * @throws ServiceInfrastructureException
	 */
	@Override
	@Transactional(readOnly = true)
	public List<IdentificationMessagesResultView> find(IdentificationContribuableCriteria bean, WebParamPagination pagination,
	                                                   IdentificationContribuableEtatFilter filter, TypeDemande... typeDemande) throws AdressesResolutionException, ServiceInfrastructureException {

		final List<MessageIdentificationIndexedData> results = searcher.search(bean, typeDemande, filter, pagination);
		final List<IdentificationMessagesResultView> view = new ArrayList<>(results.size());
		for (MessageIdentificationIndexedData data : results) {
			view.add(buildView(data));
		}
		return view;
	}

	/**
	 * Recherche des identifications correspondant seulement à l'état en cours
	 *
	 * @param bean
	 * @param pagination
	 * @param typeDemande
	 * @return
	 * @throws AdressesResolutionException
	 * @throws ServiceInfrastructureException
	 */
	@Override
	@Transactional(readOnly = true)
	public List<IdentificationMessagesResultView> findEncoursSeul(IdentificationContribuableCriteria bean, WebParamPagination pagination, TypeDemande... typeDemande)
			throws AdressesResolutionException, ServiceInfrastructureException {

		bean.setEtatMessage(Etat.A_TRAITER_MANUELLEMENT);
		return find(bean, pagination, IdentificationContribuableEtatFilter.SEULEMENT_A_TRAITER_MANUELLEMENT, typeDemande);
	}

	/**
	 * Cherche et compte les identifications correspondant aux criteres
	 *
	 *
	 * @param criterion
	 * @param typeDemande
	 * @return
	 */
	@Override
	@Transactional(readOnly = true)
	public int count(IdentificationContribuableCriteria criterion, IdentificationContribuableEtatFilter filter, TypeDemande... typeDemande) {
		return searcher.count(criterion, typeDemande, filter);
	}

	/**
	 * Cherche et compte les identifications correspondant à l'etat en cours
	 *
	 * @param criterion
	 * @param typeDemande
	 * @return
	 */
	@Override
	@Transactional(readOnly = true)
	public int countEnCoursSeul(IdentificationContribuableCriteria criterion, TypeDemande... typeDemande) {
		criterion.setEtatMessage(Etat.A_TRAITER_MANUELLEMENT);
		return count(criterion, IdentificationContribuableEtatFilter.SEULEMENT_A_TRAITER_MANUELLEMENT, typeDemande);
	}


	/**
	 * Suspendre l'identification des messages
	 *
	 * @param identificationContribuableListCriteria
	 * @throws ServiceInfrastructureException
	 * @throws EditiqueException
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void suspendreIdentificationMessages(IdentificationContribuableListCriteria identificationContribuableListCriteria) {
		LOGGER.debug("Tab Ids messages:" + Arrays.toString(identificationContribuableListCriteria.getTabIdsMessages()));
		if (identificationContribuableListCriteria.getTabIdsMessages() != null) {
			for (int i = 0; i < identificationContribuableListCriteria.getTabIdsMessages().length; i++) {
				final IdentificationContribuable identificationContribuable = identCtbDAO.get(identificationContribuableListCriteria.getTabIdsMessages()[i]);
				if (Etat.A_TRAITER_MANUELLEMENT == identificationContribuable.getEtat()) {
					identificationContribuable.setEtat(Etat.A_TRAITER_MAN_SUSPENDU);
				}
				else if (Etat.A_EXPERTISER == identificationContribuable.getEtat()) {
					identificationContribuable.setEtat(Etat.A_EXPERTISER_SUSPENDU);
				}
			}

			identCtbService.updateTypesMessagesCache();
		}
	}


	/**
	 * Re soumettre l'identification des messages qui sont remis "dans le circuit" afin d'être identifié manuellement ou expèrtisé
	 *
	 *
	 * @param identificationContribuableListCriteria
	 * @throws ServiceInfrastructureException
	 * @throws EditiqueException
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void reSoumettreIdentificationMessages(IdentificationContribuableListCriteria identificationContribuableListCriteria) {
		LOGGER.debug("Tab Ids messages:" + Arrays.toString(identificationContribuableListCriteria.getTabIdsMessages()));
		if (identificationContribuableListCriteria.getTabIdsMessages() != null) {
			for (int i = 0; i < identificationContribuableListCriteria.getTabIdsMessages().length; i++) {
				final IdentificationContribuable identificationContribuable = identCtbDAO.get(identificationContribuableListCriteria.getTabIdsMessages()[i]);
				if (Etat.A_TRAITER_MAN_SUSPENDU == identificationContribuable.getEtat()) {
					identificationContribuable.setEtat(Etat.A_TRAITER_MANUELLEMENT);
				}
				else if (Etat.A_EXPERTISER_SUSPENDU == identificationContribuable.getEtat()) {
					identificationContribuable.setEtat(Etat.A_EXPERTISER);
				}
			}

			identCtbService.updateTypesMessagesCache();
		}
	}

	/**
	 * Soumettre l'identification des messages
	 *
	 * @param identificationContribuableListCriteria
	 * @throws ServiceInfrastructureException
	 * @throws EditiqueException
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void soumettreIdentificationMessages(IdentificationContribuableListCriteria identificationContribuableListCriteria) {
		LOGGER.debug("Tab Ids messages:" + Arrays.toString(identificationContribuableListCriteria.getTabIdsMessages()));
		if (identificationContribuableListCriteria.getTabIdsMessages() != null) {
			for (int i = 0; i < identificationContribuableListCriteria.getTabIdsMessages().length; i++) {
				final IdentificationContribuable identificationContribuable = identCtbDAO.get(identificationContribuableListCriteria.getTabIdsMessages()[i]);
				identCtbService.soumettre(identificationContribuable);
			}
		}
	}

	private IdentificationMessagesResultView buildView(MessageIdentificationIndexedData data) throws ServiceInfrastructureException {
		final IdentificationMessagesResultView view = new IdentificationMessagesResultView();
		view.setId(data.getId());
		view.setAnnule(data.isAnnule());
		view.setUtilisateurTraitant(data.getUtilisateurTraitant());

		if (StringUtils.isNotBlank(data.getVisaTraitement())) {
			final IdentifiantUtilisateur identifiantUtilisateur = identCtbService.getNomUtilisateurFromVisaUser(data.getVisaTraitement());
			view.setTraitementUser(identifiantUtilisateur.getNomComplet());
		}

		view.setTraitementDate(data.getDateTraitement());
		view.setEtatMessage(data.getEtat());
		if (StringUtils.isNotBlank(data.getNavs13Upi())) {
			view.setNavs13Upi(FormatNumeroHelper.formatNumAVS(data.getNavs13Upi()));
		}

		view.setDateMessage(RegDate.asJavaDate(data.getDateMessage()));
		view.setEmetteurId(data.getEmetteurId());
		view.setPeriodeFiscale(data.getPeriodeFiscale());
		view.setTypeMessage(data.getTypeMesssage());
		view.setTransmetteur(data.getTransmetteur());
		view.setMontant(data.getMontant());
		view.setNavs13(FormatNumeroHelper.formatNumAVS(data.getNavs13()));
		view.setNavs11(FormatNumeroHelper.formatAncienNumAVS(data.getNavs11()));
		view.setNom(data.getNom());
		view.setPrenoms(data.getPrenoms());
		view.setDateNaissance(data.getDateNaissance());

		view.setMessageRetour(data.isIdentifie() ? "Identifié" : data.getMessageErreur());
		view.setNumeroContribuable(data.getNoContribuableIdentifie());

		return view;
	}
}
