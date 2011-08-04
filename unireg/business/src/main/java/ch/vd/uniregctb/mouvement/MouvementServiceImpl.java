package ch.vd.uniregctb.mouvement;

import javax.jms.JMSException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.editique.EditiqueCompositionService;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

public class MouvementServiceImpl implements MouvementService {

	private TiersDAO tiersDAO;
	private TiersService tiersService;
	private MouvementDossierDAO mouvementDossierDAO;
	private HibernateTemplate hibernateTemplate;
	private PlatformTransactionManager transactionManager;
	private EditiqueCompositionService editiqueService;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setMouvementDossierDAO(MouvementDossierDAO mouvementDossierDAO) {
		this.mouvementDossierDAO = mouvementDossierDAO;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setEditiqueService(EditiqueCompositionService editiqueService) {
		this.editiqueService = editiqueService;
	}

	/**
	 * Détermine les mouvements de dossiers pour une année
	 */
	@Override
	public DeterminerMouvementsDossiersEnMasseResults traiteDeterminationMouvements(RegDate dateTraitement, boolean archivesSeulement, StatusManager statusManager)  {
		final DeterminerMouvementsDossiersEnMasseProcessor processor = new DeterminerMouvementsDossiersEnMasseProcessor(tiersService, tiersDAO, mouvementDossierDAO, hibernateTemplate, transactionManager);
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
		final Set<MouvementDossier> contenu = new LinkedHashSet<MouvementDossier>(mvts);
		bordereau.setContenu(contenu);

		// demande d'impression éditique synchrone...
		try {
			return editiqueService.envoyerImpressionLocaleBordereau(bordereau);
		}
		catch (JMSException e) {
			throw new EditiqueException(e);
		}
	}
}
