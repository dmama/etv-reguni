<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="head" />
	<tiles:put name="title">
		<fmt:message key="label.batch.gestion" />
	</tiles:put>
	<tiles:put name="body">
	
		<!-- La liste des batchs actifs ou récemment terminés -->
		<fieldset>
			<legend><span><fmt:message key="label.batch.actif"/></span></legend> 
			<div id="jobsActif"></div>
		</fieldset>

		<script type="text/javascript" language="Javascript1.3">
			function Batch_toggleExpand(element, id) {
				element = E$('ARGS_'+ id);
				var expand = (Element.getStyle(element).display == 'none');
				Element.toggle(element);
				var img = E$('IMG_'+ id);
				if (expand)
					img.src = img.src.replace('plus','minus');
				else
					img.src = img.src.replace('minus','plus');
			}
		</script>

		<!-- La liste des batchs avec les formulaires pour les faire démarrer -->
		<fieldset>
			<legend><span><fmt:message key="label.batch.liste"/></span></legend>
			<c:if test="${not empty command.jobs}">
				<display:table name="${command.jobs}" id="job" class="tableJob">
					<display:column title="Catégorie" property="categorie"></display:column>
					<display:column title="Nom"><unireg:batchForm job="${job}" /></display:column>
					<display:column title="Dernière Progression" property="runningMessage"></display:column>
					<%--div du statut : voir WebitIncontainerTestingJobTest --%>
					<display:column title="Dernier Statut"><div id="${job.name}-status">${job.status}</div></display:column>
					<display:column title="Dernier Démarrage" property="lastStart"></display:column>
					<display:column title="Dernière Durée" property="duration"></display:column>
					<display:column title="Rapport"><unireg:document doc="${job.lastRunReport}"/></display:column>
				</display:table>
			</c:if>
	 	</fieldset>
		
		<script type="text/javascript" language="Javascript1.3"><!--
			
			var requestDone = true;
			var periodicalExecuter = new PeriodicalExecuter(function() {
				if ( !requestDone)
					return;
				requestDone = false;
				XT.doAjaxAction('loadJobActif', E$("jobsActif"), {},
				{ 
					clearQueryString: true,
					errorHandler :  function(ajaxRequest, exception) {
							requestDone = true;
						}
    			});
			}, 3);
			
			periodicalExecuter.onTimerEvent();
			
			function onRecieved() {
				requestDone = true;
			}
			
			function startJob(name) {
				var form = F$(name);
				XT.doAjaxSubmit('startJob', form, null,
					{
						formId : form.id,
						enableUpload : true,
						clearQueryString: true
			    	}
		    	);
			}
			
			function stopJob(name) {
				XT.doAjaxAction('stopJob', E$("jobsActif"),
					{
						jobName :name
					},
					{
				 		clearQueryString: true
	    			}
	    		);
			}
			
			function showAlert(options) {
                alert(options.message);
            }
		--></script>
	</tiles:put>
</tiles:insert>
