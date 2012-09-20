<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="head"></tiles:put>

	<tiles:put name="title">
		<fmt:message key="title.ajout.droit.acces">
			<fmt:param><unireg:numCTB numero="${command.numero}"/></fmt:param>
		</fmt:message>
	</tiles:put>

	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/acces-par-dossier.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
	<tiles:put name="body">
	<form:form name="formAddRestriction" id="formAddRestriction">
	<fieldset class="information">
		
		<table>
			<tr class="<unireg:nextRowClass/>" >
				<td width="25%"><fmt:message key="label.utilisateur" />&nbsp;:</td>
				<td width="75%" colspan="3">
					<div id="utilisateurs">
						<form:input path="utilisateur" id="utilisateur" />
						<form:errors path="utilisateur" cssClass="error"/>
						<form:hidden path="numeroUtilisateur" id="numeroUtilisateur"  />
						<script>
							$(function() {
								Autocomplete.security('user', '#utilisateur', false, function(item) {
									if (item) {
										$('#numeroUtilisateur').val(item.id2); // le numéro technique
									}
									else {
										$('#utilisateur').val(null);
										$('#numeroUtilisateur').val(null);
									}
								});
							});
						</script>
					</div>
				</td>
			</tr>

			<tr class="<unireg:nextRowClass/>" >
				<td width="25%"><fmt:message key="label.type.restriction" />&nbsp;:</td>
				<td width="25%">
					<form:select path="type" items="${typesDroitAcces}" />
				</td>
				<td width="25%"><fmt:message key="label.lecture.seule" />&nbsp;:</td>
				<td width="25%">
					<form:checkbox path="lectureSeule" />
				</td>
			</tr>
		</table>
		
	</fieldset>
	
	<table>
		<tr>
			<td width="25%">&nbsp;</td>
			<td width="25%">
				<input type="button" id="ajouter" value="<fmt:message key="label.bouton.ajouter" />" onclick="javascript:ajouterRestriction();">
			</td>
			<td width="25%">
				<c:if test="${!command.ajoutEffectue}">
					<input type="button" id="annuler" value="<fmt:message key="label.bouton.annuler" />" onclick="document.location.href='restrictions-pp.do?numero=${command.numero}'">
				</c:if>
				<c:if test="${command.ajoutEffectue}">
					<input type="button" id="retour" value="<fmt:message key="label.bouton.retour" />" onclick="document.location.href='restrictions-pp.do?numero=${command.numero}'">
				</c:if>
			</td>
			<td width="25%">&nbsp;</td>
		</tr>
	</table>
	</form:form>
	<script type="text/javascript" language="Javascript1.3">
		function ajouterRestriction() {
			var formAddRestriction = document.getElementById('formAddRestriction');
			formAddRestriction.submit(); 	
		}
		
	</script>
	</tiles:put>
</tiles:insert>

