package ch.vd.uniregctb.webservices.tiers3.data;

import java.util.List;

import ch.vd.uniregctb.interfaces.model.InstitutionFinanciere;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.webservices.tiers3.CompteBancaire;
import ch.vd.uniregctb.webservices.tiers3.FormatNumeroCompte;
import ch.vd.uniregctb.webservices.tiers3.impl.Context;

public class CompteBancaireBuilder {
	public static CompteBancaire newCompteBancaire(ch.vd.uniregctb.tiers.Tiers tiers, Context context) {
		final CompteBancaire c = new CompteBancaire();

		c.setNumeroTiersTitulaire(tiers.getNumero());
		c.setTitulaire(tiers.getTitulaireCompteBancaire());
		c.setNumero(tiers.getNumeroCompteBancaire());
		c.setFormat(FormatNumeroCompte.IBAN); // par définition, on ne stocke que le format IBAN dans Unireg
		c.setClearing(context.ibanValidator.getClearing(tiers.getNumeroCompteBancaire()));
		c.setAdresseBicSwift(tiers.getAdresseBicSwift());

		try {
			final List<InstitutionFinanciere> list = context.infraService.getInstitutionsFinancieres(c.getClearing());
			if (list != null && !list.isEmpty()) {
				// on peut trouver plusieurs institutions, mais laquelle choisir ?
				// la première ne semble pas un choix plus bête qu'un autre...
				final InstitutionFinanciere institution = list.get(0);
				c.setNomInstitution(institution.getNomInstitutionFinanciere());
			}
		}
		catch (ServiceInfrastructureException ignored) {
			// que faire de cette exception ?
		}
		return c;
	}
}
