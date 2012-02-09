<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
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
		
		<script>
			
			var requestDone = true;

			$(document).everyTime("3s", function() {
				if (!requestDone)
					return;
				requestDone = false;

				$.get(getContextPath() + '/admin/batch/running.do?' + new Date().getTime(), function(jobs) {
					var h = buildHtmlTableRunningJobs(jobs, false);
					$("#jobsActif").html(h);
					requestDone = true;
				});
			});

			function buildHtmlTableRunningJobs(jobs, readonly) {
				var table = '<table>';
				table += '<thead><tr><th>Action</th><th>Nom</th><th>Progression</th><th>Statut</th><th>Début</th><th>Durée</th></tr></thead>';
				table += '<tbody>';

				for(var i = 0; i < jobs.length; ++i) {
					var job = jobs[i];
					var lastStart = job.lastStart ? new Date(job.lastStart) : null;
					var lastEnd = job.lastEnd ? new Date(job.lastEnd) : null;
					var isRunning = job.status == 'JOB_RUNNING';

					// general description + status
					table += '<tr class="' + (i % 2 == 0 ? 'even' : 'odd')  + '">';

					if (!isRunning || readonly) {
						table += '<td></td>';
					}
					else {
						table += '<td><a class="stop iepngfix" classname="stop iepngfix" href="javascript:stopJob(\'' + job.name + '\');"></a></td>';
					}

					table += '<td>' + StringUtils.escapeHTML(job.description) + '</td>';

					if (job.percentProgression) {
						table += '<td>' + StringUtils.escapeHTML(job.percentProgression) + '</td>';
					}
					else {
						table += '<td></td>';
					}

					table += '<td align="left">' + StringUtils.escapeHTML(statusDescription(job.status)) + '</td>';
					table += '<td nowrap="nowrap">' + DateUtils.toCompactString(lastStart) + '</td>';

					if (lastStart) {
						table += '<td nowrap="nowrap">' + DateUtils.durationToString(lastStart, lastEnd) + '</td>';
					}
					else {
						table += '<td nowrap="nowrap"></td>';
					}
					table += '</tr>';

					// detailed parameters
					if (job.runningParams) {
						var params = '<tr class="' + (i % 2 == 0 ? 'even' : 'odd')  + '">';
						params += '<td>&nbsp;</td>';
						params += '<td colspan="5">';

						params += '<table class="jobparams"><tbody>';

						var hasParam = false;
						for(key in job.runningParams) {
							var value = job.runningParams[key];
							params += '<tr><td>' + StringUtils.escapeHTML(key) + '</td><td>➭ ' + StringUtils.escapeHTML(value) + '</td></tr>';
							hasParam = true;
						}

						params += '</tbody></table>';

						params += '</td>';
						params += '</tr>';

						if (hasParam) {
							table += params;
						}
					}
				}

				table += '</tbody></table>';
				return table;
			}

			function statusDescription(status) {
				if (status == 'JOB_OK') {
					return 'OK';
				}
				else if (status == 'JOB_READY') {
					return 'Prêt';
				}
				else if (status == 'JOB_RUNNING') {
					return 'En cours';
				}
				else if (status == 'JOB_EXCEPTION') {
					return 'Exception';
				}
				else if (status == 'JOB_INTERRUPTING') {
					return 'En cours d\'interruption';
				}
				else if (status == 'JOB_INTERRUPTED') {
					return 'Interrompu';
				}
				else {
					return '';
				}
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
		</script>
	</tiles:put>
</tiles:insert>
