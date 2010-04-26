<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="ligneTableau" value="${1}" scope="request" />
<tiles:insert template="/WEB-INF/jsp/templates/templateIFrame.jsp">
	<tiles:put name="head"></tiles:put>

	<tiles:put name="title"></tiles:put>
	<tiles:put name="body">
	
<!-- Debut Debiteur Impot a la source -->
	<jsp:include page="../../../general/debiteur.jsp">
		<jsp:param name="page" value="edit"/>
		<jsp:param name="path" value="dpi"/>
	</jsp:include>
<!-- Fin Debiteur Impot a la source -->

<!-- Debut LR -->

	<fieldset class="information">
	<legend><span><fmt:message key="label.caracteristiques.lr" /></span></legend>
		<table>
			<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
			<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
				<td width="50%"><fmt:message key="label.date.debut.periode" /> :</td>
				<td width="50%">
					<fmt:formatDate value="${command.dateDebutPeriode}" pattern="dd.MM.yyyy"/>
				</td>
			</tr>
			<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
			<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
				<td><fmt:message key="label.date.fin.periode" /> :</td>
				<td>
					<fmt:formatDate value="${command.dateFinPeriode}" pattern="dd.MM.yyyy"/>
				</td>
			</tr>
		</table>
	</fieldset>

<!-- Fin LR -->

<!-- Debut Delais -->
 
	<jsp:include page="../../common/delai/delais.jsp">
		<jsp:param name="page" value="visu"/>
	</jsp:include>

<!-- Fin Delais -->

<!-- Debut Etats declaration -->

	<jsp:include page="../../common/etat/etats.jsp" />

<!-- Fin Etats declaration -->

	<table>
		<tr>
			<td><input type="button" id="annuler" value="<fmt:message key="label.bouton.fermer" />" onclick="self.parent.tb_remove()"></td>
		</tr>
	</table>

	</tiles:put>
</tiles:insert>