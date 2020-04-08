package org.esupportail.emargement.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;
import org.springframework.stereotype.Component;
import org.xhtmlrenderer.pdf.ITextRenderer;


@Component
public class PdfGenaratorUtil {
	
	public String createPdf(String htmltemplate) throws Exception {
		String filePath = "";

		String htmlStart = "<!DOCTYPE HTML>\n" + "<html>\n" + "<head>\n" + "</head>\n" + "<body>\n";
		String htmlEnd = "</body>\n" + "</html>\n";
		String htmlString = htmlStart + htmltemplate + htmlEnd;
		String finalHtml = "";

		boolean valid = Jsoup.isValid(htmlString, Whitelist.basic());

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
}
