package ch.vd.uniregctb.webservices.tiers3.data.strategy;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.webservices.tiers3.Debiteur;
import ch.vd.uniregctb.webservices.tiers3.ModeCommunication;
import ch.vd.uniregctb.webservices.tiers3.TiersPart;
import ch.vd.uniregctb.webservices.tiers3.WebServiceException;
import ch.vd.uniregctb.webservices.tiers3.data.PeriodiciteBuilder;
import ch.vd.uniregctb.webservices.tiers3.impl.BusinessHelper;
import ch.vd.uniregctb.webservices.tiers3.impl.Context;
import ch.vd.uniregctb.webservices.tiers3.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers3.impl.EnumHelper;

public class DebiteurStrategy extends TiersStrategy<Debiteur> {

	@Override
	public Debiteur newFrom(Tiers right, @Nullable Set<TiersPart> parts, Context context) throws WebServiceException {
		final Debiteur debiteur = new Debiteur();
		initBase(debiteur, right, context);
		initParts(debiteur, right, parts, context);
		return debiteur;
	}

	@Override
	public Debiteur clone(Debiteur right, @Nullable Set<TiersPart> parts) {
		Debiteur debiteur = new Debiteur();
		copyBase(debiteur, right);
		copyParts(debiteur, right, parts, CopyMode.EXCLUSIF);
		return debiteur;
	}

	@Override
	protected void initBase(Debiteur to, Tiers from, Context context) throws WebServiceException {
		super.initBase(to, from, context);

		final DebiteurPrestationImposable debiteur =(DebiteurPrestationImposable) from;
		to.setRaisonSociale(BusinessHelper.getRaisonSociale(debiteur, null, context.adresseService));
		to.setCategorie(EnumHelper.coreToWeb(debiteur.getCategorieImpotSource()));
		to.setModeCommunication(EnumHelper.coreToWeb(debiteur.getModeCommunication()));
		to.setSansRappel(DataHelper.coreToWeb(debiteur.getSansRappel()));
		to.setSansListeRecapitulative(DataHelper.coreToWeb(debiteur.getSansListeRecapitulative()));
		to.setContribuableAssocieId(debiteur.getContribuableId());
		if (to.getModeCommunication() == ModeCommunication.ELECTRONIQUE) {
			to.setLogicielId(debiteur.getLogicielId());
		}
	}

	@Override
	protected void copyBase(Debiteur to, Debiteur from) {
		super.copyBase(to, from);
		to.setRaisonSociale(from.getRaisonSociale());
		to.setCategorie(from.getCategorie());
		to.setModeCommunication(from.getModeCommunication());
		to.setSansRappel(from.isSansRappel());
		to.setSansListeRecapitulative(from.isSansListeRecapitulative());
		to.setContribuableAssocieId(from.getContribuableAssocieId());
		to.setLogicielId(from.getLogicielId());
	}

	@Override
	protected void initParts(Debiteur to, Tiers from, @Nullable Set<TiersPart> parts, Context context) throws WebServiceException {
		super.initParts(to, from, parts, context);

		final DebiteurPrestationImposable debiteur =(DebiteurPrestationImposable) from;
		if (parts != null && parts.contains(TiersPart.PERIODICITES)) {
			initPeriodicites(to, debiteur);
		}
	}

	@Override
	protected void copyParts(Debiteur to, Debiteur from, @Nullable Set<TiersPart> parts, CopyMode mode) {
		super.copyParts(to, from, parts, mode);

		if (parts != null && parts.contains(TiersPart.PERIODICITES)) {
			copyColl(to.getPeriodicites(), from.getPeriodicites());
		}
	}

	private static void initPeriodicites(Debiteur left, DebiteurPrestationImposable right) {
		for (ch.vd.uniregctb.declaration.Periodicite periodicite : right.getPeriodicitesNonAnnules(true)) {
			left.getPeriodicites().add(PeriodiciteBuilder.newPeriodicite(periodicite));
		}
	}
}
