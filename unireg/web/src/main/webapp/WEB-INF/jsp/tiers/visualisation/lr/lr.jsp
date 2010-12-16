<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<unireg:nextRowClass reset="1"/>
<tiles:insert template="/WEB-INF/jsp/templates/templateDialog.jsp">

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
			<tr class="<unireg:nextRowClass/>" >
				<td width="50%"><fmt:message key="label.date.debut.periode" /> :</td>
				<td width="50%">
					<fmt:formatDate value="${command.dateDebutPeriode}" pattern="dd.MM.yyyy"/>
				</td>
			</tr>
			<tr class="<unireg:nextRowClass/>" >
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

	</tiles:put>
</tiles:insert>