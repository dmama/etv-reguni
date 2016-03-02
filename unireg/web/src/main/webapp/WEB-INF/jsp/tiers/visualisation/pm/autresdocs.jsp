<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<%--@elvariable id="command" type="ch.vd.uniregctb.entreprise.TiersVisuView"--%>

<unireg:setAuth var="autorisations" tiersId="${command.entreprise.id}"/>

<span><%-- span vide pour que IE8 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
<fieldset>
	<legend><span><fmt:message key="label.autres.documents.fiscaux"/></span></legend>

	<c:if test="${not empty command.autresDocumentsFiscaux}">
		<display:table name="${command.autresDocumentsFiscaux}" id="docFiscal" requestURI="visu.do" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
			<display:column sortable="true" titleKey="label.type">
				${docFiscal.libelleTypeDocument}
			</display:column>
			<display:column sortable="true" titleKey="label.type">
				${docFiscal.libelleSousType}
			</display:column>
			<display:column sortable ="true" titleKey="label.date.envoi" sortProperty="dateEnvoi">
				<unireg:regdate regdate="${docFiscal.dateEnvoi}"/>
			</display:column>
			<display:column sortable ="true" titleKey="label.date.delai.accorde" sortProperty="delaiRetour">
				<unireg:regdate regdate="${docFiscal.delaiRetour}"/>
			</display:column>
			<display:column sortable ="true" titleKey="label.date.retour" sortProperty="dateRetour">
				<unireg:regdate regdate="${docFiscal.dateRetour}"/>
			</display:column>
			<display:column sortable ="true" titleKey="label.etat.avancement" >
				<fmt:message key="option.etat.avancement.${docFiscal.etat}" />
			</display:column>
			<display:column class="action">
				<unireg:consulterLog entityNature="AutreDocumentFiscal" entityId="${docFiscal.id}"/>
			</display:column>
		</display:table>

	</c:if>

</fieldset>
