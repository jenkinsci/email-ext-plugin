var configuredTriggerCSV = "";

function hideElement(elmID)
{
	var elm = document.getElementById(elmID);
	
	elm.style.display = "none";
}

function showElement(elmID)
{
	var elm = document.getElementById(elmID);
	
	elm.style.display = "";
}

function swapEdit(swapName)
{
	var editID = swapName+"edit";
	var hideID = swapName+"hide";
	var elmID = swapName+"elm";
	var elm = document.getElementById(elmID);
	
	
	if(elm.style.display == "none")
	{
		showElement(hideID);
		showElement(elmID);
		hideElement(editID);
	}
	else
	{
		hideElement(hideID);
		hideElement(elmID);
		showElement(editID);
	}
}

//This function is called when the page loads to remove any configured email triggers
//from the dropdown select element.  This way it will be less confusing to the user
//as to which triggers they can add or already have configured.
function hideConfiguredOptions()
{
	var nonConfigOpts = document.getElementById("non-configured-options");
	var configOpts = document.getElementById("configured-options");
	
	var confTriggerNames = configuredTriggerCSV.split(",");
	
	for(var i=0;i<confTriggerNames.length;i++){
		var opt = document.getElementById(confTriggerNames[i]+"option");
		if(opt!=null)
			switchElementContainer(nonConfigOpts,configOpts,opt);
	}
}

function switchElementContainer(oldParent,newParent,child)
{
	oldParent.removeChild(child);
	newParent.appendChild(child);
}

function selectTrigger(selectElement)
{
	var selInd = selectElement.selectedIndex;
	
	if(selInd == 0)
		return;
	
	var triggerOption = selectElement.options[selInd];
	var mailerId = triggerOption.value;
	
	selectElement.selectedIndex = 0;

	addTrigger(mailerId);
}

function addTrigger(mailerId)
{
	var nonConfigTriggers = document.getElementById("non-configured-email-triggers");
	var triggerRow = document.getElementById(mailerId);
	var configTriggers = document.getElementById("configured-email-triggers");
	var afterThisElement = document.getElementById("after-last-configured-row");
	nonConfigTriggers.removeChild(triggerRow);
	configTriggers.insertBefore(triggerRow,afterThisElement);
	triggerRow.style.display="";
	
	var triggerHelp = document.getElementById(mailerId+"help");
	nonConfigTriggers.removeChild(triggerHelp);
	configTriggers.insertBefore(triggerHelp,afterThisElement);
	
	var triggerAdv = document.getElementById(mailerId+"elm");
	nonConfigTriggers.removeChild(triggerAdv);
	configTriggers.insertBefore(triggerAdv,afterThisElement);
	
	var nonConfigOptions = document.getElementById("non-configured-options");
	var configOptions = document.getElementById("configured-options");
	var option = document.getElementById(mailerId + "option");
	switchElementContainer(nonConfigOptions,configOptions,option);
	
	document.getElementById("mailer."+mailerId+".configured").value = "true";
}

function removeTrigger(mailerId)
{
	document.getElementById("mailer."+mailerId+".configured").value = "false";

	var nonConfigTriggers = document.getElementById("non-configured-email-triggers");
	var triggerRow = document.getElementById(mailerId);
	var configTriggers = document.getElementById("configured-email-triggers"); 
	switchElementContainer(configTriggers,nonConfigTriggers,triggerRow);

	var triggerHelp = document.getElementById(mailerId+"help");
	switchElementContainer(configTriggers,nonConfigTriggers,triggerHelp);

	var triggerAdv = document.getElementById(mailerId+"elm");
	switchElementContainer(configTriggers,nonConfigTriggers,triggerAdv);

	
	var nonConfigOptions = document.getElementById("non-configured-options");
	var configOptions = document.getElementById("configured-options");
	var option = document.getElementById(mailerId + "option");
	switchElementContainer(configOptions,nonConfigOptions,option);
	
	if(triggerAdv.style.display != "none")
		swapEdit(mailerId);
		
	if(triggerHelp.style.display != "none")
		triggerHelp.style.display="none";
		
	var selectElement = document.getElementById("non-configured-options");
	selectElement.selectedIndex = 0;
}

function toggleMailHelp(mailerId)
{
	var mailHelpRow = document.getElementById(mailerId+"help");
	if(mailHelpRow.style.display=="none")
		mailHelpRow.style.display="";
	else
		mailHelpRow.style.display="none";
}

function toggleAdvancedMailHelp()
{
	var mailHelp = document.getElementById("advancedEmailHelpConf");
	if(mailHelp.style.display=="none")
		mailHelp.style.display="";
	else
		mailHelp.style.display="none";
}