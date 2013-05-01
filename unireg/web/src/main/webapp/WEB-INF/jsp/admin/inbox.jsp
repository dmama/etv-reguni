<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<fmt:message key="label.inbox.gestion" />
	</tiles:put>
	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/BoiteReception_ExtractionFichier.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
	<tiles:put name="body">
	
		<!-- Les traitements en attente ou en cours -->
		<fieldset>
			<legend><span><fmt:message key="label.inbox.travaux.en.attente"/></span></legend>
			<div id="jobsEnAttente"></div>
		</fieldset>

		<!-- les messages présents dans l''inbox -->
		<fieldset>
			<legend><span><fmt:message key="title.inbox"/></span></legend>
			<div id="inboxContent"></div>
	 	</fieldset>

		<script type="text/javascript" language="Javascript1.3">
			
			var requestJobsEnAttenteDone = true;
			var requestInboxDone = true;
			$(document).everyTime("3s", refreshInboxPage);

			function refreshJobsEnAttente() {
				if (!requestJobsEnAttenteDone) {
					return;
				}
				requestJobsEnAttenteDone = false;
				$('#jobsEnAttente').load(App.curl("/admin/inbox/jobs.do?" + new Date().getTime()), function() {
					onReceivedJobsEnAttente();
				});
			}

			function refreshInboxContent() {
				if (!requestInboxDone) {
					return;
				}
				requestInboxDone = false;
				$('#inboxContent').load(App.curl("/admin/inbox/content.do?" + new Date().getTime()), function() {
					onReceivedInboxContent();
				});
			}

			function refreshInboxPage() {
				refreshJobsEnAttente();
			}

			refreshInboxPage();		// premier appel dès l''affichage de la page

			function onReceivedJobsEnAttente() {
				requestJobsEnAttenteDone = true;
				refreshInboxContent();
			}

			function onReceivedInboxContent() {
				requestInboxDone = true;
				refreshInboxSize();				// la méthode est définie dans template.jsp!
			}
		</script>
	</tiles:put>
</tiles:insert>
