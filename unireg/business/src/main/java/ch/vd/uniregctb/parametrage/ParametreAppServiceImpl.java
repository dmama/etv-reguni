package ch.vd.uniregctb.parametrage;

import static ch.vd.uniregctb.parametrage.ParametreEnum.delaiAttenteDeclarationImpotPersonneDecedee;
import static ch.vd.uniregctb.parametrage.ParametreEnum.delaiCadevImpressionDeclarationImpot;
import static ch.vd.uniregctb.parametrage.ParametreEnum.delaiCadevImpressionListesRecapitulatives;
import static ch.vd.uniregctb.parametrage.ParametreEnum.delaiEcheanceSommationDeclarationImpot;
import static ch.vd.uniregctb.parametrage.ParametreEnum.delaiEcheanceSommationListeRecapitualtive;
import static ch.vd.uniregctb.parametrage.ParametreEnum.delaiEnvoiSommationDeclarationImpot;
import static ch.vd.uniregctb.parametrage.ParametreEnum.delaiEnvoiSommationListeRecapitulative;
import static ch.vd.uniregctb.parametrage.ParametreEnum.delaiRetentionRapportTravailInactif;
import static ch.vd.uniregctb.parametrage.ParametreEnum.delaiRetourDeclarationImpotEmiseManuellement;
import static ch.vd.uniregctb.parametrage.ParametreEnum.delaiRetourListeRecapitulative;
import static ch.vd.uniregctb.parametrage.ParametreEnum.delaiRetourSommationListeRecapitulative;
import static ch.vd.uniregctb.parametrage.ParametreEnum.feteNationale;
import static ch.vd.uniregctb.parametrage.ParametreEnum.jourDuMoisEnvoiListesRecapitulatives;
import static ch.vd.uniregctb.parametrage.ParametreEnum.lendemainNouvelAn;
import static ch.vd.uniregctb.parametrage.ParametreEnum.nbMaxParListe;
import static ch.vd.uniregctb.parametrage.ParametreEnum.nbMaxParPage;
import static ch.vd.uniregctb.parametrage.ParametreEnum.noel;
import static ch.vd.uniregctb.parametrage.ParametreEnum.nouvelAn;
import static ch.vd.uniregctb.parametrage.ParametreEnum.premierePeriodeFiscale;

import java.util.HashMap;
import java.util.Map;

import org.acegisecurity.providers.AbstractAuthenticationToken;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.parametrage.ParametreApp;
import ch.vd.uniregctb.parametrage.ParametreAppDAO;
import ch.vd.uniregctb.parametrage.ParametreEnum;

/**
 * Classe métier representant les paramètres de l'application.
 *
 * @author xsifnr
 *
 */
public class ParametreAppServiceImpl implements ParametreAppService, InitializingBean {

	/**
	 * Un logger pour {@link ParametreAppServiceImpl}
	 */
	private static final Logger LOGGER = Logger.getLogger(ParametreAppServiceImpl.class);

	static {
		/*
		 * Verification de l'adéquation de la classe avec ParametreEnum. Cette classe doit définir une propriété JavaBean pour chaque valeur
		 * possible de ParametreEnum
		 */
		assert
			ParametreEnum.isClassCompatible(ParametreAppServiceImpl.class) :
			ParametreEnum.getMissingPropertiesMessage(ParametreAppServiceImpl.class)
		;
		LOGGER.debug(ParametreAppServiceImpl.class.getName() + " est en adequation avec " + ParametreEnum.class.getName());
	}

	ParametreAppDAO dao;

	Map<ParametreEnum, ParametreApp> parametres = new HashMap<ParametreEnum, ParametreApp>();

