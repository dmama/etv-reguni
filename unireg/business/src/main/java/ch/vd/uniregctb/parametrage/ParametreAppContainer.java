package ch.vd.uniregctb.parametrage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang3.tuple.Pair;

import ch.vd.registre.base.utils.Assert;

class ParametreAppContainer implements ParametreAppAccessor {

	private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
	private final Map<ParametreEnum, ParametreApp> parametres = new EnumMap<>(ParametreEnum.class);

	/**
	 * Charge le container depuis une liste de paramètres tirés de la base de données
	 * @param parametres les paramètres à charger
	 */
	void load(Collection<ParametreApp> parametres) {
		final Lock lock = rwLock.writeLock();
		lock.lock();
		try {
			for (ParametreApp param : parametres) {
				final ParametreEnum key = ParametreEnum.valueOf(param.getNom());
				this.parametres.put(key, param);
			}
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * Assigne une nouvelle valeur
	 * @param key la clé du paramètre
	 * @param value la valeur associée
	 */
	void put(ParametreEnum key, ParametreApp value) {
		final Lock lock = rwLock.writeLock();
		lock.lock();
		try {
			parametres.put(key, value);
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * Récupère la valeur associée à la clé
	 * @param key clé
	 * @return la valeur (sous forme de {@link String})
	 */
	String get(ParametreEnum key) {
		final Lock lock = rwLock.readLock();
		lock.lock();
		try {
			final ParametreApp p = parametres.get(key);
			return p != null ? p.getValeur() : null;
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * @return une photos des pairs clé/valeur actuelles
	 */
	Collection<Pair<String, String>> getValues() {
		final Lock lock = rwLock.readLock();
		lock.lock();
		try {
			final List<Pair<String, String>> liste = new ArrayList<>(parametres.size());
			for (ParametreApp pa : parametres.values()) {
				liste.add(Pair.of(pa.getNom(), pa.getValeur()));
			}
			return liste;
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * Initialize le container en plaçant la valeur par défaut de chaque paramètre
	 */
	void initDefaults() {
		final Lock lock = rwLock.writeLock();
		lock.lock();
		try {
			for (ParametreEnum key : ParametreEnum.values()) {
				parametres.put(key, new ParametreApp(key.name(), key.getDefaut()));
			}
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * @param param clé
	 * @return <code></code>
	 */
	boolean isPresent(ParametreEnum param) {
		return parametres.containsKey(param);
	}

	@Override
	public String getDefaut(ParametreEnum param) {
		return param.getDefaut();
	}

	@Override
	public Integer getDelaiAttenteDeclarationImpotPersonneDecedee() {
		return Integer.parseInt(get(ParametreEnum.delaiAttenteDeclarationImpotPersonneDecedee));
	}

	@Override
	public Integer getDelaiRetourDeclarationImpotPPEmiseManuellement() {
		return Integer.parseInt(get(ParametreEnum.delaiRetourDeclarationImpotPPEmiseManuellement));
	}

	@Override
	public Integer getDelaiRetourDeclarationImpotPMEmiseManuellement() {
		return Integer.parseInt(get(ParametreEnum.delaiRetourDeclarationImpotPMEmiseManuellement));
	}

	@Override
	public Integer getDelaiCadevImpressionDeclarationImpot() {
		return Integer.parseInt(get(ParametreEnum.delaiCadevImpressionDeclarationImpot));
	}

	@Override
	public Integer getDelaiCadevImpressionListesRecapitulatives() {
		return Integer.parseInt(get(ParametreEnum.delaiCadevImpressionListesRecapitulatives));
	}

	@Override
	public Integer getDelaiCadevImpressionLettreBienvenue() {
		return Integer.parseInt(get(ParametreEnum.delaiCadevImpressionLettreBienvenue));
	}

	@Override
	public Integer getDelaiEcheanceSommationDeclarationImpotPP() {
		return Integer.parseInt(get(ParametreEnum.delaiEcheanceSommationDeclarationImpotPP));
	}

	@Override
	public Integer getDelaiEcheanceSommationDeclarationImpotPM() {
		return Integer.parseInt(get(ParametreEnum.delaiEcheanceSommationDeclarationImpotPM));
	}

	@Override
	public Integer getDelaiEcheanceSommationListeRecapitualtive() {
		return Integer.parseInt(get(ParametreEnum.delaiEcheanceSommationListeRecapitualtive));
	}

	@Override
	public Integer getDelaiEnvoiSommationDeclarationImpotPP() {
		return Integer.parseInt(get(ParametreEnum.delaiEnvoiSommationDeclarationImpotPP));
	}

	@Override
	public Integer getDelaiEnvoiSommationDeclarationImpotPM() {
		return Integer.parseInt(get(ParametreEnum.delaiEnvoiSommationDeclarationImpotPM));
	}

	@Override
	public Integer[] getDateLimiteEnvoiMasseDeclarationsUtilitePublique() {
		return getValeurPourParametreDeTypeJoursDansAnnee(ParametreEnum.dateLimiteEnvoiMasseDeclarationsUtilitePublique);
	}

	@Override
	public Integer getDelaiEnvoiSommationListeRecapitulative() {
		return Integer.parseInt(get(ParametreEnum.delaiEnvoiSommationListeRecapitulative));
	}

	@Override
	public Integer getDelaiRetentionRapportTravailInactif() {
		return Integer.parseInt(get(ParametreEnum.delaiRetentionRapportTravailInactif));
	}

	@Override
	public Integer getDelaiRetourListeRecapitulative() {
		return Integer.parseInt(get(ParametreEnum.delaiRetourListeRecapitulative));
	}

	@Override
	public Integer getDelaiRetourSommationListeRecapitulative() {
		return Integer.parseInt(get(ParametreEnum.delaiRetourSommationListeRecapitulative));
	}

	@Override
	public Integer[] getFeteNationale() {
		return getValeurPourParametreDeTypeJoursDansAnnee(ParametreEnum.feteNationale);
	}

	@Override
	public Integer getJourDuMoisEnvoiListesRecapitulatives() {
		return Integer.parseInt(get(ParametreEnum.jourDuMoisEnvoiListesRecapitulatives));
	}

	@Override
	public Integer[] getLendemainNouvelAn() {
		return getValeurPourParametreDeTypeJoursDansAnnee(ParametreEnum.lendemainNouvelAn);
	}

	@Override
	public Integer getNbMaxParListe() {
		return Integer.parseInt(get(ParametreEnum.nbMaxParListe));
	}

	@Override
	public Integer getNbMaxParPage() {
		return Integer.parseInt(get(ParametreEnum.nbMaxParPage));
	}

	@Override
	public Integer[] getNoel() {
		return getValeurPourParametreDeTypeJoursDansAnnee(ParametreEnum.noel);
	}

	@Override
	public String getNom(ParametreEnum param) {
		final Lock lock = rwLock.readLock();
		lock.lock();
		try {
			return parametres.get(param).getNom();
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public Integer[] getNouvelAn() {
		return getValeurPourParametreDeTypeJoursDansAnnee(ParametreEnum.nouvelAn);
	}

	@Override
	public Integer getPremierePeriodeFiscalePersonnesPhysiques() {
		return Integer.parseInt(get(ParametreEnum.premierePeriodeFiscalePersonnesPhysiques));
	}

	@Override
	public Integer getPremierePeriodeFiscalePersonnesMorales() {
		return Integer.parseInt(get(ParametreEnum.premierePeriodeFiscalePersonnesMorales));
	}

	@Override
	public Integer getPremierePeriodeFiscaleDeclarationsPersonnesMorales() {
		return Integer.parseInt(get(ParametreEnum.premierePeriodeFiscaleDeclarationPersonnesMorales));
	}

	@Override
	public Integer[] getDateExclusionDecedeEnvoiDI() {
		return getValeurPourParametreDeTypeJoursDansAnnee(ParametreEnum.dateExclusionDecedeEnvoiDI);
	}

	@Override
	public Integer getAnneeMinimaleForDebiteur() {
		return Integer.parseInt(get(ParametreEnum.anneeMinimaleForDebiteur));
	}

	@Override
	public Integer getAgeRentierFemme() {
		return Integer.parseInt(get(ParametreEnum.ageRentierFemme));
	}

	@Override
	public Integer getAgeRentierHomme() {
		return Integer.parseInt(get(ParametreEnum.ageRentierHomme));
	}

	@Override
	public Integer getDelaiMinimalRetourDeclarationImpotPM() {
		return Integer.parseInt(get(ParametreEnum.delaiMinimalRetourDeclarationImpotPM));
	}

	@Override
	public Integer[] getDateDebutEnvoiLettresBienvenue() {
		return getValeurPourParametreDeTypeDate(ParametreEnum.dateDebutEnvoiLettresBienvenue);
	}

	@Override
	public Integer getDelaiRetourLettreBienvenue() {
		return Integer.parseInt(get(ParametreEnum.delaiRetourLettreBienvenue));
	}

	@Override
	public Integer getTailleTrouAssujettissementPourNouvelleLettreBienvenue() {
		return Integer.parseInt(get(ParametreEnum.tailleTrouAssujettissementPourNouvelleLettreBienvenue));
	}

	private Integer[] getValeurPourParametreDeTypeJoursDansAnnee(ParametreEnum p) {
		Assert.isEqual(ParametreEnum.Type.jourDansAnnee, p.getType());
		return (Integer[]) p.convertirStringVersValeurTypee(get(p));
	}

	private Integer[] getValeurPourParametreDeTypeDate(ParametreEnum p) {
		Assert.isEqual(ParametreEnum.Type.date, p.getType());
		return (Integer[]) p.convertirStringVersValeurTypee(get(p));
	}

	/**
	 * Remet à la valeur par défaut tous les paramètres "resetables" (voir {@link ParametreEnum#isResetable()})
	 */
	public void reset() {
		final Lock lock = rwLock.writeLock();
		lock.lock();
		try {
			for (ParametreEnum p : ParametreEnum.values()) {
				if (p.isResetable()) {
					setValeur(p, getDefaut(p));
				}
			}
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public void setDelaiAttenteDeclarationImpotPersonneDecedee(Integer val) {
		setValeur(ParametreEnum.delaiAttenteDeclarationImpotPersonneDecedee, val.toString());
	}

	@Override
	public void setDelaiRetourDeclarationImpotPPEmiseManuellement(Integer val) {
		setValeur(ParametreEnum.delaiRetourDeclarationImpotPPEmiseManuellement, val.toString());
	}

	@Override
	public void setDelaiRetourDeclarationImpotPMEmiseManuellement(Integer val) {
		setValeur(ParametreEnum.delaiRetourDeclarationImpotPMEmiseManuellement, val.toString());
	}

	@Override
	public void setDelaiCadevImpressionDeclarationImpot(Integer val) {
		setValeur(ParametreEnum.delaiCadevImpressionDeclarationImpot, val.toString());
	}

	@Override
	public void setDelaiCadevImpressionListesRecapitulatives(Integer val) {
		setValeur(ParametreEnum.delaiCadevImpressionListesRecapitulatives, val.toString());
	}

	@Override
	public void setDelaiCadevImpressionLettreBienvenue(Integer val) {
		setValeur(ParametreEnum.delaiCadevImpressionLettreBienvenue, val.toString());
	}

	@Override
	public void setDelaiEcheanceSommationDeclarationImpotPP(Integer val) {
		setValeur(ParametreEnum.delaiEcheanceSommationDeclarationImpotPP, val.toString());
	}

	@Override
	public void setDelaiEcheanceSommationDeclarationImpotPM(Integer val) {
		setValeur(ParametreEnum.delaiEcheanceSommationDeclarationImpotPM, val.toString());
	}

	@Override
	public void setDateLimiteEnvoiMasseDeclarationsUtilitePublique(Integer[] val) {
		Assert.isEqual(2, val.length);
		setValeur(ParametreEnum.dateLimiteEnvoiMasseDeclarationsUtilitePublique, String.valueOf(val[0]) + '.' + val[1]);
	}

	@Override
	public void setDelaiEcheanceSommationListeRecapitualtive(Integer val) {
		setValeur(ParametreEnum.delaiEcheanceSommationListeRecapitualtive, val.toString());
	}

	@Override
	public void setDelaiEnvoiSommationDeclarationImpotPP(Integer val) {
		setValeur(ParametreEnum.delaiEnvoiSommationDeclarationImpotPP, val.toString());
	}

	@Override
	public void setDelaiEnvoiSommationListeRecapitulative(Integer val) {
		setValeur(ParametreEnum.delaiEnvoiSommationListeRecapitulative, val.toString());
	}

	@Override
	public void setDelaiRetentionRapportTravailInactif(Integer val) {
		setValeur(ParametreEnum.delaiRetentionRapportTravailInactif, val.toString());
	}

	@Override
	public void setDelaiRetourListeRecapitulative(Integer val) {
		setValeur(ParametreEnum.delaiRetourListeRecapitulative, val.toString());
	}

	@Override
	public void setDelaiRetourSommationListeRecapitulative(Integer val) {
		setValeur(ParametreEnum.delaiRetourSommationListeRecapitulative, val.toString());
	}

	@Override
	public void setFeteNationale(Integer[] val) {
		Assert.isEqual(2, val.length);
		setValeur(ParametreEnum.feteNationale, String.valueOf(val[0]) + '.' + val[1]);
	}

	@Override
	public void setJourDuMoisEnvoiListesRecapitulatives(Integer val) {
		setValeur(ParametreEnum.jourDuMoisEnvoiListesRecapitulatives, val.toString());
	}

	@Override
	public void setLendemainNouvelAn(Integer[] val) {
		Assert.isEqual(2, val.length);
		setValeur(ParametreEnum.lendemainNouvelAn, String.valueOf(val[0]) + '.' + val[1]);
	}

	@Override
	public void setNbMaxParListe(Integer val) {
		setValeur(ParametreEnum.nbMaxParListe, val.toString());
	}

	@Override
	public void setNbMaxParPage(Integer val) {
		setValeur(ParametreEnum.nbMaxParPage, val.toString());
	}

	@Override
	public void setNoel(Integer[] val) {
		Assert.isEqual(2, val.length);
		setValeur(ParametreEnum.noel, String.valueOf(val[0]) + '.' + val[1]);
	}

	@Override
	public void setNouvelAn(Integer[] val) {
		Assert.isEqual(2, val.length);
		setValeur(ParametreEnum.nouvelAn, String.valueOf(val[0]) + '.' + val[1]);
	}

	@Override
	public void setPremierePeriodeFiscalePersonnesPhysiques(Integer val) {
		setValeur(ParametreEnum.premierePeriodeFiscalePersonnesPhysiques, val.toString());
	}

	@Override
	public void setPremierePeriodeFiscalePersonnesMorales(Integer val) {
		setValeur(ParametreEnum.premierePeriodeFiscalePersonnesMorales, val.toString());
	}

	@Override
	public void setPremierePeriodeFiscaleDeclarationsPersonnesMorales(Integer val) {
		setValeur(ParametreEnum.premierePeriodeFiscaleDeclarationPersonnesMorales, val.toString());
	}

	@Override
	public void setAnneeMinimaleForDebiteur(Integer val) {
		setValeur(ParametreEnum.anneeMinimaleForDebiteur, val.toString());
	}

	@Override
	public void setValeur(ParametreEnum param, String valeur) {
		// La validité de la valeur est verifiée dans formaterValeur()
		final Lock lock = rwLock.writeLock();
		lock.lock();
		try {
			parametres.get(param).setValeur(param.formaterValeur(valeur));
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public void setDateExclusionDecedeEnvoiDI(Integer[] val) {
		Assert.isEqual(2, val.length);
		setValeur(ParametreEnum.dateExclusionDecedeEnvoiDI, String.valueOf(val[0]) + '.' + val[1]);
	}

	@Override
	public void setAgeRentierFemme(Integer val) {
		setValeur(ParametreEnum.ageRentierFemme, val.toString());
	}

	@Override
	public void setAgeRentierHomme(Integer val) {
		setValeur(ParametreEnum.ageRentierHomme, val.toString());
	}

	@Override
	public void setDelaiMinimalRetourDeclarationImpotPM(Integer val) {
		setValeur(ParametreEnum.delaiMinimalRetourDeclarationImpotPM, val.toString());
	}

	@Override
	public void setDelaiEnvoiSommationDeclarationImpotPM(Integer val) {
		setValeur(ParametreEnum.delaiEnvoiSommationDeclarationImpotPM, val.toString());
	}

	@Override
	public void setDateDebutEnvoiLettresBienvenue(Integer[] val) {
		Assert.isEqual(3, val.length);
		setValeur(ParametreEnum.dateDebutEnvoiLettresBienvenue, String.valueOf(val[0]) + '.' + val[1] + '.' + val[2]);
	}

	@Override
	public void setDelaiRetourLettreBienvenue(Integer val) {
		setValeur(ParametreEnum.delaiRetourLettreBienvenue, val.toString());
	}

	@Override
	public void setTailleTrouAssujettissementPourNouvelleLettreBienvenue(Integer val) {
		setValeur(ParametreEnum.tailleTrouAssujettissementPourNouvelleLettreBienvenue, val.toString());
	}
}
