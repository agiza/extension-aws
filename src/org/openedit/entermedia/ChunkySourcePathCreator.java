package org.openedit.entermedia;

import org.openedit.Data;

public class ChunkySourcePathCreator implements SourcePathCreator
{

	public String createSourcePath(Data inAsset, String inStoragePath)
	{
		if( inStoragePath.length() < 3 )
		{
			return "0";
		}
		StringBuffer sourcepath = new StringBuffer();
		
		//cut off the last part of the id +3
		for (int i = 0; i + 2 < inStoragePath.length(); i++)
		{
			if( i > 0 && i % 2 == 0)
			{
				sourcepath.append("/");
			}
			sourcepath.append(inStoragePath.charAt(i));
		}
		return sourcepath.toString();
	}

}
