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