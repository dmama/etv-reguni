<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/templateSuperGra.jsp">
	<tiles:put name="menu" type="String"/>
	<tiles:put name="title" type="String">*** Mode Supergra ***</tiles:put>
	<tiles:put name="actions" type="String"/>

	<tiles:put name="body" type="String">

		<table id="exception_table" border="0">
			<tr valign="top">
				<td>
					<h3>Erreur inattendue</h3>
					<br/>
					<p>Nous sommes désolés, mais l'action demandé n'a pas pu être exécutée. Le message d'erreur est le suivant :</p>
					<div class="error_message"><c:out value="${message}"/></div>

					<p>Vous pouvez essayer de <a href="#" onclick="history.back(); return false;">revenir à la page précédente</a> ou d'annuler des changements mémorisés (menu de droite).</p>

					<unireg:callstack exception="${exception}"
					                  headerMessage="Cliquez ici pour afficher la callstack de l'exception "/>
				</td>
				<td id="actions_column">
					<jsp:include page="/WEB-INF/jsp/supergra/entityStates.jsp"/>
					<jsp:include page="/WEB-INF/jsp/supergra/actions.jsp"/>
				</td>
			</tr>
		</table>

	</tiles:put>
</tiles:insert>
