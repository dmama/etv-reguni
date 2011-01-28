<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="head">
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/jquery.timers.js"/>"></script>
	</tiles:put>
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
				var element = $('#ARGS_'+ id);
				var expand = (element.is(":hidden"));
				element.toggle();
				var img = $('#IMG_'+ id);
				var src = img.attr('src');
				if (expand) {
					src = src.replace('plus','minus');
				}
				else {
					src = src.replace('minus','plus');
				}
				img.attr('src', src);
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
			$(document).everyTime("3s", function() {
				if ( !requestDone)
					return;
				requestDone = false;
				XT.doAjaxAction('loadJobActif', $("#jobsActif").get(0), {},
				{ 
					clearQueryString: true,
					errorHandler :  function(ajaxRequest, exception) {
							requestDone = true;
						}
    			});
			});
			
			function onRecieved() {
				requestDone = true;
			}
			
			function startJob(name) {
				var form = $('#'+name).get(0);
				XT.doAjaxSubmit('startJob', form, null,
					{
						formId : form.id,
						enableUpload : true,
						clearQueryString: true
			    	}
		    	);
			}
			
			function stopJob(name) {
				XT.doAjaxAction('stopJob', $("#jobsActif").get(0),
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
