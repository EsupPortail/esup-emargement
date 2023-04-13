package org.esupportail.emargement.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.FontSelector;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;


@Component
public class PdfGenaratorUtil {
	
	public String createPdf(String htmltemplate) throws Exception {
		String filePath = "";

		String htmlStart = "<!DOCTYPE HTML>\n" + "<html>\n" + "<head>\n" + "</head>\n" + "<body>\n";
		String htmlEnd = "</body>\n" + "</html>\n";
		String htmlString = htmlStart + htmltemplate + htmlEnd;
		String finalHtml = "";

		boolean valid = Jsoup.isValid(htmlString, Safelist.basic());

		if (!valid) {
			Document dirtyDoc = Jsoup.parse(htmlString);
			dirtyDoc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
			finalHtml = dirtyDoc.html();
		}
		OutputStream os = null;
		String fileName = UUID.randomUUID().toString();
		try {
			final File outputFile = File.createTempFile(fileName, ".pdf");
			os = new FileOutputStream(outputFile);
			filePath = outputFile.getPath();
			ITextRenderer renderer = new ITextRenderer();
			renderer.setDocumentFromString(finalHtml);
			renderer.layout();
			renderer.createPDF(os, false);
			renderer.finishPDF();
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					/* ignore */ }
			}
		}

		return filePath;
	}
	
	 public class PdfHeader extends PdfPageEventHelper {
	        @Override
	        public void onEndPage(PdfWriter writer, com.itextpdf.text.Document document) {
	            try {
	                Rectangle pageSize = document.getPageSize();
	                ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER, new Phrase(""), pageSize.getLeft(275), pageSize.getTop(30), 0);
	                ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT, new Phrase(String.format("Page %s", String.valueOf(writer.getCurrentPageNumber()))),
	                        pageSize.getRight(30), pageSize.getBottom(30), 0);
	                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy"); 
	                String today = simpleDateFormat. format(new Date()); 
	                ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT, new Phrase(String.format("Edité le " + today, String.valueOf(writer.getCurrentPageNumber()))),
	                        pageSize.getLeft(30), pageSize.getBottom(30), 0);


	            } catch (Exception e) {
	                e.printStackTrace();
	            }
	        }
	    }

	public PdfPCell getIRHCell(String text, int alignment) {
		FontSelector fs = new FontSelector();
		Font font = FontFactory.getFont(FontFactory.HELVETICA, 16);
		/*	font.setColor(BaseColor.GRAY);*/
		fs.addFont(font);
		Phrase phrase = fs.process(text);
		PdfPCell cell = new PdfPCell(phrase);
		cell.setPadding(5);
		cell.setHorizontalAlignment(alignment);
		cell.setBorder(PdfPCell.NO_BORDER);
		return cell;
	}

	public PdfPCell getIRDCell(String text) {
		PdfPCell cell = new PdfPCell (new Paragraph (text));
		cell.setHorizontalAlignment (Element.ALIGN_CENTER);
		cell.setPadding (5.0f);
		cell.setBorderColor(BaseColor.LIGHT_GRAY);
		return cell;
	}

	public PdfPCell getMainHeaderCell(String text) {
		FontSelector fs = new FontSelector();
		Font font = FontFactory.getFont(FontFactory.HELVETICA, 11);
		font.setColor(BaseColor.GRAY);
		fs.addFont(font);
		Phrase phrase = fs.process(text);
		PdfPCell cell = new PdfPCell (phrase);
		cell.setHorizontalAlignment (Element.ALIGN_CENTER);
		cell.setPadding (5.0f);
		return cell;
	}

	public PdfPCell getMainRowCell(String text) {
		PdfPCell cell = new PdfPCell (new Paragraph (text));
		cell.setHorizontalAlignment (Element.ALIGN_CENTER);
		cell.setPadding (5.0f);
		cell.setBorderWidthBottom(0);
		cell.setBorderWidthTop(0);
		return cell;
	}

	public PdfPCell getTagCheckerCell(String text) {
		FontSelector fs = new FontSelector();
		Font font = FontFactory.getFont(FontFactory.HELVETICA, 10);
		font.setColor(BaseColor.GRAY);
		fs.addFont(font);
		Phrase phrase = fs.process(text);
		PdfPCell cell = new PdfPCell (phrase);		
		cell.setBorder(0);
		return cell;
	}

	public PdfPCell getRemarquesCell(String text) {
		FontSelector fs = new FontSelector();
		Font font = FontFactory.getFont(FontFactory.HELVETICA, 10);
		fs.addFont(font);
		Phrase phrase = fs.process(text);
		PdfPCell cell = new PdfPCell (phrase);		
		cell.setBorderWidthRight(0);
		cell.setBorderWidthTop(0);
		cell.setPadding (5.0f);
		return cell;
	}

	public PdfPCell getDescCell(String text) {
		FontSelector fs = new FontSelector();
		Font font = FontFactory.getFont(FontFactory.HELVETICA, 10);
		font.setColor(BaseColor.GRAY);
		fs.addFont(font);
		Phrase phrase = fs.process(text);
		PdfPCell cell = new PdfPCell (phrase);	
		cell.setHorizontalAlignment (Element.ALIGN_CENTER);
		cell.setBorder(0);
		return cell;
	}
}
