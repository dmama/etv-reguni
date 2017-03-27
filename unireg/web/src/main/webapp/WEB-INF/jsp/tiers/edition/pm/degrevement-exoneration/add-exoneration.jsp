<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="idContribuable" type="java.lang.Long"--%>
<%--@elvariable id="immeuble" type="ch.vd.uniregctb.registrefoncier.ResumeImmeubleView"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<fmt:message key="title.exoneration.ifonc.ajout"/>
	</tiles:put>
	<tiles:put name="body">
		<table border="0">
			<tr style="vertical-align: top;">
				<td style="width: 50%; padding-right: 10px;">
					<unireg:bandeauTiers numero="${idContribuable}" showAvatar="false" showValidation="false" showEvenementsCivils="false" showLinks="false" titre="Caractéristiques du contribuable"/>
				</td>
				<td>
					<jsp:include page="../../../common/degrevement-exoneration/resume-immeuble-fieldset.jsp"/>
				</td>
			</tr>
		</table>

		<form:form commandName="addExonerationCommand" action="add-exoneration.do">

			<form:hidden path="idContribuable"/>
			<form:hidden path="idImmeuble"/>

			<fieldset>
				<legend><span><fmt:message key="title.exoneration.ifonc.ajout"/></span></legend>
				<jsp:include page="../../../common/degrevement-exoneration/edit-exoneration-table.jsp">
					<jsp:param name="commandName" value="addExonerationCommand"/>
					<jsp:param name="allowPeriodeDebutEdit" value="true"/>
				</jsp:include>
			</fieldset>

			<!-- Debut Bouton -->
			<div style="padding: 0 25%;">
				<div style="padding: 0 20%; display: inline">
					<input type="submit" value="<fmt:message key='label.bouton.ajouter'/>"/>
				</div>
				<div style="padding: 0 20%; display: inline">
					<unireg:buttonTo name="Retour" action="/degrevement-exoneration/edit-exonerations.do" params="{idContribuable:${idContribuable},idImmeuble:${immeuble.idImmeuble}}" method="GET"/>
				</div>
			</div>
			<!-- Fin Bouton -->

		</form:form>

	</tiles:put>

</tiles:insert>
