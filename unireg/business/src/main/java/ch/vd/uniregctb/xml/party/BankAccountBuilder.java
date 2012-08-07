package ch.vd.uniregctb.xml.party;

import java.util.List;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.InstitutionFinanciere;
import ch.vd.unireg.xml.party.v1.AccountNumberFormat;
import ch.vd.unireg.xml.party.v1.BankAccount;
import ch.vd.uniregctb.xml.Context;

public class BankAccountBuilder {
	public static BankAccount newBankAccount(ch.vd.uniregctb.tiers.Tiers tiers, Context context) {
		final BankAccount c = new BankAccount();

		c.setOwnerPartyNumber(tiers.getNumero().intValue());
		c.setOwnerName(tiers.getTitulaireCompteBancaire());
		c.setAccountNumber(tiers.getNumeroCompteBancaire());
		c.setFormat(AccountNumberFormat.IBAN); // par définition, on ne stocke que le format IBAN dans Unireg
		c.setClearing(context.ibanValidator.getClearing(tiers.getNumeroCompteBancaire()));
		c.setBicAddress(tiers.getAdresseBicSwift());

		try {
			final List<InstitutionFinanciere> list = context.infraService.getInstitutionsFinancieres(c.getClearing());
			if (list != null && !list.isEmpty()) {
				// on peut trouver plusieurs institutions, mais laquelle choisir ?
				// la première ne semble pas un choix plus bête qu'un autre...
				final InstitutionFinanciere institution = list.get(0);
				c.setBankName(institution.getNomInstitutionFinanciere());
			}
		}
		catch (ServiceInfrastructureException ignored) {
			// que faire de cette exception ?
		}
		return c;
	}
}
