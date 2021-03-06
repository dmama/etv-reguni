package ch.vd.unireg.registrefoncier.dataimport;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import java.io.StringWriter;

import org.jetbrains.annotations.NotNull;

import ch.vd.capitastra.common.Rechteinhaber;
import ch.vd.capitastra.grundstueck.Bodenbedeckung;
import ch.vd.capitastra.grundstueck.EigentumAnteil;
import ch.vd.capitastra.grundstueck.Gebaeude;
import ch.vd.capitastra.grundstueck.Gemeinschaft;
import ch.vd.capitastra.grundstueck.Grundstueck;
import ch.vd.capitastra.grundstueck.GrundstueckExport;
import ch.vd.capitastra.grundstueck.Personstamm;
import ch.vd.capitastra.rechteregister.Dienstbarkeit;
import ch.vd.capitastra.rechteregister.DienstbarkeitExtended;
import ch.vd.capitastra.rechteregister.LastRechtGruppe;
import ch.vd.unireg.registrefoncier.dataimport.elements.principal.BergwerkElement;
import ch.vd.unireg.registrefoncier.dataimport.elements.principal.BodenbedeckungElement;
import ch.vd.unireg.registrefoncier.dataimport.elements.principal.BodenbedeckungListElement;
import ch.vd.unireg.registrefoncier.dataimport.elements.principal.EigentumAnteilListElement;
import ch.vd.unireg.registrefoncier.dataimport.elements.principal.FolioElement;
import ch.vd.unireg.registrefoncier.dataimport.elements.principal.GebaeudeElement;
import ch.vd.unireg.registrefoncier.dataimport.elements.principal.GemeinschaftElement;
import ch.vd.unireg.registrefoncier.dataimport.elements.principal.GewoehnlichesMiteigentumElement;
import ch.vd.unireg.registrefoncier.dataimport.elements.principal.GrundstueckEigentumAnteilElement;
import ch.vd.unireg.registrefoncier.dataimport.elements.principal.GrundstueckNummerElement;
import ch.vd.unireg.registrefoncier.dataimport.elements.principal.HerrenlosEigentumElement;
import ch.vd.unireg.registrefoncier.dataimport.elements.principal.JuristischePersonstammElement;
import ch.vd.unireg.registrefoncier.dataimport.elements.principal.LiegenschaftElement;
import ch.vd.unireg.registrefoncier.dataimport.elements.principal.NatuerlichePersonstammElement;
import ch.vd.unireg.registrefoncier.dataimport.elements.principal.PersonEigentumAnteilElement;
import ch.vd.unireg.registrefoncier.dataimport.elements.principal.SdrElement;
import ch.vd.unireg.registrefoncier.dataimport.elements.principal.StockwerksEinheitElement;
import ch.vd.unireg.registrefoncier.dataimport.elements.principal.UnbekanntesGrundstueckElement;
import ch.vd.unireg.registrefoncier.dataimport.elements.servitude.DienstbarkeitElement;
import ch.vd.unireg.registrefoncier.dataimport.elements.servitude.DienstbarkeitExtendedElement;
import ch.vd.unireg.registrefoncier.dataimport.elements.servitude.LastRechtGruppeElement;

public class XmlHelperRFImpl implements XmlHelperRF {

	private final JAXBContext immeubleContext;
	private final JAXBContext droitContext;
	private final JAXBContext droitListContext;
	private final JAXBContext ayantDroitContext;
	private final JAXBContext proprietaireContext;
	private final JAXBContext batimentContext;
	private final JAXBContext surfacesAuSolContext;
	private final JAXBContext surfaceListContext;
	private final JAXBContext servitudeContext;
	private final JAXBContext servitudeEtendueContext;
	private final JAXBContext communauteContext;
	private final JAXBContext communeContext;
	private final JAXBContext groupeBeneficiairesContext;
	private final JAXBContext beneficiaireContext;

	public XmlHelperRFImpl() throws JAXBException {
		immeubleContext = JAXBContext.newInstance(BergwerkElement.class, FolioElement.class, GewoehnlichesMiteigentumElement.class,
		                                          LiegenschaftElement.class, SdrElement.class, StockwerksEinheitElement.class,
		                                          UnbekanntesGrundstueckElement.class);
		droitContext = JAXBContext.newInstance(PersonEigentumAnteilElement.class, GrundstueckEigentumAnteilElement.class, HerrenlosEigentumElement.class);
		droitListContext = JAXBContext.newInstance(EigentumAnteilListElement.class);
		ayantDroitContext = JAXBContext.newInstance(NatuerlichePersonstammElement.class,
		                                            JuristischePersonstammElement.class,
		                                            GemeinschaftElement.class,
		                                            UnbekanntesGrundstueckElement.class);
		proprietaireContext = JAXBContext.newInstance(NatuerlichePersonstammElement.class, JuristischePersonstammElement.class);
		batimentContext = JAXBContext.newInstance(GebaeudeElement.class);
		surfacesAuSolContext = JAXBContext.newInstance(BodenbedeckungElement.class);
		surfaceListContext = JAXBContext.newInstance(BodenbedeckungListElement.class);
		communauteContext = JAXBContext.newInstance(GemeinschaftElement.class);
		communeContext = JAXBContext.newInstance(GrundstueckNummerElement.class);
		servitudeContext = JAXBContext.newInstance(DienstbarkeitElement.class);
		servitudeEtendueContext = JAXBContext.newInstance(DienstbarkeitExtendedElement.class);
		groupeBeneficiairesContext = JAXBContext.newInstance(LastRechtGruppeElement.class);
		beneficiaireContext = JAXBContext.newInstance(ch.vd.unireg.registrefoncier.dataimport.elements.servitude.NatuerlichePersonstammElement.class,
		                                              ch.vd.unireg.registrefoncier.dataimport.elements.servitude.JuristischePersonstammElement.class);
	}

