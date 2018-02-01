package ch.vd.unireg.xml.party.v4;

import java.util.List;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.InstitutionFinanciere;
import ch.vd.unireg.xml.party.v4.BankAccount;
import ch.vd.unireg.tiers.CoordonneesFinancieres;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.xml.Context;
import ch.vd.unireg.xml.DataHelper;

public class BankAccountBuilder {

	public static BankAccount newBankAccount(ch.vd.unireg.tiers.Tiers tiers, Context context) {
		final BankAccount c = new BankAccount();
		c.setOwnerPartyNumber(tiers.getNumero().intValue());
		c.setOwnerName(tiers.getTitulaireCompteBancaire());
		fillCoordonneesFinancieres(c, tiers.getCoordonneesFinancieres(), context);
		return c;
	}

	public static BankAccount newBankAccount(ch.vd.unireg.tiers.Mandat mandat, Context context) {
		final BankAccount c = new BankAccount();

		final Tiers mandataire = context.tiersService.getTiers(mandat.getObjetId());
		c.setOwnerPartyNumber(mandataire.getNumero().intValue());
		c.setOwnerName(context.tiersService.getNomRaisonSociale(mandataire));
		c.setDateFrom(DataHelper.coreToXMLv2(mandat.getDateDebut()));
		c.setDateTo(DataHelper.coreToXMLv2(mandat.getDateFin()));

		fillCoordonneesFinancieres(c, mandat.getCoordonneesFinancieres(), context);
		return c;
	}

	private static void fillCoordonneesFinancieres(BankAccount bankAccount, CoordonneesFinancieres cf, Context context) {
		if (cf != null) {
			bankAccount.setIban(cf.getIban());
			bankAccount.setClearing(context.ibanValidator.getClearing(cf.getIban()));
			bankAccount.setBicAddress(cf.getBicSwift());
		}

		try {
			final List<InstitutionFinanciere> list = context.infraService.getInstitutionsFinancieres(bankAccount.getClearing());
			if (list != null && !list.isEmpty()) {
				// on peut trouver plusieurs institutions, mais laquelle choisir ?
				// la première ne semble pas un choix plus bête qu'un autre...
				final InstitutionFinanciere institution = list.get(0);
				bankAccount.setBankName(institution.getNomInstitutionFinanciere());
			}
		}
		catch (ServiceInfrastructureException ignored) {
			// que faire de cette exception ?
		}
	}
}
