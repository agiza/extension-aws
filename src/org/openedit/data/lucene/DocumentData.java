package org.openedit.data.lucene;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.openedit.Data;

import com.openedit.OpenEditException;

public class DocumentData implements Data
{
	protected Document fieldDocument;
	
	public DocumentData()
	{
		// TODO Auto-generated constructor stub
	}
	public DocumentData(Document inDoc)
	{
		setDocument(inDoc);
	}
	public Document getDocument()
	{
		return fieldDocument;
	}
	public void setDocument(Document inDocument)
	{
		fieldDocument = inDocument;
	}
	public String get(String inId)
	{
		return getDocument().get(inId);
	}

	public String getId()
	{
		return get("id");
	}

	public String getName()
	{
		return get("name");
	}
	public void setName(String inName)
	{
		setProperty("name", inName);
	}
	public void setId(String inNewid)
	{
		throw new OpenEditException("Search results are not editable");
	}

	public void setProperty(String inId, String inValue)
	{
		throw new OpenEditException("Search results are not editable");
	}
	public Iterator keys()
	{
		//TODO: Use Strings
		return getDocument().getFields().iterator();
	}
	public String getSourcePath()
	{
		// TODO Auto-generated method stub
		return get("sourcepath");
	}
	public void setSourcePath(String inSourcepath)
	{
		throw new OpenEditException("Search results are not editable");
	}
	public Map getProperties() 
	{
		Map fields = new HashMap(getDocument().getFields().size());
		for (Iterator iterator = getDocument().getFields().iterator(); iterator.hasNext();)
		{
			Field	key = (Field)iterator.next();
			String value = get(key.name());
			fields.put(key.name(),value);
		}
		return fields;
	}
	@Override
	public void setProperties(Map<String, String> inProperties)
	{
		// TODO Auto-generated method stub
		
	}
	public String toString()
	{
		if(getName() != null){
		return getName();
		} else{
			return getId();
		}
	}

}