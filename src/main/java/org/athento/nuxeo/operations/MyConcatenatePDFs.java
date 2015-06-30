/**
 * 
 */
package org.athento.nuxeo.operations;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFMergerUtility;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.runtime.api.Framework;

import com.google.common.io.Files;


/**
 * @author athento
 *
 */
public class MyConcatenatePDFs {

	protected String xpathBlobToAppend = "";
	protected String filename;
	protected OperationContext ctx;
	
	public MyConcatenatePDFs(
		OperationContext ctx, String _xpathBlobToAppend, String _filename) {
		xpathBlobToAppend = _xpathBlobToAppend;
		filename = _filename;
	}

	public Blob run(Blob blob) throws OperationException, IOException,
		COSVisitorException {
		PDFMergerUtility ut = new PDFMergerUtility();
		checkPdf(blob);
		if (xpathBlobToAppend.isEmpty()) {
			return blob;
		}
		handleBlobToAppend(ut);
		ut.addSource(blob.getStream());
		return appendPDFs(ut);
	}

	public Blob run(BlobList blobs) throws IOException, OperationException,
		COSVisitorException {
		PDFMergerUtility ut = new PDFMergerUtility();
		if (!xpathBlobToAppend.isEmpty()) {
			handleBlobToAppend(ut);
		}
		for (Blob blob : blobs) {
			checkPdf(blob);
			ut.addSource(blob.getStream());
		}
		return appendPDFs(ut);
	}

	public Blob run2(BlobList blobs) throws IOException, OperationException,
	COSVisitorException {

		PDFMergerUtility merger = new PDFMergerUtility();
		//File tempFile = File.createTempFile(filename, ".pdf");
		File tempFile = Files.createTempDir();
		merger.setDestinationFileName(tempFile.getAbsolutePath());
		COSDocument cos = new COSDocument(tempFile);
		PDDocument destination = new PDDocument(cos);

		for (Blob blob : blobs) {
			PDFParser parser = new PDFParser(blob.getStream());
			parser.parse();
			PDDocument srcDoc = parser.getPDDocument();
			merger.appendDocument(destination, srcDoc);
			srcDoc.close();
		}
		destination.close();
		FileBlob fb = new FileBlob(tempFile);
		return fb;
	}

	protected FileBlob appendPDFs(PDFMergerUtility ut) throws IOException,
		COSVisitorException {
		File tempFile = File.createTempFile(filename, ".pdf");
		ut.setDestinationFileName(tempFile.getAbsolutePath());
		ut.mergeDocuments();
		FileBlob fb = new FileBlob(tempFile);
		Framework.trackFile(tempFile, fb);
		fb.setFilename(filename);
		return fb;
	}

	/**
	 * Check if blob to append is a PDF blob.
	 */
	protected void handleBlobToAppend(PDFMergerUtility ut) 
		throws IOException, OperationException {
		try {
			Blob blobToAppend = (Blob) ctx.get(xpathBlobToAppend);
			if (blobToAppend == null) {
				throw new OperationException(
					"The blob to append from variable context: '"
							+ xpathBlobToAppend + "' is null.");
				}
			checkPdf(blobToAppend);
			ut.addSource(blobToAppend.getStream());
		} catch (ClassCastException e) {
			throw new OperationException(
				"The blob to append from variable context: '"
						+ xpathBlobToAppend + "' is not a blob.", e);
		}
	}

	/**
	* Check if blob is a pdf.
	*/
	protected void checkPdf(Blob blob) throws OperationException {
		if (!"application/pdf".equals(blob.getMimeType())) {
			throw new OperationException("Blob " + blob.getFilename()
				+ " is not a PDF.");
		}
	}
}
