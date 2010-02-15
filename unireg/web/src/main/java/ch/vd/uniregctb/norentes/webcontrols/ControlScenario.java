package ch.vd.uniregctb.norentes.webcontrols;

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import annotation.CheckAttribute;
import annotation.EtapeAttribute;
import ch.vd.uniregctb.norentes.common.NorentesFactory;
import ch.vd.uniregctb.norentes.common.NorentesManager;
import ch.vd.uniregctb.norentes.common.NorentesScenario;
import ch.vd.uniregctb.norentes.common.ScenarioEtat;
import ch.vd.uniregctb.norentes.common.NorentesContext.EtapeContext;
import ch.vd.uniregctb.web.HtmlTextWriter;
import ch.vd.uniregctb.web.HtmlTextWriterAttribute;
import ch.vd.uniregctb.web.HtmlTextWriterStyle;
import ch.vd.uniregctb.web.HtmlTextWriterTag;
import ch.vd.uniregctb.web.xt.component.PlaceHolder;

public final class ControlScenario extends PlaceHolder {

	private static final long serialVersionUID = -7157557464923111550L;

	private NorentesScenario scenario;

	public ControlScenario(HttpServletRequest request) {
		super(request);
	}

	public NorentesScenario getScenario() {
		return scenario;
	}

	public void setScenario(NorentesScenario scenario) {
		this.scenario = scenario;
	}

	@Override
	public void renderContent(HtmlTextWriter writer) {
		writer.addAttribute(HtmlTextWriterAttribute.Class, "title-norentes");
		writer.renderBeginTag(HtmlTextWriterTag.Div);
		writer.write("Scénario ");
		writer.write(getScenario().getDescription());
		writer.renderEndTag(); // tag div
		writer.addAttribute(HtmlTextWriterAttribute.Class, "norentes");
		writer.renderBeginTag(HtmlTextWriterTag.Table);
		renderHeader(writer);
		renderEtapes(writer);
		writer.renderEndTag(); // tag Table
	}

	@Override
	protected void onUnload() {
		super.onUnload();
		scenario = null;
	}

	protected void renderHeader(HtmlTextWriter writer) {
		writer.renderBeginTag(HtmlTextWriterTag.Thead);
		writer.renderBeginTag(HtmlTextWriterTag.Tr);
		// Etape
		writer.addAttribute(HtmlTextWriterAttribute.Align, "center");
		writer.renderBeginTag(HtmlTextWriterTag.Th);
		writer.write("Etape");
		writer.renderEndTag(); // tag th
		// Action
		writer.addAttribute(HtmlTextWriterAttribute.Align, "center");
		writer.renderBeginTag(HtmlTextWriterTag.Th);
		writer.renderEndTag(); // tag th
		// Description
		writer.addAttribute(HtmlTextWriterAttribute.Align, "center");
		writer.addAttribute(HtmlTextWriterAttribute.Width, "30%");
		writer.renderBeginTag(HtmlTextWriterTag.Th);
		writer.write("Description");
		writer.renderEndTag(); // tag th
		// Etat
		writer.addAttribute(HtmlTextWriterAttribute.Align, "center");
		writer.renderBeginTag(HtmlTextWriterTag.Th);
		writer.write("Etat");
		writer.renderEndTag(); // tag th
		// Commentaire
		writer.addAttribute(HtmlTextWriterAttribute.Align, "center");
		writer.addAttribute(HtmlTextWriterAttribute.Width, "60%");
		writer.renderBeginTag(HtmlTextWriterTag.Th);
		writer.write("Message");
		writer.renderEndTag(); // tag th

		writer.renderEndTag(); // tag tr

		writer.renderEndTag(); // tag thead
	}

