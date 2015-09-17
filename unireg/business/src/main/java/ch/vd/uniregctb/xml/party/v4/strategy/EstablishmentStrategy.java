package ch.vd.uniregctb.xml.party.v4.strategy;

import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.xml.party.establishment.v1.Establishment;
import ch.vd.unireg.xml.party.v4.PartyPart;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.xml.Context;
import ch.vd.uniregctb.xml.ServiceException;

public class EstablishmentStrategy extends TaxPayerStrategy<Establishment> {

	private static final Logger LOGGER = LoggerFactory.getLogger(EstablishmentStrategy.class);

	@Override
	public Establishment newFrom(Tiers right, @Nullable Set<PartyPart> parts, Context context) throws ServiceException {
		final Establishment e = new Establishment();
		initBase(e, right, context);
		initParts(e, right, parts, context);
		return e;
	}

	@Override
	public Establishment clone(Establishment right, @Nullable Set<PartyPart> parts) {
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
			// TODO SIPM aller chercher les données dans RCEnt
		}
		else {
			to.setName(etb.getRaisonSociale());
			to.setSign(etb.getEnseigne());
		}
	}

	@Override
	protected void copyBase(Establishment to, Establishment from) {
		super.copyBase(to, from);

		to.setName(from.getName());
		to.setSign(from.getSign());
	}
}
