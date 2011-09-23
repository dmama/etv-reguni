package ch.vd.uniregctb.webservices.tiers3.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.webservices.tiers3.PartyPart;
import ch.vd.unireg.xml.party.taxdeclaration.v1.OrdinaryTaxDeclaration;
import ch.vd.unireg.xml.party.taxdeclaration.v1.TaxDeclaration;
import ch.vd.unireg.xml.party.taxdeclaration.v1.TaxDeclarationStatus;
import ch.vd.unireg.xml.party.taxdeclaration.v1.WithholdingTaxDeclaration;
import ch.vd.uniregctb.webservices.tiers3.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers3.impl.EnumHelper;

public class TaxDeclarationBuilder {

	public static OrdinaryTaxDeclaration newOrdinaryTaxDeclaration(ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire declaration, @Nullable Set<PartyPart> parts) {

		final OrdinaryTaxDeclaration d = new OrdinaryTaxDeclaration();
		fillTaxDeclarationBase(d, declaration);
		fillTaxDeclarationParts(d, declaration, parts);

		d.setSequenceNumber(Long.valueOf(declaration.getNumero()));
		d.setDocumentType(EnumHelper.coreToWeb(declaration.getTypeDeclaration()));

		final Integer numeroOfsForGestion = declaration.getNumeroOfsForGestion();
		if (numeroOfsForGestion == null) {
			d.setManagingMunicipalityFSOId(0L);
		}
		else {
			d.setManagingMunicipalityFSOId(numeroOfsForGestion);
		}

		return d;
	}

	public static WithholdingTaxDeclaration newWithholdingTaxDeclaration(ch.vd.uniregctb.declaration.DeclarationImpotSource declaration, @Nullable Set<PartyPart> parts) {
		final WithholdingTaxDeclaration d = new WithholdingTaxDeclaration();
		fillTaxDeclarationBase(d, declaration);
		fillTaxDeclarationParts(d, declaration, parts);
		d.setPeriodicity(EnumHelper.coreToWeb(declaration.getPeriodicite()));
		d.setCommunicationMode(EnumHelper.coreToWeb(declaration.getModeCommunication()));
		return d;
	}

	private static void fillTaxDeclarationBase(TaxDeclaration d, ch.vd.uniregctb.declaration.Declaration declaration) {
		d.setId(declaration.getId()); // [SIFISC-2392]
		d.setDateFrom(DataHelper.coreToWeb(declaration.getDateDebut()));
		d.setDateTo(DataHelper.coreToWeb(declaration.getDateFin()));
		d.setCancellationDate(DataHelper.coreToWeb(declaration.getAnnulationDate()));
		d.setTaxPeriod(TaxPeriodBuilder.newTaxPeriod(declaration.getPeriode()));
	}

	private static void fillTaxDeclarationParts(TaxDeclaration d, ch.vd.uniregctb.declaration.Declaration declaration, Set<PartyPart> parts) {
		if (parts != null && parts.contains(PartyPart.TAX_DECLARATIONS_STATUSES)) {
			for (ch.vd.uniregctb.declaration.EtatDeclaration etat : declaration.getEtats()) {
				d.getStatuses().add(newTaxDeclarationStatus(etat));
			}
		}
	}

	public static TaxDeclarationStatus newTaxDeclarationStatus(ch.vd.uniregctb.declaration.EtatDeclaration etat) {
		final TaxDeclarationStatus e = new TaxDeclarationStatus();
		e.setType(EnumHelper.coreToWeb(etat.getEtat()));
		e.setDateFrom(DataHelper.coreToWeb(getAdjustedStatusDateFrom(etat)));
		e.setCancellationDate(DataHelper.coreToWeb(etat.getAnnulationDate()));
		return e;
	}

	/**
	 * [UNIREG-3407] Pour les états de sommation, c'est la date de l'envoi du courrier qu'il faut renvoyer
	 *
	 * @param etatDeclaration etat de la déclaration
	 * @return valeur de la date à mettre dans le champ {@link ch.vd.uniregctb.webservices.tiers3.EtatDeclaration#dateObtention}
	 */
	private static RegDate getAdjustedStatusDateFrom(ch.vd.uniregctb.declaration.EtatDeclaration etatDeclaration) {
		final RegDate date;
		if (etatDeclaration instanceof ch.vd.uniregctb.declaration.EtatDeclarationSommee) {
			final ch.vd.uniregctb.declaration.EtatDeclarationSommee etatSommee = (ch.vd.uniregctb.declaration.EtatDeclarationSommee) etatDeclaration;
			date = etatSommee.getDateEnvoiCourrier();
		}
		else {
			date = etatDeclaration.getDateObtention();
		}
		return date;
	}

	public static TaxDeclaration clone(TaxDeclaration d) {
		if (d == null) {
			return null;
		}
		final ArrayList<TaxDeclarationStatus> clonedEtats = cloneStatuses(d.getStatuses());
		if (d instanceof OrdinaryTaxDeclaration) {
			return new OrdinaryTaxDeclaration(d.getId(), d.getDateFrom(), d.getDateTo(), d.getCancellationDate(), d.getTaxPeriod(), clonedEtats, ((OrdinaryTaxDeclaration) d).getSequenceNumber(),
					((OrdinaryTaxDeclaration) d).getDocumentType(), ((OrdinaryTaxDeclaration) d).getManagingMunicipalityFSOId(), null);
		}
		else if (d instanceof WithholdingTaxDeclaration) {
			return new WithholdingTaxDeclaration(d.getId(), d.getDateFrom(), d.getDateTo(), d.getCancellationDate(), d.getTaxPeriod(), clonedEtats, ((WithholdingTaxDeclaration) d).getPeriodicity(),
					((WithholdingTaxDeclaration) d).getCommunicationMode(), null);
		}
		else {
			throw new IllegalArgumentException("Type de déclaration d'impôt inconnu = [" + d.getClass() + "]");
		}
	}

	private static ArrayList<TaxDeclarationStatus> cloneStatuses(List<TaxDeclarationStatus> etats) {
		final ArrayList<TaxDeclarationStatus> clonedEtats;
		if (etats == null) {
			clonedEtats = null;
		}
		else {
			clonedEtats = new ArrayList<TaxDeclarationStatus>(etats.size());
			for (TaxDeclarationStatus etat : etats) {
				clonedEtats.add(clone(etat));
			}
		}
		return clonedEtats;
	}

	private static TaxDeclarationStatus clone(TaxDeclarationStatus etat) {
		if (etat == null) {
			return null;
		}
		return new TaxDeclarationStatus(etat.getDateFrom(), etat.getCancellationDate(), etat.getType(), null);
	}
}
