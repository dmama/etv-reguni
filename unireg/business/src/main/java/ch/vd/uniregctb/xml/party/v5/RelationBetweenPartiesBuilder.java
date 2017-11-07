package ch.vd.uniregctb.xml.party.v5;

import ch.vd.unireg.xml.party.relation.v4.Absorbed;
import ch.vd.unireg.xml.party.relation.v4.Absorbing;
import ch.vd.unireg.xml.party.relation.v4.Administration;
import ch.vd.unireg.xml.party.relation.v4.AfterSplit;
import ch.vd.unireg.xml.party.relation.v4.BeforeSplit;
import ch.vd.unireg.xml.party.relation.v4.Child;
import ch.vd.unireg.xml.party.relation.v4.EconomicActivity;
import ch.vd.unireg.xml.party.relation.v4.Guardian;
import ch.vd.unireg.xml.party.relation.v4.HouseholdMember;
import ch.vd.unireg.xml.party.relation.v4.InheritanceFrom;
import ch.vd.unireg.xml.party.relation.v4.InheritanceTo;
import ch.vd.unireg.xml.party.relation.v4.LegalAdviser;
import ch.vd.unireg.xml.party.relation.v4.ManagementCompany;
import ch.vd.unireg.xml.party.relation.v4.Parent;
import ch.vd.unireg.xml.party.relation.v4.RelationBetweenParties;
import ch.vd.unireg.xml.party.relation.v4.Replaced;
import ch.vd.unireg.xml.party.relation.v4.ReplacedBy;
import ch.vd.unireg.xml.party.relation.v4.Representative;
import ch.vd.unireg.xml.party.relation.v4.TaxLiabilitySubstitute;
import ch.vd.unireg.xml.party.relation.v4.TaxLiabilitySubstituteFor;
import ch.vd.unireg.xml.party.relation.v4.TaxableRevenue;
import ch.vd.unireg.xml.party.relation.v4.WealthTransferOriginator;
import ch.vd.unireg.xml.party.relation.v4.WealthTransferRecipient;
import ch.vd.unireg.xml.party.relation.v4.WelfareAdvocate;
import ch.vd.unireg.xml.party.relation.v4.WithholdingTaxContact;
import ch.vd.uniregctb.tiers.ActiviteEconomique;
import ch.vd.uniregctb.tiers.AdministrationEntreprise;
import ch.vd.uniregctb.tiers.AnnuleEtRemplace;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.AssujettissementParSubstitution;
import ch.vd.uniregctb.tiers.ConseilLegal;
import ch.vd.uniregctb.tiers.ContactImpotSource;
import ch.vd.uniregctb.tiers.Curatelle;
import ch.vd.uniregctb.tiers.FusionEntreprises;
import ch.vd.uniregctb.tiers.Heritage;
import ch.vd.uniregctb.tiers.Parente;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RapportPrestationImposable;
import ch.vd.uniregctb.tiers.RepresentationConventionnelle;
import ch.vd.uniregctb.tiers.ScissionEntreprise;
import ch.vd.uniregctb.tiers.SocieteDirection;
import ch.vd.uniregctb.tiers.TransfertPatrimoine;
import ch.vd.uniregctb.tiers.Tutelle;
import ch.vd.uniregctb.xml.DataHelper;

public class RelationBetweenPartiesBuilder {

	private static void fillBaseData(RelationBetweenParties to, RapportEntreTiers from, int numeroAutreTiers) {
		to.setDateFrom(DataHelper.coreToXMLv2(from.getDateDebut()));
		to.setDateTo(DataHelper.coreToXMLv2(from.getDateFin()));
		to.setCancellationDate(DataHelper.coreToXMLv2(from.getAnnulationDate()));
		to.setOtherPartyNumber(numeroAutreTiers);
	}

	public static Absorbed newAbsorbed(FusionEntreprises fusion, int numeroAutreTiers) {
		final Absorbed abs = new Absorbed();
		fillBaseData(abs, fusion, numeroAutreTiers);
		return abs;
	}

	public static Absorbing newAbsorbing(FusionEntreprises fusion, int numeroAutreTiers) {
		final Absorbing abs = new Absorbing();
		fillBaseData(abs, fusion, numeroAutreTiers);
		return abs;
	}

	public static AfterSplit newAfterSplit(ScissionEntreprise scission, int numeroAutreTiers) {
		final AfterSplit as = new AfterSplit();
		fillBaseData(as, scission, numeroAutreTiers);
		return as;
	}

	public static BeforeSplit newBeforeSplit(ScissionEntreprise scission, int numeroAutreTiers) {
		final BeforeSplit bs = new BeforeSplit();
		fillBaseData(bs, scission, numeroAutreTiers);
		return bs;
	}

	public static ReplacedBy newReplacedBy(AnnuleEtRemplace ret, int numeroAutreTiers) {
		final ReplacedBy rb = new ReplacedBy();
		fillBaseData(rb, ret, numeroAutreTiers);
		return rb;
	}

	public static Replaced newReplaced(AnnuleEtRemplace ret, int numeroAutreTiers) {
		final Replaced r = new Replaced();
		fillBaseData(r, ret, numeroAutreTiers);
		return r;
	}

