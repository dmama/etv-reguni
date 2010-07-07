package ch.vd.uniregctb.registrefoncier;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.tiers.TiersDAO;

public interface RegistreFoncierService {



	public TiersDAO getTiersDAO();

	public void setTiersDAO(TiersDAO tiersDAO);

	public ServiceCivilService getServiceCivilService();

	public void setServiceCivilService(ServiceCivilService serviceCivil);

	public void setAdresseService(AdresseService adresseService);

	public AdresseService getAdresseService();

	/**
	 * Fait le rapprochement entre les contribuables et les données transmises par le registre foncier
	 * @param listeProprietaireFoncier
	 * 					La liste des propriétaires fonciers à rapprocher
	 * @param dateTraitement TODO
	 * @return
	 */
	RapprocherCtbResults rapprocherCtbRegistreFoncier(List<ProprietaireFoncier> listeProprietaireFoncier, StatusManager s, RegDate dateTraitement);

}
