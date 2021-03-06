package ch.vd.unireg.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.data.CivilDataEventNotifier;
import ch.vd.unireg.data.FiscalDataEventNotifier;
import ch.vd.unireg.indexer.tiers.GlobalTiersIndexer;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;

public class IndexationManagerImpl implements IndexationManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(IndexationManagerImpl.class);

	private GlobalTiersIndexer tiersIndexer;
	private CivilDataEventNotifier civilDataEventNotifier;
	private FiscalDataEventNotifier fiscalDataEventNotifier;
	private TiersDAO tiersDAO;

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void reindexTiers(long id) {

		final Long noIndividu = getNumeroIndividu(id);
		final Long noCantonal = getNumeroCantonalRegistreEntreprises(id);
		if (noIndividu != null) {
			LOGGER.info("Demande de réindexation manuelle du tiers n°" + id + " (avec éviction du cache des données de l'individu n°" + noIndividu + ')');
			// on en profite pour forcer l'éviction des données cachées pour l'individu
			civilDataEventNotifier.notifyIndividuChange(noIndividu);
		}
		else if (noCantonal != null) {
			LOGGER.info("Demande de réindexation manuelle du tiers n°" + id + " (avec éviction du cache des données de l'entreprise/de l'établissement civil n°" + noCantonal + ')');
			civilDataEventNotifier.notifyEntrepriseChange(noCantonal);
		}
		else {
			LOGGER.info("Demande de réindexation manuelle du tiers n°" + id);
		}
		fiscalDataEventNotifier.notifyTiersChange(id); // on force l'éviction des donées cachées pour tous les types de tiers (pas seulement pour les habitants)

		// on demande la réindexation du tiers
		tiersIndexer.schedule(id);
	}

	/**
	 * Retourne le numéro d'individu d'un tiers à partir de son numéro.
	 *
	 * @param id un numéro de tiers
	 * @return le numéro d'individu du tiers spécifié; ou <b>null</b> si cette information n'est pas disponible.
	 */
	private Long getNumeroIndividu(long id) {
		final Tiers tiers = tiersDAO.get(id);
		final Long indNo;
		if (tiers instanceof PersonnePhysique) {
			final PersonnePhysique pp = (PersonnePhysique) tiers;
			indNo = pp.getNumeroIndividu();
		}
		else {
			indNo = null;
		}
		return indNo;
	}

	/**
	 * Retourne le numéro cantonal du registre des entreprises lié au tiers indiqué par son identifiant fiscal
	 * @param id un numéro fiscal de tiers
	 * @return le numéro cantonal du tiers entreprise (ou établissement) spécifié, ou <b>null</b> si cette information n'est pas disponible
	 */
	private Long getNumeroCantonalRegistreEntreprises(long id) {
		final Tiers tiers = tiersDAO.get(id);
		final Long noCantonal;
		if (tiers instanceof Entreprise) {
			noCantonal = ((Entreprise) tiers).getNumeroEntreprise();
		}
		else if (tiers instanceof Etablissement) {
			noCantonal = ((Etablissement) tiers).getNumeroEtablissement();
		}
		else {
			noCantonal = null;
		}
		return noCantonal;
	}

	public void setTiersIndexer(GlobalTiersIndexer tiersIndexer) {
		this.tiersIndexer = tiersIndexer;
	}

	public void setCivilDataEventNotifier(CivilDataEventNotifier civilDataEventNotifier) {
		this.civilDataEventNotifier = civilDataEventNotifier;
	}

	public void setFiscalDataEventNotifier(FiscalDataEventNotifier fiscalDataEventNotifier) {
		this.fiscalDataEventNotifier = fiscalDataEventNotifier;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}
}
