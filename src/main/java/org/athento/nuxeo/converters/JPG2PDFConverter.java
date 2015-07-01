package org.athento.nuxeo.converters;
/**
 * @author athento
 *
 */


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.operations.ConvertAttachedFilesToPDFContent;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.InputStreamBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;

import java.io.FileOutputStream;





//The image class which will hold the input image
import com.itextpdf.text.Image;
////PdfWriter object to write the PDF document
import com.itextpdf.text.pdf.PdfWriter;
////Document object to add logical image files to PDF
import com.itextpdf.text.Document;

public class JPG2PDFConverter implements Converter {

	@Override
	public void init(ConverterDescriptor descriptor) {
		if (_log.isInfoEnabled()) {
			_log.info("Converter JPG2PDFConverter has been initialized");
		}
	}

	@Override
	public BlobHolder convert(
		BlobHolder blobHolder, Map<String, Serializable> parameters) 
		throws ConversionException {
		if (_log.isDebugEnabled()) {
			_log.debug("Converting JPG to PDF the file: " 
				+ blobHolder.getFilePath());
		}
		Blob originalBlob;
		String path;
		try {
			originalBlob = blobHolder.getBlob();
			path = blobHolder.getFilePath();
		} catch (ClientException e) {
			throw new ConversionException("Cannot fetch content of blob", e);
		}
		Blob transcodedBlob;
		try {
			transcodedBlob = convert(originalBlob);
		} catch (Exception e) {
			throw new ConversionException("Cannot convert " + path + " to PDF", e);
		}
		return new SimpleBlobHolder(transcodedBlob);
	}

	private Blob convert(Blob originalBlob) {
		Blob newBlob = null;
		try{
			if (_log.isDebugEnabled()) {
				_log.debug("  converting " + originalBlob.getFilename());
			}
			//Create Document Object
			Document document = new Document();
			//Create PdfWriter for Document to hold physical file
			File tmpFile = File.createTempFile(
				"tempPDF", "_tmp.pdf");
			if (_log.isDebugEnabled()) {
				_log.debug("  tmpFile: " + tmpFile.getAbsolutePath());
			}
			FileOutputStream fos = new FileOutputStream(tmpFile);
			PdfWriter writer = PdfWriter.getInstance(document, fos);
			if (_log.isDebugEnabled()) {
				_log.debug("  opening PDF document");
			}
			document.open();
			if (_log.isDebugEnabled()) {
				_log.debug("  opening image");
			}
			float indentation = 2;
			Image image = Image.getInstance(originalBlob.getByteArray());
			float scaler = (
				(
					document.getPageSize().getWidth() - document.leftMargin() 
					- 
					document.rightMargin() - indentation
				) 
				/ image.getWidth()) * 100;

			image.scalePercent(scaler);
			document.add(image);
			if (_log.isDebugEnabled()) {
				_log.debug("  closing streams");
			}
			document.close();
			fos.close();
			if (_log.isInfoEnabled()) {
				_log.info("Successfully converted " + originalBlob.getFilename() + " to " + tmpFile.getAbsolutePath());
			}
			FileInputStream fis = new FileInputStream(tmpFile);
			newBlob =  new InputStreamBlob(fis);
			tmpFile.delete();
		}
		catch (Exception e){
			_log.error("Unable to convert to PDF", e);
		}
		return newBlob;
	}
	private static final Log _log = LogFactory.getLog(JPG2PDFConverter.class);

}