	/**
	 * Initialisation des parametres
	 *
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		assert dao != null;
		assert parametres != null;

		final AbstractAuthenticationToken auth = AuthenticationHelper.getAuthentication();
		if (auth == null) {
			AuthenticationHelper.setPrincipal(AuthenticationHelper.SYSTEM_USER);
		}
		try {
			for (ParametreApp p : dao.getAll()) {
				try {
					parametres.put(ParametreEnum.valueOf(p.getNom()), p);
				}
				catch (IllegalArgumentException e) {
					LOGGER.error(p.getNom() + " n'est pas défini dans ParametreEnum", e);
					throw e;
				}
			}
			for (ParametreEnum p : ParametreEnum.values()) {
				ParametreApp pa = parametres.get(p);
				if (pa == null) {
					pa = new ParametreApp(p.name(), p.getDefaut());
					parametres.put(p, pa);
					dao.save(pa);
				}
			}
		}
		finally {
			if (auth == null) {
				AuthenticationHelper.resetAuthentication();
			}
		}
	}

	public ParametreAppDAO getDao() {
		return dao;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.vd.uniregctb.parametrage.ParametreAppService#getDefaut(ch.vd.uniregctb.parametrage.ParametreEnum)
	 */
	public String getDefaut(ParametreEnum param) {
		return param.getDefaut();
	}

	public Integer getDelaiAttenteDeclarationImpotPersonneDecedee() {
		return Integer.parseInt(parametres.get(delaiAttenteDeclarationImpotPersonneDecedee).getValeur());
	}

	public Integer getDelaiRetourDeclarationImpotEmiseManuellement() {
		return Integer.parseInt(parametres.get(delaiRetourDeclarationImpotEmiseManuellement).getValeur());
	}

	public Integer getDelaiCadevImpressionDeclarationImpot() {
		return Integer.parseInt(parametres.get(delaiCadevImpressionDeclarationImpot).getValeur());
	}

	public Integer getDelaiCadevImpressionListesRecapitulatives() {
		return Integer.parseInt(parametres.get(delaiCadevImpressionListesRecapitulatives).getValeur());
	}

	public Integer getDelaiEcheanceSommationDeclarationImpot() {
		return Integer.parseInt(parametres.get(delaiEcheanceSommationDeclarationImpot).getValeur());
	}

	public Integer getDelaiEcheanceSommationListeRecapitualtive() {
		return Integer.parseInt(parametres.get(delaiEcheanceSommationListeRecapitualtive).getValeur());
	}

	public Integer getDelaiEnvoiSommationDeclarationImpot() {
		return Integer.parseInt(parametres.get(delaiEnvoiSommationDeclarationImpot).getValeur());
	}

	public Integer getDelaiEnvoiSommationListeRecapitulative() {
		return Integer.parseInt(parametres.get(delaiEnvoiSommationListeRecapitulative).getValeur());
	}

	public Integer getDelaiRetentionRapportTravailInactif() {
		return Integer.parseInt(parametres.get(delaiRetentionRapportTravailInactif).getValeur());
	}

	public Integer getDelaiRetourListeRecapitulative() {
		return Integer.parseInt(parametres.get(delaiRetourListeRecapitulative).getValeur());
	}

	public Integer getDelaiRetourSommationListeRecapitulative() {
		return Integer.parseInt(parametres.get(delaiRetourSommationListeRecapitulative).getValeur());
	}

	public Integer[] getFeteNationale() {
		return getValeurPourParametreDeTypeJoursDansAnnee(feteNationale);
	}

	public Integer getJourDuMoisEnvoiListesRecapitulatives() {
		return Integer.parseInt(parametres.get(jourDuMoisEnvoiListesRecapitulatives).getValeur());
	}

	public Integer[] getLendemainNouvelAn() {
		return getValeurPourParametreDeTypeJoursDansAnnee(lendemainNouvelAn);
	}

	public Integer getNbMaxParListe() {
		return Integer.parseInt(parametres.get(nbMaxParListe).getValeur());
	}

	public Integer getNbMaxParPage() {
		return Integer.parseInt(parametres.get(nbMaxParPage).getValeur());
	}

	public Integer[] getNoel() {
		return getValeurPourParametreDeTypeJoursDansAnnee(noel);
	}

	public String getNom(ParametreEnum param) {
		return parametres.get(param).getNom();
	}

	public Integer[] getNouvelAn() {
		return getValeurPourParametreDeTypeJoursDansAnnee(nouvelAn);
	}

	public Integer getPremierePeriodeFiscale() {
		return Integer.parseInt(parametres.get(premierePeriodeFiscale).getValeur());
	}

