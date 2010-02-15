package ch.vd.uniregctb.contribuableAssocie.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.contribuableAssocie.view.ContribuableAssocieEditView;
import ch.vd.uniregctb.contribuableAssocie.view.ContribuableAssocieListView;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.rt.view.SourcierListView;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.manager.TiersManager;
import ch.vd.uniregctb.utils.WebContextUtils;

public class ContribuableAssocieEditManagerImpl extends TiersManager  implements ContribuableAssocieEditManager{

	public ContribuableAssocieEditView get(Long numeroDebiteur, Long numeroContribuable) throws AdressesResolutionException {

		DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiersService.getTiers(numeroDebiteur);

		if (debiteur == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.debiteur.inexistant" , null,  WebContextUtils.getDefaultLocale()));
		}

		ContribuableAssocieEditView contribuableAssocieEditView =  new ContribuableAssocieEditView();
		//vérification des droits
//		contribuableAssocieEditView.setAllowed(checkDroitEdit(debiteur));
//		if(contribuableAssocieEditView.isAllowed()){
			//Debiteur
			TiersGeneralView debiteurView = tiersGeneralManager.get(debiteur, true);
			contribuableAssocieEditView.setDebiteur(debiteurView);

			//Contribuable
			Contribuable contribuable = (Contribuable) tiersService.getTiers(numeroContribuable);
			if (contribuable == null) {
				throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.contribuable.inexistant" , null,  WebContextUtils.getDefaultLocale()));
			}

			TiersGeneralView contribuableView = tiersGeneralManager.get(contribuable);
			contribuableAssocieEditView.setContribuable(contribuableView);
//		}

		return contribuableAssocieEditView;
	}

	/**
	 * Persiste le contact impôt source entre le débiteur et le contribuable
	 * @param contribuableAssocieEditView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void save(ContribuableAssocieEditView contribuableAssocieEditView) {
		DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiersService.getTiers(contribuableAssocieEditView.getDebiteur().getNumero());
		Contribuable contribuable = (Contribuable) tiersService.getTiers(contribuableAssocieEditView.getContribuable().getNumero());
		tiersService.addContactImpotSource(debiteur, contribuable);
	}

	public ContribuableAssocieListView getContribuableList(Long numeroDpi) {
		DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(numeroDpi) ;
		ContribuableAssocieListView bean = new ContribuableAssocieListView();
		TiersGeneralView dpiView = tiersGeneralManager.get(dpi, true);
		bean.setDebiteur(dpiView);
		bean.setTypeRechercheDuNom(SourcierListView.TypeRecherche.EST_EXACTEMENT);
		bean.setTypeTiers(SourcierListView.TypeTiers.CONTRIBUABLE);
		bean.setNumeroDebiteur(numeroDpi);
		return bean;
	}

}
