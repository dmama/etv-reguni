<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<%--@elvariable id="immeuble" type="ch.vd.uniregctb.registrefoncier.ResumeImmeubleView"--%>

<span><%-- span vide pour que IE8 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
<fieldset class="information">
	<legend><span><fmt:message key="label.resume.immeuble"/></span></legend>
	<table border="0" class="display_table">
		<unireg:nextRowClass reset="1"/>
		<tr class="<unireg:nextRowClass/>">
			<td style="width: 15%;"><fmt:message key="label.commune"/>&nbsp;:</td>
			<td style="width: 30%;"><span title="${immeuble.ofsCommune}"><c:out value="${immeuble.nomCommune}"/></span></td>
		</tr>
		<tr class="<unireg:nextRowClass/>">
			<td style="width: 15%;"><fmt:message key="label.parcelle"/>&nbsp;:</td>
			<td style="width: 30%;"><c:out value="${immeuble.noParcelleComplet}"/></td>
		</tr>
		<tr class="<unireg:nextRowClass/>">
			<td style="width: 15%;"><fmt:message key="label.nature"/>&nbsp;:</td>
			<td style="width: 30%;"><c:out value="${immeuble.nature}"/></td>
		</tr>
		<tr class="<unireg:nextRowClass/>">
			<td style="width: 15%;"><fmt:message key="label.estimation.fiscale"/>&nbsp;:</td>
			<td style="width: 30%;"><fmt:formatNumber value="${immeuble.estimationFiscale}"/></td>
		</tr>
		<tr class="<unireg:nextRowClass/>">
			<td style="width: 15%;"><fmt:message key="label.ref.estimation.fiscale"/>&nbsp;:</td>
			<td style="width: 30%;"><c:out value="${immeuble.referenceEstimationFiscale}"/></td>
		</tr>
	</table>
</fieldset>
