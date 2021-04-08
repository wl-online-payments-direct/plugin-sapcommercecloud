package com.ingenico.ogone.direct.forms;


public class IngenicoHostedTokenizationForm
{
	private String securityCode;
	private boolean termsCheck;

	public String getSecurityCode()
	{
		return securityCode;
	}

	public void setSecurityCode(final String securityCode)
	{
		this.securityCode = securityCode;
	}

	public boolean isTermsCheck()
	{
		return termsCheck;
	}

	public void setTermsCheck(final boolean termsCheck)
	{
		this.termsCheck = termsCheck;
	}

}
