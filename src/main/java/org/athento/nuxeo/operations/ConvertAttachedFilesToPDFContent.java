package org.athento.nuxeo.operations;

/**
 * @author athento
 *
 */

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.InputStreamBlob;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.storage.StorageBlob;



@Operation(id = ConvertAttachedFilesToPDFContent.ID, category = Constants.CAT_FETCH, label = "ConvertAttachedFilesToPDFContent", description = "Convert attached files to PDF content")
public class ConvertAttachedFilesToPDFContent {
	public static final String ID = "Athento.ConvertAttachedFilesToPDFContent";
	public static final String XPATH_FILES = "files:files";
	public static final String XPATH_FILE_CONTENT = "file:content";

	@OperationMethod
	public Blob run(DocumentModel doc) throws Exception {
		if (_log.isInfoEnabled()) {
			_log.info("Running operation: " + ConvertAttachedFilesToPDFContent.ID);
			_log.info(" Document: " + doc);
		}
		if (doc == null) {
			throw new OperationException(
				"No DocumentModel received. Operation chain must inject a Document in Context");
		}
		Object o = doc.getPropertyValue("file:content");
		if (_log.isDebugEnabled()) {
			_log.debug("Document property " 
				+ ConvertAttachedFilesToPDFContent.XPATH_FILE_CONTENT 
				+ " is: " + o);
		}
		if (o != null && !overwrite) {
			throw new OperationException(
				"This document has a content. If you want to replace it, invoke "
				+"this operation with param \"overwrite\" set to true");
		}
		if (_log.isInfoEnabled()) {
			_log.info("Fetching all attachments for document: " + doc.getName());
		}
		BlobList convertedFiles = new BlobList();
		BlobList attachedFiles = getBlobsList(doc);
		Iterator<Blob> it = attachedFiles.iterator();
		JSONArray array = new JSONArray();
		JSONArray arrayFiles = new JSONArray();
		while (it.hasNext()) {
			StorageBlob item = (StorageBlob)it.next();
			JSONObject object = new JSONObject();
			object.put(item.getFilename(), item.getMimeType());
			if (_log.isDebugEnabled()) {
				_log.debug("  + Converting " + object);
			}
			Blob converted = convertToPDF(item);
			convertedFiles.add(converted);
			if (_log.isDebugEnabled()) {
				_log.debug("  - converted PDF: " + converted);
			}
			arrayFiles.add(object);
		}
		JSONObject obj = new JSONObject();
		obj.put("attachedFiles",arrayFiles);
		array.add(obj);
		
		String filename = "All_attached_files.pdf";
		if (_log.isDebugEnabled()) {
			_log.debug("Creating resulting PDF using " + convertedFiles.size() 
				+ "  converted PDFs");
		}
		MyConcatenatePDFs op = new MyConcatenatePDFs(ctx, "", filename);
		Blob result = op.run(convertedFiles);
		if (_log.isDebugEnabled()) {
			_log.debug("Setting Document file content to resulting PDF");
		}
		DocumentHelper.addBlob(doc.getProperty(
			ConvertAttachedFilesToPDFContent.XPATH_FILE_CONTENT), result);
		if (_log.isInfoEnabled()) {
			_log.info("Resulting PDF: " + result.getFilename());
			_log.info("Saving content to Document " 
				+ ConvertAttachedFilesToPDFContent.XPATH_FILE_CONTENT);	
		}
		doc = session.saveDocument(doc);
		JSONObject object = new JSONObject();
		object.put("convertedPDF", result.getFilename());
		array.add(object);
		if (_log.isInfoEnabled()) {
			_log.info("Successfully concatenated " + array.toString());
		}
		return new InputStreamBlob(new ByteArrayInputStream(array.toString()
			.getBytes("UTF-8")), "application/json");
	}

	protected void adjustBlobName(Blob in, Blob out) {
		String fname = in.getFilename();
		if (fname == null) {
			fname = "Unknown_" + System.identityHashCode(in);
		}
		out.setFilename(fname + ".pdf");
		out.setMimeType("application/pdf");
	}
	
	@OperationMethod
	public Blob convertToPDF(Blob blob) throws Exception {
		if ("application/pdf".equals(blob.getMimeType())) {
			return blob;
		}
		
		Blob result = null;
		try {
			BlobHolder bh = new SimpleBlobHolder(blob);
			bh = service.convertToMimeType("application/pdf", bh,
				new HashMap<String, Serializable>());
			result = bh.getBlob();
			adjustBlobName(blob, result);
		}catch (ConversionException e) {
			_log.error("Unable to convert file of mimeType: " 
				+ blob.getMimeType(), e);
		}
		return result;
	}

	private BlobList getBlobsList(DocumentModel doc) throws Exception {
		BlobList blobs = new BlobList();
		ListProperty list = (ListProperty) doc.getProperty(
			ConvertAttachedFilesToPDFContent.XPATH_FILES);
		if (list == null) {
			BlobHolder bh = doc.getAdapter(BlobHolder.class);
			if (bh != null) {
				List<Blob> docBlobs = bh.getBlobs();
				if (docBlobs != null) {
					for (Blob blob : docBlobs) {
						blobs.add(blob);
					}
				}
			}
			return blobs;
		}
		for (Property p : list) {
			blobs.add((Blob) p.getValue("file"));
		}
		return blobs;
	}

	@Context
	protected CoreSession session;

	@Context
	protected ConversionService service;

	@Context
	protected OperationContext ctx;
	/**
	 * By default, this operation will NOT overwrite document file contents
	 */
	private boolean overwrite = false; 

	private static final Log _log = LogFactory
		.getLog(ConvertAttachedFilesToPDFContent.class);
}
