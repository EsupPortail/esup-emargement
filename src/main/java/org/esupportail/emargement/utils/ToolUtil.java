package org.esupportail.emargement.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

@Component
public class ToolUtil {
	
	public int compareDate(Date date1, Date date2, String pattern) {
		DateFormat dateFormat = new SimpleDateFormat(pattern);  
        String strDate1= dateFormat.format(date1);
        String strDate2 =  dateFormat.format(date2);
		
        return strDate1.compareTo(strDate2);
	}
	
	public Pageable updatePageable(final Pageable source, final int size)
	{
	    return PageRequest.of(source.getPageNumber(), size, source.getSort());
	}
	
	public InputStream generateQRCodeImage(String text, int width, int height)
			throws WriterException, IOException {
		QRCodeWriter qrCodeWriter = new QRCodeWriter();
		BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "png", bos);
        byte[] bytes = bos.toByteArray();
        InputStream inputStream = new ByteArrayInputStream(bytes);
        bos.close();
        return inputStream;
	}
	
	public boolean isLong(String strNum) {
	    if (strNum == null) {
	        return false;
	    }
	    try {
	        Long.parseLong(strNum);
	        return true;
	    } catch (NumberFormatException nfe) {
	        return false;
	    }
	}
	
	public String getDureeEpreuve(SessionEpreuve se) {
		
		String duree ="";
		
		Date heureEpreuve = se.getHeureEpreuve();
		Date finEpreuve = se.getFinEpreuve();
		//Date dureeEpreuve = sameSe.getDureeEpreuve();
		long diff = finEpreuve.getTime() - heureEpreuve.getTime();
		
		long diffMinutes = diff / (60 * 1000) % 60;
		long diffHours = diff / (60 * 60 * 1000) % 24;
		if(diffHours != 0) {
			duree = String.valueOf(diffHours).concat("H");
		}
		if(diffMinutes != 0) {
			duree = duree.concat(StringUtils.leftPad(String.valueOf(diffMinutes), 2, "0"));
			if(diffHours == 0) {
				duree = duree.concat("mn");
			}
		}
		
		return duree;
	}
}
