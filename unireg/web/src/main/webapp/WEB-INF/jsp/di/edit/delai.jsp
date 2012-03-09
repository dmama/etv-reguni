<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="index" value="${param.index}" />
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="head"></tiles:put>

	<tiles:put name="title">
  		<fmt:message key="title.ajout.delai.di">
  			<fmt:param>${command.declarationPeriode}</fmt:param>
  			<fmt:param><unireg:date date="${command.declarationRange.dateDebut}"/></fmt:param>
  			<fmt:param><unireg:date date="${command.declarationRange.dateFin}"/></fmt:param>
  			<fmt:param><unireg:numCTB numero="${command.tiersId}"/></fmt:param>
  		</fmt:message>
	</tiles:put>
	<tiles:put name="body">
		<form:form method ="post" name="theForm" id="formAddDelai" action="delai.do">
		<input type="hidden"  name="__TARGET__" value="">
		<form:hidden path="idDeclaration" value="${command.idDeclaration}"/>
		<fieldset><legend><span><fmt:message key="label.delais" /></span></legend>
		<table border="0">
			<unireg:nextRowClass reset="0"/>
			<tr class="<unireg:nextRowClass/>" >
				<td/>
				<td/>
				<td><fmt:message key="label.date.ancien.delai"/>&nbsp;:</td>
				<td><unireg:date date="${command.oldDelaiAccorde}"/></td>
			</tr>
			<tr class="<unireg:nextRowClass/>" >
				<td><fmt:message key="label.date.demande"/>&nbsp;:</td>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="dateDemande" />
						<jsp:param name="id" value="dateDemande" />
					</jsp:include>
					<FONT COLOR="#FF0000">*</FONT>
				</td>
				<td><fmt:message key="label.date.delai.accorde"/>&nbsp;:</td>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="delaiAccordeAu" />
						<jsp:param name="id" value="delaiAccordeAu" />
					</jsp:include>
					<FONT COLOR="#FF0000">*</FONT>
				</td>
			</tr>
			<unireg:nextRowClass reset="0"/>
			<tr class="<unireg:nextRowClass/>" >
				<td><fmt:message key="label.confirmation.ecrite"/>&nbsp;:</td>
				<td>
					<form:checkbox path="confirmationEcrite" id="confirmation" onchange="toggleActionDelai();" onclick="toggleActionDelai();"/>
				</td>
				<td>&nbsp;</td>
				<td>&nbsp;</td>
			</tr>
		</table>

	</fieldset>

	<table border="0">
		<tr>
			<td width="25%">&nbsp;</td>
			<td width="25%">
				<input type="button" name="actionDelai" id="imprimer" value="Imprimer" style="display:none;" onclick="javascript:ajouterDelaiAvecConfirmation('${command.dateExpedition}','${command}');">
				<input type="button" name="actionDelai" id="ajouter" value="Ajouter" onclick="javascript:ajouterDelaiDI('${command.dateExpedition}','${command}');">
				<input type="button" name="actionDelai" id="retour" value="Retour" style="display:none;" onclick="document.location.href='../di/edit.do?action=editdi&id=' + ${command.idDeclaration}">
			</td>
			<td width="25%">
				<input type="button" id="annuler" value="Annuler" onclick="document.location.href='../di/edit.do?action=editdi&id=' + ${command.idDeclaration}">
			<td width="25%">&nbsp;</td>
		</tr>
	</table>
	</form:form>
	<script type="text/javascript" language="Javascript1.3">

	function verifierDate(dateExpedition, formAddDelai) {
		var delaiAccordeAu = formAddDelai.delaiAccordeAu.value;
		if (DateUtils.compare(DateUtils.addYear(dateExpedition, 1, 'yyyy.MM.dd' ), DateUtils.getDate(delaiAccordeAu, 'dd.MM.yyyy')) == -1) {
			return confirm("Ce délai est située plus d'un an dans le futur à compter de la date d'expédition de la DI. Voulez-vous le sauver ?");
		}
		return true;
	}

	function ajouterDelaiDI(dateExpedition) {
		var formAddDelai = document.getElementById('formAddDelai');
		if (verifierDate(dateExpedition, formAddDelai)) {
		    Form.doPostBack("theForm", "ajouter", "");
		}
	}

	function ajouterDelaiAvecConfirmation(dateExpedition) {
		var formAddDelai = document.getElementById('formAddDelai');
		if (verifierDate(dateExpedition, formAddDelai)) {
	    	Form.doPostBack("theForm", "imprimer", "");

			/*On desactive les boutons */

			var eltButtonImprime = document.getElementById('imprimer');
			var eltButtonAjout = document.getElementById('ajouter');
			var eltButtonAnnule = document.getElementById('annuler');
			var eltButtonRetour = document.getElementById('retour');
			var eltBoxConfirmation = document.getElementById('confirmation');

			eltButtonImprime.style.display = "none";
			eltButtonAjout.style.display = "none";
			eltButtonAnnule.style.display = "none";
			eltBoxConfirmation.disabled = true;
			eltButtonRetour.style.display = "";
		}
	}

    function toggleActionDelai() {
    	var elt = document.getElementById('confirmation');
    	var eltButtonImprime = document.getElementById('imprimer');
    	var eltButtonAjout = document.getElementById('ajouter');
    	if ($(elt).attr('checked')){
    		eltButtonImprime.style.display = "";
    		eltButtonAjout.style.display = "none";
    	}
    	else {
    		eltButtonImprime.style.display = "none";
    		eltButtonAjout.style.display = "";
    	}
    }

	// première exécution au chargement de la page...
	toggleActionDelai();
	
	</script>
	</tiles:put>
</tiles:insert>
