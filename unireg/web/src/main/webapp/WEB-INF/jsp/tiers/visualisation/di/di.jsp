<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<unireg:nextRowClass reset="1"/>
<tiles:insert template="/WEB-INF/jsp/templates/templateDialog.jsp">
	<tiles:put name="body">
	
	<!-- Debut Contribuable -->
	<jsp:include page="../../../general/contribuable.jsp">
		<jsp:param name="page" value="edit"/>
		<jsp:param name="path" value="contribuable"/>
	</jsp:include>
	<!-- Fin Contribuable -->

	<!-- Debut DI -->
	<fieldset class="information">
	<legend><span><fmt:message key="label.caracteristiques.di" /></span></legend>
		<table>
			<tr class="<unireg:nextRowClass/>" >
				<td width="25%"><fmt:message key="label.periode.fiscale" />&nbsp;:</td>
				<td width="25%">${command.periodeFiscale}</td>
				<td width="25%"><fmt:message key="label.code.controle"/>&nbsp;:</td>
				<td width="25%">${command.codeControle}</td>
			</tr>
			<tr class="<unireg:nextRowClass/>" >
				<td width="25%"><fmt:message key="label.debut.periode.imposition" />&nbsp;:</td>
				<td width="25%">
					<fmt:formatDate value="${command.dateDebutPeriodeImposition}" pattern="dd.MM.yyyy"/>
				</td>
				<td width="25%"><fmt:message key="label.fin.periode.imposition" />&nbsp;:</td>
				<td width="25%">
					<fmt:formatDate value="${command.dateFinPeriodeImposition}" pattern="dd.MM.yyyy"/>
				</td>
			</tr>
			<tr class="<unireg:nextRowClass/>" >
				<td width="25%"><fmt:message key="label.type.declaration" />&nbsp;:</td>
				<td width="25%">
					<fmt:message key="option.type.document.${command.typeDeclarationImpot}" />
				</td>
				<td width="25%">&nbsp;</td>
				<td width="25%">&nbsp;</td>
			</tr>
		</table>
	</fieldset>
	<!-- Fin DI -->

	<!-- Debut delais documents -->
	<jsp:include page="../../common/delai/delais.jsp">
		<jsp:param name="page" value="visu"/>
	</jsp:include>
	<!-- Fin Delais documents -->

	<!-- Debut Etats declaration -->
	<jsp:include page="../../common/etat/etats.jsp" />
	<!-- Fin Etats declaration -->

	</tiles:put>
</tiles:insert>