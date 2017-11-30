<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="depuisTache" value="${param.depuisTache}" />

<%--@elvariable id="command" type="ch.vd.uniregctb.documentfiscal.AutreDocumentFiscalView"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.edition.document.fiscal" /></tiles:put>
  	<%--<tiles:put name="fichierAide"><li><a href="#" onClick="ouvrirAide('<c:url value="/docs/maj-di.pdf"/>');" title="AccessKey: a" accesskey="e">Aide</a></li></tiles:put>--%>
  	<tiles:put name="fichierAide"><li><a href="#" title="AccessKey: a" accesskey="e">Aide</a></li></tiles:put>
	<tiles:put name="body">

		<unireg:nextRowClass reset="1"/>
		<unireg:bandeauTiers numero="${command.tiersId}" showAvatar="false" showValidation="false" showEvenementsCivils="false" showLinks="false" />

		<!-- Debut Document fiscal -->
		<fieldset class="information">
			<legend><span><fmt:message key="label.caracteristiques.document.fiscal" /></span></legend>

			<table border="0">
				<tr class="<unireg:nextRowClass/>" >
					<td width="25%"><fmt:message key="label.autre.document.type" />&nbsp;:</td>
					<td width="25%"><c:if test="${command.libelleTypeDocument != null}"><fmt:message key="${command.libelleTypeDocument}"/></c:if></td>
				</tr>
				<tr class="<unireg:nextRowClass/>" >
					<td width="25%"><fmt:message key="label.autre.document.sous.type" />&nbsp;:</td>
					<td width="75%"><c:if test="${command.libelleSousType != null}"><fmt:message key="${command.libelleSousType}"/></c:if></td>
				</tr>
				<c:if test="${!(command.periodeFiscale == 0)}">
					<tr class="<unireg:nextRowClass/>" >
						<td width="25%"><fmt:message key="label.periode.fiscale" />&nbsp;:</td>
						<td width="25%">${command.periodeFiscale}</td>
						<td width="50%"></td>
					</tr>
				</c:if>
				<tr class="<unireg:nextRowClass/>" >
					<td width="25%"><fmt:message key="label.etat.courant" />&nbsp;:</td>
					<td width="25%"><fmt:message key="option.etat.avancement.m.${command.etat}"/></td>
					<td width="50%"></td>
				</tr>
			</table>
		</fieldset>
		<!-- Fin  Document fiscal -->

		<!-- Debut Delais -->
		<jsp:include page="delai/lister.jsp"/>
		<!-- Fin Delais -->

		<!-- Debut Etats -->
		<jsp:include page="etat/lister.jsp"/>
		<!-- Fin Etats -->

		<div style="margin-top:1em;">
			<!-- Debut Boutons -->
			<unireg:buttonTo name="Retour" action="/autresdocs/edit-list.do" id="boutonRetour" method="get" params="{pmId:${command.tiersId}}"/>

			<!-- Duplicata Document fiscal -->
			<input type="button" id="bouton_duplicata" value="<fmt:message key="label.bouton.imprimer.duplicata" />" onclick="return open_imprime_docfisc(${command.id});">
			<script type="text/javascript">
				function open_imprime_docfisc(id) {
					var dialog = Dialog.create_dialog_div('imprime-docfisc-dialog');

					// charge le contenu de la boîte de dialogue
					dialog.load(App.curl('/autresdocs/duplicata.do') + '?id=' + id + '&' + new Date().getTime());

					dialog.dialog({
						              title: "Impression d'un duplicata",
						              height: 350,
						              width:  500,
						              modal: true,
						              buttons: {
							              "Imprimer": function() {
								              // les boutons ne font pas partie de la boîte de dialogue (au niveau du DOM), on peut donc utiliser le sélecteur jQuery normal

								              // correction des nombres de feuilles invalides
								              var form = dialog.find('#formImpression');
								              var invalidNumbers = form.find(':text').filter(function() {return !(/^[0-9]+/.test(this.value));});
								              invalidNumbers.val('0');

								              // il doit y avoir au moins une feuille de demandée
								              var nbtotal = 0;
								              form.find(":text").each(function() {nbtotal += Number($(this).val());});
								              if (nbtotal < 1) {
									              alert("Il faut sélectionner au moins une feuille à imprimer !");
									              return;
								              }

								              var buttons = $('.ui-button');
								              buttons.each(function () {
									              if ($(this).text() == 'Imprimer') {
										              $(this).addClass('ui-state-disabled');
										              $(this).attr('disabled', true);
									              }
								              });

								              form.attr('action', App.curl('/autresdocs/duplicata.do'));
								              form.submit();
							              },
							              "Fermer": function() {
								              dialog.dialog("close");
							              }
						              }
					              });
				}
			</script>

			<!-- Annulation Document fiscal -->
			<unireg:buttonTo name="Annuler document fiscal" confirm="Voulez-vous vraiment annuler ce document fiscal ?"
			                 action="/autresdocs/annuler.do" method="post" params='{id:${command.id}}'/>
		</div>

	</tiles:put>
</tiles:insert>