	@Override
	public JAXBContext getImmeubleContext() {
		return immeubleContext;
	}

	@Override
	public JAXBContext getDroitContext() {
		return droitContext;
	}

	@Override
	public JAXBContext getDroitListContext() {
		return droitListContext;
	}

	@Override
	public JAXBContext getAyantDroitContext() {
		return ayantDroitContext;
	}

	@Override
	public JAXBContext getProprietaireContext() {
		return proprietaireContext;
	}

	@Override
	public JAXBContext getBatimentContext() {
		return batimentContext;
	}

	@Override
	public JAXBContext getSurfacesAuSolContext() {
		return surfacesAuSolContext;
	}

	public JAXBContext getSurfaceListContext() {
		return surfaceListContext;
	}

	@Override
	public JAXBContext getCommunauteContext() {
		return communauteContext;
	}

	@Override
	public JAXBContext getCommuneContext() {
		return communeContext;
	}

	@Override
	public JAXBContext getServitudeContext() {
		return servitudeContext;
	}

	@Override
	public JAXBContext getServitudeEtendueContext() {
		return servitudeEtendueContext;
	}

	public JAXBContext getGroupeBeneficiairesContext() {
		return groupeBeneficiairesContext;
	}

	@Override
	public JAXBContext getBeneficiaireContext() {
		return beneficiaireContext;
	}

	@Override
	public String toXMLString(Grundstueck obj) {
		try {
			final Marshaller m = immeubleContext.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			final StringWriter w = new StringWriter();
			final QName name = buildGrundstueckQName(obj);
			m.marshal(new JAXBElement<>(name, (Class<Grundstueck>) obj.getClass(), null, obj), w);
			return w.toString();
		}
		catch (JAXBException e) {
			return e.getMessage();
		}
	}

	@Override
	public String toXMLString(EigentumAnteil obj) {
		try {
			final Marshaller m = droitContext.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			final StringWriter w = new StringWriter();
			final QName name = buildGrundstueckQName(obj);
			m.marshal(new JAXBElement<>(name, (Class<EigentumAnteil>) obj.getClass(), null, obj), w);
			return w.toString();
		}
		catch (JAXBException e) {
			return e.getMessage();
		}
	}

	@Override
	public String toXMLString(Personstamm obj) {
		try {
			final Marshaller m = proprietaireContext.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			final StringWriter w = new StringWriter();
			final QName name = buildGrundstueckQName(obj);
			m.marshal(new JAXBElement<>(name, (Class<Personstamm>) obj.getClass(), null, obj), w);
			return w.toString();
		}
		catch (JAXBException e) {
			return e.getMessage();
		}
	}

	@Override
	public String toXMLString(Gebaeude obj) {
		try {
			final Marshaller m = batimentContext.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			final StringWriter w = new StringWriter();
			final QName name = buildGrundstueckQName(obj);
			m.marshal(new JAXBElement<>(name, (Class<Gebaeude>) obj.getClass(), null, obj), w);
			return w.toString();
		}
		catch (JAXBException e) {
			return e.getMessage();
		}
	}

	@Override
	public String toXMLString(Bodenbedeckung obj) {
		try {
			final Marshaller m = surfacesAuSolContext.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			final StringWriter w = new StringWriter();
			final QName name = buildGrundstueckQName(obj);
			m.marshal(new JAXBElement<>(name, (Class<Bodenbedeckung>) obj.getClass(), null, obj), w);
			return w.toString();
		}
		catch (JAXBException e) {
			return e.getMessage();
		}
	}

	@Override
	public String toXMLString(GrundstueckExport.BodenbedeckungList obj) {
		try {
			final Marshaller m = surfaceListContext.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			final StringWriter w = new StringWriter();
			final QName name = buildGrundstueckQName(obj);
			m.marshal(new JAXBElement<>(name, (Class<GrundstueckExport.BodenbedeckungList>) obj.getClass(), null, obj), w);
			return w.toString();
		}
		catch (JAXBException e) {
			return e.getMessage();
		}
	}

