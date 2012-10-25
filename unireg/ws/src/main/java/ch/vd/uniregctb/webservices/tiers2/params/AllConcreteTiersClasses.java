package ch.vd.uniregctb.webservices.tiers2.params;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import ch.vd.uniregctb.webservices.tiers2.data.Debiteur;
import ch.vd.uniregctb.webservices.tiers2.data.DebiteurHisto;
import ch.vd.uniregctb.webservices.tiers2.data.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.webservices.tiers2.data.DeclarationImpotSource;
import ch.vd.uniregctb.webservices.tiers2.data.MenageCommun;
import ch.vd.uniregctb.webservices.tiers2.data.MenageCommunHisto;
import ch.vd.uniregctb.webservices.tiers2.data.PersonneMorale;
import ch.vd.uniregctb.webservices.tiers2.data.PersonneMoraleHisto;
import ch.vd.uniregctb.webservices.tiers2.data.PersonnePhysique;
import ch.vd.uniregctb.webservices.tiers2.data.PersonnePhysiqueHisto;

/**
 * <b>Dans la version 3 du web-service :</b> supprimé.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AllConcreteTiersClasses")
public class AllConcreteTiersClasses {
	PersonnePhysique pp;
	PersonnePhysiqueHisto pp_histo;
	Debiteur deb;
	DebiteurHisto deb_histo;
	MenageCommun mc;
	MenageCommunHisto mc_histo;
	DeclarationImpotSource lr;
	DeclarationImpotOrdinaire di;
	PersonneMorale pM;
	PersonneMoraleHisto pm_histo;
}