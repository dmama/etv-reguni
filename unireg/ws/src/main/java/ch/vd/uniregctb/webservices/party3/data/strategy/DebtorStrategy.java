package ch.vd.uniregctb.webservices.party3.data.strategy;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.webservices.party3.PartyPart;
import ch.vd.unireg.webservices.party3.WebServiceException;
import ch.vd.unireg.xml.party.debtor.v1.CommunicationMode;
import ch.vd.unireg.xml.party.debtor.v1.Debtor;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.webservices.party3.data.DebtorPeriodicityBuilder;
import ch.vd.uniregctb.webservices.party3.impl.BusinessHelper;
import ch.vd.uniregctb.webservices.party3.impl.Context;
import ch.vd.uniregctb.webservices.party3.impl.DataHelper;
import ch.vd.uniregctb.webservices.party3.impl.EnumHelper;

public class DebtorStrategy extends PartyStrategy<Debtor> {

	@Override
	public Debtor newFrom(Tiers right, @Nullable Set<PartyPart> parts, Context context) throws WebServiceException {
		final Debtor debiteur = new Debtor();
		initBase(debiteur, right, context);
		initParts(debiteur, right, parts, context);
		return debiteur;
	}

	@Override
	public Debtor clone(Debtor right, @Nullable Set<PartyPart> parts) {
		Debtor debiteur = new Debtor();
		copyBase(debiteur, right);
		copyParts(debiteur, right, parts, CopyMode.EXCLUSIVE);
		return debiteur;
	}

	@Override
	protected void initBase(Debtor to, Tiers from, Context context) throws WebServiceException {
		super.initBase(to, from, context);

		final DebiteurPrestationImposable debiteur =(DebiteurPrestationImposable) from;
		to.setName(BusinessHelper.getDebtorName(debiteur, null, context.adresseService));
		to.setCategory(EnumHelper.coreToWeb(debiteur.getCategorieImpotSource()));
		to.setCommunicationMode(EnumHelper.coreToWeb(debiteur.getModeCommunication()));
		to.setWithoutReminder(DataHelper.coreToWeb(debiteur.getSansRappel()));
		to.setWithoutWithholdingTaxDeclaration(DataHelper.coreToWeb(debiteur.getSansListeRecapitulative()));
		final Long contribuableId = debiteur.getContribuableId();
		to.setAssociatedTaxpayerNumber(contribuableId == null ? null : contribuableId.intValue());
		if (to.getCommunicationMode() == CommunicationMode.UPLOAD) {
			to.setSoftwareId(debiteur.getLogicielId());
		}
	}

	@Override
	protected void copyBase(Debtor to, Debtor from) {
		super.copyBase(to, from);
		to.setName(from.getName());
		to.setCategory(from.getCategory());
		to.setCommunicationMode(from.getCommunicationMode());
		to.setWithoutReminder(from.isWithoutReminder());
		to.setWithoutWithholdingTaxDeclaration(from.isWithoutWithholdingTaxDeclaration());
		to.setAssociatedTaxpayerNumber(from.getAssociatedTaxpayerNumber());
		to.setSoftwareId(from.getSoftwareId());
	}

	@Override
	protected void initParts(Debtor to, Tiers from, @Nullable Set<PartyPart> parts, Context context) throws WebServiceException {
		super.initParts(to, from, parts, context);

		final DebiteurPrestationImposable debiteur =(DebiteurPrestationImposable) from;
		if (parts != null && parts.contains(PartyPart.DEBTOR_PERIODICITIES)) {
			initPeriodicities(to, debiteur);
		}
	}

	@Override
	protected void copyParts(Debtor to, Debtor from, @Nullable Set<PartyPart> parts, CopyMode mode) {
		super.copyParts(to, from, parts, mode);

		if (parts != null && parts.contains(PartyPart.DEBTOR_PERIODICITIES)) {
			copyColl(to.getPeriodicities(), from.getPeriodicities());
		}
	}

	private static void initPeriodicities(Debtor left, DebiteurPrestationImposable right) {
		for (ch.vd.uniregctb.declaration.Periodicite periodicite : right.getPeriodicitesNonAnnules(true)) {
			left.getPeriodicities().add(DebtorPeriodicityBuilder.newPeriodicity(periodicite));
		}
	}
}