	protected void renderEtapes(HtmlTextWriter writer) {
		writer.renderBeginTag(HtmlTextWriterTag.Tbody);
		NorentesScenario scenario = getScenario();
		Collection<EtapeAttribute> etapes = scenario.getEtapeAttributes();
		NorentesManager manager = NorentesFactory.getNorentesManager();
		int count = 0;
		for (EtapeAttribute etape : etapes) {
			EtapeContext etapeContext = manager.getEtapeContext(scenario, etape.getIndex());
			// writer.addAttribute(HtmlTextWriterAttribute.Class, (count % 2 == 0 ? "odd" : "even"));
			writer.renderBeginTag(HtmlTextWriterTag.Tr);
			// etape
			writer.addAttribute(HtmlTextWriterAttribute.Align, "center");
			writer.renderBeginTag(HtmlTextWriterTag.Td);
			writer.write(etape.getIndex());
			writer.renderEndTag(); // tag td
			// Action
			writer.addAttribute(HtmlTextWriterAttribute.Align, "center");
			writer.renderBeginTag(HtmlTextWriterTag.Td);
			writer.addAttribute(HtmlTextWriterAttribute.Class, "start");
			writer.addAttribute(HtmlTextWriterAttribute.Id, "buttonStartEtape");
			writer.addAttribute(HtmlTextWriterAttribute.Href, "#");
			writer.addAttribute(HtmlTextWriterAttribute.Onclick, "javascript:startEtape(this, " + etape.getIndex() + " );");
			writer.renderBeginTag(HtmlTextWriterTag.A);
			writer.renderEndTag(); // tag A
			writer.renderEndTag(); // tag td
			// description
			writer.addAttribute(HtmlTextWriterAttribute.Valign, "top");
			writer.addAttribute(HtmlTextWriterAttribute.Nowrap, "nowrap");
			writer.renderBeginTag(HtmlTextWriterTag.Td);
			writer.writeLine(etape.getDescription());
			CheckAttribute check = etape.getCheckAttribute();
			if (check != null) {
				writer.addAttribute(HtmlTextWriterAttribute.Class, "check");
				writer.renderBeginTag(HtmlTextWriterTag.Div);
				writer.write(check.getDescription());
				writer.renderEndTag(); // tag div
			}
			writer.renderEndTag(); // tag td

			// etat
			writer.addAttribute(HtmlTextWriterAttribute.Align, "center");
			writer.addAttribute(HtmlTextWriterAttribute.Valign, "top");
			writer.renderBeginTag(HtmlTextWriterTag.Td);
			writer.addAttribute(HtmlTextWriterAttribute.Id, "etat-etape-" + etape.getIndex());
			writer.renderBeginTag(HtmlTextWriterTag.Div);
			writer.renderEndTag();
			if (etapeContext != null) {
				renderState(writer, etapeContext.getStateEtape(), "Etat de l'etape");
				if (etape.hasCheckAssociated()) {
					renderState(writer, etapeContext.getStateCheck(), "Etat du check");
				}
				// renderState(writer, etapeContext.getState(), "Etat global de l'etape");
			}
			writer.renderEndTag(); // tag td
			// commentaire
			writer.addAttribute(HtmlTextWriterAttribute.Valign, "top");
			writer.renderBeginTag(HtmlTextWriterTag.Td);
			if (etapeContext != null && etapeContext.getReturnedMessage() != null) {
				writer.write(etapeContext.getReturnedMessage());
			}
			writer.renderEndTag(); // tag td
			writer.renderEndTag(); // tag tr
			count++;
		}
		writer.renderEndTag(); // tag tbody
	}

	private void renderState(HtmlTextWriter writer, ScenarioEtat etat, String title) {
		writer.addAttribute(HtmlTextWriterAttribute.Title, title);
		writer.addStyleAttribute(HtmlTextWriterStyle.MarginTop, "4px");
		writer.addStyleAttribute(HtmlTextWriterStyle.MarginBottom, "2px");
		writer.renderBeginTag(HtmlTextWriterTag.Div);
		if (ScenarioEtat.InProgress == etat) {
			writer.addAttribute(HtmlTextWriterAttribute.Src, this.getRequest().getContextPath() + "/images/loadingAnimation.gif");
			writer.renderBeginTag(HtmlTextWriterTag.Img);
			writer.renderEndTag();
		}
		else if (ScenarioEtat.Finish == etat) {
			writer.addAttribute(HtmlTextWriterAttribute.Src, this.getRequest().getContextPath() + "/images/status-available.gif");
			writer.renderBeginTag(HtmlTextWriterTag.Img);
			writer.renderEndTag();
		}
		else if (ScenarioEtat.InError == etat) {
			writer.addAttribute(HtmlTextWriterAttribute.Src, this.getRequest().getContextPath() + "/images/status-away.gif");
			writer.renderBeginTag(HtmlTextWriterTag.Img);
			writer.renderEndTag();
		}
		writer.renderEndTag(); // tag DIV
	}

}
