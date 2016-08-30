package ch.vd.uniregctb.xml.party.v1;

import java.util.List;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.InstitutionFinanciere;
import ch.vd.unireg.xml.party.v1.AccountNumberFormat;
import ch.vd.unireg.xml.party.v1.BankAccount;
import ch.vd.uniregctb.tiers.CoordonneesFinancieres;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.xml.Context;
import ch.vd.uniregctb.xml.DataHelper;

public class BankAccountBuilder {

	public static BankAccount newBankAccount(ch.vd.uniregctb.tiers.Tiers tiers, Context context) {
		final BankAccount c = new BankAccount();
		c.setOwnerPartyNumber(tiers.getNumero().intValue());
		c.setOwnerName(tiers.getTitulaireCompteBancaire());
		fillCoordonneesFinancieres(c, tiers.getCoordonneesFinancieres(), context);
		return c;
	}

	public static BankAccount newBankAccount(ch.vd.uniregctb.tiers.Mandat mandat, Context context) {
		final BankAccount c = new BankAccount();

		final Tiers mandataire = context.tiersService.getTiers(mandat.getObjetId());
		c.setOwnerPartyNumber(mandataire.getNumero().intValue());
		c.setOwnerName(context.tiersService.getNomRaisonSociale(mandataire));
		c.setDateFrom(DataHelper.coreToXMLv1(mandat.getDateDebut()));
		c.setDateTo(DataHelper.coreToXMLv1(mandat.getDateFin()));

		fillCoordonneesFinancieres(c, mandat.getCoordonneesFinancieres(), context);
		return c;
	}

	private static void fillCoordonneesFinancieres(BankAccount c, CoordonneesFinancieres cf, Context context) {
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
	}
}
