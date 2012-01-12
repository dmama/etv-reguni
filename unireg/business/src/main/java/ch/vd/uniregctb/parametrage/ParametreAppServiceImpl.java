package ch.vd.uniregctb.parametrage;

import java.util.EnumMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.uniregctb.common.AuthenticationHelper;

import static ch.vd.uniregctb.parametrage.ParametreEnum.*;

/**
 * Classe métier representant les paramètres de l'application.
 *
 * @author xsifnr
 */
public class ParametreAppServiceImpl implements ParametreAppService, InitializingBean {

	/**
	 * Un logger pour {@link ParametreAppServiceImpl}
	 */
	private static final Logger LOGGER = Logger.getLogger(ParametreAppServiceImpl.class);

	private ParametreAppDAO dao;
	private PlatformTransactionManager transactionManager;

	private final Map<ParametreEnum, ParametreApp> parametres = new EnumMap<ParametreEnum, ParametreApp>(ParametreEnum.class);

	/**
	 * Initialisation des parametres
	 *
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		assert dao != null;
		assert parametres != null;

		AuthenticationHelper.pushPrincipal(AuthenticationHelper.SYSTEM_USER);
		try {
			final TransactionTemplate template = new TransactionTemplate(transactionManager);
			template.execute(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
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
					return null;
				}
			});
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.vd.uniregctb.parametrage.ParametreAppService#getDefaut(ch.vd.uniregctb.parametrage.ParametreEnum)
	 */

	@Override
	public String getDefaut(ParametreEnum param) {
		return param.getDefaut();
	}

	@Override
	public Integer getDelaiAttenteDeclarationImpotPersonneDecedee() {
		return Integer.parseInt(parametres.get(delaiAttenteDeclarationImpotPersonneDecedee).getValeur());
	}

	@Override
	public Integer getDelaiRetourDeclarationImpotEmiseManuellement() {
		return Integer.parseInt(parametres.get(delaiRetourDeclarationImpotEmiseManuellement).getValeur());
	}

	@Override
	public Integer getDelaiCadevImpressionDeclarationImpot() {
		return Integer.parseInt(parametres.get(delaiCadevImpressionDeclarationImpot).getValeur());
	}

	@Override
	public Integer getDelaiCadevImpressionListesRecapitulatives() {
		return Integer.parseInt(parametres.get(delaiCadevImpressionListesRecapitulatives).getValeur());
	}

	@Override
	public Integer getDelaiEcheanceSommationDeclarationImpot() {
		return Integer.parseInt(parametres.get(delaiEcheanceSommationDeclarationImpot).getValeur());
	}

	@Override
	public Integer getDelaiEcheanceSommationListeRecapitualtive() {
		return Integer.parseInt(parametres.get(delaiEcheanceSommationListeRecapitualtive).getValeur());
	}

	@Override
	public Integer getDelaiEnvoiSommationDeclarationImpot() {
		return Integer.parseInt(parametres.get(delaiEnvoiSommationDeclarationImpot).getValeur());
	}

	@Override
	public Integer getDelaiEnvoiSommationListeRecapitulative() {
		return Integer.parseInt(parametres.get(delaiEnvoiSommationListeRecapitulative).getValeur());
	}

	@Override
	public Integer getDelaiRetentionRapportTravailInactif() {
		return Integer.parseInt(parametres.get(delaiRetentionRapportTravailInactif).getValeur());
	}

	@Override
	public Integer getDelaiRetourListeRecapitulative() {
		return Integer.parseInt(parametres.get(delaiRetourListeRecapitulative).getValeur());
	}

	@Override
	public Integer getDelaiRetourSommationListeRecapitulative() {
		return Integer.parseInt(parametres.get(delaiRetourSommationListeRecapitulative).getValeur());
	}

	@Override
	public Integer[] getFeteNationale() {
		return getValeurPourParametreDeTypeJoursDansAnnee(feteNationale);
	}

	@Override
	public Integer getJourDuMoisEnvoiListesRecapitulatives() {
		return Integer.parseInt(parametres.get(jourDuMoisEnvoiListesRecapitulatives).getValeur());
	}

	@Override
	public Integer[] getLendemainNouvelAn() {
		return getValeurPourParametreDeTypeJoursDansAnnee(lendemainNouvelAn);
	}

	@Override
	public Integer getNbMaxParListe() {
		return Integer.parseInt(parametres.get(nbMaxParListe).getValeur());
	}

	@Override
	public Integer getNbMaxParPage() {
		return Integer.parseInt(parametres.get(nbMaxParPage).getValeur());
	}

	@Override
	public Integer[] getNoel() {
		return getValeurPourParametreDeTypeJoursDansAnnee(noel);
	}

	@Override
	public String getNom(ParametreEnum param) {
		return parametres.get(param).getNom();
	}

	@Override
	public Integer[] getNouvelAn() {
		return getValeurPourParametreDeTypeJoursDansAnnee(nouvelAn);
	}

	@Override
	public Integer getPremierePeriodeFiscale() {
		return Integer.parseInt(parametres.get(premierePeriodeFiscale).getValeur());
	}

