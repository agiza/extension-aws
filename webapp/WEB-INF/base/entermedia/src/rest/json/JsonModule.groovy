package rest.json

import groovy.json.JsonSlurper

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.entermedia.upload.FileUpload
import org.entermedia.upload.UploadRequest
import org.json.simple.JSONObject
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.scanner.AssetImporter
import org.openedit.entermedia.search.AssetSearcher
import org.openedit.util.DateStorageUtil

import com.openedit.OpenEditException
import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.Page
import com.openedit.util.OutputFiller

class JsonModule {
	private static final Log log = LogFactory.getLog(JsonModule.class);


	public void handleAssetRequest(WebPageRequest inReq){
		JSONObject object = null;
		String method = inReq.getMethod();

		if(method == "POST"){
			object = handleAssetPost(inReq);
		}
		if(method == "PUT"){
			object = handleAssetPut(inReq);
		}
		if(method == "DELETE"){
			object = handleAssetDelete(inReq);
		}

		if(object != null){

			try {
				OutputFiller filler = new OutputFiller();
				InputStream stream = new ByteArrayInputStream(object.toJSONString().getBytes("UTF-8"));

				//filler.setBufferSize(40000);
				//InputStream input = object.
				filler.fill(stream, inReq.getOutputStream());
			}
			finally {
				//			stream.close();
				//			inOut.getStream().close();
				//			log.info("Document sent");
				//			//archive.logDownload(filename, "success", inReq.getUser());
			}
		}
		//	jsondata = obj.toString();
		//
		//	log.info(jsondata);
		//	inReq.putPageValue("json", jsondata);

	}


	public void handleAssetSearch(WebPageRequest inReq){
		//Could probably handle this generically, but I think they want tags, keywords etc.
		JSONObject object = null;
		SearcherManager sm = inReq.getPageValue("searcherManager");

		String catalogid =  findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);
		AssetSearcher searcher = sm.getSearcher(catalogid,"asset" );
		JsonSlurper slurper = new JsonSlurper();
		def request = null;
		String content = inReq.getPageValue("jsondata");
		if(content != null){
			request = slurper.parseText(content); //NOTE:  This is for unit tests.
		} else{
			request = slurper.parse(inReq.getRequest().getReader()); //this is real, the other way is just for testing
		}

		
		ArrayList <String> fields = new ArrayList();
		ArrayList <String> operations = new ArrayList();
		
		request.query.each{
			println it;
			fields.add(it.field);
			operations.add(it.operator.toLowerCase());
			StringBuffer values = new StringBuffer();
			it.values.each{
				values.append(it);
				values.append(" ");
			}
			inReq.setRequestParameter(it.field + ".value", values.toString());
		}
		
		println "field" + fields;
		println "operations: " + operations;
		String[] fieldarray = fields.toArray(new String[fields.size()]) as String[];
		String[] opsarray = operations.toArray(new String[operations.size()]) as String[];
		
		inReq.setRequestParameter("field", fieldarray);
		inReq.setRequestParameter("operation", opsarray);

		SearchQuery query = searcher.addStandardSearchTerms(inReq);
		println "Query was: " + query;
		HitTracker hits = searcher.cachedSearch(inReq, query);
		println hits.size();
		JSONObject parent = new JSONObject();
		
