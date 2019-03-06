package ch.vd.unireg.mouvement;

import javax.jms.JMSException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.editique.EditiqueCompositionService;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.EditiqueResultat;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.interfaces.service.ServiceSecuriteService;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;

public class MouvementServiceImpl implements MouvementService, ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {

	private static final Logger LOGGER = LoggerFactory.getLogger(MouvementServiceImpl.class);

	private TiersDAO tiersDAO;
	private TiersService tiersService;
	private MouvementDossierDAO mouvementDossierDAO;
	private HibernateTemplate hibernateTemplate;
	private PlatformTransactionManager transactionManager;
	private EditiqueCompositionService editiqueService;
	private AssujettissementService assujettissementService;
	private AdresseService adresseService;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setMouvementDossierDAO(MouvementDossierDAO mouvementDossierDAO) {
		this.mouvementDossierDAO = mouvementDossierDAO;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEditiqueService(EditiqueCompositionService editiqueService) {
		this.editiqueService = editiqueService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setAssujettissementService(AssujettissementService assujettissementService) {
		this.assujettissementService = assujettissementService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
	}

	/**
	 * Détermine les mouvements de dossiers pour une année
	 */
	@Override
	public DeterminerMouvementsDossiersEnMasseResults traiteDeterminationMouvements(RegDate dateTraitement, boolean archivesSeulement, StatusManager statusManager) {
		final DeterminerMouvementsDossiersEnMasseProcessor processor =
				new DeterminerMouvementsDossiersEnMasseProcessor(tiersService, tiersDAO, mouvementDossierDAO, hibernateTemplate, transactionManager, assujettissementService, adresseService);
		return processor.run(dateTraitement, archivesSeulement, statusManager);
	}

	private BordereauMouvementDossier creerBordereau() {
		final BordereauMouvementDossier bordereau = new BordereauMouvementDossier();
		return hibernateTemplate.merge(bordereau);
	}

	/**
	 * Fait la demande d'impression d'un bordereau de mouvement de dossiers en masse avec les mouvements indiqués
	 * @param mvts les mouvements constituant le bordereau
	 * @return le document imprimé
	 */
	@Override
	public EditiqueResultat envoyerImpressionBordereau(List<MouvementDossier> mvts) throws EditiqueException {

		// 1. création d'un objet bordereau
		final BordereauMouvementDossier bordereau = creerBordereau();

		// 2. changement d'états et attibution d'une date de mouvement et d'attachement à un bordereau
		for (MouvementDossier mvt : mvts) {
			final ElementDeBordereau elt = (ElementDeBordereau) mvt;
			mvt.setEtat(EtatMouvementDossier.TRAITE);
			mvt.setDateMouvement(RegDate.get());
			elt.setBordereau(bordereau);
		}

		// LinkedHashSet pour conserver l'ordre de la liste originale dans l'itération
		// qui va mener à l'impression...
		final Set<MouvementDossier> contenu = new LinkedHashSet<>(mvts);
		bordereau.setContenu(contenu);

		// demande d'impression éditique synchrone...
		try {
			return editiqueService.envoyerImpressionLocaleBordereau(bordereau);
		}
		catch (JMSException e) {
			throw new EditiqueException(e);
		}
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		// SIFISC-29729, les numéros opérateurs sont décommisionnés, rien à faire
	}
}
