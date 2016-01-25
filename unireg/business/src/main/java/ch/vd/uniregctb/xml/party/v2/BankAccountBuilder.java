package ch.vd.uniregctb.xml.party.v2;

import java.util.List;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.InstitutionFinanciere;
import ch.vd.unireg.xml.party.v2.AccountNumberFormat;
import ch.vd.unireg.xml.party.v2.BankAccount;
import ch.vd.uniregctb.tiers.CoordonneesFinancieres;
import ch.vd.uniregctb.xml.Context;

public class BankAccountBuilder {
	public static BankAccount newBankAccount(ch.vd.uniregctb.tiers.Tiers tiers, Context context) {
		final BankAccount c = new BankAccount();

		c.setOwnerPartyNumber(tiers.getNumero().intValue());
		c.setOwnerName(tiers.getTitulaireCompteBancaire());

		final CoordonneesFinancieres cf = tiers.getCoordonneesFinancieres();
		if (cf != null) {
			c.setAccountNumber(cf.getIban());
			c.setClearing(context.ibanValidator.getClearing(cf.getIban()));
			c.setBicAddress(cf.getBicSwift());
		}
		c.setFormat(AccountNumberFormat.IBAN); // par définition, on ne stocke que le format IBAN dans Unireg

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