		JSONObject result = getAssetJson(searcher, asset);
		String jsondata = result.toString();
	}



	public JSONObject handleAssetPost(WebPageRequest inReq){
		SearcherManager sm = inReq.getPageValue("searcherManager");

		String catalogid =  findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);
		AssetSearcher searcher = sm.getSearcher(catalogid,"asset" );
		//We will need to handle this differently depending on whether or not this asset has a real file attached to it.
		//if it does, we should move it and use the asset importer to create it so metadata gets read, etc.

		FileUpload command = archive.getSearcherManager().getModuleManager().getBean("fileUpload");
		UploadRequest properties = command.parseArguments(inReq);

		JsonSlurper slurper = new JsonSlurper();
		def request = null;
		String content = inReq.getPageValue("jsondata");
		if(properties != null){

		}
		if(content != null){
			request = slurper.parseText(content); //NOTE:  This is for unit tests.
		} else{
			request = slurper.parse(inReq.getRequest().getReader()); //this is real, the other way is just for testing
		}

		AssetImporter importer = archive.getAssetImporter();
		HashMap keys = new HashMap();

		request.each{
			println it;
			String key = it.key;
			String value = it.value;
			keys.put(key, value);
			keys.put("formatteddate", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
		}
		String id = request.id;
		if(id == null){
			id = searcher.nextAssetNumber()
		}

		String sourcepath = keys.get("sourcepath");

		if(sourcepath == null){
			sourcepath = archive.getCatalogSettingValue("catalogassetupload");  //${division.uploadpath}/${user.userName}/${formateddate}
		}
		if(sourcepath.length() == 0){
			sourcepath = "receivedfiles/${id}";
		}
		sourcepath = sm.getValue(catalogid, sourcepath, keys);
		Asset asset = null;

		if(properties.getFirstItem() != null){
			String path = "/WEB-INF/data/" + archive.getCatalogId()	+ "/originals/" + sourcepath + "/${properties.getFirstItem().getName()}";
			properties.saveFileAs(properties.getFirstItem(), path, inReq.getUser());
			Page newfile = archive.getPageManager().getPage(path);
			asset = importer.createAssetFromPage(archive, inReq.getUser(), newfile);
		}


		if(asset == null && keys.get("fetchURL") != null){
			asset = importer.createAssetFromFetchUrl(archive, keys.get("fetchURL"), inReq.getUser(), sourcepath);
		}

		if(asset == null && keys.get("localPath") != null)
		{
			log.info("HERE!!!");
			File file = new File(keys.get("localPath"));
			if(file.exists())
			{
				String path = "/WEB-INF/data/" + archive.getCatalogId()	+ "/originals/" + sourcepath + "/${file.getName()}";
				Page newfile = archive.getPageManager().getPage(path);
				String realpath = newfile.getContentItem().getAbsolutePath();
				File target = new File(realpath);
				target.getParentFile().mkdirs();
				if(file.renameTo(realpath)){
					asset = importer.createAssetFromPage(archive, inReq.getUser(), newfile);
				} else{
					throw new OpenEditException("Error moving file: " + realpath);
				}
			}
		}
		if(asset == null){
			asset = new Asset();//Empty Record
			asset.setId(id);
		}



		request.each{
			println it;
			String key = it.key;
			String value = it.value;
			asset.setProperty(key, value);
		}


		asset.setProperty("sourcepath", sourcepath);
		searcher.saveData(asset, inReq.getUser());




		JSONObject result = getAssetJson(searcher, asset);
		String jsondata = result.toString();

		inReq.putPageValue("json", jsondata);
		return result;

	}


	public JSONObject handleAssetPut(WebPageRequest inReq){

		SearcherManager sm = inReq.getPageValue("searcherManager");
		//	slurper.parse(inReq.getRequest().getReader()); //this is real, the other way is just for testing
		JsonSlurper slurper = new JsonSlurper();
		def request = null;
		String content = inReq.getPageValue("jsondata");
		if(properties != null){

		}
		if(content != null){
			request = slurper.parseText(content); //NOTE:  This is for unit tests.
		} else{
			request = slurper.parse(inReq.getRequest().getReader()); //this is real, the other way is just for testing
		}


		String catalogid = request.catalogid;
		MediaArchive archive = getMediaArchive(catalogid);
		AssetSearcher searcher = sm.getSearcher(catalogid,"asset" );
		//We will need to handle this differently depending on whether or not this asset has a real file attached to it.
		//if it does, we should move it and use the asset importer to create it so metadata gets read, etc.
		String id = getId(inReq);




		Asset asset = archive.getAsset(id);

		if(asset == null){
			throw new OpenEditException("Asset was not found!");
		}

		request.each{
			println it;
			String key = it.key;
			String value = it.value;
			asset.setProperty(key, value);
		}

		searcher.saveData(asset, context.getUser());
		JSONObject result = getAssetJson(searcher, asset);
		String jsondata = result.toString();

		inReq.putPageValue("json", jsondata);
		return result;

	}


	public void handleAssetDelete(WebPageRequest inReq){
		JsonSlurper slurper = new JsonSlurper();

		SearcherManager sm = inReq.getPageValue("searcherManager");
		String catalogid = findCatalogId(inReq);

		MediaArchive archive = getMediaArchive(inReq, catalogid);
		AssetSearcher searcher = sm.getSearcher(catalogid,"asset" );
		//We will need to handle this differently depending on whether or not this asset has a real file attached to it.
		//if it does, we should move it and use the asset importer to create it so metadata gets read, etc.
		String id = getId(inReq);




		Asset asset = archive.getAsset(id);

		if(asset != null){
			searcher.delete(asset, null);
		}


	}





	public MediaArchive getMediaArchive(WebPageRequest inReq,  String inCatalogid)
	{
		SearcherManager sm = inReq.getPageValue("searcherManager");

		if (inCatalogid == null)
		{
			return null;
		}
		MediaArchive archive = (MediaArchive) sm.getModuleManager().getBean(inCatalogid, "mediaArchive");
		return archive;
	}

	public JSONObject getAssetJson(Searcher inSearcher, Data inAsset){

		JSONObject asset = new JSONObject();
		inSearcher.getPropertyDetails().each{
			String key = it.id;
			String value=inAsset.get(it.id);
			if(key && value){
				asset.put(key, value);
			}
		}
		//need to add tags and categories, etc



		return asset;
	}


	public String getId(WebPageRequest inReq){
		String root  = "/entermedia/services/json/asset/";
		String url = inReq.getPath();
		if(!url.endsWith("/")){
			url = url + "/";
		}
		String id = url.substring(root.length(), url.length())
		id = id.substring(0, id.indexOf("/"));
		return id;
	}



	public String findSearchType(WebPageRequest inReq){
		String root  = "/entermedia/services/json/data/";
		String url = inReq.getPath();
		if(!url.endsWith("/")){
			url = url + "/";
		}
		String id = url.substring(root.length(), url.length())
		id = id.substring(0, id.indexOf("/"));
		return id;
	}



	public String findCatalogId(WebPageRequest inReq){
		String catalogid = inReq.findValue("catalogid");
		if(catalogid == null){
			if(inReq.getRequest()){
				catalogid = inReq.getRequest().getHeader("catalogid");
			}
		}
		return catalogid;
	}


}
