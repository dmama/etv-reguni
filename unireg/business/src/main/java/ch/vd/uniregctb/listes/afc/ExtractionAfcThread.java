package ch.vd.uniregctb.listes.afc;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.ListesThread;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.HorsCanton;
import ch.vd.uniregctb.metier.assujettissement.HorsSuisse;
import ch.vd.uniregctb.metier.assujettissement.SourcierPur;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

public class ExtractionAfcThread extends ListesThread<ExtractionAfcResults> {

	private final int periodeFiscale;
	private final TypeExtractionAfc mode;

	public ExtractionAfcThread(BlockingQueue<List<Long>> queue, StatusManager status, AtomicInteger compteur, ServiceCivilCacheWarmer serviceCivilCacheWarmer, TiersService tiersService,
	                           ServiceInfrastructureService infraService, PlatformTransactionManager transactionManager, TiersDAO tiersDAO, HibernateTemplate hibernateTemplate, RegDate dateTraitement,
	                           int periodeFiscale, TypeExtractionAfc mode, int nbThreads) {
		super(queue, status, compteur, serviceCivilCacheWarmer, tiersService, transactionManager, tiersDAO, hibernateTemplate, new ExtractionAfcResults(dateTraitement, periodeFiscale, mode, nbThreads, tiersService, infraService));
		this.periodeFiscale = periodeFiscale;
		this.mode = mode;
	}

	@Override
	protected final void traiteContribuable(Contribuable ctb) throws Exception {

		final List<Assujettissement> assujettissements = Assujettissement.determine(ctb, periodeFiscale);
		final TypeAssujettissement typeAssujettissement;
		if (mode == TypeExtractionAfc.REVENU) {
			typeAssujettissement = acceptePourRevenu(assujettissements);
		}
		else if (mode == TypeExtractionAfc.FORTUNE) {
			typeAssujettissement = acceptePourFortune(assujettissements, periodeFiscale);
		}
		else {
			throw new IllegalArgumentException("Mode non-supporté : " + mode);
		}

		switch (typeAssujettissement) {
			case NON_ASSUJETTI:
				getResults().addContribuableNonAssujettiOuSourcierPur(ctb);
				break;

			case LIMITE_HS:
				getResults().addContribuableLimiteHS(ctb);
				break;

			case LIMITE_HC:
				getResults().addContribuableLimiteHC(ctb);
				break;

			case ILLIMITE:
				super.traiteContribuable(ctb);
				break;

			default:
				throw new IllegalArgumentException("Positionnement " + typeAssujettissement + " non supporté!");
		}
	}

	private static enum TypeAssujettissement {
		NON_ASSUJETTI,
		ILLIMITE,
		LIMITE_HS,
		LIMITE_HC
	}

	/**
	 * Un contribuable sera listé dans l'extraction de type "revenu" s'il possède au moins
	 * une journée d'assujettissement IFD sur la période fiscale considérée
	 * @param assujettissements liste d'assujettissement pour l'année de la période fiscale
	 * @return <code>true</code> si le contribuable doit faire partie de la liste "revenu", <code>false</code> sinon
	 */
	private static TypeAssujettissement acceptePourRevenu(List<Assujettissement> assujettissements) {

		// pas d'assujettissement du tout...
		if (assujettissements == null || assujettissements.size() == 0) {
			return TypeAssujettissement.NON_ASSUJETTI;
		}

		// les hors-canton et sourciers purs sont exclus
		boolean trouveHorsCanton = false;
		boolean trouveHorsSuisse = false;
		for (Assujettissement a : assujettissements) {
			final boolean sourcierPur = a instanceof SourcierPur;
			final boolean horsCanton = a instanceof HorsCanton;
			final boolean horsSuisse = a instanceof HorsSuisse;
			trouveHorsCanton |= horsCanton;
			trouveHorsSuisse |= horsSuisse;
			if (sourcierPur || horsCanton || horsSuisse) {
				continue;
			}
			return TypeAssujettissement.ILLIMITE;
		}
		return trouveHorsSuisse ? TypeAssujettissement.LIMITE_HS : (trouveHorsCanton ? TypeAssujettissement.LIMITE_HC : TypeAssujettissement.NON_ASSUJETTI);
	}

	/**
	 * Un contribuable sera listé dans l'extraction de type "fortune" s'il possède un
	 * assujettissement (limité ou illimité) au 31 décembre de la période fiscale considérée
	 * @param assujettissements liste d'assujettissements pour l'année de la période fiscale
	 * @param periodeFiscale période fiscale de référence
	 * @return <code>true</code> si le contribuable doit faire partie de la liste "fortune", <code>false</code> sinon
	 */
	private static TypeAssujettissement acceptePourFortune(List<Assujettissement> assujettissements, int periodeFiscale) {

		// pas d'assujettissement du tout...
		if (assujettissements == null || assujettissements.size() == 0) {
			return TypeAssujettissement.NON_ASSUJETTI;
		}

		// on cherge un assujettissement au 31.12
		final Assujettissement assujettissement = DateRangeHelper.rangeAt(assujettissements, RegDate.get(periodeFiscale, 12, 31));
		if (assujettissement == null || assujettissement instanceof SourcierPur) {
			// pas d'assujettissement au 31.12, ou sourcier pur (ignoré)
			return TypeAssujettissement.NON_ASSUJETTI;
		}

		if (assujettissement instanceof HorsCanton) {
			return TypeAssujettissement.LIMITE_HC;
		}
		else if (assujettissement instanceof HorsSuisse) {
			return TypeAssujettissement.LIMITE_HS;
		}
		else {
			return TypeAssujettissement.ILLIMITE;
		}
	}
}
