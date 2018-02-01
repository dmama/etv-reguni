<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="command" type="ch.vd.unireg.evenement.organisation.view.EvenementOrganisationDetailView"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
<tiles:put name="title"><fmt:message key="label.caracteristiques.evenement.organisation"/></tiles:put>
<tiles:put name="fichierAide">
	<li>
		<c:url var="doc" value="/docs/evenements.pdf"/>
		<a href="#" onClick="ouvrirAide('${doc}');" title="AccessKey: a" accesskey="e">Aide</a>
	</li>
</tiles:put>
<tiles:put name="body">

<%@ include file="/WEB-INF/jsp/evenement/organisation/detail.jsp" %>

</tiles:put>
</tiles:insert>