	@Override
	public String toXMLString(Gemeinschaft gemeinschaft) {
		try {
			final Marshaller m = communauteContext.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			final StringWriter w = new StringWriter();
			final QName name = buildGrundstueckQName(gemeinschaft);
			m.marshal(new JAXBElement<>(name, (Class<Gemeinschaft>) gemeinschaft.getClass(), null, gemeinschaft), w);
			return w.toString();
		}
		catch (JAXBException e) {
			return e.getMessage();
		}
	}

	@Override
	public String toXMLString(EigentumAnteilListElement obj) {
		try {
			final Marshaller m = droitListContext.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			final StringWriter w = new StringWriter();
			final QName name = buildGrundstueckQName(obj);
			m.marshal(new JAXBElement<>(name, (Class<EigentumAnteilListElement>) obj.getClass(), null, obj), w);
			return w.toString();
		}
		catch (JAXBException e) {
			e.printStackTrace();
			return e.getMessage();
		}
	}

	@Override
	public String toXMLString(Rechteinhaber rechteinhaber) {
		if (rechteinhaber instanceof Personstamm) {
			return toXMLString((Personstamm) rechteinhaber);
		}
		else if (rechteinhaber instanceof Gemeinschaft) {
			return toXMLString((Gemeinschaft) rechteinhaber);
		}
		else if (rechteinhaber instanceof ch.vd.capitastra.rechteregister.Personstamm) {
			return toXMLString((ch.vd.capitastra.rechteregister.Personstamm) rechteinhaber);
		}
		else if (rechteinhaber instanceof Grundstueck) {
			return toXMLString((Grundstueck) rechteinhaber);
		}
		else {
			throw new IllegalArgumentException("Type d'ayant-droit inconnu = [" + rechteinhaber.getClass() + "]");
		}
	}

	@Override
	public String toXMLString(GrundstueckNummerElement obj) {
		try {
			final Marshaller m = communeContext.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			final StringWriter w = new StringWriter();
			final QName name = buildGrundstueckQName(obj);
			m.marshal(new JAXBElement<>(name, (Class<GrundstueckNummerElement>) obj.getClass(), null, obj), w);
			return w.toString();
		}
		catch (JAXBException e) {
			e.printStackTrace();
			return e.getMessage();
		}
	}

	@Override
	public String toXMLString(Dienstbarkeit obj) {
		try {
			final Marshaller m = servitudeContext.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			final StringWriter w = new StringWriter();
			final QName name = buildRechtQName(obj);
			m.marshal(new JAXBElement<>(name, (Class<Dienstbarkeit>) obj.getClass(), null, obj), w);
			return w.toString();
		}
		catch (JAXBException e) {
			e.printStackTrace();
			return e.getMessage();
		}
	}

	@Override
	public String toXMLString(DienstbarkeitExtended obj) {
		try {
			final Marshaller m = servitudeEtendueContext.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			final StringWriter w = new StringWriter();
			final QName name = buildRechtQName(obj);
			m.marshal(new JAXBElement<>(name, (Class<DienstbarkeitExtended>) obj.getClass(), null, obj), w);
			return w.toString();
		}
		catch (JAXBException e) {
			e.printStackTrace();
			return e.getMessage();
		}
	}

	@Override
	public String toXMLString(LastRechtGruppe obj) {
		try {
			final Marshaller m = groupeBeneficiairesContext.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			final StringWriter w = new StringWriter();
			final QName name = buildRechtQName(obj);
			m.marshal(new JAXBElement<>(name, (Class<LastRechtGruppe>) obj.getClass(), null, obj), w);
			return w.toString();
		}
		catch (JAXBException e) {
			e.printStackTrace();
			return e.getMessage();
		}
	}

	@Override
	public String toXMLString(ch.vd.capitastra.rechteregister.Personstamm obj) {
		try {
			final Marshaller m = beneficiaireContext.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			final StringWriter w = new StringWriter();
			final QName name = buildRechtQName(obj);
			m.marshal(new JAXBElement<>(name, (Class<ch.vd.capitastra.rechteregister.Personstamm>) obj.getClass(), null, obj), w);
			return w.toString();
		}
		catch (JAXBException e) {
			e.printStackTrace();
			return e.getMessage();
		}
	}

	@NotNull
	private static QName buildGrundstueckQName(@NotNull Object o) {
		final String simpleName = o.getClass().getSimpleName().replaceAll("Element$", "");
		return new QName(FichierImmeublesRFParser.GRUNDSTUECK_NAMESPACE, simpleName);
	}

	@NotNull
	private static QName buildRechtQName(@NotNull Object o) {
		final String simpleName = o.getClass().getSimpleName().replaceAll("Element$", "");
		return new QName(FichierServitudeRFParser.RECHTREGISTER_NAMESPACE, simpleName);
	}

}