	public String getValeur(ParametreEnum param) {
		return parametres.get(param).getValeur();
	}

	private Integer[] getValeurPourParametreDeTypeJoursDansAnnee(ParametreEnum p) {
		return (Integer[])p.convertirStringVersValeurTypee(parametres.get(p).getValeur());
	}

	public void reset() {
		for (ParametreEnum p : ParametreEnum.values()) {
			if(p.isResetable()) {
				setValeur(p, getDefaut(p));
			}
		}
		save();
	}

	public void save() {
		for (ParametreApp p : parametres.values()) {
			ParametreApp pa = dao.get(p.getNom());
			pa.setValeur(p.getValeur());
		}
	}

	public void setDao(ParametreAppDAO dao) {
		this.dao = dao;
	}

	public void setDelaiAttenteDeclarationImpotPersonneDecedee(Integer val) {
		setValeur(delaiAttenteDeclarationImpotPersonneDecedee, val.toString());
	}

	public void setDelaiRetourDeclarationImpotEmiseManuellement(Integer val) {
		setValeur(delaiRetourDeclarationImpotEmiseManuellement, val.toString());
	}

	public void setDelaiCadevImpressionDeclarationImpot(Integer val) {
		setValeur(delaiCadevImpressionDeclarationImpot, val.toString());
		{
		}
	}

	public void setDelaiCadevImpressionListesRecapitulatives(Integer val) {
		setValeur(delaiCadevImpressionListesRecapitulatives, val.toString());
		{
		}
	}

	public void setDelaiEcheanceSommationDeclarationImpot(Integer val) {
		setValeur(delaiEcheanceSommationDeclarationImpot, val.toString());
	}

	public void setDelaiEcheanceSommationListeRecapitualtive(Integer val) {
		setValeur(delaiEcheanceSommationListeRecapitualtive, val.toString());
	}

	public void setDelaiEnvoiSommationDeclarationImpot(Integer val) {
		setValeur(delaiEnvoiSommationDeclarationImpot, val.toString());
	}

	public void setDelaiEnvoiSommationListeRecapitulative(Integer val) {
		setValeur(delaiEnvoiSommationListeRecapitulative, val.toString());
	}

	public void setDelaiRetentionRapportTravailInactif(Integer val) {
		setValeur(delaiRetentionRapportTravailInactif, val.toString());
	}

	public void setDelaiRetourListeRecapitulative(Integer val) {
		setValeur(delaiRetourListeRecapitulative, val.toString());
	}

	public void setDelaiRetourSommationListeRecapitulative(Integer val) {
		setValeur(delaiRetourSommationListeRecapitulative, val.toString());
	}

	public void setFeteNationale(Integer[] val) {
		assert val.length == 2;
		setValeur(feteNationale, "" + val[0] + "." + val[1]);
	}

	public void setJourDuMoisEnvoiListesRecapitulatives(Integer val) {
		setValeur(jourDuMoisEnvoiListesRecapitulatives, val.toString());
	}

	public void setLendemainNouvelAn(Integer[] val) {
		assert val.length == 2;
		setValeur(lendemainNouvelAn, "" + val[0] + "." + val[1]);
	}

	public void setNbMaxParListe(Integer val) {
		setValeur(nbMaxParListe, val.toString());
	}

	public void setNbMaxParPage(Integer val) {
		setValeur(nbMaxParPage, val.toString());
	}

	public void setNoel(Integer[] val) {
		assert val.length == 2;
		setValeur(noel, "" + val[0] + "." + val[1]);
	}

	public void setNouvelAn(Integer[] val) {
		assert val.length == 2;
		setValeur(nouvelAn, "" + val[0] + "." + val[1]);
	}

	public void setPremierePeriodeFiscale(Integer val) {
		setValeur(premierePeriodeFiscale, val.toString());
	}

	public void setValeur(ParametreEnum param, String valeur) {
		// La validité de la valeur est verifiée dans formaterValeur()
		parametres.get(param).setValeur(param.formaterValeur(valeur));
	}

	public static void main(String[] args) {

	}

}
