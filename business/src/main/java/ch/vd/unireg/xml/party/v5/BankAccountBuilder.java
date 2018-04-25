package ch.vd.unireg.xml.party.v5;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.InstitutionFinanciere;
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
		c.setDateFrom(DataHelper.coreToXMLv2(mandat.getDateDebut()));
		c.setDateTo(DataHelper.coreToXMLv2(mandat.getDateFin()));

		fillCoordonneesFinancieres(c, mandat.getCompteBancaire(), context);
		return c;
	}

	private static void fillCoordonneesFinancieres(BankAccount bankAccount, CompteBancaire cf, Context context) {
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
