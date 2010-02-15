package ch.vd.uniregctb.mouvement;

import javax.jms.JMSException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueService;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

public class MouvementServiceImpl implements MouvementService {

	private TiersDAO tiersDAO;
	private TiersService tiersService;
	private MouvementDossierDAO mouvementDossierDAO;
	private HibernateTemplate hibernateTemplate;
	private PlatformTransactionManager transactionManager;
	private EditiqueService editiqueService;

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

	public void setEditiqueService(EditiqueService editiqueService) {
		this.editiqueService = editiqueService;
	}

	/**
	 * Détermine les mouvements de dossiers pour une année
	 */
	public DeterminerMouvementsDossiersEnMasseResults traiteDeterminationMouvements(RegDate dateTraitement, StatusManager statusManager)  {
		final DeterminerMouvementsDossiersEnMasseProcessor processor = new DeterminerMouvementsDossiersEnMasseProcessor(tiersService, tiersDAO, mouvementDossierDAO, hibernateTemplate, transactionManager);
		return processor.run(dateTraitement, statusManager);
	}

	private BordereauMouvementDossier creerBordereau() {
		final BordereauMouvementDossier bordereau = new BordereauMouvementDossier();
		return (BordereauMouvementDossier) hibernateTemplate.merge(bordereau);
	}

	/**
	 * Renvoie le document (PCL) du bordereau
	 * @param mvts mouvements à inclure
	 * @return tableau de bytes, contenu du document pcl
	 */
	public byte[] creerEtImprimerBordereau(List<MouvementDossier> mvts) throws EditiqueException {

		// 1. création d'un objet bordereau
		final BordereauMouvementDossier bordereau = creerBordereau();

		// 2. changement d'états et attibution d'une date de mouvement et d'attachement à un bordereau
		for (MouvementDossier mvt : mvts) {
			final ElementDeBordereau elt = (ElementDeBordereau) mvt;
			mvt.setEtat(EtatMouvementDossier.TRAITE);
			mvt.setDateMouvement(RegDate.get());
			elt.setBordereau(bordereau);
		}

		final Set<MouvementDossier> contenu = new HashSet<MouvementDossier>(mvts);
		bordereau.setContenu(contenu);

		// impression éditique...
		final String docId = editiqueService.envoyerImpressionLocaleBordereau(bordereau);
		try {
			final EditiqueResultat doc = editiqueService.getDocument(docId, true);
			if (doc != null) {
				if (doc.hasError()) {
					throw new EditiqueException(doc.getError());
				}
				return doc.getDocument();
			}
			else {
				throw new EditiqueException("Le service Editique ne répond pas à la demande d'impression, merci de ré-essayer plus tard.");
			}
		}
		catch (JMSException e) {
			throw new EditiqueException(e);
		}
	}
}