	public static Child newChild(Parente parente, int numeroAutreTiers) {
		final Child child = new Child();
		fillBaseData(child, parente, numeroAutreTiers);
		return child;
	}

	public static EconomicActivity newEconomicActivite(ActiviteEconomique ae, int numeroAutreTiers) {
		final EconomicActivity ea = new EconomicActivity();
		fillBaseData(ea, ae, numeroAutreTiers);
		ea.setPrincipal(ae.isPrincipal());
		return ea;
	}

	public static Guardian newGuardian(Tutelle tutelle, int numeroAutreTiers) {
		final Guardian g = new Guardian();
		fillBaseData(g, tutelle, numeroAutreTiers);
		return g;
	}

	public static HouseholdMember newHouseholdMember(AppartenanceMenage am, int numeroAutreTiers) {
		final HouseholdMember hm = new HouseholdMember();
		fillBaseData(hm, am, numeroAutreTiers);
		return hm;
	}

	public static LegalAdviser newLegalAdviser(ConseilLegal cl, int numeroAutreTiers) {
		final LegalAdviser la = new LegalAdviser();
		fillBaseData(la, cl, numeroAutreTiers);
		return la;
	}

	public static Parent newParent(Parente parente, int numeroAutreTiers) {
		final Parent parent = new Parent();
		fillBaseData(parent, parente, numeroAutreTiers);
		return parent;
	}

	public static InheritanceTo newInheritanceTo(Heritage heritage, int numeroAutreTiers) {
		final InheritanceTo inheritance = new InheritanceTo();
		fillBaseData(inheritance, heritage, numeroAutreTiers);
		final Boolean principal = heritage.getPrincipalCommunaute();
		inheritance.setPrincipal(principal != null && principal);
		return inheritance;
	}

	public static InheritanceFrom newInheritanceFrom(Heritage heritage, int numeroAutreTiers) {
		final InheritanceFrom inheritance = new InheritanceFrom();
		fillBaseData(inheritance, heritage, numeroAutreTiers);
		final Boolean principal = heritage.getPrincipalCommunaute();
		inheritance.setPrincipal(principal != null && principal);
		return inheritance;
	}

	public static Representative newRepresentative(RepresentationConventionnelle rc, int numeroAutreTiers) {
		final Representative r = new Representative();
		fillBaseData(r, rc, numeroAutreTiers);
		r.setExtensionToForcedExecution(rc.getExtensionExecutionForcee());
		return r;
	}

	public static TaxableRevenue newTaxableRevenue(RapportPrestationImposable rpi, int numeroAutreTiers) {
		final TaxableRevenue tr = new TaxableRevenue();
		fillBaseData(tr, rpi, numeroAutreTiers);
		tr.setEndDateOfLastTaxableItem(DataHelper.coreToXMLv2(rpi.getFinDernierElementImposable()));
		return tr;
	}

	public static WealthTransferOriginator newWealthTransferOriginator(TransfertPatrimoine tp, int numeroAutreTiers) {
		final WealthTransferOriginator wto = new WealthTransferOriginator();
		fillBaseData(wto, tp, numeroAutreTiers);
		return wto;
	}

	public static WealthTransferRecipient newWealthTransferRecipient(TransfertPatrimoine tp, int numeroAutreTiers) {
		final WealthTransferRecipient wtr = new WealthTransferRecipient();
		fillBaseData(wtr, tp, numeroAutreTiers);
		return wtr;
	}

	public static WelfareAdvocate newWelfareAdvocate(Curatelle curatelle, int numeroAutreTiers) {
		final WelfareAdvocate wa = new WelfareAdvocate();
		fillBaseData(wa, curatelle, numeroAutreTiers);
		return wa;
	}

	public static WithholdingTaxContact newWithholdingTaxContact(ContactImpotSource cis, int numeroAutreTiers) {
		final WithholdingTaxContact wtc = new WithholdingTaxContact();
		fillBaseData(wtc, cis, numeroAutreTiers);
		return wtc;
	}

	public static Administration newAdministration(AdministrationEntreprise ae, int numeroAutreTiers) {
		final Administration adm = new Administration();
		fillBaseData(adm, ae, numeroAutreTiers);
		adm.setChairman(ae.isPresident());
		return adm;
	}

	public static ManagementCompany newManagementCompany(SocieteDirection sd, int numeroAutreTiers) {
		final ManagementCompany mc = new ManagementCompany();
		fillBaseData(mc, sd, numeroAutreTiers);
		return mc;
	}

	public static TaxLiabilitySubstitute newTaxLiabilitySubstitute(AssujettissementParSubstitution aps, int numeroAutreTiers) {
		final TaxLiabilitySubstitute tls = new TaxLiabilitySubstitute();
		fillBaseData(tls, aps, numeroAutreTiers);
		return tls;
	}

	public static TaxLiabilitySubstituteFor newTaxLiabilitySubstituteFor(AssujettissementParSubstitution aps, int numeroAutreTiers) {
		final TaxLiabilitySubstituteFor tlsf = new TaxLiabilitySubstituteFor();
		fillBaseData(tlsf, aps, numeroAutreTiers);
		return tlsf;
	}
}
