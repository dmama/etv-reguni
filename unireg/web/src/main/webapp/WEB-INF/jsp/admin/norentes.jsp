<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="head">
		<style type="text/css" >
			TABLE.detail {
				width: 50%;						
			}
			TABLE.detail TD {
				padding: 3px;
				height:15px;			
			}
			
			TABLE.norentes {
				margin-left:5px;
				border-width: 0px;
				background-color: #FEFEFE;		 
			}
			
			TABLE.norentes TH {
				text-align: center;
				background-color: RoyalBlue;
				color: white;
				padding: 2px;
			}
			
			TABLE.norentes TD {
				padding: 2px;
			}
			
			TABLE.norentes TR {
				color: DarkSlateGray;
			}
			
			TABLE.norentes TR.inactif {
				color: #737373;		
			}
			
			DIV.check {
				color: ForestGreen;
			}
			TABLE.norentes TR.inactif DIV.check {
				color: #739973;
			}
			DIV.title-norentes {
				margin-top:10px;
				margin-left:5px;
				color: #595959;
				font-size: 12pt;
				font-weight: bold;
			}
  		</style>
	</tiles:put>
	<tiles:put name="title">
		Norentes
	</tiles:put>
	<tiles:put name="body">
		<form:form method="post" id="theForm" name="theForm">			
			<fieldset>
				<legend>
					<span>Norentes</span>
				</legend>
				<table class="detail" border="0">
					<tr>
						<td width="2%" nowrap="nowrap">Type d'événement</td>
						<td width="98%">
							<spring:bind path="command.currentEvenementCivil">
							<select id="<c:out value="${status.expression}"/>" name="<c:out value="${status.expression}"/>" onchange="javascript:Form.doAjaxSubmitPostBack('theForm', 'OnChange',this);" >
								<option>--------------------------------------------------------------------------------------------------------------------------------</option>
					         </select>
					         </spring:bind>
	                 	</td>
					</tr>
					<tr>
						<td nowrap="nowrap">Scénario</td>
						<td>
							<spring:bind path="command.currentScenarioName">
								<select id="<c:out value="${status.expression}"/>" name="<c:out value="${status.expression}"/>" onchange="javascript:Form.doAjaxSubmitPostBack('theForm', 'OnChange',this);" >
									<option>-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------</option>
					            </select>
				         	</spring:bind>
						</td>
					</tr>
				</table>
				<div id="buttonStart.div"></div>
				<div id="scenario.content">
				</div>
		 	</fieldset>		 	
		</form:form>
		<script type="text/javascript" language="Javascript1.3">
				Element.addObserver( window, "load", function() {
					Form.doAjaxActionPostBack( 'theForm', 'OnLoad','currentEvenementCivil');
				}, false);
				var periodicalExecuter;
				function startEtape( link, etape) {
					var element = E$( "etat-etape-" + etape);
			        Form.doAjaxSubmitPostBack( 'theForm', 'OnClick',link, {etapeIndex: etape });
					periodicalExecuter = new PeriodicalExecuter(function() {
						Form.doAjaxActionPostBack('theForm', 'OnLoad', 'currentEvenementCivil', {periodical:true, lastEtape: etape},
						{ 
							clearQueryString: true,
							errorHandler :  function(ajaxRequest, exception) {
								}
		    			});
					}, 1);	        
				}
				function stopPeriodical(param) {
					if (periodicalExecuter)
						periodicalExecuter.stop();
					//window.location.reload();
				}
			</script>		
	</tiles:put>
</tiles:insert>
