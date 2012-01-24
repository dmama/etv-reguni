<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Mouvement de dossier -->
<fieldset class="information">
	<legend><span><fmt:message key="caracteristiques.mouvement.dossier" /></span></legend>

	<table>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.type.mouvement" />&nbsp;:</td>
			<td width="25%">
				<form:select path="typeMouvement" items="${typesMouvement}" 
						id="type_mouvement" onchange="selectTypeMouvement(this.options[this.selectedIndex].value);" /> 			
			</td>
			<td width="50%" colspan="2">&nbsp;</td>
			<%-- 
			<td width="25%"><fmt:message key="label.date.mouvement" />&nbsp;:</td>
			<td width="25%">
				<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
					<jsp:param name="path" value="dateMouvement" />
					<jsp:param name="id" value="dateMouvement" />
				</jsp:include>
			</td>
			--%>
		</tr>
	</table>
	
	<hr />
	
	<div id="envoi">
	<table>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.destination" />&nbsp;:</td>
			<td width="25%">
				<form:radiobutton path="destinationEnvoi" value="utilisateurEnvoi" onclick="selectEnvoi('utilisateurEnvoi');" /><label for="utilisateur"><fmt:message key="label.utilisateur" /></label>
			</td>
			<td width="50%" colspan="2">
				<div id="utilisateursEnvoi" <c:if test="${param.depuisTache == 'true'}">style="display:none;"</c:if> >
					<form:input path="utilisateurEnvoi" id="utilisateurEnvoi" />
					<form:errors path="utilisateurEnvoi" cssClass="error"/>
					<form:hidden path="numeroUtilisateurEnvoi" id="numeroUtilisateurEnvoi"  />
					<script>
						$(function() {
							Autocomplete.security('user', '#utilisateurEnvoi', false, function(item) {
								if (item) {
									$('#numeroUtilisateurEnvoi').val(item.id2); // le numéro technique
								}
								else {
									$('#utilisateurEnvoi').val(null);
									$('#numeroUtilisateurEnvoi').val(null);
								}
							});
						});
					</script>
				</div>
			</td>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%">&nbsp;</td>
			<td width="25%">
				<form:radiobutton path="destinationEnvoi" value="collectivite" onclick="selectEnvoi('collectivite');" /><label for="collectivite"><fmt:message key="label.collectivite.administrative" /></label>
			</td>
			<td width="50%" colspan="2">
				<div id="collectivites" <c:if test="${param.depuisTache != 'true'}">style="display:none;"</c:if> >
					<form:input path="collAdmDestinataireEnvoi" id="collAdmDestinataireEnvoi" />
					<form:hidden path="noCollAdmDestinataireEnvoi" id="noCollAdmDestinataireEnvoi"  />
					<script>
						$(function() {
							Autocomplet.infra('collectiviteAdministrative', '#collAdmDestinataireEnvoi', true, function(item) {
								$('#noCollAdmDestinataireEnvoi').val(item ? item.id1 : null); // le numéro de collectivité
							});
						});
					</script>
				</div>
			</td>
		</tr>
	</table>
	</div>	
		
	<div id="reception" style="display:none;">
	<table>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.destination" />&nbsp;:</td>
			<td width="25%">
				<form:radiobutton path="localisation"  value="PERSONNE" onclick="selectReception('PERSONNE');" /><label for="PERSONNE"><fmt:message key="label.utilisateur" /></label>
			</td>
			<td width="50%" colspan="2">
				<div id="utilisateursReception">
					<form:input path="utilisateurReception" id="utilisateurReception" />
					<form:errors path="utilisateurReception" cssClass="error"/>
					<form:hidden path="numeroUtilisateurReception" id="numeroUtilisateurReception"  />
					<script>
						$(function() {
							Autocomplete.security('user', '#utilisateurReception', false, function(item) {
								if (item) {
									$('#numeroUtilisateurReception').val(item.id2); // le numéro technique
								}
								else {
									$('#numeroUtilisateurEnvoi').val(null);
									$('#numeroUtilisateurReception').val(null);
								}
							});
						});
					</script>
				</div>
			</td>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%">&nbsp;</td>
			<td width="25%">
				<form:radiobutton path="localisation" value="CLASSEMENT_GENERAL" onclick="selectReception('CLASSEMENT_GENERAL');" /><label for="CLASSEMENT_GENERAL"><fmt:message key="option.localisation.CLASSEMENT_GENERAL" /></label>
			</td>
			<td width="50%" colspan="2">&nbsp;</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%">&nbsp;</td>
			<td width="25%">
				<form:radiobutton path="localisation" value="CLASSEMENT_INDEPENDANTS" onclick="selectReception('CLASSEMENT_INDEPENDANTS');" /><label for="CLASSEMENT_INDEPENDANTS"><fmt:message key="option.localisation.CLASSEMENT_INDEPENDANTS" /></label>
			</td>
			<td width="50%" colspan="2">&nbsp;</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%">&nbsp;</td>
			<td width="25%">
				<form:radiobutton path="localisation" value="ARCHIVES" onclick="selectReception('ARCHIVES');" /><label for="ARCHIVES"><fmt:message key="option.localisation.ARCHIVES" /></label>
			</td>
			<td width="50%" colspan="2">&nbsp;</td>
		</tr>
	</table>
	</div>

	<script type="text/javascript" language="Javascript" src="<c:url value="/js/mouvement.js"/>"></script>

</fieldset>
<!-- Fin  Mouvement de dossier -->