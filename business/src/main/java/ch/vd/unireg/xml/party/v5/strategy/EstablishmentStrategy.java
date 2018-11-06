package ch.vd.unireg.xml.party.v5.strategy;

import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.interfaces.entreprise.data.EtablissementCivil;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.type.FormulePolitesse;
import ch.vd.unireg.xml.Context;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.party.establishment.v2.Establishment;
import ch.vd.unireg.xml.party.v5.InternalPartyPart;

public class EstablishmentStrategy extends TaxPayerStrategy<Establishment> {

	private static final Logger LOGGER = LoggerFactory.getLogger(EstablishmentStrategy.class);

	@Override
	public Establishment newFrom(Tiers right, @Nullable Set<InternalPartyPart> parts, Context context) throws ServiceException {
		final Establishment e = new Establishment();
		initBase(e, right, context);
		initParts(e, right, parts, context);
		return e;
	}

	@Override
	public Establishment clone(Establishment right, @Nullable Set<InternalPartyPart> parts) {
		final Establishment e = new Establishment();
		copyBase(e, right);
		copyParts(e, right, parts, CopyMode.EXCLUSIVE);
		return e;
	}

	@Override
	protected void initBase(Establishment to, Tiers from, Context context) throws ServiceException {
		super.initBase(to, from, context);

		final Etablissement etb = (Etablissement) from;
		if (etb.isConnuAuCivil()) {
			final EtablissementCivil etablissement = context.tiersService.getEtablissementCivil(etb);
			to.setName(etablissement.getNom(null));
		}
		else {
			to.setName(etb.getRaisonSociale());
		}

		to.setSign(etb.getEnseigne());

		// [SIFISC-29739]
		final FormulePolitesse formule = context.adresseService.getFormulePolitesse(etb, null);
		if (formule != null) {
			to.setFormalGreeting(formule.getFormuleAppel());
		}
	}

	@Override
	protected void copyBase(Establishment to, Establishment from) {
		super.copyBase(to, from);
		to.setName(from.getName());
		to.setSign(from.getSign());
		to.setFormalGreeting(from.getFormalGreeting());
	}
}
