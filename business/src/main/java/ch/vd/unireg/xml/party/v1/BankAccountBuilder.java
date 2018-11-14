package ch.vd.unireg.xml.party.v1;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.tiers.CompteBancaire;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.xml.Context;
import ch.vd.unireg.xml.DataHelper;

public class BankAccountBuilder {

	public static BankAccount newBankAccount(long ctbId, String titulaire, @Nullable CompteBancaire compteBancaire, Context context) {
		final BankAccount c = new BankAccount();
		c.setOwnerPartyNumber((int) ctbId);
		c.setOwnerName(titulaire);
		fillCoordonneesFinancieres(c, compteBancaire, context);
		return c;
	}

	public static BankAccount newBankAccount(ch.vd.unireg.tiers.Mandat mandat, Context context) {
		final BankAccount c = new BankAccount();

		final Tiers mandataire = context.tiersService.getTiers(mandat.getObjetId());
		c.setOwnerPartyNumber(mandataire.getNumero().intValue());
		c.setOwnerName(context.tiersService.getNomRaisonSociale(mandataire));
		c.setDateFrom(DataHelper.coreToXMLv1(mandat.getDateDebut()));
		c.setDateTo(DataHelper.coreToXMLv1(mandat.getDateFin()));

		fillCoordonneesFinancieres(c, mandat.getCompteBancaire(), context);
		return c;
	}

	private static void fillCoordonneesFinancieres(BankAccount c, CompteBancaire cf, Context context) {
		if (cf != null) {
			c.setAccountNumber(cf.getIban());
			c.setClearing(context.ibanValidator.getClearing(cf.getIban()));
			c.setBicAddress(cf.getBicSwift());
		}
		c.setFormat(AccountNumberFormat.IBAN); // par définition, on ne stocke que le format IBAN dans Unireg
	}
}
