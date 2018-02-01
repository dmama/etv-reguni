package ch.vd.unireg.xml.party.v3.strategy;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.xml.party.debtor.v3.Debtor;
import ch.vd.unireg.xml.party.v3.PartyPart;
import ch.vd.unireg.xml.party.withholding.v1.CommunicationMode;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.xml.BusinessHelper;
import ch.vd.unireg.xml.Context;
import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.EnumHelper;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.party.v3.DebtorPeriodicityBuilder;

public class DebtorStrategy extends PartyStrategy<Debtor> {

	@Override
	public Debtor newFrom(Tiers right, @Nullable Set<PartyPart> parts, Context context) throws ServiceException {
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
	protected void initBase(Debtor to, Tiers from, Context context) throws ServiceException {
		super.initBase(to, from, context);

		final DebiteurPrestationImposable debiteur =(DebiteurPrestationImposable) from;
		to.setName(BusinessHelper.getDebtorName(debiteur, context.tiersService));
		to.setCategory(EnumHelper.coreToXMLv3(debiteur.getCategorieImpotSource()));
		to.setCommunicationMode(EnumHelper.coreToXMLv3(debiteur.getModeCommunication()));
		to.setWithoutReminder(DataHelper.coreToXML(debiteur.getSansRappel()));
		to.setWithoutWithholdingTaxDeclaration(DataHelper.coreToXML(debiteur.getSansListeRecapitulative()));
		to.setOtherCantonTaxAdministration(DataHelper.coreToXML(debiteur.getAciAutreCanton()));
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
		to.setOtherCantonTaxAdministration(from.isOtherCantonTaxAdministration());
		to.setAssociatedTaxpayerNumber(from.getAssociatedTaxpayerNumber());
		to.setSoftwareId(from.getSoftwareId());
	}

	@Override
	protected void initParts(Debtor to, Tiers from, @Nullable Set<PartyPart> parts, Context context) throws ServiceException {
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
		for (ch.vd.unireg.declaration.Periodicite periodicite : right.getPeriodicitesNonAnnulees(true)) {
			left.getPeriodicities().add(DebtorPeriodicityBuilder.newPeriodicity(periodicite));
		}
	}
}