	@Override
	public Integer[] getDateExclusionDecedeEnvoiDI() {
		return getValeurPourParametreDeTypeJoursDansAnnee(dateExclusionDecedeEnvoiDI);
	}

	@Override
	public Integer getAnneeMinimaleForDebiteur() {
		return Integer.parseInt(parametres.get(ParametreEnum.anneeMinimaleForDebiteur).getValeur());
	}

	@Override
	public String getValeur(ParametreEnum param) {
		return parametres.get(param).getValeur();
	}

	private Integer[] getValeurPourParametreDeTypeJoursDansAnnee(ParametreEnum p) {
		return (Integer[]) p.convertirStringVersValeurTypee(parametres.get(p).getValeur());
	}

	@Override
	public void reset() {
		for (ParametreEnum p : ParametreEnum.values()) {
			if (p.isResetable()) {
				setValeur(p, getDefaut(p));
			}
		}
		save();
	}

	@Override
	public void save() {
		for (ParametreApp p : parametres.values()) {
			ParametreApp pa = dao.get(p.getNom());
			pa.setValeur(p.getValeur());
		}
	}

	public void setDao(ParametreAppDAO dao) {
		this.dao = dao;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Override
	public void setDelaiAttenteDeclarationImpotPersonneDecedee(Integer val) {
		setValeur(delaiAttenteDeclarationImpotPersonneDecedee, val.toString());
	}

	@Override
	public void setDelaiRetourDeclarationImpotEmiseManuellement(Integer val) {
		setValeur(delaiRetourDeclarationImpotEmiseManuellement, val.toString());
	}

	@Override
	public void setDelaiCadevImpressionDeclarationImpot(Integer val) {
		setValeur(delaiCadevImpressionDeclarationImpot, val.toString());
	}

	@Override
	public void setDelaiCadevImpressionListesRecapitulatives(Integer val) {
		setValeur(delaiCadevImpressionListesRecapitulatives, val.toString());
	}

	@Override
	public void setDelaiEcheanceSommationDeclarationImpot(Integer val) {
		setValeur(delaiEcheanceSommationDeclarationImpot, val.toString());
	}

	@Override
	public void setDelaiEcheanceSommationListeRecapitualtive(Integer val) {
		setValeur(delaiEcheanceSommationListeRecapitualtive, val.toString());
	}

	@Override
	public void setDelaiEnvoiSommationDeclarationImpot(Integer val) {
		setValeur(delaiEnvoiSommationDeclarationImpot, val.toString());
	}

	@Override
	public void setDelaiEnvoiSommationListeRecapitulative(Integer val) {
		setValeur(delaiEnvoiSommationListeRecapitulative, val.toString());
	}

	@Override
	public void setDelaiRetentionRapportTravailInactif(Integer val) {
		setValeur(delaiRetentionRapportTravailInactif, val.toString());
	}

	@Override
	public void setDelaiRetourListeRecapitulative(Integer val) {
		setValeur(delaiRetourListeRecapitulative, val.toString());
	}

	@Override
	public void setDelaiRetourSommationListeRecapitulative(Integer val) {
		setValeur(delaiRetourSommationListeRecapitulative, val.toString());
	}

	@Override
	public void setFeteNationale(Integer[] val) {
		assert val.length == 2;
		setValeur(feteNationale, String.valueOf(val[0]) + '.' + val[1]);
	}

	@Override
	public void setJourDuMoisEnvoiListesRecapitulatives(Integer val) {
		setValeur(jourDuMoisEnvoiListesRecapitulatives, val.toString());
	}

	@Override
	public void setLendemainNouvelAn(Integer[] val) {
		assert val.length == 2;
		setValeur(lendemainNouvelAn, String.valueOf(val[0]) + '.' + val[1]);
	}

	@Override
	public void setNbMaxParListe(Integer val) {
		setValeur(nbMaxParListe, val.toString());
	}

	@Override
	public void setNbMaxParPage(Integer val) {
		setValeur(nbMaxParPage, val.toString());
	}

	@Override
	public void setNoel(Integer[] val) {
		assert val.length == 2;
		setValeur(noel, String.valueOf(val[0]) + '.' + val[1]);
	}

	@Override
	public void setNouvelAn(Integer[] val) {
		assert val.length == 2;
		setValeur(nouvelAn, String.valueOf(val[0]) + '.' + val[1]);
	}

	@Override
	public void setPremierePeriodeFiscale(Integer val) {
		setValeur(premierePeriodeFiscale, val.toString());
	}

	@Override
	public void setAnneeMinimaleForDebiteur(Integer val) {
		setValeur(ParametreEnum.anneeMinimaleForDebiteur, val.toString());
	}

	@Override
	public void setValeur(ParametreEnum param, String valeur) {
		// La validité de la valeur est verifiée dans formaterValeur()
		parametres.get(param).setValeur(param.formaterValeur(valeur));
	}

	@Override
	public void setDateExclusionDecedeEnvoiDI(Integer[] val) {
		   assert val.length == 2;
		setValeur(dateExclusionDecedeEnvoiDI, String.valueOf(val[0]) + '.' + val[1]);
	}
}
