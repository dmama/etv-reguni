<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title"><fmt:message key="title.admin.evtExterne" /></tiles:put>
  	<tiles:put name="head">
  		<style type="text/css" >
  			H3.sub-title {
  				color:gray;
  				font-weight: bold;
  				font-size: 12pt;
  			}
  			DIV.content_tab {
  				padding: 5px;
  				border: solid 1px gray;
  				height: 350px
  			}
  		</style>
  	</tiles:put>
  	<tiles:put name="body">  		
  		<form:form method="post" id="theForm" name="theForm">

		    <div id="externeEventTabs">
				<ul>
					<li id="envoyer"><a href="#tabContent_envoyer">Envoie</a></li>
					<li id="historique"><a href="#tabContent_historique">Historique</a></li>
				</ul>

				<div id="tabContent_envoyer" class="content_tab">
					<h3 class="sub-title">Envoyer un événement externe</h3>

					<table style="width:50%;margin-top: 10px" cellpadding="3">
						<tr>
							<td nowrap="nowrap" width="10%">Emmetteur du message</td>
							<td>
								<select id="emmetteurs" name="emmetteurs" onchange="Form.doAjaxActionPostBack('theForm', 'OnChange',this);">
								</select>
							</td>
						</tr>
						<tr>
							<td nowrap="nowrap">Type du message</td>
							<td>
								 <select id="evenements" name="evenements"  onchange="Form.doAjaxActionPostBack('theForm','OnChange', 'evenements');">
								 </select>
							</td>
						</tr>
					 </table>
					 <div id="evenement.content"></div>
				</div>
				<div id="tabContent_historique" class="content_tab">
					<h3 class="sub-title">Historique des évenement externe</h3>
					<table style="width:50%;margin-top: 10px">
						<tr>
							<td nowrap="nowrap" width="10%">Etat Evénement</td>
							<td>
								<select id="etats" name="etats" onchange="Form.doAjaxActionPostBack('theForm', 'OnChange',this);">
								</select>
							</td>
						</tr>
					 </table>
					 <table>
							<thead>
								<tr>
									<th>Tiers</th>
									<th>etat</th>
									<th>dateEvenement</th>
									<th>dateTraitement</th>
									<th>errorMessage</th>
									<th>businessId</th>
								</tr>
							</thead>
							<tbody id="evenementsList">
							</tbody>
					  </table>
				</div>
			</div>

			<script>
				$(function() {
					Form.doAjaxActionPostBack("theForm", 'OnLoad','emmetteurs');
					Form.doAjaxActionPostBack("theForm", 'OnLoad', 'etats');

					$("#externeEventTabs").tabs({
						select: function(event, ui) {
							if (ui.tab.id === "tabContent_historique") {
								$('#etats').change();
							}
						}
					});
				});
			</script>

		</form:form>
  	</tiles:put>
</tiles:insert>

