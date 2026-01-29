package org.esupportail.emargement.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.time.Year;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.UUID;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.FontSelector;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
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
		return getIRDCell(text, Element.ALIGN_CENTER);
	}

	public PdfPCell getIRDCell(String text, int align) {
		PdfPCell cell = new PdfPCell(new Paragraph(text));
		cell.setHorizontalAlignment(align);
		cell.setPadding(5.0f);
		cell.setBorderColor(BaseColor.LIGHT_GRAY);
		return cell;
	}

	public PdfPCell getMainHeaderCell(String text) {
		return this.getMainHeaderCell(text, 11);
	}

	public PdfPCell getMainHeaderCell(String text, int fontSize) {
		return this.getMainHeaderCell(text, fontSize, 5.0f);
	}

	public PdfPCell getMainHeaderCell(String text, int fontSize, float padding) {
		FontSelector fs = new FontSelector();
		Font font = FontFactory.getFont(FontFactory.HELVETICA, fontSize);
		font.setColor(BaseColor.GRAY);
		fs.addFont(font);
		Phrase phrase = fs.process(text);
		PdfPCell cell = new PdfPCell (phrase);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setPadding(padding);
		return cell;
	}

	public PdfPCell getMainRowCell(String text) {
		return getMainRowCell(text, 0);
	}

	public PdfPCell getMainRowCell(String text, int fontSize) {
		return this.getMainRowCell(text, fontSize, 5.0f);
	}

	public PdfPCell getMainRowCell(String text, int fontSize, float padding) {
		return this.getMainRowCell(text, fontSize, padding, Element.ALIGN_CENTER);
	}

	public PdfPCell getMainRowCell(String text, int fontSize, float padding, int alignment) {
		return this.getMainRowCell(text, fontSize, padding, alignment, 0);
	}

	public PdfPCell getMainRowCell(String text, int fontSize, float padding, int alignment, float bottomBorderWidth) {
		Paragraph paragraph;
		if (0 != fontSize) {
			Font font = FontFactory.getFont(FontFactory.HELVETICA, fontSize);
			paragraph = new Paragraph(text, font);
		} else {
			paragraph = new Paragraph(text);
		}

		PdfPCell cell = new PdfPCell(paragraph);
		cell.setHorizontalAlignment(alignment);
		cell.setPadding(padding);
		cell.setBorderWidthBottom(bottomBorderWidth);
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

	public PdfPTable getHorizontalCartoucheTable(LinkedHashMap<String, String> cartoucheFields)
	{
		PdfPTable headerTable = new PdfPTable(cartoucheFields.size());

		for (String key: cartoucheFields.keySet()) {
			headerTable.addCell(getIRDCell(key));
		}

		for (String key: cartoucheFields.keySet()) {
			headerTable.addCell(getIRDCell(cartoucheFields.get(key)));
		}

		return headerTable;
	}

	public PdfPTable getVerticalCartoucheTable(LinkedHashMap<String, String> cartoucheFields, float[] colWidths) throws DocumentException
	{
		PdfPTable headerTable = new PdfPTable(2);

		headerTable.setWidths(colWidths);
		for (String key: cartoucheFields.keySet()) {
			headerTable.addCell(getIRDCell(key, Element.ALIGN_LEFT));
			headerTable.addCell(getIRDCell(cartoucheFields.get(key), Element.ALIGN_LEFT));
		}

		return headerTable;
	}

	/**
	 * Ajoute en pied de page, 
	 * au centre: la mention "Esup-emargement" + année 
	 * à droite: le N° de la page/nb total de pages
	 */
	public void addPageFooter(PdfWriter writer, com.itextpdf.text.Document document, int currentPageNb, int totalLineCount, int nbMaxLinePerPage) throws DocumentException {
		String pageFooterTtext = "Page "+currentPageNb+"/"+((int)Math.ceil((float)totalLineCount/(float)nbMaxLinePerPage));
		Font pageFooterFont   = FontFactory.getFont(FontFactory.HELVETICA, 10);

		Paragraph pageFooterParagraph = new Paragraph(pageFooterTtext, pageFooterFont);
		PdfPCell pageFooterCell       = new PdfPCell(pageFooterParagraph);

		pageFooterCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		pageFooterCell.setBorder(0);

		PdfPCell emptyCell = new PdfPCell();
		emptyCell.setBorder(0);

		PdfPTable pageFooter = new PdfPTable(3);
		pageFooter.setTotalWidth(document.right(document.rightMargin()) - document.left(document.leftMargin()));
		pageFooter.setWidths(new float[] {0.15f, 0.7f, 0.15f});
		pageFooter.addCell(emptyCell);
		pageFooter.addCell(getDescCell("Esup-emargement - " + Year.now().getValue()));
		pageFooter.addCell(pageFooterCell);
		pageFooter.writeSelectedRows(0, -1,
			document.left(document.leftMargin()),
			pageFooter.getTotalHeight() + document.bottom(document.bottomMargin()),
			writer.getDirectContent()
		);
	}
}
