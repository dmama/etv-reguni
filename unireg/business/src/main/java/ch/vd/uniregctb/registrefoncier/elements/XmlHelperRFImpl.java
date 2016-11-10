package ch.vd.uniregctb.registrefoncier.elements;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import java.io.StringWriter;

import org.jetbrains.annotations.NotNull;

import ch.vd.capitastra.grundstueck.Bodenbedeckung;
import ch.vd.capitastra.grundstueck.Gebaeude;
import ch.vd.capitastra.grundstueck.Grundstueck;
import ch.vd.capitastra.grundstueck.GrundstueckExport;
import ch.vd.capitastra.grundstueck.PersonEigentumAnteil;
import ch.vd.capitastra.grundstueck.Personstamm;
import ch.vd.uniregctb.registrefoncier.FichierImmeublesRFParser;

public class XmlHelperRFImpl implements XmlHelperRF {

	private final JAXBContext immeubleContext;
	private final JAXBContext droitContext;
	private final JAXBContext proprietaireContext;
	private final JAXBContext batimentContext;
	private final JAXBContext surfacesAuSolContext;
	private final JAXBContext surfaceListContext;
	private final JAXBContext autreDroitContext;

	public XmlHelperRFImpl() throws JAXBException {
		immeubleContext = JAXBContext.newInstance(BergwerkElement.class, FolioElement.class, GewoehnlichesMiteigentumElement.class,
		                                          LiegenschaftElement.class, SdrElement.class, StockwerksEinheitElement.class,
		                                          UnbekanntesGrundstueckElement.class);
		droitContext = JAXBContext.newInstance(PersonEigentumAnteilElement.class);
		proprietaireContext = JAXBContext.newInstance(NatuerlichePersonstammElement.class, JuristischePersonstammElement.class);
		batimentContext = JAXBContext.newInstance(GebaeudeElement.class);
		surfacesAuSolContext = JAXBContext.newInstance(BodenbedeckungElement.class);
		surfaceListContext = JAXBContext.newInstance(BodenbedeckungListElement.class);
		autreDroitContext = JAXBContext.newInstance(DienstbarkeitElement.class);
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
	public JAXBContext getAutreDroitContext() {
		return autreDroitContext;
	}

	@Override
	public String toXMLString(Grundstueck obj) {
		try {
			final Marshaller m = immeubleContext.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			final StringWriter w = new StringWriter();
			final QName name = buildQName(obj);
			m.marshal(new JAXBElement<>(name, (Class<Grundstueck>) obj.getClass(), null, obj), w);
			return w.toString();
		}
		catch (JAXBException e) {
			return e.getMessage();
		}
	}

	@Override
	public String toXMLString(PersonEigentumAnteil obj) {
		try {
			final Marshaller m = droitContext.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			final StringWriter w = new StringWriter();
			final QName name = buildQName(obj);
			m.marshal(new JAXBElement<>(name, (Class<PersonEigentumAnteil>) obj.getClass(), null, obj), w);
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
			final QName name = buildQName(obj);
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
			final QName name = buildQName(obj);
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
			final QName name = buildQName(obj);
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
			final QName name = buildQName(obj);
			m.marshal(new JAXBElement<>(name, (Class<GrundstueckExport.BodenbedeckungList>) obj.getClass(), null, obj), w);
			return w.toString();
		}
		catch (JAXBException e) {
			return e.getMessage();
		}
	}

	@NotNull
	private static QName buildQName(@NotNull Object o) {
		final String simpleName = o.getClass().getSimpleName().replaceAll("Element$", "");
		return new QName(FichierImmeublesRFParser.GRUNDSTUECK_NAMESPACE, simpleName);
	}

}

